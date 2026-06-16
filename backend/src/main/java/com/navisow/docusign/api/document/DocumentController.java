package com.navisow.docusign.api.document;

import java.util.Map;
import java.util.UUID;

import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.navisow.docusign.domain.document.DocumentNotFoundException;
import com.navisow.docusign.domain.document.DocumentRecord;
import com.navisow.docusign.domain.document.DocumentService;
import com.navisow.docusign.domain.user.AppUser;
import com.navisow.docusign.domain.user.AppUserService;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentService documentService;
    private final AppUserService userService;

    public DocumentController(DocumentService documentService, AppUserService userService) {
        this.documentService = documentService;
        this.userService = userService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> upload(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal Jwt jwt) {

        AppUser user = userService.getOrCreateUser(jwt);
        DocumentRecord record = documentService.upload(user, file);

        return ResponseEntity.ok(Map.of(
            "id", record.getId().toString(),
            "fileName", record.getFileName(),
            "status", record.getStatus().name(),
            "createdAt", record.getCreatedAt().toString()
        ));
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal Jwt jwt) {

        AppUser user = userService.getOrCreateUser(jwt);
        Page<DocumentRecord> results = documentService.listForUser(user.getId(), page, size);

        return ResponseEntity.ok(Map.of(
            "content", results.getContent().stream().map(this::toDto).toList(),
            "totalElements", results.getTotalElements(),
            "totalPages", results.getTotalPages(),
            "page", page
        ));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<InputStreamResource> download(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {

        AppUser user = userService.getOrCreateUser(jwt);
        DocumentRecord record = documentService.getForUser(id, user.getId());

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + record.getFileName() + "\"")
            .contentType(record.getContentType() != null
                ? MediaType.parseMediaType(record.getContentType())
                : MediaType.APPLICATION_OCTET_STREAM)
            .body(new InputStreamResource(documentService.openContent(record)));
    }

    private Map<String, Object> toDto(DocumentRecord d) {
        return Map.of(
            "id", d.getId().toString(),
            "fileName", d.getFileName(),
            "status", d.getStatus().name(),
            "createdAt", d.getCreatedAt().toString()
        );
    }

    @ExceptionHandler(DocumentNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(DocumentNotFoundException ex) {
        return ResponseEntity.status(404).body(Map.of("error", ex.getMessage()));
    }
}
