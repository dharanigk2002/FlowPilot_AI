package com.flowpilot.cases;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;

import com.flowpilot.auth.RegisterRequest;
import com.flowpilot.user.UserRole;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.databind.JsonNode;
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

    @Test
    void supportAgentCreatesCaseWithOpenStatus() throws Exception {
        String token = register("aman.agent@swiftcart.test", UserRole.SUPPORT_AGENT);

        mockMvc.perform(post("/api/cases")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
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
        String token = register("list.agent@swiftcart.test", UserRole.SUPPORT_AGENT);
        long caseId = createCase(token);

        mockMvc.perform(get("/api/cases")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(caseId))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(1));

        mockMvc.perform(get("/api/cases/{id}", caseId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(caseId))
                .andExpect(jsonPath("$.description").value(rahulCase().description()));
    }

    @Test
    void managerUpdatesCaseStatus() throws Exception {
        String agentToken = register("create.agent@swiftcart.test", UserRole.SUPPORT_AGENT);
        String managerToken = register("priya.manager@swiftcart.test", UserRole.MANAGER);
        long caseId = createCase(agentToken);

        UpdateCaseStatusRequest request = new UpdateCaseStatusRequest(CaseStatus.PENDING_MANAGER_REVIEW);
        mockMvc.perform(patch("/api/cases/{id}/status", caseId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(managerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING_MANAGER_REVIEW"));
    }

    @Test
    void supportAgentCannotUpdateCaseStatus() throws Exception {
        String token = register("restricted.agent@swiftcart.test", UserRole.SUPPORT_AGENT);
        long caseId = createCase(token);

        UpdateCaseStatusRequest request = new UpdateCaseStatusRequest(CaseStatus.RESOLVED);
        mockMvc.perform(patch("/api/cases/{id}/status", caseId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void createCaseRejectsInvalidRequest() throws Exception {
        String token = register("validation.agent@swiftcart.test", UserRole.SUPPORT_AGENT);
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
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
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
        String token = register("lookup.agent@swiftcart.test", UserRole.SUPPORT_AGENT);

        mockMvc.perform(get("/api/cases/{id}", 999999)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Case not found."));
    }

    private long createCase(String token) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/cases")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rahulCase())))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private String register(String email, UserRole role) throws Exception {
        RegisterRequest request = new RegisterRequest(email, "FlowPilot User", "StrongPass123", role);
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        return response.get("accessToken").asText();
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

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
