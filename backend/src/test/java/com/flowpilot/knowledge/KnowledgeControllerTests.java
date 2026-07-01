package com.flowpilot.knowledge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static com.flowpilot.testsupport.SecurityTestSupport.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import com.flowpilot.auth.RegisterRequest;
import com.flowpilot.user.UserRepository;
import com.flowpilot.user.UserRole;

import jakarta.servlet.http.Cookie;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.security.web.csrf.CsrfTokenRepository;

import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class KnowledgeControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private KnowledgeDocumentRepository documentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CsrfTokenRepository csrfTokenRepository;

    @MockitoBean
    private VectorStore vectorStore;

    @BeforeEach
    void cleanDatabase() {
        documentRepository.deleteAll();
        userRepository.deleteAll();
        reset(vectorStore);
    }

    @Test
    void adminUploadsAndIndexesTextDocument() throws Exception {
        Cookie authCookie = register("knowledge.admin@flowpilot.test", UserRole.ADMIN);
        MockMultipartFile file = textPolicyFile();

        mockMvc.perform(multipart("/api/documents")
                        .file(file)
                        .cookie(authCookie)
                        .with(csrf(csrfTokenRepository)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fileName").value("premium-delivery-policy.txt"))
                .andExpect(jsonPath("$.contentType").value(MediaType.TEXT_PLAIN_VALUE))
                .andExpect(jsonPath("$.status").value("READY"))
                .andExpect(jsonPath("$.chunkCount").value(1))
                .andExpect(jsonPath("$.uploadedBy.email").value("knowledge.admin@flowpilot.test"));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Document>> chunksCaptor = ArgumentCaptor.forClass(List.class);
        verify(vectorStore).add(chunksCaptor.capture());
        Document chunk = chunksCaptor.getValue().getFirst();
        assertThat(chunk.getText()).contains("premium delivery fee refund");
        assertThat(chunk.getMetadata())
                .containsEntry("fileName", "premium-delivery-policy.txt")
                .containsEntry("contentType", MediaType.TEXT_PLAIN_VALUE)
                .containsEntry("chunkIndex", 0);
        assertThat(chunk.getMetadata().get("documentId")).isNotNull();
    }

    @Test
    void supportAgentCannotUploadKnowledgeDocument() throws Exception {
        Cookie authCookie = register("knowledge.agent@flowpilot.test", UserRole.SUPPORT_AGENT);

        mockMvc.perform(multipart("/api/documents")
                        .file(textPolicyFile())
                        .cookie(authCookie)
                        .with(csrf(csrfTokenRepository)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You do not have permission to perform this action."));
    }

    @Test
    void rejectsUnsupportedFileType() throws Exception {
        Cookie authCookie = register("media.admin@flowpilot.test", UserRole.ADMIN);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "policy.exe",
                MediaType.APPLICATION_OCTET_STREAM_VALUE,
                "not a policy".getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/documents")
                        .file(file)
                        .cookie(authCookie)
                        .with(csrf(csrfTokenRepository)))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.message").value("Only PDF, DOCX, and plain-text documents are supported."));
    }

    @Test
    void failedVectorWriteLeavesFailedDocumentRecord() throws Exception {
        Cookie authCookie = register("failure.admin@flowpilot.test", UserRole.ADMIN);
        doThrow(new IllegalStateException("Vector store unavailable"))
                .when(vectorStore).add(any());

        mockMvc.perform(multipart("/api/documents")
                        .file(textPolicyFile())
                        .cookie(authCookie)
                        .with(csrf(csrfTokenRepository)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("The document could not be processed."));

        KnowledgeDocument failedDocument = documentRepository.findAll().getFirst();
        assertThat(failedDocument.getStatus()).isEqualTo(KnowledgeDocumentStatus.FAILED);
        assertThat(failedDocument.getChunkCount()).isZero();
        assertThat(failedDocument.getErrorMessage()).isEqualTo("Document extraction or embedding failed.");
    }

    @Test
    void authenticatedUserListsAndReadsDocuments() throws Exception {
        Cookie adminCookie = register("catalog.admin@flowpilot.test", UserRole.ADMIN);
        upload(adminCookie);
        Cookie agentCookie = register("catalog.agent@flowpilot.test", UserRole.SUPPORT_AGENT);
        Long documentId = documentRepository.findAll().getFirst().getId();

        mockMvc.perform(get("/api/documents")
                        .cookie(agentCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(documentId))
                .andExpect(jsonPath("$.totalElements").value(1));

        mockMvc.perform(get("/api/documents/{id}", documentId)
                        .cookie(agentCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(documentId))
                .andExpect(jsonPath("$.status").value("READY"));
    }

    @Test
    void authenticatedUserSearchesByMeaning() throws Exception {
        Cookie authCookie = register("search.agent@flowpilot.test", UserRole.SUPPORT_AGENT);
        Document result = Document.builder()
                .text("Premium customers receive a delivery fee refund after a 48-hour delay.")
                .metadata(Map.of(
                        "documentId", "42",
                        "fileName", "premium-delivery-policy.txt",
                        "chunkIndex", 3
                ))
                .score(0.92)
                .build();
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(result));
        KnowledgeSearchRequest request = new KnowledgeSearchRequest("compensation for late delivery", 3, 0.7);

        mockMvc.perform(post("/api/documents/search")
                        .cookie(authCookie)
                        .with(csrf(csrfTokenRepository))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].score").value(0.92))
                .andExpect(jsonPath("$.results[0].documentId").value("42"))
                .andExpect(jsonPath("$.results[0].chunkIndex").value(3));

        ArgumentCaptor<SearchRequest> requestCaptor = ArgumentCaptor.forClass(SearchRequest.class);
        verify(vectorStore).similaritySearch(requestCaptor.capture());
        assertThat(requestCaptor.getValue().getQuery()).isEqualTo("compensation for late delivery");
        assertThat(requestCaptor.getValue().getTopK()).isEqualTo(3);
        assertThat(requestCaptor.getValue().getSimilarityThreshold()).isEqualTo(0.7);
    }

    private void upload(Cookie authCookie) throws Exception {
        mockMvc.perform(multipart("/api/documents")
                        .file(textPolicyFile())
                        .cookie(authCookie)
                        .with(csrf(csrfTokenRepository)))
                .andExpect(status().isCreated());
    }

    private Cookie register(String email, UserRole role) throws Exception {
        RegisterRequest request = new RegisterRequest(email, "Knowledge User", "StrongPass123", role);
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .with(csrf(csrfTokenRepository))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        return result.getResponse().getCookie("FLOWPILOT_ACCESS_TOKEN");
    }

    private MockMultipartFile textPolicyFile() {
        String content = "Premium customers are eligible for a premium delivery fee refund "
                + "when delivery is delayed beyond 48 hours. Support managers may approve compensation.";
        return new MockMultipartFile(
                "file",
                "premium-delivery-policy.txt",
                MediaType.TEXT_PLAIN_VALUE,
                content.getBytes(StandardCharsets.UTF_8)
        );
    }

}
