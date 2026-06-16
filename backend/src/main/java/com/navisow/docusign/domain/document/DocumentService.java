package com.navisow.docusign.domain.document;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.navisow.docusign.domain.user.AppUser;
import com.navisow.docusign.storage.StorageService;

@Service
public class DocumentService {

    private final DocumentRecordRepository repository;
    private final StorageService storageService;

    public DocumentService(DocumentRecordRepository repository,
                           StorageService storageService) {
        this.repository = repository;
        this.storageService = storageService;
    }

    @Transactional
    public DocumentRecord upload(AppUser user, MultipartFile file) {
        try (InputStream stream = file.getInputStream()) {
            String key = storageService.store(
                file.getOriginalFilename() != null ? file.getOriginalFilename() : "document",
                file.getContentType(),
                stream,
                file.getSize()
            );
            DocumentRecord record = new DocumentRecord(
                user,
                file.getOriginalFilename() != null ? file.getOriginalFilename() : "document",
                file.getContentType(),
                key
            );
            return repository.save(record);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read uploaded file", e);
        }
    }

    @Transactional(readOnly = true)
    public Page<DocumentRecord> listForUser(UUID userId, int page, int size) {
        return repository.findByUser_Id(userId,
            PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    @Transactional(readOnly = true)
    public DocumentRecord getForUser(UUID documentId, UUID userId) {
        return repository.findById(documentId)
            .filter(d -> d.getUser().getId().equals(userId))
            .orElseThrow(() -> new DocumentNotFoundException(documentId));
    }

    public InputStream openContent(DocumentRecord record) {
        return storageService.retrieve(record.getStorageKey());
    }
}
