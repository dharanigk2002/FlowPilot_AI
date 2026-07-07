package com.flowpilot.knowledge;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

@Component
public class KnowledgeDocumentPipeline {

    private final TokenTextSplitter textSplitter = TokenTextSplitter.builder()
            .withChunkSize(500)
            .withMinChunkSizeChars(100)
            .withMinChunkLengthToEmbed(20)
            .withMaxNumChunks(2000)
            .withKeepSeparator(true)
            .build();

    public List<Document> extractAndSplit(
            byte[] content,
            Long documentId,
            String fileName,
            String contentType
    ) {
        Resource resource = namedResource(content, fileName);
        List<Document> extractedDocuments = new TikaDocumentReader(resource).read();
        for (Document doc : extractedDocuments) {
            String text = doc.getText();
            System.out.println("Text length: " + (text == null ? 0 : text.length()));
            System.out.println("Preview: " + (text == null ? null : text.substring(0, Math.min(300, text.length()))));
        }
        List<Document> sourceDocuments = extractedDocuments.stream()
                .filter(Document::isText)
                .filter(document -> document.getText() != null && !document.getText().isBlank())
                .map(document -> withSourceMetadata(document, documentId, fileName, contentType))
                .toList();

        List<Document> chunks = textSplitter.apply(sourceDocuments);
        List<Document> indexedChunks = new ArrayList<>(chunks.size());
        for (int index = 0; index < chunks.size(); index++) {
            Document chunk = chunks.get(index);
            Map<String, Object> metadata = new HashMap<>(chunk.getMetadata());
            metadata.put("chunkIndex", index);
            indexedChunks.add(new Document(chunk.getText(), metadata));
        }
        return indexedChunks;
    }

    private Document withSourceMetadata(
            Document document,
            Long documentId,
            String fileName,
            String contentType
    ) {
        Map<String, Object> metadata = new HashMap<>(document.getMetadata());
        metadata.put("documentId", documentId.toString());
        metadata.put("fileName", fileName);
        metadata.put("contentType", contentType);
        return new Document(document.getText(), metadata);
    }

    private Resource namedResource(byte[] content, String fileName) {
        return new ByteArrayResource(content) {
            @Override
            public String getFilename() {
                return fileName;
            }
        };
    }
}
