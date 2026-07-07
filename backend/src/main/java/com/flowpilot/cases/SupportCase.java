package com.flowpilot.cases;

import java.math.BigDecimal;
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
@Table(name = "cases")
public class SupportCase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String description;

    @Column(nullable = false, length = 120)
    private String customerName;

    @Column(length = 320)
    private String customerEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CustomerTier customerTier;

    @Column(length = 80)
    private String orderReference;

    @Column(precision = 12, scale = 2)
    private BigDecimal orderValue;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CasePriority priority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private CaseStatus status;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private AppUser createdBy;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @Column(nullable = false)
    private boolean active = true;

    private Instant closedAt;

    private Instant archivedAt;

    @Enumerated(EnumType.STRING)
    @Column(length = 40)
    private AgentSuggestedAction agentSuggestedAction;

    @Column(columnDefinition = "text")
    private String agentRecommendationNotes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recommended_by")
    private AppUser recommendedBy;

    private Instant recommendedAt;

    protected SupportCase() {
    }

    public SupportCase(
            String title,
            String description,
            String customerName,
            String customerEmail,
            CustomerTier customerTier,
            String orderReference,
            BigDecimal orderValue,
            CasePriority priority,
            AppUser createdBy
    ) {
        this.title = title;
        this.description = description;
        this.customerName = customerName;
        this.customerEmail = customerEmail;
        this.customerTier = customerTier;
        this.orderReference = orderReference;
        this.orderValue = orderValue;
        this.priority = priority;
        this.status = CaseStatus.OPEN;
        this.createdBy = createdBy;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public void changeStatus(CaseStatus status) {
        if (status == CaseStatus.CLOSED && this.status != CaseStatus.CLOSED) {
            closedAt = Instant.now();
        }
        if (status != CaseStatus.CLOSED) {
            closedAt = null;
            archivedAt = null;
            active = true;
        }
        this.status = status;
    }

    public void archive(Instant archivedAt) {
        active = false;
        this.archivedAt = archivedAt;
    }

    public void submitAgentRecommendation(
            AgentSuggestedAction suggestedAction,
            String notes,
            AppUser recommendedBy,
            Instant recommendedAt
    ) {
        this.agentSuggestedAction = suggestedAction;
        this.agentRecommendationNotes = notes;
        this.recommendedBy = recommendedBy;
        this.recommendedAt = recommendedAt;
        changeStatus(CaseStatus.PENDING_MANAGER_REVIEW);
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public CustomerTier getCustomerTier() {
        return customerTier;
    }

    public String getOrderReference() {
        return orderReference;
    }

    public BigDecimal getOrderValue() {
        return orderValue;
    }

    public CasePriority getPriority() {
        return priority;
    }

    public CaseStatus getStatus() {
        return status;
    }

    public AppUser getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getClosedAt() {
        return closedAt;
    }

    public Instant getArchivedAt() {
        return archivedAt;
    }

    public AgentSuggestedAction getAgentSuggestedAction() {
        return agentSuggestedAction;
    }

    public String getAgentRecommendationNotes() {
        return agentRecommendationNotes;
    }

    public AppUser getRecommendedBy() {
        return recommendedBy;
    }

    public Instant getRecommendedAt() {
        return recommendedAt;
    }
}
