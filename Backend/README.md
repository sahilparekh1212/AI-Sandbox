# AI-Sandbox Backend

A Spring Boot 3.4 / Java 17 multi-module microservice backend, built with Gradle and
deployable to OpenShift. Each service is an independently buildable, runnable and
scalable Gradle subproject.

## Services

| Service        | Port | Module         | Purpose                                              |
|----------------|------|----------------|------------------------------------------------------|
| Auth           | 8085 | `Auth`         | Google OAuth2 login, issues/refreshes JWTs, JWKS     |
| Account        | 8081 | `Account`      | Account CRUD                                          |
| Transaction    | 8082 | `Transaction`  | Transaction CRUD                                      |
| Audit          | 8083 | `Audit`        | Audit-log create/read/delete (immutable)             |
| Notification   | 8084 | `Notification` | Notification CRUD                                     |
| Report         | 8086 | `Report`       | Spring Batch report generation + CRUD                |

All services share: JWT resource-server security, Swagger/OpenAPI docs, structured
logging, a global exception handler, an actuator health endpoint, and the rate
limiter described below.

---

## Prerequisites

- JDK 17 (`java -version` should report 17.x)
- No local Gradle needed — use the included wrapper (`./gradlew` / `gradlew.bat`)

---

## Build

```bash
# Build everything
./gradlew build

# Build a single service
./gradlew :Account:build

# Run the tests for one service
./gradlew :Account:test
```

---

## Run locally

Each service runs standalone. The default profile is **LOCAL**, which uses an
in-memory H2 database — no external database required to get started.

```bash
# Start the Auth service first (other services validate JWTs against its JWKS endpoint)
./gradlew :Auth:bootRun

# Then, in separate terminals:
./gradlew :Account:bootRun
./gradlew :Transaction:bootRun
./gradlew :Audit:bootRun
./gradlew :Notification:bootRun
./gradlew :Report:bootRun
```

To run a service under a non-default profile:

```bash
SPRING_PROFILES_ACTIVE=DEV ./gradlew :Account:bootRun
# Windows PowerShell:
$env:SPRING_PROFILES_ACTIVE="DEV"; ./gradlew :Account:bootRun
```

### Google OAuth2 (Auth service)

Create OAuth credentials in the Google Cloud Console and set the redirect URI to
`http://localhost:8085/login/oauth2/code/google`, then export:

```bash
export GOOGLE_CLIENT_ID=...
export GOOGLE_CLIENT_SECRET=...
```

Login flow: open `http://localhost:8085/oauth2/authorization/google`. On success the
Auth service returns an access token (30-min TTL) and a refresh token (7-day TTL).
Refresh via `POST /auth/refresh` with `{"refreshToken":"..."}`.

---

## Connecting to the database

### LOCAL profile (H2)

Each service exposes the H2 web console. With the service running, open:

| Service      | H2 console URL                       | JDBC URL                    |
|--------------|--------------------------------------|-----------------------------|
| Account      | http://localhost:8081/h2-console     | `jdbc:h2:mem:accountdb`     |
| Transaction  | http://localhost:8082/h2-console     | `jdbc:h2:mem:transactiondb` |
| Audit        | http://localhost:8083/h2-console     | `jdbc:h2:mem:auditdb`       |
| Notification | http://localhost:8084/h2-console     | `jdbc:h2:mem:notificationdb`|
| Report       | http://localhost:8086/h2-console     | `jdbc:h2:mem:reportdb`      |

Login with user `sa`, blank password, and the JDBC URL above. (In-memory data is
reset on each restart.)

### External database (DEV/SIT/UAT/PROD)

Higher profiles read the datasource from environment variables so you can point a
service at PostgreSQL, MySQL, etc. Example for PostgreSQL:

1. Add the driver to the service's `build.gradle`:
   ```groovy
   runtimeOnly 'org.postgresql:postgresql'
   ```
2. Provide the connection via env vars:
   ```bash
   export SPRING_PROFILES_ACTIVE=DEV
   export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/accountdb
   export SPRING_DATASOURCE_USERNAME=app
   export SPRING_DATASOURCE_PASSWORD=secret
   ./gradlew :Account:bootRun
   ```

---

## Profiles

