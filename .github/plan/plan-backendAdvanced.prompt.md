# Plan: Create Advanced DocuSign Microservice Backend

**TL;DR**: Create a second Spring Boot 3.5.0 microservice (`backend-advanced`) running on port 8081 that handles templates, bulk sending, reporting, webhooks, and admin operations. It shares the PostgreSQL database with the existing backend but runs as a separate containerized service. This approach maintains clean separation while reusing database and security patterns.

## Relevant Files

### To Create
- `backend-advanced/pom.xml` — Maven configuration
- `backend-advanced/src/main/java/com/navisow/docusign/AdvancedDocumentManagementApplication.java`
- `backend-advanced/src/main/resources/application.yml`
- `backend-advanced/src/main/resources/db/migration/V2__templates_schema.sql`
- `backend-advanced/src/main/resources/db/migration/V3__bulk_operations_schema.sql`
- `backend-advanced/src/main/resources/db/migration/V4__reporting_schema.sql`
- All API controllers, services, repositories (per phases 4-8)
- `backend-advanced/Dockerfile`
- `shared-libs/pom.xml` — Shared utilities
- `shared-libs/src/main/java/com/navisow/docusign/shared/` — Shared code

### To Modify
- `docker-compose.yml` — Add backend-advanced service
- Root `pom.xml` — Add modules (backend-advanced, shared-libs)
- Existing `backend/pom.xml` — Add shared-libs dependency
- `README.md` — Document 2-service architecture

---

## 10-Phase Implementation Plan

### Phase 1: Database Schema Extensions *(no dependencies)*
**Steps:**
1. Create Flyway migration `V2__templates_schema.sql`:
   - Template metadata (name, subject, description, owner_id, created_at, updated_at)
   - Template versions (template_id, version, definition, is_active)
   - Template recipients (template_id, role_id, recipient_name, recipient_email, recipient_type)

2. Create Flyway migration `V3__bulk_operations_schema.sql`:
   - Bulk operations (id, user_id, created_at, status, total_count, processed_count)
   - Bulk operation items (bulk_op_id, document_id, recipient_list, status, envelope_id, error_message)

3. Create Flyway migration `V4__reporting_schema.sql`:
   - Analytics snapshots (user_id, date, envelope_count, signed_count, pending_count, metric_type)
   - Audit logs (user_id, action, resource_type, resource_id, old_value, new_value, timestamp)

**Verification:**
- Schema exists in PostgreSQL; Flyway logs clean

---

### Phase 2: Shared Libraries Foundation *(parallel with Phase 1)*
**Steps:**
1. Create `shared-libs/` Maven module with parent POM inheritance from root
2. Move common components:
   - JWT validation utilities from backend's `SecurityConfig`
   - Exception hierarchy (DocumentNotFoundException, EnvelopeNotFoundException, etc.)
   - Correlation ID & logging utilities
   - Encrypted credential handling

3. Update existing backend's `pom.xml` to depend on `shared-libs`

4. Create shared domain model annotations/base classes:
   - Auditable entity base (createdAt, updatedAt, createdBy, modifiedBy)
   - Tenant isolation enforcer

**Verification:**
- Existing backend compiles against shared-libs
- Tests pass in both modules

---

### Phase 3: Backend-Advanced Base Setup *(depends on Phase 2)*
**Steps:**
1. Create `backend-advanced/` module with Maven POM:
   - Same Spring Boot 3.5.0 version
   - Java 25 version
   - Dependencies: Spring Web, Spring Data JPA, PostgreSQL driver, DocuSign eSign Java SDK, shared-libs

2. Create `AdvancedDocumentManagementApplication.java`:
   - Main Spring Boot application entry
   - Base configuration (profiles, actuator, Swagger UI)

3. Create base config classes:
   - `SecurityConfig` (inherit JWT validation from shared-libs)
   - `JpaConfig` (multi-tenancy filter, audit listeners)
   - `WebConfig` (CORS, custom error handling)
   - `DocuSignProperties` (shared with existing backend)
   - `DocuSignClientConfig` (RestClient for SDK calls)

4. Create `application.yml`:
   - Spring profile-based config (dev, test, prod)
   - Port 8081 (existing backend on 8080)
   - Database same URL as backend

