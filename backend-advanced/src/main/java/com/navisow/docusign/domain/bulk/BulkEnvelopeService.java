package com.navisow.docusign.domain.bulk;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.navisow.docusign.domain.user.AppUser;
import com.navisow.docusign.domain.user.AppUserService;
import com.navisow.docusign.integration.bulk.BulkEnvelopeProcessor;

@Service
public class BulkEnvelopeService {

    private final BulkOperationRepository bulkOperationRepository;
    private final BulkOperationItemRepository bulkOperationItemRepository;
    private final AppUserService appUserService;
    private final BulkEnvelopeProcessor bulkEnvelopeProcessor;

    public BulkEnvelopeService(
        BulkOperationRepository bulkOperationRepository,
        BulkOperationItemRepository bulkOperationItemRepository,
        AppUserService appUserService,
        BulkEnvelopeProcessor bulkEnvelopeProcessor) {
        this.bulkOperationRepository = bulkOperationRepository;
        this.bulkOperationItemRepository = bulkOperationItemRepository;
        this.appUserService = appUserService;
        this.bulkEnvelopeProcessor = bulkEnvelopeProcessor;
    }

    @Transactional
    public BulkOperation createBulkOperation(Jwt jwt, CreateBulkOperationCommand command) {
        AppUser user = appUserService.getOrCreateUser(jwt);
        BulkOperation operation = bulkOperationRepository.save(
            new BulkOperation(user, command.name(), command.items().size()));

        List<BulkOperationItem> items = command.items().stream()
            .map(item -> new BulkOperationItem(operation, item.documentId(), item.recipientListJson()))
            .toList();
        bulkOperationItemRepository.saveAll(items);

        return operation;
    }

    @Transactional(readOnly = true)
    public Page<BulkOperation> listForUser(Jwt jwt, Pageable pageable) {
        AppUser user = appUserService.getOrCreateUser(jwt);
        return bulkOperationRepository.findByUserId(user.getId(), pageable);
    }

    @Transactional(readOnly = true)
    public BulkOperationDetail getForUser(Jwt jwt, UUID operationId) {
        AppUser user = appUserService.getOrCreateUser(jwt);
        BulkOperation operation = bulkOperationRepository.findByIdAndUserId(operationId, user.getId())
            .orElseThrow(() -> new BulkOperationNotFoundException(operationId));
        List<BulkOperationItem> items = bulkOperationItemRepository.findByBulkOperationId(operation.getId());
        return new BulkOperationDetail(operation, items);
    }

    @Transactional
    public BulkOperation processBulkOperation(Jwt jwt, UUID operationId) {
        BulkOperation operation = getForUser(jwt, operationId).operation();
        bulkEnvelopeProcessor.processBulkOperationAsync(operation);
        operation.markInProgress();
        return bulkOperationRepository.save(operation);
    }

    @Transactional
    public BulkOperation cancelBulkOperation(Jwt jwt, UUID operationId) {
        BulkOperation operation = getForUser(jwt, operationId).operation();
        operation.markCancelled();
        return bulkOperationRepository.save(operation);
    }

    @Transactional
    public BulkOperation retryFailedItems(Jwt jwt, UUID operationId) {
        BulkOperation operation = getForUser(jwt, operationId).operation();
        operation.markInProgress();
        return bulkOperationRepository.save(operation);
    }

    public record CreateBulkOperationCommand(String name, List<CreateBulkItemCommand> items) {
    }

    public record CreateBulkItemCommand(UUID documentId, String recipientListJson) {
    }

    public record BulkOperationDetail(BulkOperation operation, List<BulkOperationItem> items) {
    }
}
