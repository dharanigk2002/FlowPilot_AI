package com.flowpilot.knowledge;

import com.flowpilot.common.config.OpenApiConfig;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Validated
@RestController
@RequestMapping("/api/documents")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class KnowledgeController {

    private final KnowledgeIngestionService ingestionService;
    private final KnowledgeDocumentService documentService;
    private final KnowledgeSearchService searchService;

    public KnowledgeController(
            KnowledgeIngestionService ingestionService,
            KnowledgeDocumentService documentService,
            KnowledgeSearchService searchService
    ) {
        this.ingestionService = ingestionService;
        this.documentService = documentService;
        this.searchService = searchService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public KnowledgeDocumentResponse upload(
            @RequestPart("file") MultipartFile file,
            Authentication authentication
    ) {
        return ingestionService.upload(file, authentication);
    }

    @GetMapping
    public KnowledgeDocumentPageResponse findAll(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return documentService.findAll(page, size);
    }

    @GetMapping("/{id}")
    public KnowledgeDocumentResponse findById(@PathVariable @Positive Long id) {
        return documentService.findById(id);
    }

    @PostMapping("/search")
    public KnowledgeSearchResponse search(@Valid @RequestBody KnowledgeSearchRequest request) {
        return searchService.search(request);
    }
}