**Verification:**
- Service starts: `mvn spring-boot:run`
- Health check responds: `GET http://localhost:8081/actuator/health`
- Swagger UI available: `http://localhost:8081/swagger-ui.html`

---

### Phase 4: Template Management Feature *(depends on Phase 3)*
**Steps:**
1. Create domain entities:
   - `DocuSignTemplate` (id, userId, externalTemplateId, name, subject, description, recipientDefinition, createdAt, updatedAt)
   - `TemplateVersion` (id, templateId, versionNumber, definition, isActive, createdAt)
   - `TemplateRecipient` (id, templateId, roleId, recipientName, recipientEmail, recipientType, sequenceOrder)

2. Create repository interfaces:
   - `DocuSignTemplateRepository` (findByUserId, findByExternalTemplateId, etc.)
   - `TemplateRecipientRepository`

3. Create service layer:
   - `DocuSignTemplateService`:
     - `createTemplate()` — call DocuSign API to create template, store in DB
     - `updateTemplate()` — versioning support
     - `getTemplate()` — fetch with recipient details
     - `listTemplates()` — pagination by user
     - `deleteTemplate()` — mark as deleted
     - `syncTemplatesFromDocuSign()` — bulk fetch from DocuSign API

4. Create REST API layer:
   - `POST /api/templates` — create new template
   - `GET /api/templates?page=0&size=20` — list paginated
   - `GET /api/templates/{id}` — get with recipients
   - `PUT /api/templates/{id}` — update
   - `DELETE /api/templates/{id}` — delete
   - `POST /api/templates/sync` — sync from DocuSign

**Verification:**
- Unit tests for TemplateService (mock DocuSign client)
- Integration tests with embedded PostgreSQL
- Manual test via Swagger UI

---

### Phase 5: Bulk Envelope Sending Feature *(parallel with Phase 4)*
**Steps:**
1. Create domain entities:
   - `BulkOperation` (id, userId, name, status, totalCount, processedCount, successCount, failureCount, createdAt, startedAt, completedAt)
   - `BulkOperationItem` (id, bulkOpId, documentId, recipientList, status, envelopeId, errorMessage, createdAt, completedAt)

2. Create repository interfaces:
   - `BulkOperationRepository` (findByUserId, findByStatus)
   - `BulkOperationItemRepository`

3. Create service layer:
   - `BulkEnvelopeService`:
     - `createBulkOperation()` — prepare operation, validate recipients
     - `processBulkOperation()` — async/batch send envelopes
     - `getBulkStatus()` — check progress
     - `cancelBulkOperation()` — stop in-progress
     - `retryFailedItems()` — retry failed sends

4. Create REST API layer:
   - `POST /api/bulk-operations` — create bulk send
   - `GET /api/bulk-operations?page=0` — list operations
   - `GET /api/bulk-operations/{id}` — get with items
   - `POST /api/bulk-operations/{id}/process` — trigger async processing
   - `POST /api/bulk-operations/{id}/cancel` — cancel
   - `POST /api/bulk-operations/{id}/retry` — retry failures

5. Create async job processor:
   - `BulkEnvelopeProcessor` — scheduled or queue-based batch sender
   - Error handling & partial success tracking

**Verification:**
- Unit tests with mock DocuSign client
- Integration tests simulating bulk CSV upload
- Manual test: trigger bulk send, verify envelopes created

---

### Phase 6: Advanced Webhook Processing *(parallel with Phases 4-5)*
**Steps:**
1. Create domain entities:
   - `WebhookEvent` (id, externalEventId, envelopeId, eventType, payload, processedAt, status, error)
   - `WebhookProcessingRule` (id, userId, eventType, action, targetUrl, isActive)

2. Create service layer:
   - `WebhookEventService`:
     - `processEvent()` — validate signature, deserialize, route
     - `applyProcessingRules()` — execute custom rules
     - `retryFailedEvents()` — DLQ pattern

3. Create REST API layer:
   - `POST /api/webhook/events` — public endpoint (HMAC-validated)
   - `GET /api/webhook/events?page=0` — list processed events
   - `POST /api/webhook/rules` — define custom processing rules

