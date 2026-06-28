package com.flowpilot.cases;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CaseRepository extends JpaRepository<SupportCase, Long> {

    @Override
    @EntityGraph(attributePaths = "createdBy")
    Page<SupportCase> findAll(Pageable pageable);
}
