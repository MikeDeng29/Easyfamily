# Easyfamily MVP Go-Live Checklist

This checklist is the release gate for MVP environments.

## 1) Logging and audit

- [x] API access logs enabled for auth/query/phone/admin routes. (`ApiSummaryLogAspect` wraps every `com.easyfamily..controller..*` method)
- [x] Query audit log includes requester userId, target phone, query type, source, timestamp. (`QueryRecordService` writes `report_event(user_id, login_phone, target_phone, query_type, source, ...)`)
- [ ] Admin configuration changes are logged with operator identity and before/after values. (quota/strategy PATCH is logged via `ApiSummaryLogAspect` req/resp, but no explicit before/after diff — follow-up)
- [ ] Logs are retained according to compliance policy and are immutable in target environment. (currently default journald retention on the ECS host — define retention window and ship to durable storage as a follow-up)

## 2) Monitoring and alerting

- [ ] Alert on backend 5xx ratio spike (for example, 5 min > 2%).
- [ ] Alert on third-party provider timeout/error spike.
- [ ] Alert on Redis unavailability and cache degradation.
- [ ] Alert on authentication anomaly (SMS send burst, repeated login failures).

(Not yet wired up — needs a monitoring stack, e.g. Aliyun CMS/SLS alarm rules on the ECS instance. Out of scope for the initial IP-only deploy.)

## 3) Data masking and privacy

- [x] Phone number masking is applied in logs (`138****0000` style). (`ApiSummaryLogAspect.maskPhone`, verified in test/prod logs)
- [x] Bill amounts/notes are masked in logs (`amount`/`note` → `***` in `ApiSummaryLogAspect`).
- [x] Bill and report_event tables use MySQL InnoDB tablespace encryption at rest (`ALTER TABLE ... ENCRYPTION='Y'`, `V5__bill_encryption.sql`, keyring_file plugin enabled on the production MySQL container).
- [x] Query response fields are minimal and avoid excessive personal data.
- [x] Cached third-party response TTL is enforced (`cache-ttl-seconds: 86400`).
- [ ] Privacy policy and user consent text are present in Android and web clients. (Android login screen shows consent text referencing《用户协议》《隐私政策》, but no linked policy document yet; admin web has none — follow-up)

## 4) Security controls

- [x] Captcha-before-SMS flow is verified end-to-end. (verified against production deploy)
- [x] JWT access flow validated, and protected APIs reject missing/invalid tokens. (verified: missing token → 401 on production deploy)
- [x] Quota controls are verified on user/phone/IP dimensions. (covered by `ApiFlowTest`, 16/16 passing)
- [x] Third-party credentials (AppCode, secrets) are managed outside source code. (`.vault.properties` on the ECS host, mode 600, not in git)

## 5) Regression gate

- [ ] `bash scripts/regression_smoke.sh` passes.
- [ ] If device is available: `bash scripts/regression_smoke.sh --with-device` passes.
- [ ] Android install/start/monkey smoke completed without crash/ANR.
- [ ] OpenAPI and key docs are updated with latest runtime behavior.

## 6) Rollback and readiness

- [x] Rollback version/tag identified before release. (current deploy built from working tree at commit `f341b63`; tag before next deploy)
- [x] Runtime config rollback strategy documented (provider key, quota defaults, cache mode). (`/api/v1/admin/query-settings` PATCH endpoint controls provider key/quota/cache mode at runtime without redeploy)
- [ ] On-call owner and escalation path confirmed.

## Production deployment record (2026-06-14)

- Host: Aliyun ECS `47.102.126.67` (SSH port 52521), Ubuntu 24.04.
- Self-hosted MySQL 8.0 (docker container `easyfamily-mysql`, port 127.0.0.1:3306, keyring_file encryption enabled) and Redis 7 (`easyfamily-redis`, port 127.0.0.1:6379), both bound to localhost only.
- Backend runs as systemd service `easyfamily-backend` (`/opt/easyfamily/app/easyfamily-backend.jar`, `--spring.profiles.active=prod`), auto-restart on failure, listening on `0.0.0.0:8080`.
- Secrets in `/opt/easyfamily/app/.vault.properties` (mode 600, not in git): fresh JWT secret, DB/Redis passwords, Aliyun SMS + DeepSeek keys.
- Aliyun SMS sign name "无锡圆滚滚科技" is approved and verified working — SMS login flow confirmed end-to-end on the production deploy.