4. Enhance existing webhook validation:
   - Implement HMAC signature verification per DocuSign docs
   - Add event deduplication (idempotency)

**Verification:**
- Unit tests for HMAC validation
- Integration tests with mock webhook payloads
- Manual test: trigger envelope event, verify processing

---

### Phase 7: Reporting & Analytics *(parallel)*
**Steps:**
1. Create domain entities:
   - `AnalyticsSnapshot` (id, userId, date, envelopeCount, signedCount, pendingCount, averageTimeToSign, metricType)
   - `AuditLog` (id, userId, action, resourceType, resourceId, oldValue, newValue, timestamp)

2. Create service layer:
   - `ReportingService`:
     - `generateDailyReport()` — aggregate envelope statuses
     - `generateUserReport()` — per-user metrics
     - `getAuditLog()` — query audit entries

3. Create REST API layer:
   - `GET /api/reporting/dashboard?startDate=...&endDate=...` — aggregated metrics
   - `GET /api/reporting/user-stats` — user's metrics
   - `GET /api/reporting/audit-log?page=0` — audit trail

4. Scheduled job:
   - `@Scheduled` task to compute daily snapshots overnight

**Verification:**
- Unit tests for aggregation logic
- Integration tests with test data
- Manual test: verify dashboard data

---

### Phase 8: Admin Operations *(parallel)*
**Steps:**
1. Create domain entities (if not shared):
   - Use existing `AppUser` entity
   - Add admin-specific attributes (role, permissions) if needed

2. Create service layer:
   - `AdminService`:
     - `listAllUsers()` — paginated
     - `getUserDetails()` — with statistics
     - `suspendUser()` — soft delete or flag
     - `resetUserTokens()` — force DocuSign re-auth
     - `exportUserData()` — GDPR/compliance

3. Create REST API layer (admin-only, role-based):
   - `GET /api/admin/users?page=0` — list
   - `GET /api/admin/users/{id}` — details
   - `PUT /api/admin/users/{id}/suspend` — suspend
   - `POST /api/admin/users/{id}/reset-tokens` — force re-auth
   - `GET /api/admin/users/{id}/data-export` — export

4. Add role-based authorization:
   - Spring Security role checks (@PreAuthorize("hasRole('ADMIN')"))
   - Separate JWT claims for admin status

**Verification:**
- Unit tests for authorization
- Integration tests with admin/non-admin users
- Manual test: verify non-admin blocked

---

### Phase 9: Docker & Deployment Integration *(depends on all services ready)*
**Steps:**
1. Create `backend-advanced/Dockerfile`:
   - Multi-stage build: Maven + OpenJDK 25
   - Build jar, copy to runtime stage
   - Expose port 8081

2. Update root `docker-compose.yml`:
   - Add `backend-advanced` service (port 8081)
   - Same PostgreSQL, Redis (if caching added)
   - Environment variables for app.docusign, OIDC_ISSUER_URI
   - Depends on postgres (health check)

3. Create `backend-advanced/.env` file (or update root .env):
   - Port, database URL, DocuSign credentials, OIDC config

4. Update root `pom.xml` to include `backend-advanced` module

**Verification:**
- `docker-compose up` — both services start
- `curl http://localhost:8080/actuator/health` and `http://localhost:8081/actuator/health`
- Frontend can call both services

---

### Phase 10: Integration Testing & Documentation *(depends on Phase 9)*
**Steps:**
1. Create end-to-end test scenarios:
   - Template → Bulk send using template → Monitor webhook events
   - Document upload → Analytics snapshot → Reporting

2. Update root README with:
   - Architecture diagram (2 backends)
   - API documentation for each service
   - Deployment instructions

3. Add deployment guide:
   - Local development (docker-compose)
   - Production deployment (K8s, cloud, etc.)

**Verification:**
- E2E tests pass
- Documentation accurate and complete

---

## Architecture Overview
```
navisow-docusign/
├── shared-libs/               (NEW - reusable components)
├── backend/                   (existing - core service, port 8080)
├── backend-advanced/          (NEW - advanced features, port 8081)
├── frontend/                  (existing)
└── docker-compose.yml         (updated)
```

