# Easyfamily MVP Go-Live Checklist

This checklist is the release gate for MVP environments.

## 1) Logging and audit

- [ ] API access logs enabled for auth/query/phone/admin routes.
- [ ] Query audit log includes requester userId, target phone, query type, source, timestamp.
- [ ] Admin configuration changes are logged with operator identity and before/after values.
- [ ] Logs are retained according to compliance policy and are immutable in target environment.

## 2) Monitoring and alerting

- [ ] Alert on backend 5xx ratio spike (for example, 5 min > 2%).
- [ ] Alert on third-party provider timeout/error spike.
- [ ] Alert on Redis unavailability and cache degradation.
- [ ] Alert on authentication anomaly (SMS send burst, repeated login failures).

## 3) Data masking and privacy

- [ ] Phone number masking is applied in logs (`138****0000` style).
- [ ] Query response fields are minimal and avoid excessive personal data.
- [ ] Cached third-party response TTL is enforced and periodically verified.
- [ ] Privacy policy and user consent text are present in Android and web clients.

## 4) Security controls

- [ ] Captcha-before-SMS flow is verified end-to-end.
- [ ] JWT access flow validated, and protected APIs reject missing/invalid tokens.
- [ ] Quota controls are verified on user/phone/IP dimensions.
- [ ] Third-party credentials (AppCode, secrets) are managed outside source code.

## 5) Regression gate

- [ ] `bash scripts/regression_smoke.sh` passes.
- [ ] If device is available: `bash scripts/regression_smoke.sh --with-device` passes.
- [ ] Android install/start/monkey smoke completed without crash/ANR.
- [ ] OpenAPI and key docs are updated with latest runtime behavior.

## 6) Rollback and readiness

- [ ] Rollback version/tag identified before release.
- [ ] Runtime config rollback strategy documented (provider key, quota defaults, cache mode).
- [ ] On-call owner and escalation path confirmed.
