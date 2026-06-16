package com.navisow.docusign.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.docusign")
public record DocuSignProperties(
    String basePath,
    String authServer,
    String clientId,
    String clientSecret,
    String redirectUri
) {}
