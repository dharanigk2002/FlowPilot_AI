package com.flowpilot.knowledge;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
public class KnowledgeSearchService {

    private final KnowledgeRetriever knowledgeRetriever;

    public KnowledgeSearchService(KnowledgeRetriever knowledgeRetriever) {
        this.knowledgeRetriever = knowledgeRetriever;
    }

    @PreAuthorize("isAuthenticated()")
    public KnowledgeSearchResponse search(KnowledgeSearchRequest request) {
        List<KnowledgeSearchResponse.Result> results = knowledgeRetriever.retrieve(
                        request.query(),
                        request.topK() == null ? 5 : request.topK(),
                        request.similarityThreshold() == null ? 0.5 : request.similarityThreshold()
                )
                .stream()
                .map(this::toResult)
                .toList();
        return new KnowledgeSearchResponse(results);
    }

    private KnowledgeSearchResponse.Result toResult(RetrievedKnowledgeChunk chunk) {
        return new KnowledgeSearchResponse.Result(
                chunk.text(),
                chunk.score(),
                chunk.documentId(),
                chunk.fileName(),
                chunk.chunkIndex()
        );
    }
}
