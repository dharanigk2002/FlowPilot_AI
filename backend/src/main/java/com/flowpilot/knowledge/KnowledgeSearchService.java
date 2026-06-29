package com.flowpilot.knowledge;

import java.util.List;
import java.util.Map;

import com.flowpilot.common.exception.ApplicationException;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
public class KnowledgeSearchService {

    private final ObjectProvider<VectorStore> vectorStoreProvider;

    public KnowledgeSearchService(ObjectProvider<VectorStore> vectorStoreProvider) {
        this.vectorStoreProvider = vectorStoreProvider;
    }

    @PreAuthorize("isAuthenticated()")
    public KnowledgeSearchResponse search(KnowledgeSearchRequest request) {
        VectorStore vectorStore = vectorStoreProvider.getIfAvailable();
        if (vectorStore == null) {
            throw new ApplicationException(HttpStatus.SERVICE_UNAVAILABLE, "Document embeddings are not configured.");
        }

        SearchRequest searchRequest = SearchRequest.builder()
                .query(request.query().trim())
                .topK(request.topK() == null ? 5 : request.topK())
                .similarityThreshold(request.similarityThreshold() == null ? 0.5 : request.similarityThreshold())
                .build();
        List<KnowledgeSearchResponse.Result> results = vectorStore.similaritySearch(searchRequest)
                .stream()
                .map(this::toResult)
                .toList();
        return new KnowledgeSearchResponse(results);
    }

    private KnowledgeSearchResponse.Result toResult(Document document) {
        Map<String, Object> metadata = document.getMetadata();
        return new KnowledgeSearchResponse.Result(
                document.getText(),
                document.getScore(),
                valueAsString(metadata.get("documentId")),
                valueAsString(metadata.get("fileName")),
                valueAsInteger(metadata.get("chunkIndex"))
        );
    }

    private String valueAsString(Object value) {
        return value == null ? null : value.toString();
    }

    private Integer valueAsInteger(Object value) {
        return value instanceof Number number ? number.intValue() : null;
    }
}
