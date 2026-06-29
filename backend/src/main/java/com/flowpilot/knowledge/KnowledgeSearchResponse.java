package com.flowpilot.knowledge;

import java.util.List;

public record KnowledgeSearchResponse(List<Result> results) {

    public record Result(
            String text,
            Double score,
            String documentId,
            String fileName,
            Integer chunkIndex
    ) {
    }
}
