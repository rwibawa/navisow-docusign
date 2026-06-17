package com.navisow.docusign.integration.docusign;

import com.navisow.docusign.config.DocuSignProperties;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;

@Service
public class DocuSignWebhookSignatureService {

    private static final String HMAC_SHA256 = "HmacSHA256";

    private final DocuSignProperties properties;

    public DocuSignWebhookSignatureService(DocuSignProperties properties) {
        this.properties = properties;
    }

    public boolean isEnabled() {
        String secret = properties.webhookSecret();
        return secret != null && !secret.isBlank();
    }

    public boolean verify(String payload, String signature) {
        if (!isEnabled()) {
            return true;
        }
        if (signature == null || signature.isBlank()) {
            return false;
        }
        try {
            String expected = sign(payload, properties.webhookSecret());
            return constantTimeEquals(expected, signature.trim());
        } catch (GeneralSecurityException ex) {
            return false;
        }
    }

    private String sign(String payload, String secret) throws GeneralSecurityException {
        Mac hmac = Mac.getInstance(HMAC_SHA256);
        hmac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
        byte[] digest = hmac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(digest);
    }

    private boolean constantTimeEquals(String left, String right) {
        byte[] a = left.getBytes(StandardCharsets.UTF_8);
        byte[] b = right.getBytes(StandardCharsets.UTF_8);
        if (a.length != b.length) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < a.length; i++) {
            result |= a[i] ^ b[i];
        }
        return result == 0;
    }
}
