# Navisow DocuSign Document Management

Full-stack document management and signing workflow application backed by the DocuSign eSignature API.

| Layer | Stack |
|---|---|
| Backend API | Java 25 · Spring Boot 3.5 · Spring Security (JWT resource server) · Spring Data JPA · Flyway |
| Frontend | Node.js 24 · React 19 · Vite · TypeScript · react-oidc-context |
| Auth | OIDC (Keycloak dev container) + DocuSign OAuth Authorization Code per user |
| Database | PostgreSQL 16 |
| Local services | Docker Compose (PostgreSQL + Keycloak) |

---

## Project Layout

```
navisow-docusign/
├── backend/          Spring Boot API & workflow services
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
   - URL: `http://<your-tunnel>/api/webhook/docusign`
   - Trigger events: `envelope-sent`, `envelope-delivered`, `envelope-completed`, `envelope-declined`, `envelope-voided`
   - Use a tunnel (e.g. `ngrok http 8080`) to expose localhost for webhook delivery

### 6. Run the backend

```bash
cd backend
mvn spring-boot:run
```

API available at http://localhost:8080
- Health: http://localhost:8080/actuator/health
- OpenAPI: http://localhost:8080/api-docs
- Swagger UI: http://localhost:8080/swagger-ui

### 7. Run the frontend

```bash
cd frontend
npm install
npm run dev
```

App available at http://localhost:5173

---

## API Endpoints

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

---

## Running Backend Tests

```bash
cd backend
mvn test
```

The smoke test (`DocumentManagementApplicationTests`) uses an in-memory H2 database and does not require a running PostgreSQL or DocuSign account.

---

## Current Status

All 8 implementation phases complete:
- Phase 1: Scaffolding and dev environment
- Phase 2: JPA domain model and Flyway migrations
- Phase 3: OIDC resource server + frontend login
- Phase 4: DocuSign OAuth per-user connect/disconnect + token refresh
- Phase 5: Document upload, envelope send, embedded signing, certificate download
- Phase 6: Webhook handler, event persistence, status sync
- Phase 7: Full frontend UX (documents, envelopes, compose, detail, timeline)
- Phase 8: Token refresh, correlation IDs, rate limiting, structured logging, smoke tests

---

## License

This project is licensed under the ISC License. See [LICENSE](LICENSE).

