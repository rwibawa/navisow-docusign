package com.navisow.docusign.domain.template;

import java.util.UUID;

public class TemplateNotFoundException extends RuntimeException {

    public TemplateNotFoundException(UUID templateId) {
        super("Template not found: " + templateId);
    }
}
