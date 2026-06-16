# Backend

Spring Boot backend for DocuSign document workflows.

## Run
1. Ensure PostgreSQL is available (for local, use root `docker-compose.yml`).
2. Set environment variables from `.env.example`.
3. Start the app:

```bash
mvn spring-boot:run
```

## Endpoints
- Health: `/actuator/health`
- System status: `/api/system/status`
- OpenAPI: `/api-docs`
- Swagger UI: `/swagger-ui`