---

## Key Features by Service

### Backend (Existing - Core Service, Port 8080)
- Document management & signing (existing)
- DocuSign OAuth (per-user account linking)
- Envelope management
- Basic webhook processing
- Multi-tenancy via JWT

### Backend-Advanced (New - Advanced Features, Port 8081)
1. **Template Management** — Create, version, manage reusable templates
2. **Bulk Envelope Sending** — Send multiple envelopes with progress tracking & retry
3. **Advanced Webhooks** — Event processing rules, HMAC validation, deduplication
4. **Reporting & Analytics** — Daily metrics, user statistics, audit logs
5. **Admin Operations** — User management, token reset, data export

---

## Key Decisions & Assumptions

1. **Shared Database** — Both services use same PostgreSQL for simplicity. If scaling, can move to separate DBs later with replication/federation.

2. **Separate Codebases** — No hard-coded dependencies between services; they communicate via REST API if needed.

3. **Shared Libraries** — Reuse security patterns, exception handling, and utilities to avoid duplication.

4. **Async Bulk Processing** — Bulk send uses async/scheduled jobs to avoid blocking HTTP requests.

5. **HMAC Webhook Validation** — Enhanced security for webhook processing in new service.

6. **Admin Role** — Assumes JWT claims can include role; if not, add claim during token issuance.

7. **Port Configuration** — Backend on 8080, Backend-Advanced on 8081 (both accessible internally and from frontend).

---

## Scope Boundaries

### ✅ Included
- Template CRUD + versioning
- Bulk envelope sending with progress tracking
- Advanced webhook processing with rules
- Daily reporting snapshots
- Admin user management
- Both services in docker-compose
- Shared utilities library

### ❌ Excluded (Future/Optional)
- Elasticsearch for advanced searching
- Redis caching layer
- Kubernetes deployment (focus on docker-compose first)
- Approval workflows / signing order optimization
- Multi-language support
- Frontend updates for new features (backend-first)

---

## Verification Strategy

| Phase | Verification Method |
|-------|-------------------|
| 1 | Schema exists in PostgreSQL; Flyway logs clean |
| 2 | Existing backend tests pass; no compilation errors |
| 3 | New service starts; health endpoints respond on both ports |
| 4-8 | Unit tests + integration tests for each feature |
| 9 | `docker-compose up` brings up both services with health checks passing |
| 10 | E2E tests pass; documentation accurate & complete |

---

## Blockers & Risks

| Risk | Mitigation |
|------|-----------|
| Database migration conflicts (both services run migrations) | Single Flyway schema per DB; use versioning convention (V2, V3, V4) |
| Token refresh race conditions | Existing backend's 60s refresh buffer should handle; consider distributed lock if scaling |
| Webhook event duplication | Add idempotency key to DeduplicationService |
| Admin authorization leakage | All admin endpoints require JWT role claim validation |

---

## Implementation Notes

### Technology Stack (Both Services)
- **Framework**: Spring Boot 3.5.0
- **Language**: Java 25
- **Database**: PostgreSQL (shared)
- **API Client**: Spring RestClient (non-blocking)
- **Authentication**: OAuth2 + JWT (OIDC)
- **Migration**: Flyway
- **API Docs**: SpringDoc OpenAPI 2.8.0
- **DocuSign SDK**: `docusign-esign-java`

### Development Workflow
1. Create shared-libs independently (Phase 2)
2. Update existing backend to use shared-libs (parallel with Phase 2)
3. Create backend-advanced base (Phase 3)
4. Implement features in parallel (Phases 4-8)
5. Integration test and Docker setup (Phases 9-10)

### Configuration Management
- Both services read from `application.yml` + environment variables
- Use Spring profiles: `dev`, `test`, `prod`
- Secrets stored in `.env` file (not committed to repo)
- DocuSign credentials: Same across both services (shared auth)

---

## Next Steps
1. ✅ Plan confirmed
2. Create Phase 1 (database schema)
3. Create Phase 2 (shared libraries)
4. Create Phase 3 (backend-advanced base)
5. Implement Phases 4-8 (features)
6. Docker setup & testing (Phases 9-10)
7. Deploy to production
