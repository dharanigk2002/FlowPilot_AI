package com.flowpilot.auth;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.flowpilot.user.UserRepository;
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
class AuthControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Test
    void registerCreatesUserAndReturnsJwt() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "Aman.Agent@swiftcart.test",
                "Aman Agent",
                "StrongPass123",
                UserRole.SUPPORT_AGENT
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.accessToken", not(blankOrNullString())))
                .andExpect(jsonPath("$.user.email").value("aman.agent@swiftcart.test"))
                .andExpect(jsonPath("$.user.displayName").value("Aman Agent"))
                .andExpect(jsonPath("$.user.role").value("SUPPORT_AGENT"));
    }

    @Test
    void registerRejectsDuplicateEmail() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "priya.manager@swiftcart.test",
                "Priya Manager",
                "StrongPass123",
                UserRole.MANAGER
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("A user with this email already exists."));
    }

    @Test
    void loginReturnsJwtForValidCredentials() throws Exception {
        registerRahulSupportAgent();

        LoginRequest loginRequest = new LoginRequest("rahul.agent@swiftcart.test", "StrongPass123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.accessToken", not(blankOrNullString())))
                .andExpect(jsonPath("$.user.email").value("rahul.agent@swiftcart.test"));
    }

    @Test
    void meReturnsCurrentUserForBearerToken() throws Exception {
        String token = registerRahulSupportAgent();

        mockMvc.perform(get("/api/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("rahul.agent@swiftcart.test"))
                .andExpect(jsonPath("$.displayName").value("Rahul Agent"))
                .andExpect(jsonPath("$.role").value("SUPPORT_AGENT"));
    }

    @Test
    void meRejectsMissingToken() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void corsPreflightAllowsConfiguredFrontendOrigin() throws Exception {
        mockMvc.perform(options("/api/auth/login")
                        .header(HttpHeaders.ORIGIN, "http://localhost:3000")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:3000"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
    }

    @Test
    void storesHashedPassword() throws Exception {
        registerRahulSupportAgent();

        String passwordHash = userRepository.findByEmail("rahul.agent@swiftcart.test")
                .orElseThrow()
                .getPasswordHash();

        org.assertj.core.api.Assertions.assertThat(passwordHash)
                .isNotEqualTo("StrongPass123")
                .startsWith("$2");
    }

    private String registerRahulSupportAgent() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "rahul.agent@swiftcart.test",
                "Rahul Agent",
                "StrongPass123",
                UserRole.SUPPORT_AGENT
        );

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        return response.get("accessToken").asText();
    }
}
