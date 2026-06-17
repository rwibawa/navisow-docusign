# Navisow DocuSign Document Management

Full-stack document management and signing workflow application backed by the DocuSign eSignature API.

| Layer | Stack |
|---|---|
| Backend API | Java 25 · Spring Boot 3.5 · Spring Security (JWT resource server) · Spring Data JPA · Flyway |
| Backend Advanced API | Java 25 · Spring Boot 3.5 · Async/Scheduled workers · Webhook rule engine · Reporting analytics |
| Frontend | Node.js 24 · React 19 · Vite · TypeScript · react-oidc-context |
| Auth | OIDC (Keycloak dev container) + DocuSign OAuth Authorization Code per user |
| Database | PostgreSQL 16 |
| Local services | Docker Compose (PostgreSQL + Keycloak) |

---

## Project Layout

```
navisow-docusign/
├── backend/          Spring Boot API & workflow services
├── backend-advanced/ Spring Boot advanced API (templates, bulk, reporting, webhooks)
├── frontend/         React Vite app
├── .github/plan/     Implementation planning artifacts
├── docker-compose.yml  Local PostgreSQL + Keycloak
├── .env.example      Environment variable template
├── LICENSE           ISC license
└── README.md
```

---

## Local Developer Quick Start

### 1. Prerequisites

| Tool | Version |
|---|---|
| Java | 25 |
| Maven | 3.9+ |
| Node.js | 24 LTS |
| Docker Desktop | latest |

### 2. Clone and configure environment

```bash
git clone <repo-url>
cd navisow-docusign
cp .env.example .env
# Edit .env — fill in DOCUSIGN_INTEGRATION_KEY, DOCUSIGN_CLIENT_SECRET, OIDC values
```

### 3. Start local infrastructure

```bash
docker compose up -d
```

