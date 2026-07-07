package com.flowpilot.cases;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CaseRepository extends JpaRepository<SupportCase, Long> {

    @EntityGraph(attributePaths = {"createdBy", "recommendedBy"})
    Page<SupportCase> findByActiveTrue(Pageable pageable);

    List<SupportCase> findByStatusAndActiveTrueAndClosedAtLessThanEqual(CaseStatus status, Instant closedBefore);
}
