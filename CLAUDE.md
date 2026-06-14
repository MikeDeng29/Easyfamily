# Easyfamily

Monorepo for a phone-number query platform with three ends:

| Module | Stack | Purpose |
|---|---|---|
| `easyfamily-backend` | Spring Boot 3.4.2, Java 17, MySQL, Redis | REST API — auth, query, phone management, reporting |
| `easyfamily-admin` | Vue 3, Vite, Element Plus, ECharts | Web admin — reporting dashboard, runtime quota/strategy editor |
| `easyfamily-android` | Kotlin 2, Jetpack Compose, Material3 | Android client — login, query, phone management |

## Repository Layout

```
easyfamily-backend/   Spring Boot app
easyfamily-admin/     Vue3 SPA
easyfamily-android/   Kotlin Compose APK
docs/                 Architecture, API contract, feature list, go-live checklist
scripts/              regression_smoke.sh
```

---

## Backend

### Build & Test

```bash
cd easyfamily-backend
mvn -s settings.xml test                        # runs on H2 in-memory DB
mvn -s settings.xml -DskipTests package         # produces target/*.jar
```

JDK 17 required. `settings.xml` points Maven to Central direct (no internal mirror).

### Run Locally

```bash
java -jar target/easyfamily-backend-0.0.1-SNAPSHOT.jar \
  --spring.datasource.url=jdbc:mysql://127.0.0.1:3306/easyfamily \
  --spring.datasource.username=root \
  --spring.datasource.password=<pw> \
  --easyfamily.security.jwt-secret=$(openssl rand -base64 32) \
  --easyfamily.sms.provider=mock
```

Or populate `easyfamily-backend/.vault.local.properties` (never committed) and let `application.yml`'s `spring.config.import` pick it up.

### Key Config

All sensitive values come from environment variables or the vault file:

| Variable | Purpose |
|---|---|
| `DB_URL` / `DB_USER` / `DB_PASSWORD` | MySQL connection |
| `EASYFAMILY_JWT_SECRET` | JWT signing key (≥ 32 chars) |
| `EASYFAMILY_ADMIN_PASSWORD` | Admin login password |
| `SMS_PROVIDER` | `mock` (default) or `aliyun` |
| `ALIYUN_SMS_*` | SMS credentials (Aliyun only) |
| `ALIYUN_MARKET_APP_CODE` | Third-party binding query API key |
| `EASYFAMILY_VAULT_FILE` | Override vault properties file path |

### Architecture Overview

```
auth/       Login flow: CAPTCHA → SMS → JWT
query/      Phone binding query — quota enforcement, Redis cache, provider routing, circuit breaker
phone/      Bind / unbind / set primary phone
report/     Reporting endpoints (DAU, feature-hot, query-overview)
family/     Family member stub (extensible)
security/   JWT filter, token blacklist, AuthContext
common/     ApiResponse<T> envelope, GlobalExceptionHandler, AOP logging
```

Database migrations: Flyway — `src/main/resources/db/migration/`.

### Testing

`ApiFlowTest.java` is the end-to-end integration test. It covers:
- CAPTCHA → SMS → login
- Unauthorized rejection
- Authorized query
- Phone management
- Admin quota + report endpoints
- Multi-dimensional quota enforcement

Uses H2 (MySQL compatibility mode), mocked SMS, simulated query provider. Each test method gets a fresh context (`@DirtiesContext`).

---

## Admin (Web)

```bash
cd easyfamily-admin
npm ci
npm run dev      # dev server on http://localhost:5173
npm run build    # static output to dist/
```

Node/npm required. The app targets a backend at `http://localhost:8080` by default.

Views: `/login` (admin auth), `/report` (charts), `/quota` (runtime strategy editor).

---

## Android

```bash
cd easyfamily-android
./gradlew --no-daemon assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Requires JDK 17, Android SDK (minSdk 26, targetSdk 35). Set `sdk.dir` in `local.properties` if not in environment.

Debug builds point the API at `http://10.0.2.2:8080` (emulator loopback to host). Release builds use `https://api.easyfamily.com`.

---

## Regression

```bash
bash scripts/regression_smoke.sh                # R-001 to R-004
bash scripts/regression_smoke.sh --with-device  # + R-005 device smoke
```

| Case | What it checks |
|---|---|
| R-001 | Backend unit/integration tests |
| R-002 | Backend packaging (JAR) |
| R-003 | Admin build (dist/) |
| R-004 | Android assembleDebug (APK) |
| R-005 | Device install + adb monkey smoke |

All four core cases must pass before any release.

---

## Key Docs

- `docs/architecture.md` — system diagram and data flow
- `docs/api/API-contract.yaml` — full OpenAPI contract with request/response schemas
- `docs/testing/regression-cases.md` — regression case definitions
- `docs/operations-go-live-checklist.md` — pre-production gate checklist
- `docs/security-risk-control.md` — security controls
