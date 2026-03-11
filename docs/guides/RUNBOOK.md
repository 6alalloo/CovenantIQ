# CovenantIQ Runbook

## Prerequisites
- Java 21+
- Maven 3.9+
- Node.js 20+ and npm
- Docker Desktop (for container run)
- A sample-content Spring profile for local backend startup: `demo` or `test`

## Local Backend Run
1. `mvn clean package`
2. Set a profile before startup:
   - PowerShell: ``$env:SPRING_PROFILES_ACTIVE='demo'``
   - Optional test mode: ``$env:SPRING_PROFILES_ACTIVE='test'``
3. `mvn spring-boot:run`
4. Open:
   - App: `http://localhost:38080`
   - API: `http://localhost:38080/api/v1/loans`
   - Swagger: `http://localhost:38080/swagger-ui.html`
   - H2: `http://localhost:38080/h2-console`
5. Demo users seeded in `demo` and `test` modes include:
   - Analyst: `analyst@demo.com` / `Demo123!`
   - Risk lead: `risklead@demo.com` / `Demo123!`
   - Admin: `admin@demo.com` / `Demo123!`

## Local Frontend Run
1. `cd frontend`
2. `npm install`
3. `npm run dev`
4. Open `http://localhost:5173`

## Local E2E Run (Playwright)
1. From `frontend/`, install dependencies once:
   - `npm install`
   - `npm run e2e:install`
2. Run tests:
   - `npm run e2e`
3. Optional interactive mode:
   - `npm run e2e:ui`
4. Run a single domain suite:
   - `npx playwright test tests/e2e/auth.spec.ts`
   - `npx playwright test tests/e2e/loans.spec.ts`

Notes:
- E2E config starts backend (`mvn spring-boot:run`) and frontend (`npm run dev`) automatically.
- Existing running servers on ports `38080` and `5173` are reused.

## Single-Container Docker Run
1. `docker compose build`
2. `docker compose up`
3. Open `http://localhost:38080`

Notes:
- The container defaults to the `demo` Spring profile.
- Override `SPRING_PROFILES_ACTIVE` only if you explicitly want `test`.

## Dokploy Deploy
1. Create an `Application` in Dokploy.
2. Connect this repository and select your deployment branch.
3. Set:
   - Build Type: `Dockerfile`
   - Dockerfile Path: `Dockerfile`
   - Docker Context Path: `.`
   - Container Port: `38080`
4. Add a domain and point it to container port `38080`.
5. Deploy.

Notes:
- No extra environment variables are required for demo-mode Dokploy deployment.
- To run in test mode instead, set `SPRING_PROFILES_ACTIVE=test` in Dokploy.
- This deployment uses in-memory H2, so data resets on restart.

## Smoke Test API Flow
1. Log in and capture a bearer token:
```bash
curl -X POST http://localhost:38080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"analyst@demo.com\",\"password\":\"Demo123!\"}"
```
2. Copy the returned `accessToken` and export it:
```bash
$env:TOKEN="paste-access-token-here"
```
3. Create loan:
```bash
curl -X POST http://localhost:38080/api/v1/loans \
  -H "Authorization: Bearer $env:TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"borrowerName\":\"Demo Corp\",\"principalAmount\":1000000,\"startDate\":\"2025-01-01\"}"
```
4. Add covenant:
```bash
curl -X POST http://localhost:38080/api/v1/loans/1/covenants \
  -H "Authorization: Bearer $env:TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"type\":\"CURRENT_RATIO\",\"thresholdValue\":1.2,\"comparisonType\":\"GREATER_THAN_EQUAL\",\"severityLevel\":\"HIGH\"}"
```
5. Submit statement:
```bash
curl -X POST http://localhost:38080/api/v1/loans/1/financial-statements \
  -H "Authorization: Bearer $env:TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"periodType\":\"QUARTERLY\",\"fiscalYear\":2025,\"fiscalQuarter\":1,\"currentAssets\":120,\"currentLiabilities\":100,\"totalDebt\":200,\"totalEquity\":100,\"ebit\":50,\"interestExpense\":10}"
```
6. Fetch risk summary:
```bash
curl http://localhost:38080/api/v1/loans/1/risk-summary \
  -H "Authorization: Bearer $env:TOKEN"
```
