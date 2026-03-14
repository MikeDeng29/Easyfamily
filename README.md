# easyfamily

Monorepo for the Easyfamily phone query platform.

## Modules

- `easyfamily-backend`: Spring Boot backend APIs.
- `easyfamily-admin`: Vue3 web admin console.
- `easyfamily-android`: Android client skeleton.
- `docs`: architecture, API, and security docs.

## MVP Scope

- Phone number login with captcha + SMS code.
- Query bank/social account binding by phone.
- Phone number management for current user.
- Admin quota configuration and basic reports.

## Regression

- Regression cases: `docs/testing/regression-cases.md`
- Quick smoke script: `scripts/regression_smoke.sh`
  - Core regression: `bash scripts/regression_smoke.sh`
  - Include device smoke (if adb device connected): `bash scripts/regression_smoke.sh --with-device`

## Go-Live Gate

- Security and risk baseline: `docs/security-risk-control.md`
- Release checklist: `docs/operations-go-live-checklist.md`
- Feature list tracker: `docs/feature-list.md`
- Development and release log: `docs/development-release-log.md`
