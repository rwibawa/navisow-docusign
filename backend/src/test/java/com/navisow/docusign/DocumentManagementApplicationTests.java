package com.navisow.docusign;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * Smoke test — verifies that the Spring application context loads without errors.
 * Uses an in-memory H2 database so it runs without a real PostgreSQL or DocuSign connection.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.flyway.enabled=true",
    "spring.flyway.locations=classpath:db/migration",
    "spring.security.oauth2.resourceserver.jwt.issuer-uri=",
    "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost:9999/.well-known/jwks.json",
    "app.docusign.client-id=test",
    "app.docusign.client-secret=test",
    "app.docusign.auth-server=account-d.docusign.com",
    "app.docusign.redirect-uri=http://localhost:5173/auth/docusign/callback",
    "app.docusign.base-path=https://demo.docusign.net/restapi",
    "app.storage.local-path=target/test-uploads"
})
class DocumentManagementApplicationTests {

    @Test
    void contextLoads() {
        // Passes if the Spring context starts successfully
    }
}
