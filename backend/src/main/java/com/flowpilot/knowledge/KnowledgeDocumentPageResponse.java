package com.flowpilot.knowledge;

import java.util.List;

public record KnowledgeDocumentPageResponse(
        List<KnowledgeDocumentResponse> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
