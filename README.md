# CovenantIQ

Commercial Loan Risk Surveillance Platform.

## Stack
- Backend: Java 21, Spring Boot, Spring Web, Spring Data JPA, H2
- Frontend: React, TypeScript, Vite, Tailwind CSS, Recharts
- Docs: `docs/BRD.md`, `docs/TDD.md`, `docs/IMPLEMENTATION_PLAN.md`, `docs/RUNBOOK.md`

## Quick Start

### Backend
```bash
mvn clean package
mvn spring-boot:run
```

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
