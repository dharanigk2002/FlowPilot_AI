package com.flowpilot.knowledge;

import java.time.Instant;

import com.flowpilot.user.AppUser;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "knowledge_documents")
public class KnowledgeDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false, length = 150)
    private String contentType;

    @Column(nullable = false)
    private long sizeBytes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private KnowledgeDocumentStatus status;

    @Column(nullable = false)
    private int chunkCount;

    @Column(length = 500)
    private String errorMessage;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "uploaded_by", nullable = false)
    private AppUser uploadedBy;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected KnowledgeDocument() {
    }

    public KnowledgeDocument(String fileName, String contentType, long sizeBytes, AppUser uploadedBy) {
        this.fileName = fileName;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.status = KnowledgeDocumentStatus.PROCESSING;
        this.uploadedBy = uploadedBy;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public void markReady(int chunkCount) {
        status = KnowledgeDocumentStatus.READY;
        this.chunkCount = chunkCount;
        errorMessage = null;
    }

    public void markFailed(String errorMessage) {
        status = KnowledgeDocumentStatus.FAILED;
        chunkCount = 0;
        this.errorMessage = errorMessage;
    }

    public Long getId() {
        return id;
    }

    public String getFileName() {
        return fileName;
    }

    public String getContentType() {
        return contentType;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public KnowledgeDocumentStatus getStatus() {
        return status;
    }

    public int getChunkCount() {
        return chunkCount;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public AppUser getUploadedBy() {
        return uploadedBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
