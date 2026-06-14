# Easyfamily Regression Cases

This document defines the baseline regression checks for the MVP scope.

## Scope

- Backend auth + query + phone management + report endpoints
- Admin web build and static quality gate
- Android app build and optional device smoke check

## Environment Baseline

- JDK 17+
- Maven available in shell
- Node.js 18+ and npm
- Android SDK (optional for Android build), adb (optional for device smoke)

## Case Matrix

### R-001 Backend Unit/Integration Regression

- **Goal**: Ensure backend flow is not broken by code changes.
- **Command**: `cd easyfamily-backend && mvn -s settings.xml -q test`
- **Expected**:
  - Exit code is `0`
  - `ApiFlowTest` and related tests pass

### R-002 Backend Packaging Regression

- **Goal**: Ensure backend can still produce deployable artifact.
- **Command**: `cd easyfamily-backend && mvn -s settings.xml -q -DskipTests package`
- **Expected**:
  - Exit code is `0`
  - Jar is generated under `target/`

### R-003 Admin Build Regression

- **Goal**: Ensure admin console is buildable.
- **Command**: `cd easyfamily-admin && npm ci && npm run build`
- **Expected**:
  - Exit code is `0`
  - Build output exists in `dist/`

### R-004 Android Debug Build Regression

- **Goal**: Ensure Android app can be assembled in debug mode.
- **Command**: `cd easyfamily-android && ./gradlew --no-daemon assembleDebug`
- **Expected**:
  - Exit code is `0`
  - APK generated at `app/build/outputs/apk/debug/`

### R-005 Android Runtime Smoke (Optional)

- **Goal**: Basic stability check on connected device/emulator.
- **Precondition**: `adb devices` has at least one `device`.
- **Command sequence**:
  - `adb install -r easyfamily-android/app/build/outputs/apk/debug/app-debug.apk`
  - `adb shell am start -n "com.easyfamily/.MainActivity"`
  - `adb shell monkey -p com.easyfamily --pct-syskeys 0 --ignore-crashes --ignore-timeouts --monitor-native-crashes -v 50`
- **Expected**:
  - Install succeeds
  - App launches successfully
  - No crash/ANR traces in logcat during smoke window

### R-006 SMS Login with Auto-Provisioned Account

- **Goal**: A phone number with no pre-existing account can log in directly — the account is created implicitly on first login (no separate registration step).
- **Precondition**: Backend reachable at `$BASE` (e.g. `http://47.102.126.67:8080`); `easyfamily.sms.mock-code` configured (default `123456`).
- **Steps**:
  1. `POST $BASE/api/v1/auth/captcha/verify` with `{"captchaProvider":"mock","ticket":"e2e"}` → capture `data.captchaToken`
  2. `POST $BASE/api/v1/auth/sms/send` with `{"phone":"13800000000","captchaToken":"<token>"}` (fixed test phone — Aliyun rejects arbitrary non-real numbers, so a brand-new number can only be exercised locally with `easyfamily.sms.provider=mock`)
  3. `POST $BASE/api/v1/auth/login` with `{"phone":"13800000000","smsCode":"123456"}`
- **Expected**:
  - All three calls return `"code":"OK"`
  - Login response `data.userId == "U13800000000"` and includes non-empty `accessToken`/`refreshToken`
  - `GET $BASE/api/v1/phones/mine` with the new `accessToken` returns `"code":"OK"` with the phone listed (account usable immediately, no separate signup needed)

### R-007 Bill Feature Basic Usability

- **Goal**: A logged-in user can create a bill, see it in the list, and have it reflected in stats.
- **Precondition**: Valid `accessToken` from R-006 (or any test account).
- **Steps**:
  1. `POST $BASE/api/v1/bill` with `{"category":"餐饮","amount":20.00,"note":"e2e smoke","billedAt":"<today>"}`, capture `data.id`
  2. `GET $BASE/api/v1/bill` → confirm the created bill appears
  3. `GET $BASE/api/v1/bill/stats?month=<yyyy-MM>` → confirm `总支出`/`byCategory.餐饮` includes the 20.00
  4. `DELETE $BASE/api/v1/bill/{id}` → cleanup
- **Expected**:
  - Steps 1-3 return `"code":"OK"` with the bill visible in both the list and stats
  - Step 4 returns `"code":"OK"` and the bill no longer appears in step-2's list on re-check

## Recommended Execution Order

1. Run `scripts/regression_smoke.sh`
2. If Android device is connected, run `scripts/regression_smoke.sh --with-device`
3. Record result and timestamp in release notes / PR description

## Maintenance Rules

- Add one or more regression cases whenever a production bug is fixed.
- Keep each case linked to either:
  - A stable command, or
  - A reproducible manual step with clear expected behavior.
- Keep case IDs stable (`R-xxx`) so historical references in PRs remain valid.
