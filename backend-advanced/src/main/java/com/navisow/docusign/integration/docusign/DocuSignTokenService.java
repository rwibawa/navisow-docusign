package com.navisow.docusign.integration.docusign;

import com.navisow.docusign.config.DocuSignProperties;
import com.navisow.docusign.domain.docusign.DocuSignAccountLink;
import com.navisow.docusign.domain.docusign.DocuSignAccountLinkRepository;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

@Service
public class DocuSignTokenService {

    private final DocuSignProperties properties;
    private final DocuSignAccountLinkRepository linkRepository;
    private final RestClient restClient;

    public DocuSignTokenService(
        DocuSignProperties properties,
        DocuSignAccountLinkRepository linkRepository,
        RestClient.Builder builder) {
        this.properties = properties;
        this.linkRepository = linkRepository;
        this.restClient = builder.build();
    }

    @Transactional
    public DocuSignAccountLink refreshIfExpired(DocuSignAccountLink link) {
        Instant expiresAt = link.getTokenExpiresAt();
        if (expiresAt != null && Instant.now().isBefore(expiresAt)) {
            return link;
        }

        String refreshToken = link.getRefreshTokenCipher();
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new IllegalStateException(
                "DocuSign access token expired and no refresh token available for account "
                    + link.getAccountId());
        }

        TokenResponse token = refreshToken(refreshToken);
        link.setAccessTokenCipher(token.accessToken());
        if (token.refreshToken() != null && !token.refreshToken().isBlank()) {
            link.setRefreshTokenCipher(token.refreshToken());
        }
        link.setTokenExpiresAt(Instant.now().plusSeconds(token.expiresIn() - 60));
        return linkRepository.save(link);
    }

    private TokenResponse refreshToken(String refreshToken) {
        String credentials = Base64.getEncoder().encodeToString(
            (properties.clientId() + ":" + properties.clientSecret()).getBytes(StandardCharsets.UTF_8));

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "refresh_token");
        form.add("refresh_token", refreshToken);

        @SuppressWarnings("unchecked")
        Map<String, Object> body = restClient.post()
            .uri("https://" + properties.authServer() + "/oauth/token")
            .header(HttpHeaders.AUTHORIZATION, "Basic " + credentials)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(form)
            .retrieve()
            .body(Map.class);

        if (body == null) {
            throw new IllegalStateException("Empty token refresh response from DocuSign");
        }

        long expiresIn = body.get("expires_in") instanceof Number n ? n.longValue() : 3600L;
        return new TokenResponse(
            (String) body.get("access_token"),
            (String) body.get("refresh_token"),
            expiresIn);
    }

    private record TokenResponse(String accessToken, String refreshToken, long expiresIn) {
    }
}
