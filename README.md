# LangGraph4j incident-response agent (Spring Boot 3 + Java 17 + SQL Server)

## Run
```
docker compose up -d --build  # SQL Server (host port 1434, MSSQL_PORT) + creates sag + builds and runs the app on http://localhost:9001 (APP_PORT)
docker compose up -d mssql mssql-init   # database only, to run the app/tests from Maven
mvn test                      # integration tests (need the container)
mvn spring-boot:run           # http://localhost:8080
```
Optional real LLM: set `OPENAI_API_KEY` (any OpenAI-compatible endpoint via `incident.llm.base-url`). Without it a rule-based stand-in is used.

## API
- `POST /incidents` `{"serviceName":"payment-service","message":"High CPU and connection pool exhaustion"}`
- `POST /incidents/{id}/approval` `{"approved":true}`
- `GET /incidents/{id}`, `GET /incidents/{id}/audit`

## Milestones (see lo-trinh.txt)
1. JPA + SQL Server (IDENTITY keys, `trustServerCertificate=true`), tables Alerts/AuditLogs/Runbooks/ServiceMetrics, retry loop (max 3).
2. Tools `queryActiveIncidents` / `fetchServiceMetrics` feed the LLM; it must return strict JSON `{action_type, target_service, risk_level}` (validated, 1 retry).
3. `SqlServerCheckpointSaver` persists graph state in `lg_checkpoints`; `interruptBefore(approval_gate)` for `risk_level == HIGH`; survives app restarts.
4. Parallel fan-out (`lookup_history`, `lookup_runbooks`, `scan_metrics`) -> fan-in at `decide`; non-clustered indexes on `(service_name, created_at)`.
   LangGraph4j has no `Send` API; parallel edges are its equivalent.
