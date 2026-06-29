package com.flowpilot.knowledge;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KnowledgeDocumentRepository extends JpaRepository<KnowledgeDocument, Long> {

    @Override
    @EntityGraph(attributePaths = "uploadedBy")
    Page<KnowledgeDocument> findAll(Pageable pageable);
}