| Profile | Datasource                         | `ddl-auto` | SQL logging | H2 console | App log level |
|---------|------------------------------------|------------|-------------|------------|---------------|
| LOCAL   | In-memory H2                       | update     | on          | enabled    | DEBUG         |
| DEV     | Env vars, H2 fallback              | update     | on          | off        | DEBUG         |
| SIT     | Env vars, H2 fallback              | update     | off         | off        | INFO          |
| UAT     | Env vars (required)                | validate   | off         | off        | INFO          |
| PROD    | Env vars (required)                | validate   | off         | off        | INFO (root WARN) |

Profile-specific config lives in each module's
`src/main/resources/application-<PROFILE>.properties`; common settings are in
`application.properties`. The active profile defaults to `LOCAL` and is overridden
with `SPRING_PROFILES_ACTIVE`.

---

## API documentation (Swagger)

With a service running, Swagger UI is at `http://localhost:<port>/swagger-ui.html`
and the raw spec at `http://localhost:<port>/v3/api-docs`. Use the **Authorize**
button to supply a `Bearer <token>` obtained from the Auth service.

---

## Rate limiting

Every service enforces **one active request per user per endpoint**, newest-wins:

- The key is `userId + HTTP method + path`.
- If a new request arrives for the same key while an older one is still in flight,
  the **older** request is **discarded**: its worker thread is interrupted and its
  `@Transactional` work is **rolled back**, so no partial writes survive.
- The discarded request's client receives **HTTP 429** with a **`Retry-After: 30`**
  header — i.e. it may retry after 30 seconds.

Implementation (per service, package `...ratelimit`):

| Component                      | Role                                                                 |
|--------------------------------|----------------------------------------------------------------------|
| `RateLimitInterceptor`         | Registers/deregisters each request; cancels superseded ones          |
| `ActiveRequestRegistry`        | Lock-free (`ConcurrentHashMap`) registry of the active request/key   |
| `ActiveRequest`                | Per-request handle with interrupt-based, race-safe cancellation      |
| `DiscardContext`               | `ThreadLocal` checkpoint used inside transactions                    |
| `TransactionalRequestExecutor` | Runs mutations in a tx that rolls back if the request was discarded  |
| `RequestDiscardedException`    | Mapped to `429 + Retry-After` by the global exception handler        |

Configurable via `ratelimit.enabled` and `ratelimit.retry-after-seconds`.

The design is built for high concurrency: no global locks (only per-key
`ConcurrentHashMap` bin locks), volatile flags for cancellation signalling, and
per-request `ThreadLocal` state that is cleared (along with any lingering thread
interrupt) when the request completes.

---

## Logging

Logback (`logback-spring.xml`) emits one structured line per event:

```
[<service>] <ISO-8601 timestamp> userId=<id> threadId=<thread> url=<last 2 path parts> - <message>
```

`userId` and `url` come from an MDC filter; `threadId` is the executing thread
(logback's native `%thread`, so it renders on every line). The global exception handler
logs the originating method, exception class, message, and the first 300 chars of
the stack trace.

---

## Deploying to OpenShift

Manifests live under `openshift/<service>/` (Deployment, Service, Route, ConfigMap,
HPA; plus a Secret for Auth). Each service scales independently via its
HorizontalPodAutoscaler.

```bash
# 1. Namespace
oc apply -f openshift/namespace.yaml

# 2. Auth secret — fill in real values first (Google creds + RSA private key)
oc apply -f openshift/auth/secret.yaml

# 3. Build & push each image (build context is the repo root)
docker build -f Account/Dockerfile -t <registry>/ai-sandbox/account-service:latest .
# ...repeat per service, then push

# 4. Apply each service's manifests
oc apply -f openshift/auth/
oc apply -f openshift/account/
oc apply -f openshift/transaction/
oc apply -f openshift/audit/
oc apply -f openshift/notification/
oc apply -f openshift/report/
```

Scale a service manually at any time:

```bash
oc scale deployment account-service --replicas=3 -n ai-sandbox
```

> **Auth + scaling:** set `AUTH_RSA_PRIVATE_KEY` (in `openshift/auth/secret.yaml`) so
> every Auth replica signs JWTs with the same key. Without it each pod generates an
> ephemeral key and tokens fail validation across replicas/restarts.
