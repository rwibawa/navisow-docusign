package com.navisow.docusign.integration.docusign;

import com.navisow.docusign.domain.docusign.DocuSignAccountLink;
import com.navisow.docusign.domain.docusign.DocuSignAccountLinkRepository;
import com.navisow.docusign.domain.user.AppUser;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class DocuSignTemplateIntegrationService {

    private final DocuSignAccountLinkRepository linkRepository;
    private final DocuSignTokenService tokenService;
    private final RestClient restClient;

    public DocuSignTemplateIntegrationService(
        DocuSignAccountLinkRepository linkRepository,
        DocuSignTokenService tokenService,
        RestClient.Builder builder) {
        this.linkRepository = linkRepository;
        this.tokenService = tokenService;
        this.restClient = builder.build();
    }

    public List<TemplateSnapshot> listTemplatesForCurrentAccount(AppUser user) {
        DocuSignAccountLink link = linkRepository.findFirstByUser_Id(user.getId())
            .orElseThrow(() -> new IllegalStateException(
                "No DocuSign account connected for user " + user.getId()));
        DocuSignAccountLink refreshed = tokenService.refreshIfExpired(link);

        @SuppressWarnings("unchecked")
        Map<String, Object> response = restClient.get()
            .uri(refreshed.getBaseUri() + "/v2.1/accounts/{accountId}/templates", refreshed.getAccountId())
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + refreshed.getAccessTokenCipher())
            .retrieve()
            .body(Map.class);

        if (response == null) {
            return List.of();
        }

        Object envelopeTemplates = response.get("envelopeTemplates");
        if (!(envelopeTemplates instanceof List<?> templates)) {
            return List.of();
        }

        return templates.stream()
            .filter(Map.class::isInstance)
            .map(Map.class::cast)
            .map(this::toSnapshot)
            .toList();
    }

    private TemplateSnapshot toSnapshot(Map<?, ?> item) {
        String id = value(item, "templateId");
        String name = value(item, "name");
        String subject = value(item, "subject");
        return new TemplateSnapshot(id, name, subject);
    }

    private String value(Map<?, ?> item, String key) {
        Object value = item.get(key);
        return value == null ? "" : String.valueOf(value);
    }

    public record TemplateSnapshot(String externalTemplateId, String name, String subject) {
    }
}