Starts:
- **PostgreSQL** on `localhost:5432`
- **Keycloak** on `localhost:8090` (admin UI at http://localhost:8090 — user: `admin` / `admin`)

### 4. Configure Keycloak (first-time only)

1. Open http://localhost:8090
2. Create realm: `navisow`
3. Create client: `navisow-frontend` (type: **public**, redirect URI `http://localhost:5173/*`)
4. Create a test user with a password

### 5. Configure DocuSign Developer Account (first-time only)

1. Register at https://developers.docusign.com
2. Create an **Integration Key** (OAuth app)
3. Set redirect URI: `http://localhost:5173/auth/docusign/callback`
4. Copy Integration Key → `DOCUSIGN_INTEGRATION_KEY` in `.env`
5. Copy Secret Key → `DOCUSIGN_CLIENT_SECRET` in `.env`
6. In DocuSign Admin → **Connect**, add a new configuration:
   - URL (backend): `http://<your-tunnel>/api/webhook/docusign`
   - URL (backend-advanced): `http://<your-tunnel>/api/webhook/events`
   - Trigger events: `envelope-sent`, `envelope-delivered`, `envelope-completed`, `envelope-declined`, `envelope-voided`
   - Use a tunnel (e.g. `ngrok http 8080`) to expose localhost for webhook delivery

Optional security hardening for backend-advanced:
- Set `DOCUSIGN_WEBHOOK_SECRET` in `.env` to enforce HMAC signature validation on incoming webhook events

### 6. Run the backend

```bash
cd backend
mvn spring-boot:run
```

API available at http://localhost:8080
- Health: http://localhost:8080/actuator/health
- OpenAPI: http://localhost:8080/api-docs
- Swagger UI: http://localhost:8080/swagger-ui

### 6b. Run the advanced backend

```bash
cd backend-advanced
mvn spring-boot:run
```

Advanced API available at http://localhost:8081
- Health: http://localhost:8081/actuator/health
- OpenAPI: http://localhost:8081/api-docs
- Swagger UI: http://localhost:8081/swagger-ui

### 7. Run the frontend

```bash
cd frontend
npm install
npm run dev
```

App available at http://localhost:5173

---

## API Endpoints (backend)

| Method | Path | Description |
|---|---|---|
| GET | `/api/system/status` | Service status |
| GET | `/api/docusign/auth/authorize-url` | Get DocuSign OAuth URL |
| POST | `/api/docusign/auth/callback` | Exchange code + link account |
| GET | `/api/docusign/auth/status` | Connection status |
| DELETE | `/api/docusign/auth/connection` | Disconnect account |
| POST | `/api/documents` | Upload document (multipart) |
| GET | `/api/documents` | List documents |
| GET | `/api/documents/{id}/download` | Download original file |
| POST | `/api/envelopes` | Send envelope for signing |
| GET | `/api/envelopes` | List envelopes |
| GET | `/api/envelopes/{id}` | Envelope detail + event timeline |
| POST | `/api/envelopes/{id}/signing-url` | Get embedded signing URL |
| GET | `/api/envelopes/{id}/certificate` | Download certificate of completion |
| POST | `/api/webhook/docusign` | DocuSign Connect webhook (public) |

## API Endpoints (backend-advanced)

### Templates

| Method | Path | Description |
|---|---|---|
| POST | `/api/templates` | Create template |
| GET | `/api/templates` | List templates |
| GET | `/api/templates/{id}` | Template details (versions + recipients) |
| PUT | `/api/templates/{id}` | Update template |
| POST | `/api/templates/sync` | Sync templates from DocuSign account |
| DELETE | `/api/templates/{id}` | Delete template |

### Bulk Operations

| Method | Path | Description |
|---|---|---|
| POST | `/api/bulk-operations` | Create bulk operation |
| GET | `/api/bulk-operations` | List bulk operations |
| GET | `/api/bulk-operations/{id}` | Bulk operation details |
| POST | `/api/bulk-operations/{id}/process` | Start async processing |
| POST | `/api/bulk-operations/{id}/cancel` | Cancel bulk operation |
| POST | `/api/bulk-operations/{id}/retry` | Retry failed items |

### Webhook Processing

| Method | Path | Description |
|---|---|---|
| POST | `/api/webhook/events` | Ingest webhook event (HMAC validated when secret configured) |
| POST | `/api/webhook/events/retry-failed` | Retry failed webhook events |
| GET | `/api/webhook/events` | List webhook events |
| POST | `/api/webhook/rules` | Create webhook processing rule |
| GET | `/api/webhook/rules` | List active webhook rules |

### Reporting & Audit

| Method | Path | Description |
|---|---|---|
| GET | `/api/reporting/dashboard` | Date-range dashboard metrics |
| GET | `/api/reporting/user-stats` | Current user statistics |
| GET | `/api/reporting/audit-log` | Paginated audit log |

---

## Running Backend Tests

```bash
cd backend
mvn test
```

The smoke test (`DocumentManagementApplicationTests`) uses an in-memory H2 database and does not require a running PostgreSQL or DocuSign account.

---

## Current Status

### Core module (`backend` + `frontend`)

- Phase 1: Scaffolding and dev environment
- Phase 2: JPA domain model and Flyway migrations
- Phase 3: OIDC resource server + frontend login
- Phase 4: DocuSign OAuth per-user connect/disconnect + token refresh
- Phase 5: Document upload, envelope send, embedded signing, certificate download
- Phase 6: Webhook handler, event persistence, status sync
- Phase 7: Full frontend UX (documents, envelopes, compose, detail, timeline)
- Phase 8: Token refresh, correlation IDs, rate limiting, structured logging, smoke tests

### Advanced module (`backend-advanced`)

- Templates: CRUD + live sync from DocuSign account
- Webhooks: HMAC signature validation, event deduplication, retry failed events
- Webhook rules: event-type filtering with `LOG_ONLY` and `FORWARD_HTTP_POST`
- Reporting: dashboard aggregation, completion rates, average time-to-sign, user stats, audit logs
- Bulk operations: async processing engine with scheduled worker, per-item success/failure tracking

---

## License

This project is licensed under the ISC License. See [LICENSE](LICENSE).

