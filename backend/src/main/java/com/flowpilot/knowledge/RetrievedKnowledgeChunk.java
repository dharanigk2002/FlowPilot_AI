package com.flowpilot.knowledge;

public record RetrievedKnowledgeChunk(
        String text,
        Double score,
        String documentId,
        String fileName,
        Integer chunkIndex
) {
}
