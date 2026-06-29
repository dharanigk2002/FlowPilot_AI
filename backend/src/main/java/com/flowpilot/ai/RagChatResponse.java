package com.flowpilot.ai;

import java.util.List;

public record RagChatResponse(
        String answer,
        List<Citation> citations,
        boolean insufficientEvidence
) {

    public record Citation(
            int sourceNumber,
            String documentId,
            String fileName,
            Integer chunkIndex,
            Double score
    ) {
    }
}
