package com.flowpilot.knowledge;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import com.flowpilot.common.exception.ApplicationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class KnowledgeIngestionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(KnowledgeIngestionService.class);
    private static final Set<String> SUPPORTED_CONTENT_TYPES = Set.of(
            "application/pdf",
            "text/plain",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );

    private final KnowledgeDocumentService documentService;
    private final KnowledgeDocumentPipeline documentPipeline;
    private final ObjectProvider<VectorStore> vectorStoreProvider;
    private final long maxFileSizeBytes;

    public KnowledgeIngestionService(
            KnowledgeDocumentService documentService,
            KnowledgeDocumentPipeline documentPipeline,
            ObjectProvider<VectorStore> vectorStoreProvider,
            @Value("${app.knowledge.max-file-size-bytes}") long maxFileSizeBytes
    ) {
        this.documentService = documentService;
        this.documentPipeline = documentPipeline;
        this.vectorStoreProvider = vectorStoreProvider;
        this.maxFileSizeBytes = maxFileSizeBytes;
    }

    @PreAuthorize("hasRole('ADMIN')")
    public KnowledgeDocumentResponse upload(MultipartFile file, Authentication authentication) {
        ValidatedUpload upload = validate(file);
        byte[] content = readContent(file);
        Long documentId = documentService.startUpload(
                upload.fileName(),
                upload.contentType(),
                content.length,
                authentication.getName()
        );

        try {
            VectorStore vectorStore = requireVectorStore();
            List<Document> chunks = documentPipeline.extractAndSplit(
                    content,
                    documentId,
                    upload.fileName(),
                    upload.contentType()
            );
            if (chunks.isEmpty()) {
                throw new ApplicationException(HttpStatus.UNPROCESSABLE_ENTITY, "The document contains no readable text.");
            }

            vectorStore.add(chunks);
            return documentService.markReady(documentId, chunks.size());
        }
        catch (ApplicationException exception) {
            documentService.markFailed(documentId, exception.getMessage());
            throw exception;
        }
        catch (RuntimeException exception) {
            LOGGER.error("Failed to process knowledge document {}", documentId, exception);
            documentService.markFailed(documentId, "Document extraction or embedding failed.");
            throw new ApplicationException(HttpStatus.UNPROCESSABLE_ENTITY, "The document could not be processed.");
        }
    }

    private ValidatedUpload validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApplicationException(HttpStatus.BAD_REQUEST, "A non-empty document file is required.");
        }
        if (file.getSize() > maxFileSizeBytes) {
            throw new ApplicationException(HttpStatus.PAYLOAD_TOO_LARGE, "The document exceeds the 10 MB limit.");
        }

        String contentType = file.getContentType();
        if (contentType == null || !SUPPORTED_CONTENT_TYPES.contains(contentType)) {
            throw new ApplicationException(
                    HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "Only PDF, DOCX, and plain-text documents are supported."
            );
        }

        return new ValidatedUpload(sanitizeFileName(file.getOriginalFilename()), contentType);
    }

    private byte[] readContent(MultipartFile file) {
        try {
            return file.getBytes();
        }
        catch (IOException exception) {
            throw new ApplicationException(HttpStatus.BAD_REQUEST, "The uploaded document could not be read.");
        }
    }

    private VectorStore requireVectorStore() {
        VectorStore vectorStore = vectorStoreProvider.getIfAvailable();
        if (vectorStore == null) {
            throw new ApplicationException(HttpStatus.SERVICE_UNAVAILABLE, "Document embeddings are not configured.");
        }
        return vectorStore;
    }

    private String sanitizeFileName(String originalFileName) {
        if (originalFileName == null || originalFileName.isBlank()) {
            return "document";
        }
        String normalized = originalFileName.replace('\\', '/');
        String fileName = normalized.substring(normalized.lastIndexOf('/') + 1).trim();
        if (fileName.isEmpty()) {
            return "document";
        }
        return fileName.length() <= 255 ? fileName : fileName.substring(fileName.length() - 255);
    }

    private record ValidatedUpload(String fileName, String contentType) {
    }
}
