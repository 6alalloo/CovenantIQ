# CovenantIQ

Commercial Loan Risk Surveillance Platform.

## Stack
- Backend: Java 21, Spring Boot, Spring Web, Spring Data JPA, H2
- Frontend: React, TypeScript, Vite, Tailwind CSS, Recharts
- Docs: `docs/specs/BRD_CURRENT_STATE_2026-03-10.md`, `docs/specs/TDD_CURRENT_STATE_2026-03-10.md`, `docs/plans/backend_completion_plan.md`, `docs/guides/RUNBOOK.md`

## Quick Start

### Backend
```bash
mvn clean package
$env:SPRING_PROFILES_ACTIVE='demo'
mvn spring-boot:run
```

The backend requires a sample-content profile for local startup unless you provide non-placeholder runtime secrets. Use `demo` for seeded data or `test` for test-mode startup.

### Frontend
```bash
cd frontend
npm install
npm run dev
```

### Docker (single container)
```bash
docker compose build
docker compose up
```

The container runtime defaults to the `demo` Spring profile so it can start cleanly in Docker and Dockploy without production secrets. Override `SPRING_PROFILES_ACTIVE` if you want `test` instead.

### Dokploy
Create an `Application` in Dokploy and deploy this repo as a Dockerfile-based app with:
- Build Type: `Dockerfile`
- Dockerfile Path: `Dockerfile`
- Docker Context Path: `.`
- Container Port: `38080`

The image already defaults to the `demo` Spring profile, so no extra environment variables are required for demo deployments. If you want test mode instead, set `SPRING_PROFILES_ACTIVE=test` in Dokploy.

If Dokploy serves the frontend from a different origin than local development, set `APP_CORS_ALLOWED_ORIGIN_PATTERNS` to that domain, for example `https://your-app.example.com`.

### Local Reverse Proxy Harness
To simulate a Dokploy-style reverse proxy locally:

1. Add this line to your hosts file:
   `127.0.0.1 covenantiq.local`
2. Start the proxy harness:
   `docker compose -f docker-compose.proxy.yml up --build`
3. Open:
   `http://covenantiq.local:8081`

This runs Nginx in front of the Spring container and forwards `Host`, `Forwarded`, and `X-Forwarded-*` headers so you can inspect proxy-sensitive behavior like CORS and origin handling.
