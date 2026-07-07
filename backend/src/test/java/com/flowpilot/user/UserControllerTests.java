package com.flowpilot.user;

import static com.flowpilot.testsupport.SecurityTestSupport.csrf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.flowpilot.auth.LoginRequest;
import com.flowpilot.cases.CaseRepository;
import com.flowpilot.knowledge.KnowledgeDocumentRepository;

import jakarta.servlet.http.Cookie;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CaseRepository caseRepository;

    @Autowired
    private KnowledgeDocumentRepository knowledgeDocumentRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private CsrfTokenRepository csrfTokenRepository;

    @BeforeEach
    void cleanDatabase() {
        caseRepository.deleteAll();
        knowledgeDocumentRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void adminListsUsers() throws Exception {
        Cookie adminCookie = createAndLogin("admin@flowpilot.test", UserRole.ADMIN);
        createUser("agent@flowpilot.test", UserRole.SUPPORT_AGENT);

        mockMvc.perform(get("/api/users")
                        .cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.items[?(@.email == 'admin@flowpilot.test')]").exists())
                .andExpect(jsonPath("$.items[?(@.email == 'agent@flowpilot.test')]").exists());
    }

    @Test
    void adminUpdatesUserRole() throws Exception {
        Cookie adminCookie = createAndLogin("admin@flowpilot.test", UserRole.ADMIN);
        AppUser agent = createUser("agent@flowpilot.test", UserRole.SUPPORT_AGENT);
        UpdateUserRoleRequest request = new UpdateUserRoleRequest(UserRole.MANAGER);

        mockMvc.perform(patch("/api/users/{id}/role", agent.getId())
                        .cookie(adminCookie)
                        .with(csrf(csrfTokenRepository))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("agent@flowpilot.test"))
                .andExpect(jsonPath("$.role").value("MANAGER"));

        assertThat(userRepository.findByEmail("agent@flowpilot.test").orElseThrow().getRole())
                .isEqualTo(UserRole.MANAGER);
    }

    @Test
    void supportAgentCannotManageUsers() throws Exception {
        Cookie agentCookie = createAndLogin("agent@flowpilot.test", UserRole.SUPPORT_AGENT);

        mockMvc.perform(get("/api/users")
                        .cookie(agentCookie))
                .andExpect(status().isForbidden());
    }

    @Test
    void cannotDemoteLastAdmin() throws Exception {
        Cookie adminCookie = createAndLogin("admin@flowpilot.test", UserRole.ADMIN);
        Long adminId = userRepository.findByEmail("admin@flowpilot.test").orElseThrow().getId();
        UpdateUserRoleRequest request = new UpdateUserRoleRequest(UserRole.SUPPORT_AGENT);

        mockMvc.perform(patch("/api/users/{id}/role", adminId)
                        .cookie(adminCookie)
                        .with(csrf(csrfTokenRepository))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("At least one administrator must remain."));
    }

    @Test
    void usersRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isUnauthorized());
    }

    private Cookie createAndLogin(String email, UserRole role) throws Exception {
        createUser(email, role);
        LoginRequest request = new LoginRequest(email, "StrongPass123");
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .with(csrf(csrfTokenRepository))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        Cookie authCookie = result.getResponse().getCookie("FLOWPILOT_ACCESS_TOKEN");
        assertThat(authCookie).isNotNull();
        return authCookie;
    }

    private AppUser createUser(String email, UserRole role) {
        return userRepository.save(new AppUser(
                email,
                "FlowPilot User",
                passwordEncoder.encode("StrongPass123"),
                role
        ));
    }
}
