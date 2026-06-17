package com.navisow.docusign.domain.admin;

import java.util.UUID;

public class AdminUserNotFoundException extends RuntimeException {

    public AdminUserNotFoundException(UUID userId) {
        super("User not found: " + userId);
    }
}
