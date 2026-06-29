package com.flowpilot.knowledge;

import java.util.List;
import java.util.Map;

import com.flowpilot.common.exception.ApplicationException;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class KnowledgeRetriever {

    private final ObjectProvider<VectorStore> vectorStoreProvider;

    public KnowledgeRetriever(ObjectProvider<VectorStore> vectorStoreProvider) {
        this.vectorStoreProvider = vectorStoreProvider;
    }

    public List<RetrievedKnowledgeChunk> retrieve(
            String query,
            int topK,
            double similarityThreshold
    ) {
        VectorStore vectorStore = vectorStoreProvider.getIfAvailable();
        if (vectorStore == null) {
            throw new ApplicationException(HttpStatus.SERVICE_UNAVAILABLE, "Document embeddings are not configured.");
        }

        SearchRequest searchRequest = SearchRequest.builder()
                .query(query.trim())
                .topK(topK)
                .similarityThreshold(similarityThreshold)
                .build();
        return vectorStore.similaritySearch(searchRequest)
                .stream()
                .map(this::toRetrievedChunk)
                .toList();
    }

    private RetrievedKnowledgeChunk toRetrievedChunk(Document document) {
        Map<String, Object> metadata = document.getMetadata();
        return new RetrievedKnowledgeChunk(
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
