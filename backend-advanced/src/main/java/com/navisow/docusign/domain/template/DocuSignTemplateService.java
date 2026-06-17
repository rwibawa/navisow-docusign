package com.navisow.docusign.domain.template;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.navisow.docusign.domain.user.AppUser;
import com.navisow.docusign.domain.user.AppUserService;
import com.navisow.docusign.integration.docusign.DocuSignTemplateIntegrationService;

@Service
public class DocuSignTemplateService {

    private final DocuSignTemplateRepository templateRepository;
    private final TemplateVersionRepository templateVersionRepository;
    private final TemplateRecipientRepository templateRecipientRepository;
    private final DocuSignTemplateIntegrationService integrationService;
    private final AppUserService appUserService;

    public DocuSignTemplateService(
        DocuSignTemplateRepository templateRepository,
        TemplateVersionRepository templateVersionRepository,
        TemplateRecipientRepository templateRecipientRepository,
        DocuSignTemplateIntegrationService integrationService,
        AppUserService appUserService) {
        this.templateRepository = templateRepository;
        this.templateVersionRepository = templateVersionRepository;
        this.templateRecipientRepository = templateRecipientRepository;
        this.integrationService = integrationService;
        this.appUserService = appUserService;
    }

    @Transactional
    public DocuSignTemplate create(Jwt jwt, CreateTemplateCommand command) {
        AppUser user = appUserService.getOrCreateUser(jwt);
        DocuSignTemplate template = new DocuSignTemplate(
            user,
            command.externalTemplateId(),
            command.name(),
            command.subject(),
            command.description());
        DocuSignTemplate saved = templateRepository.save(template);

        templateVersionRepository.save(new TemplateVersion(
            saved,
            1,
            command.definitionJson() == null ? "{}" : command.definitionJson(),
            true));

        saveRecipients(saved, command.recipients());
        return saved;
    }

    @Transactional(readOnly = true)
    public Page<DocuSignTemplate> listForUser(Jwt jwt, Pageable pageable) {
        AppUser user = appUserService.getOrCreateUser(jwt);
        return templateRepository.findByUserId(user.getId(), pageable);
    }

    @Transactional(readOnly = true)
    public DocuSignTemplate getForUser(Jwt jwt, UUID templateId) {
        AppUser user = appUserService.getOrCreateUser(jwt);
        return templateRepository.findByIdAndUserId(templateId, user.getId())
            .orElseThrow(() -> new TemplateNotFoundException(templateId));
    }

    @Transactional(readOnly = true)
    public TemplateDetails getDetailForUser(Jwt jwt, UUID templateId) {
        DocuSignTemplate template = getForUser(jwt, templateId);
        return new TemplateDetails(
            template,
            templateVersionRepository.findByTemplateIdOrderByVersionNumberDesc(template.getId()),
            templateRecipientRepository.findByTemplateIdOrderBySequenceOrderAsc(template.getId()));
    }

    @Transactional
    public DocuSignTemplate update(Jwt jwt, UUID templateId, UpdateTemplateCommand command) {
        DocuSignTemplate template = getForUser(jwt, templateId);
        template.setName(command.name());
        template.setSubject(command.subject());
        template.setDescription(command.description());
        template.setExternalTemplateId(command.externalTemplateId());
        DocuSignTemplate saved = templateRepository.save(template);

        int nextVersion = templateVersionRepository.findTopByTemplateIdOrderByVersionNumberDesc(saved.getId())
            .map(v -> v.getVersionNumber() + 1)
            .orElse(1);

        templateVersionRepository.findByTemplateIdAndIsActiveTrue(saved.getId())
            .forEach(v -> v.setActive(false));

        templateVersionRepository.save(new TemplateVersion(
            saved,
            nextVersion,
            command.definitionJson() == null ? "{}" : command.definitionJson(),
            true));

        templateRecipientRepository.deleteByTemplateId(saved.getId());
        saveRecipients(saved, command.recipients());
        return saved;
    }

    @Transactional
    public void delete(Jwt jwt, UUID templateId) {
        DocuSignTemplate template = getForUser(jwt, templateId);
        templateRepository.delete(template);
    }

    @Transactional
    public List<DocuSignTemplate> syncTemplatesFromDocuSign(Jwt jwt) {
        AppUser user = appUserService.getOrCreateUser(jwt);
        List<DocuSignTemplateIntegrationService.TemplateSnapshot> snapshots = integrationService.listTemplatesForCurrentAccount(user);

        return snapshots.stream().map(snapshot -> templateRepository
            .findByUserIdAndExternalTemplateId(user.getId(), snapshot.externalTemplateId())
            .map(existing -> {
                existing.setName(snapshot.name());
                existing.setSubject(snapshot.subject());
                return templateRepository.save(existing);
            })
            .orElseGet(() -> templateRepository.save(new DocuSignTemplate(
                user,
                snapshot.externalTemplateId(),
                snapshot.name(),
                snapshot.subject(),
                null))))
            .toList();
    }

    private void saveRecipients(DocuSignTemplate template, List<RecipientCommand> recipients) {
        if (recipients == null || recipients.isEmpty()) {
            return;
        }
        List<TemplateRecipient> entities = recipients.stream()
            .map(r -> new TemplateRecipient(
                template,
                r.roleId(),
                r.recipientName(),
                r.recipientEmail(),
                r.recipientType(),
                r.sequenceOrder()))
            .toList();
        templateRecipientRepository.saveAll(entities);
    }

    public record CreateTemplateCommand(
        String externalTemplateId,
        String name,
        String subject,
        String description,
        String definitionJson,
        List<RecipientCommand> recipients) {
    }

    public record UpdateTemplateCommand(
        String externalTemplateId,
        String name,
        String subject,
        String description,
        String definitionJson,
        List<RecipientCommand> recipients) {
    }

    public record RecipientCommand(
        String roleId,
        String recipientName,
        String recipientEmail,
        String recipientType,
        int sequenceOrder) {
    }

    public record TemplateDetails(
        DocuSignTemplate template,
        List<TemplateVersion> versions,
        List<TemplateRecipient> recipients) {
    }
}
