package com.flowpilot.knowledge;

import com.flowpilot.common.exception.ApplicationException;
import com.flowpilot.user.AppUser;
import com.flowpilot.user.UserRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class KnowledgeDocumentService {

    private final KnowledgeDocumentRepository documentRepository;
    private final UserRepository userRepository;

    public KnowledgeDocumentService(
            KnowledgeDocumentRepository documentRepository,
            UserRepository userRepository
    ) {
        this.documentRepository = documentRepository;
        this.userRepository = userRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Long startUpload(String fileName, String contentType, long sizeBytes, String uploaderEmail) {
        AppUser uploader = userRepository.findByEmail(uploaderEmail.trim().toLowerCase())
                .orElseThrow(() -> new ApplicationException(HttpStatus.UNAUTHORIZED, "Authenticated user not found."));

        return documentRepository.save(new KnowledgeDocument(fileName, contentType, sizeBytes, uploader)).getId();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public KnowledgeDocumentResponse markReady(Long id, int chunkCount) {
        KnowledgeDocument document = findEntity(id);
        document.markReady(chunkCount);
        return toResponse(documentRepository.saveAndFlush(document));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(Long id, String errorMessage) {
        KnowledgeDocument document = findEntity(id);
        document.markFailed(truncate(errorMessage, 500));
        documentRepository.saveAndFlush(document);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public KnowledgeDocumentPageResponse findAll(int page, int size) {
        PageRequest request = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<KnowledgeDocument> documents = documentRepository.findAll(request);

        return new KnowledgeDocumentPageResponse(
                documents.getContent().stream().map(this::toResponse).toList(),
                documents.getNumber(),
                documents.getSize(),
                documents.getTotalElements(),
                documents.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public KnowledgeDocumentResponse findById(Long id) {
        return toResponse(findEntity(id));
    }

    private KnowledgeDocument findEntity(Long id) {
        return documentRepository.findById(id)
                .orElseThrow(() -> new ApplicationException(HttpStatus.NOT_FOUND, "Knowledge document not found."));
    }

    private KnowledgeDocumentResponse toResponse(KnowledgeDocument document) {
        AppUser uploader = document.getUploadedBy();
        KnowledgeDocumentResponse.UploaderSummary uploaderSummary =
                new KnowledgeDocumentResponse.UploaderSummary(
                        uploader.getId(),
                        uploader.getEmail(),
                        uploader.getDisplayName()
                );

        return new KnowledgeDocumentResponse(
                document.getId(),
                document.getFileName(),
                document.getContentType(),
                document.getSizeBytes(),
                document.getStatus(),
                document.getChunkCount(),
                document.getErrorMessage(),
                uploaderSummary,
                document.getCreatedAt(),
                document.getUpdatedAt()
        );
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return "Document processing failed.";
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
