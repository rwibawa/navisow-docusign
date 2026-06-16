package com.navisow.docusign.api.docusign;

import java.util.Map;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.navisow.docusign.domain.docusign.DocuSignAccountLink;
import com.navisow.docusign.domain.docusign.DocuSignAccountLinkRepository;
import com.navisow.docusign.domain.user.AppUser;
import com.navisow.docusign.domain.user.AppUserService;
import com.navisow.docusign.integration.docusign.DocuSignTokenService;

/**
 * Handles DocuSign OAuth 2.0 Authorization Code flow per app user.
 *
 * Flow:
 *  1. GET /api/docusign/auth/authorize-url  → returns {url, state}
 *  2. Frontend redirects user to the returned URL
 *  3. DocuSign redirects to VITE_APP host /auth/docusign/callback?code=...&state=...
 *  4. Frontend POSTs {code, state} to /api/docusign/auth/callback
 *  5. Backend exchanges code for tokens and persists them
 */
@RestController
@RequestMapping("/api/docusign/auth")
public class DocuSignOAuthController {

    private final DocuSignTokenService tokenService;
    private final AppUserService userService;
    private final DocuSignAccountLinkRepository linkRepository;

    public DocuSignOAuthController(DocuSignTokenService tokenService,
                                   AppUserService userService,
                                   DocuSignAccountLinkRepository linkRepository) {
        this.tokenService = tokenService;
        this.userService = userService;
        this.linkRepository = linkRepository;
    }

    /** Step 1 — returns DocuSign authorization URL with a random CSRF state token. */
    @GetMapping("/authorize-url")
    public ResponseEntity<Map<String, String>> authorizeUrl() {
        String state = UUID.randomUUID().toString();
        String url = tokenService.buildAuthorizationUrl(state);
        return ResponseEntity.ok(Map.of("url", url, "state", state));
    }

    /** Step 4 — receives the authorization code and completes the OAuth exchange. */
    @PostMapping("/callback")
    public ResponseEntity<Map<String, Object>> callback(
            @RequestBody CallbackRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        AppUser user = userService.getOrCreateUser(jwt);
        DocuSignAccountLink link = tokenService.exchangeCodeAndLink(user, request.code());

        return ResponseEntity.ok(Map.of(
            "connected", true,
            "accountId", link.getAccountId(),
            "baseUri", link.getBaseUri()
        ));
    }

    /** Returns the current DocuSign connection status for the authenticated user. */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status(@AuthenticationPrincipal Jwt jwt) {
        AppUser user = userService.getOrCreateUser(jwt);
        return linkRepository.findFirstByUser_Id(user.getId())
            .map(link -> ResponseEntity.ok(Map.<String, Object>of(
                "connected", true,
                "accountId", link.getAccountId(),
                "expiresAt", link.getTokenExpiresAt() != null
                    ? link.getTokenExpiresAt().toString() : null
            )))
            .orElseGet(() -> ResponseEntity.ok(Map.of("connected", false)));
    }

    /** Disconnects the user's DocuSign account link. */
    @DeleteMapping("/connection")
    public ResponseEntity<Void> disconnect(@AuthenticationPrincipal Jwt jwt) {
        AppUser user = userService.getOrCreateUser(jwt);
        linkRepository.findFirstByUser_Id(user.getId())
            .ifPresent(linkRepository::delete);
        return ResponseEntity.noContent().build();
    }

    public record CallbackRequest(String code, String state) {}
}
