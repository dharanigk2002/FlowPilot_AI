package com.flowpilot.cases;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import com.flowpilot.ai.RagChatRequest;
import com.flowpilot.ai.RagChatResponse;
import com.flowpilot.ai.RagService;
import com.flowpilot.common.exception.ApplicationException;
import com.flowpilot.user.AppUser;
import com.flowpilot.user.UserRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CaseService {

    private final CaseRepository caseRepository;
    private final UserRepository userRepository;
    private final RagService ragService;
    private final Clock clock;

    public CaseService(
            CaseRepository caseRepository,
            UserRepository userRepository,
            RagService ragService,
            Clock clock
    ) {
        this.caseRepository = caseRepository;
        this.userRepository = userRepository;
        this.ragService = ragService;
        this.clock = clock;
    }

    @Transactional
    @PreAuthorize("hasAnyRole('SUPPORT_AGENT', 'MANAGER', 'ADMIN')")
    public CaseResponse create(CreateCaseRequest request, Authentication authentication) {
        AppUser creator = findAuthenticatedUser(authentication);
        SupportCase supportCase = new SupportCase(
                request.title().trim(),
                request.description().trim(),
                request.customerName().trim(),
                trimToNull(request.customerEmail()),
                request.customerTier(),
                trimToNull(request.orderReference()),
                request.orderValue(),
                request.priority(),
                creator
        );

        return toResponse(caseRepository.save(supportCase));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public CasePageResponse findAll(int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<SupportCase> cases = caseRepository.findByActiveTrue(pageRequest);

        return new CasePageResponse(
                cases.getContent().stream().map(this::toResponse).toList(),
                cases.getNumber(),
                cases.getSize(),
                cases.getTotalElements(),
                cases.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public CaseResponse findById(Long id) {
        return toResponse(findCase(id));
    }

    @Transactional
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public CaseResponse updateStatus(Long id, UpdateCaseStatusRequest request) {
        SupportCase supportCase = findCase(id);
        supportCase.changeStatus(request.status());
        return toResponse(caseRepository.saveAndFlush(supportCase));
    }

    @Transactional
    @PreAuthorize("hasRole('SUPPORT_AGENT')")
    public CaseResponse submitAgentRecommendation(
            Long id,
            SubmitAgentRecommendationRequest request,
            Authentication authentication
    ) {
        SupportCase supportCase = findCase(id);
        AppUser recommender = findAuthenticatedUser(authentication);
        supportCase.submitAgentRecommendation(
                request.suggestedAction(),
                request.notes().trim(),
                recommender,
                Instant.now(clock)
        );
        return toResponse(caseRepository.saveAndFlush(supportCase));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public RagChatResponse recommendResolution(Long id) {
        SupportCase supportCase = findCase(id);
        return ragService.answer(new RagChatRequest(buildRecommendationQuestion(supportCase)));
    }

    @Transactional
    @Scheduled(cron = "${app.cases.archive-closed-cron:0 0 2 * * *}")
    public void archiveClosedCases() {
        Instant closedBefore = Instant.now(clock).minus(Duration.ofDays(10));
        caseRepository.findByStatusAndActiveTrueAndClosedAtLessThanEqual(CaseStatus.CLOSED, closedBefore)
                .forEach(supportCase -> supportCase.archive(Instant.now(clock)));
    }

    private SupportCase findCase(Long id) {
        return caseRepository.findById(id)
                .orElseThrow(() -> new ApplicationException(HttpStatus.NOT_FOUND, "Case not found."));
    }

    private AppUser findAuthenticatedUser(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName().trim().toLowerCase())
                .orElseThrow(() -> new ApplicationException(HttpStatus.UNAUTHORIZED, "Authenticated user not found."));
    }

    private CaseResponse toResponse(SupportCase supportCase) {
        AppUser creator = supportCase.getCreatedBy();
        CaseResponse.CreatorSummary creatorSummary = new CaseResponse.CreatorSummary(
                creator.getId(),
                creator.getEmail(),
                creator.getDisplayName()
        );

        return new CaseResponse(
                supportCase.getId(),
                supportCase.getTitle(),
                supportCase.getDescription(),
                supportCase.getCustomerName(),
                supportCase.getCustomerEmail(),
                supportCase.getCustomerTier(),
                supportCase.getOrderReference(),
                supportCase.getOrderValue(),
                supportCase.getPriority(),
                supportCase.getStatus(),
                supportCase.isActive(),
                supportCase.getClosedAt(),
                supportCase.getArchivedAt(),
                toAgentRecommendationSummary(supportCase),
                creatorSummary,
                supportCase.getCreatedAt(),
                supportCase.getUpdatedAt()
        );
    }

    private CaseResponse.AgentRecommendationSummary toAgentRecommendationSummary(SupportCase supportCase) {
        if (supportCase.getAgentSuggestedAction() == null) {
            return null;
        }
        AppUser recommendedBy = supportCase.getRecommendedBy();
        CaseResponse.CreatorSummary recommenderSummary = recommendedBy == null
                ? null
                : new CaseResponse.CreatorSummary(
                        recommendedBy.getId(),
                        recommendedBy.getEmail(),
                        recommendedBy.getDisplayName()
                );

        return new CaseResponse.AgentRecommendationSummary(
                supportCase.getAgentSuggestedAction(),
                supportCase.getAgentRecommendationNotes(),
                recommenderSummary,
                supportCase.getRecommendedAt()
        );
    }

    private String buildRecommendationQuestion(SupportCase supportCase) {
        return """
                Recommend the next support action for this case using only company policy evidence.
                Include whether a refund, compensation, rejection, or manager review is appropriate.
                Treat the case facts below as authoritative and do not change them.
                If a policy source mentions a monetary threshold, compare it against the exact order value below before applying that rule.
                Do not describe the order as high-value or above a threshold unless the exact order value below meets that threshold.

                Authoritative case facts:
                Title: %s
                Description: %s
                Customer tier: %s
                Order reference: %s
                Order value INR: %s
                Priority: %s
                Current status: %s
                """.formatted(
                supportCase.getTitle(),
                supportCase.getDescription(),
                supportCase.getCustomerTier(),
                supportCase.getOrderReference() == null ? "Not provided" : supportCase.getOrderReference(),
                supportCase.getOrderValue() == null ? "Not provided" : supportCase.getOrderValue(),
                supportCase.getPriority(),
                supportCase.getStatus()
        );
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
