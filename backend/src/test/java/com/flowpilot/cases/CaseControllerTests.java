package com.flowpilot.cases;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static com.flowpilot.testsupport.SecurityTestSupport.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import com.flowpilot.auth.LoginRequest;
import com.flowpilot.user.UserRole;
import com.flowpilot.user.AppUser;
import com.flowpilot.user.UserRepository;

import jakarta.servlet.http.Cookie;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.web.csrf.CsrfTokenRepository;

import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CaseControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CsrfTokenRepository csrfTokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private VectorStore vectorStore;

    @MockitoBean
    private ChatClient.Builder chatClientBuilder;

    private ChatClient chatClient;
    private ChatClient.ChatClientRequestSpec requestSpec;
    private ChatClient.CallResponseSpec responseSpec;

    @BeforeEach
    void configureChatClient() {
        reset(vectorStore, chatClientBuilder);
        chatClient = mock(ChatClient.class);
        requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        responseSpec = mock(ChatClient.CallResponseSpec.class);

        when(chatClientBuilder.build()).thenReturn(chatClient);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
    }

    @Test
    void supportAgentCreatesCaseWithOpenStatus() throws Exception {
        Cookie authCookie = register("aman.agent@swiftcart.test", UserRole.SUPPORT_AGENT);

        mockMvc.perform(post("/api/cases")
                        .cookie(authCookie)
                        .with(csrf(csrfTokenRepository))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rahulCase())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Premium delivery delayed"))
                .andExpect(jsonPath("$.customerName").value("Rahul Sharma"))
                .andExpect(jsonPath("$.customerTier").value("PREMIUM"))
                .andExpect(jsonPath("$.orderValue").value(85000.00))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.createdBy.email").value("aman.agent@swiftcart.test"));
    }

    @Test
    void authenticatedUserListsAndReadsCases() throws Exception {
        Cookie authCookie = register("list.agent@swiftcart.test", UserRole.SUPPORT_AGENT);
        long caseId = createCase(authCookie);

        mockMvc.perform(get("/api/cases")
                        .cookie(authCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(caseId))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(1));

        mockMvc.perform(get("/api/cases/{id}", caseId)
                        .cookie(authCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(caseId))
                .andExpect(jsonPath("$.description").value(rahulCase().description()));
    }

    @Test
    void managerUpdatesCaseStatus() throws Exception {
        Cookie agentCookie = register("create.agent@swiftcart.test", UserRole.SUPPORT_AGENT);
        Cookie managerCookie = register("priya.manager@swiftcart.test", UserRole.MANAGER);
        long caseId = createCase(agentCookie);

        UpdateCaseStatusRequest request = new UpdateCaseStatusRequest(CaseStatus.PENDING_MANAGER_REVIEW);
        mockMvc.perform(patch("/api/cases/{id}/status", caseId)
                        .cookie(managerCookie)
                        .with(csrf(csrfTokenRepository))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING_MANAGER_REVIEW"));
    }

    @Test
    void supportAgentCannotUpdateCaseStatus() throws Exception {
        Cookie authCookie = register("restricted.agent@swiftcart.test", UserRole.SUPPORT_AGENT);
        long caseId = createCase(authCookie);

        UpdateCaseStatusRequest request = new UpdateCaseStatusRequest(CaseStatus.RESOLVED);
        mockMvc.perform(patch("/api/cases/{id}/status", caseId)
                        .cookie(authCookie)
                        .with(csrf(csrfTokenRepository))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void supportAgentSubmitsRecommendationForManagerReview() throws Exception {
        Cookie authCookie = register("suggest.agent@swiftcart.test", UserRole.SUPPORT_AGENT);
        long caseId = createCase(authCookie, damagedItemCase());
        SubmitAgentRecommendationRequest request = new SubmitAgentRecommendationRequest(
                AgentSuggestedAction.REPLACEMENT,
                "Customer shared photos of the damaged product, so replacement is the recommended resolution."
        );

        mockMvc.perform(post("/api/cases/{id}/agent-recommendation", caseId)
                        .cookie(authCookie)
                        .with(csrf(csrfTokenRepository))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING_MANAGER_REVIEW"))
                .andExpect(jsonPath("$.agentRecommendation.suggestedAction").value("REPLACEMENT"))
                .andExpect(jsonPath("$.agentRecommendation.notes").value(request.notes()))
                .andExpect(jsonPath("$.agentRecommendation.recommendedBy.email").value("suggest.agent@swiftcart.test"))
                .andExpect(jsonPath("$.agentRecommendation.recommendedAt").exists());
    }

    @Test
    void managerCannotSubmitAgentRecommendation() throws Exception {
        Cookie agentCookie = register("manager.block.agent@swiftcart.test", UserRole.SUPPORT_AGENT);
        Cookie managerCookie = register("manager.block@swiftcart.test", UserRole.MANAGER);
        long caseId = createCase(agentCookie, damagedItemCase());
        SubmitAgentRecommendationRequest request = new SubmitAgentRecommendationRequest(
                AgentSuggestedAction.REPLACEMENT,
                "Manager should review recommendations, not create the agent recommendation."
        );

        mockMvc.perform(post("/api/cases/{id}/agent-recommendation", caseId)
                        .cookie(managerCookie)
                        .with(csrf(csrfTokenRepository))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void createCaseRejectsInvalidRequest() throws Exception {
        Cookie authCookie = register("validation.agent@swiftcart.test", UserRole.SUPPORT_AGENT);
        CreateCaseRequest invalidRequest = new CreateCaseRequest(
                " ",
                "Delayed delivery",
                "Rahul Sharma",
                "not-an-email",
                CustomerTier.PREMIUM,
                null,
                new BigDecimal("-1.00"),
                CasePriority.HIGH
        );

        mockMvc.perform(post("/api/cases")
                        .cookie(authCookie)
                        .with(csrf(csrfTokenRepository))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request validation failed."))
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'title')]").exists())
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'customerEmail')]").exists())
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'orderValue')]").exists());
    }

    @Test
    void casesRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/cases"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void returnsNotFoundForUnknownCase() throws Exception {
        Cookie authCookie = register("lookup.agent@swiftcart.test", UserRole.SUPPORT_AGENT);

        mockMvc.perform(get("/api/cases/{id}", 999999)
                        .cookie(authCookie))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Case not found."));
    }

    @Test
    void recommendationPromptPreservesExactOrderValueForThresholdComparison() throws Exception {
        Cookie authCookie = register("recommend.agent@swiftcart.test", UserRole.SUPPORT_AGENT);
        long caseId = createCase(authCookie, damagedItemCase());
        Document policyChunk = Document.builder()
                .text("High-value damaged orders above INR 50,000 may receive goodwill compensation after photo verification.")
                .metadata(Map.of(
                        "documentId", "42",
                        "fileName", "damaged-item-policy.pdf",
                        "chunkIndex", 0
                ))
                .score(0.91)
                .build();
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(policyChunk));
        when(responseSpec.content()).thenReturn("The order value is below INR 50,000, so high-value compensation does not apply [Source 1].");

        mockMvc.perform(post("/api/cases/{id}/recommendation", caseId)
                        .cookie(authCookie)
                        .with(csrf(csrfTokenRepository)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("The order value is below INR 50,000, so high-value compensation does not apply [Source 1]."));

        ArgumentCaptor<String> userPrompt = ArgumentCaptor.forClass(String.class);
        verify(requestSpec).user(userPrompt.capture());
        org.assertj.core.api.Assertions.assertThat(userPrompt.getValue())
                .contains("Order value INR: 6799.00")
                .contains("compare it against the exact order value below")
                .contains("Do not describe the order as high-value or above a threshold unless the exact order value below meets that threshold");
    }

    private long createCase(Cookie authCookie) throws Exception {
        return createCase(authCookie, rahulCase());
    }

    private long createCase(Cookie authCookie, CreateCaseRequest request) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/cases")
                        .cookie(authCookie)
                        .with(csrf(csrfTokenRepository))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private Cookie register(String email, UserRole role) throws Exception {
        userRepository.save(new AppUser(
                email,
                "FlowPilot User",
                passwordEncoder.encode("StrongPass123"),
                role
        ));

        LoginRequest request = new LoginRequest(email, "StrongPass123");
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .with(csrf(csrfTokenRepository))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        return result.getResponse().getCookie("FLOWPILOT_ACCESS_TOKEN");
    }

    private CreateCaseRequest rahulCase() {
        return new CreateCaseRequest(
                "Premium delivery delayed",
                "Laptop order is delayed by five days and the customer requested compensation.",
                "Rahul Sharma",
                "rahul@example.com",
                CustomerTier.PREMIUM,
                "SWC-85000",
                new BigDecimal("85000.00"),
                CasePriority.HIGH
        );
    }

    private CreateCaseRequest damagedItemCase() {
        return new CreateCaseRequest(
                "Damaged item received",
                "Customer received a damaged mixer grinder and shared photos of the damaged product.",
                "Aman Verma",
                "aman.verma@example.com",
                CustomerTier.STANDARD,
                "SWC-67990",
                new BigDecimal("6799.00"),
                CasePriority.HIGH
        );
    }

}
