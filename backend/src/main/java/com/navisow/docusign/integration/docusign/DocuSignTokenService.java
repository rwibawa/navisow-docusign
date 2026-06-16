package com.navisow.docusign.integration.docusign;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import com.navisow.docusign.config.DocuSignProperties;
import com.navisow.docusign.domain.docusign.DocuSignAccountLink;
import com.navisow.docusign.domain.docusign.DocuSignAccountLinkRepository;
import com.navisow.docusign.domain.user.AppUser;

@Service
public class DocuSignTokenService {

    private final DocuSignProperties props;
    private final DocuSignAccountLinkRepository linkRepository;
    private final RestClient restClient;

    public DocuSignTokenService(DocuSignProperties props,
                                DocuSignAccountLinkRepository linkRepository,
                                RestClient.Builder builder) {
        this.props = props;
        this.linkRepository = linkRepository;
        this.restClient = builder.build();
    }

    /** Builds the DocuSign OAuth authorization URL. The caller should persist state for CSRF validation. */
    public String buildAuthorizationUrl(String state) {
        return "https://" + props.authServer() + "/oauth/auth"
            + "?response_type=code"
            + "&scope=signature"
            + "&client_id=" + props.clientId()
            + "&redirect_uri=" + props.redirectUri()
            + "&state=" + state;
    }

    @Transactional
    public DocuSignAccountLink exchangeCodeAndLink(AppUser user, String code) {
        TokenResponse token = exchangeCode(code);
        UserInfoResponse info = fetchUserInfo(token.accessToken());

        DocuSignAccountLink link = linkRepository
            .findByUser_IdAndAccountId(user.getId(), info.accountId())
            .orElse(new DocuSignAccountLink(user, info.accountId(), info.baseUri()));

        // NOTE: store encrypted tokens for production deployments
        link.setAccessTokenCipher(token.accessToken());
        link.setRefreshTokenCipher(token.refreshToken());
        link.setTokenExpiresAt(Instant.now().plusSeconds(token.expiresIn() - 60));
        link.setBaseUri(info.baseUri());

        return linkRepository.save(link);
    }

    /**
     * Proactively refreshes the access token if it is within 60 seconds of expiry.
     * Should be called before every DocuSign API request.
     */
    @Transactional
    public DocuSignAccountLink refreshIfExpired(DocuSignAccountLink link) {
        Instant expiresAt = link.getTokenExpiresAt();
        if (expiresAt != null && Instant.now().isBefore(expiresAt)) {
            return link;  // still valid
        }
        String refreshToken = link.getRefreshTokenCipher();
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new IllegalStateException(
                "DocuSign access token expired and no refresh token available for account "
                    + link.getAccountId());
        }
        TokenResponse newToken = doRefresh(refreshToken);
        link.setAccessTokenCipher(newToken.accessToken());
        if (newToken.refreshToken() != null && !newToken.refreshToken().isBlank()) {
            link.setRefreshTokenCipher(newToken.refreshToken());
        }
        link.setTokenExpiresAt(Instant.now().plusSeconds(newToken.expiresIn() - 60));
        return linkRepository.save(link);
    }

    private TokenResponse doRefresh(String refreshToken) {
        String credentials = Base64.getEncoder().encodeToString(
            (props.clientId() + ":" + props.clientSecret()).getBytes(StandardCharsets.UTF_8));

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "refresh_token");
        form.add("refresh_token", refreshToken);

        @SuppressWarnings("unchecked")
        Map<String, Object> body = restClient.post()
            .uri("https://" + props.authServer() + "/oauth/token")
            .header(HttpHeaders.AUTHORIZATION, "Basic " + credentials)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(form)
            .retrieve()
            .body(Map.class);

        if (body == null) throw new IllegalStateException("Empty token refresh response from DocuSign");

        long expiresIn = body.get("expires_in") instanceof Number n ? n.longValue() : 3600L;
        return new TokenResponse(
            (String) body.get("access_token"),
            (String) body.get("refresh_token"),
            expiresIn
        );
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private TokenResponse exchangeCode(String code) {
        String credentials = Base64.getEncoder().encodeToString(
            (props.clientId() + ":" + props.clientSecret()).getBytes(StandardCharsets.UTF_8));

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("code", code);
        form.add("redirect_uri", props.redirectUri());

        @SuppressWarnings("unchecked")
        Map<String, Object> body = restClient.post()
            .uri("https://" + props.authServer() + "/oauth/token")
            .header(HttpHeaders.AUTHORIZATION, "Basic " + credentials)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(form)
            .retrieve()
            .body(Map.class);

        if (body == null) throw new IllegalStateException("Empty token response from DocuSign");

        long expiresIn = body.get("expires_in") instanceof Number n ? n.longValue() : 3600L;
        return new TokenResponse(
            (String) body.get("access_token"),
            (String) body.get("refresh_token"),
            expiresIn
        );
    }

    private UserInfoResponse fetchUserInfo(String accessToken) {
        @SuppressWarnings("unchecked")
        Map<String, Object> body = restClient.get()
            .uri("https://" + props.authServer() + "/oauth/userinfo")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
            .retrieve()
            .body(Map.class);

        if (body == null) throw new IllegalStateException("Empty userinfo response from DocuSign");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> accounts = (List<Map<String, Object>>) body.get("accounts");

        Map<String, Object> defaultAccount = accounts.stream()
            .filter(a -> Boolean.TRUE.equals(a.get("is_default")))
            .findFirst()
            .orElse(accounts.get(0));

        String baseUri = (String) defaultAccount.get("base_uri");
        return new UserInfoResponse(
            (String) defaultAccount.get("account_id"),
            baseUri + "/restapi"
        );
    }

    private record TokenResponse(String accessToken, String refreshToken, long expiresIn) {}
    private record UserInfoResponse(String accountId, String baseUri) {}
}
