package com.flowpilot.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.hamcrest.Matchers.not;
import static com.flowpilot.testsupport.SecurityTestSupport.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.flowpilot.user.UserRepository;
import com.flowpilot.user.UserRole;

import jakarta.servlet.http.Cookie;

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
import org.springframework.security.web.csrf.CsrfTokenRepository;

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

    @Autowired
    private CsrfTokenRepository csrfTokenRepository;

    @Test
    void registerCreatesUserAndSetsSecureCookie() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "Aman.Agent@swiftcart.test",
                "Aman Agent",
                "StrongPass123",
                UserRole.SUPPORT_AGENT
        );

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf(csrfTokenRepository))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").doesNotExist())
                .andExpect(jsonPath("$.tokenType").doesNotExist())
                .andExpect(jsonPath("$.expiresInSeconds").value(3600))
                .andExpect(jsonPath("$.user.email").value("aman.agent@swiftcart.test"))
                .andExpect(jsonPath("$.user.displayName").value("Aman Agent"))
                .andExpect(jsonPath("$.user.role").value("SUPPORT_AGENT"))
                .andExpect(header().stringValues(
                        HttpHeaders.SET_COOKIE,
                        org.hamcrest.Matchers.hasItem(org.hamcrest.Matchers.allOf(
                                org.hamcrest.Matchers.startsWith("FLOWPILOT_ACCESS_TOKEN="),
                                org.hamcrest.Matchers.containsString("HttpOnly"),
                                org.hamcrest.Matchers.containsString("SameSite=Lax"),
                                org.hamcrest.Matchers.containsString("Path=/")
                        ))
                ));
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
                        .with(csrf(csrfTokenRepository))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf(csrfTokenRepository))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("A user with this email already exists."));
    }

    @Test
    void loginSetsAuthenticationCookieForValidCredentials() throws Exception {
        registerRahulSupportAgent();

        LoginRequest loginRequest = new LoginRequest("rahul.agent@swiftcart.test", "StrongPass123");

        mockMvc.perform(post("/api/auth/login")
                        .with(csrf(csrfTokenRepository))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").doesNotExist())
                .andExpect(jsonPath("$.user.email").value("rahul.agent@swiftcart.test"));
    }

    @Test
    void meReturnsCurrentUserForAuthenticationCookie() throws Exception {
        Cookie authCookie = registerRahulSupportAgent();

        mockMvc.perform(get("/api/auth/me")
                        .cookie(authCookie))
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
    void csrfReturnsTokenForUnsafeRequests() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", not(blankOrNullString())))
                .andExpect(jsonPath("$.headerName").value("X-XSRF-TOKEN"))
                .andExpect(header().stringValues(
                        HttpHeaders.SET_COOKIE,
                        org.hamcrest.Matchers.hasItem(org.hamcrest.Matchers.allOf(
                                org.hamcrest.Matchers.startsWith("XSRF-TOKEN="),
                                org.hamcrest.Matchers.containsString("HttpOnly")
                        ))
                ))
                .andReturn();

        Cookie csrfCookie = result.getResponse().getCookie("XSRF-TOKEN");
        assertThat(csrfCookie).isNotNull();
        assertThat(csrfCookie.getAttribute("SameSite")).isEqualTo("Lax");
    }

    @Test
    void rejectsUnsafeRequestWithoutCsrfToken() throws Exception {
        LoginRequest request = new LoginRequest("nobody@swiftcart.test", "StrongPass123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void logoutClearsAuthenticationCookie() throws Exception {
        Cookie authCookie = registerRahulSupportAgent();

        mockMvc.perform(post("/api/auth/logout")
                        .cookie(authCookie)
                        .with(csrf(csrfTokenRepository)))
                .andExpect(status().isNoContent())
                .andExpect(header().stringValues(
                        HttpHeaders.SET_COOKIE,
                        org.hamcrest.Matchers.hasItem(org.hamcrest.Matchers.allOf(
                                org.hamcrest.Matchers.startsWith("FLOWPILOT_ACCESS_TOKEN="),
                                org.hamcrest.Matchers.containsString("Max-Age=0")
                        ))
                ));
    }

    @Test
    void corsPreflightAllowsConfiguredFrontendOrigin() throws Exception {
        mockMvc.perform(options("/api/auth/login")
                        .header(HttpHeaders.ORIGIN, "http://localhost:3000")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "content-type,x-xsrf-token"))
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

        assertThat(passwordHash)
                .isNotEqualTo("StrongPass123")
                .startsWith("$2");
    }

    private Cookie registerRahulSupportAgent() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "rahul.agent@swiftcart.test",
                "Rahul Agent",
                "StrongPass123",
                UserRole.SUPPORT_AGENT
        );

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .with(csrf(csrfTokenRepository))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        Cookie authCookie = result.getResponse().getCookie("FLOWPILOT_ACCESS_TOKEN");
        assertThat(authCookie).isNotNull();
        return authCookie;
    }
}
