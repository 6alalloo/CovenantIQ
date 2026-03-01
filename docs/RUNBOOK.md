# CovenantIQ Runbook

## Prerequisites
- Java 21+
- Maven 3.9+
- Node.js 20+ and npm
- Docker Desktop (for container run)

## Local Backend Run
1. `mvn clean package`
2. `mvn spring-boot:run`
3. Open:
   - API: `http://localhost:8080/api/v1/loans`
   - Swagger: `http://localhost:8080/swagger-ui.html`
   - H2: `http://localhost:8080/h2-console`

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
- Existing running servers on ports `8080` and `5173` are reused.

## Single-Container Docker Run
1. `docker compose build`
2. `docker compose up`
3. Open `http://localhost:8080`

## Smoke Test API Flow
1. Create loan:
```bash
curl -X POST http://localhost:8080/api/v1/loans \
  -H "Content-Type: application/json" \
  -d "{\"borrowerName\":\"Demo Corp\",\"principalAmount\":1000000,\"startDate\":\"2025-01-01\"}"
```
2. Add covenant:
```bash
curl -X POST http://localhost:8080/api/v1/loans/1/covenants \
  -H "Content-Type: application/json" \
  -d "{\"type\":\"CURRENT_RATIO\",\"thresholdValue\":1.2,\"comparisonType\":\"GREATER_THAN_EQUAL\",\"severityLevel\":\"HIGH\"}"
```
3. Submit statement:
```bash
curl -X POST http://localhost:8080/api/v1/loans/1/financial-statements \
  -H "Content-Type: application/json" \
  -d "{\"periodType\":\"QUARTERLY\",\"fiscalYear\":2025,\"fiscalQuarter\":1,\"currentAssets\":120,\"currentLiabilities\":100,\"totalDebt\":200,\"totalEquity\":100,\"ebit\":50,\"interestExpense\":10}"
```
4. Fetch risk summary:
```bash
curl http://localhost:8080/api/v1/loans/1/risk-summary
```
