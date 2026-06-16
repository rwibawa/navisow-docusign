## Plan: DocuSign Document Management MVP

Build a two-app monorepo MVP with Spring Boot (Java 25) backend and React (Node 24, Vite, TypeScript) frontend, centered on OAuth-per-user DocuSign connections, full envelope lifecycle workflows, webhook-driven status sync, and OIDC-secured app access. The approach prioritizes local Docker development first while keeping storage and auth abstractions cloud-ready.

**Steps**
1. Phase 1 - Repository and baseline scaffolding
2. Create a clear root layout with separate backend and frontend app folders, plus shared docs and docker compose assets.
3. Scaffold backend with Spring Boot 3.x compatible with Java 25, including modules for web, security, validation, JPA, Actuator, OAuth2 client/resource server, and OpenAPI. *blocks later backend work*
4. Scaffold frontend with Vite + React + TypeScript under Node 24, including routing, API client layer, state/query library, and form handling baseline. *parallel with step 3 after root structure exists*
5. Add root-level developer ergonomics: consistent env template files, lint/format configs, make/npm helper commands, and Docker Compose for PostgreSQL + optional object storage emulator. *parallel with steps 3-4*

6. Phase 2 - Domain model and persistence
7. Define backend entities and migrations for users, identity provider subjects, DocuSign account links, document records, envelopes, recipients, envelope events, and audit artifacts.
8. Implement storage abstraction for uploaded source documents and signed artifacts (local disk first, S3-compatible adapter behind interface).
9. Implement repository and service boundaries so DocuSign-facing workflow state is persisted independently from provider API responses.

10. Phase 3 - Security and identity integration
11. Configure backend as OIDC-protected API (resource server) and map authenticated principal to app user model.
12. Implement frontend OIDC login flow and protected routes; enforce token propagation for API requests.
13. Introduce authorization checks for sender/viewer/admin actions based on role claims or mapped app roles.

14. Phase 4 - DocuSign integration foundation
15. Implement DocuSign OAuth Authorization Code integration per app user: connect account, store encrypted refresh/access token metadata, rotate access tokens, disconnect account.
16. Build DocuSign API client layer with retry/backoff, correlation IDs, and error mapping.
17. Add account discovery and default account/base URI handling to support multiple DocuSign account contexts per connected user if needed.

18. Phase 5 - Core document and envelope workflows
19. Implement file upload and metadata capture endpoint + UI.
20. Implement send-envelope workflow using uploaded file or selected DocuSign template with role/recipient assignment, reminder and expiration options.
21. Implement embedded signing flow endpoints and UI handoff for recipient signing sessions.
22. Implement envelope status dashboard (list/detail/timeline) backed by local persisted events and periodic refresh fallback.
23. Implement certificate of completion retrieval and download workflow for completed envelopes.

24. Phase 6 - Eventing and synchronization
25. Add DocuSign Connect webhook endpoint with signature/secret validation, idempotent event handling, and dead-letter logging path.
26. Persist event history and update envelope/document read models from webhook events.
27. Add reconciliation job to backfill missed events by polling DocuSign envelope status when webhook gaps are detected.

28. Phase 7 - Frontend UX completion
29. Build pages for connection management, document upload, envelope composer, envelope details, audit trail download, and operational status.
30. Implement robust form validation, optimistic UI where safe, and explicit error states for DocuSign auth/token failures.
31. Add activity timeline and filter/search capabilities for envelope status operations.

32. Phase 8 - Observability, hardening, and delivery
33. Add structured logging, request tracing IDs, health checks, and basic metrics (API latency, webhook throughput, DocuSign failure rates).
34. Add rate limiting and input constraints for uploads and webhook endpoints.
35. Provide Docker-based local run profile and seeded demo data path.
36. Document setup and operational runbook, including DocuSign developer account/app setup, OAuth redirect URIs, and webhook configuration.

**Relevant files**
- /Users/wibawar0/workspaces/github/rwibawa/navisow-docusign/.vscode/settings.json - Existing workspace convention (Snyk integration) to preserve.
- /Users/wibawar0/workspaces/github/rwibawa/navisow-docusign/.github - Place project-level CI and guidance docs as they are introduced.
- New files to create under planned folders:
  - backend/ (Spring Boot application, configs, migrations, tests)
  - frontend/ (Vite React app, routes, components, API client)
  - infra/ or docker/ (compose and local services)
  - docs/ (architecture, setup, workflows)

**Verification**
1. Environment checks: verify Java 25, Node 24, Docker versions; confirm backend and frontend install/build cleanly.
2. Backend quality gates: run unit/integration tests, migration validation, static analysis, and OpenAPI generation checks.
3. Frontend quality gates: run type-check, lint, unit tests, and production build.
4. End-to-end local flow test:
5. Sign in via OIDC.
6. Connect DocuSign account via OAuth.
7. Upload document.
8. Send envelope with template/roles and reminder settings.
9. Open embedded signing session.
10. Confirm webhook updates dashboard status.
11. Download certificate of completion.
12. Fault-path checks: expired DocuSign token refresh, webhook duplicate delivery idempotency, missing webhook reconciliation by polling.

**Decisions**
- Included scope: OAuth Authorization Code per user, all requested DocuSign workflows, PostgreSQL + local/S3-compatible storage, OIDC app auth, local Docker-first delivery.
- Excluded in MVP: multi-tenant org billing, advanced analytics/reporting, production cloud IaC, external message broker requirement unless webhook load demands it.
- Architecture preference: clean separation between provider integration client, domain services, and persistence read model to keep future provider swap/expansion feasible.

**Further Considerations**
1. OIDC provider choice for local-first (recommended options): Keycloak container (self-hosted local parity) vs Auth0/Okta dev tenant (faster setup).
2. Storage choice for local runs: direct filesystem volume (simplest) vs MinIO S3-compatible (better cloud parity).
3. Webhook exposure strategy in local dev: tunneling service (quick) vs reverse proxy with public endpoint in shared dev environment (more stable).
