package com.flowpilot.cases;

import com.flowpilot.common.exception.ApplicationException;
import com.flowpilot.user.AppUser;
import com.flowpilot.user.UserRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CaseService {

    private final CaseRepository caseRepository;
    private final UserRepository userRepository;

    public CaseService(CaseRepository caseRepository, UserRepository userRepository) {
        this.caseRepository = caseRepository;
        this.userRepository = userRepository;
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
        Page<SupportCase> cases = caseRepository.findAll(pageRequest);

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
                creatorSummary,
                supportCase.getCreatedAt(),
                supportCase.getUpdatedAt()
        );
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
