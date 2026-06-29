package com.flowpilot.knowledge;

import java.time.Instant;

public record KnowledgeDocumentResponse(
        Long id,
        String fileName,
        String contentType,
        long sizeBytes,
        KnowledgeDocumentStatus status,
        int chunkCount,
        String errorMessage,
        UploaderSummary uploadedBy,
        Instant createdAt,
        Instant updatedAt
) {
    public record UploaderSummary(Long id, String email, String displayName) {
    }
}
