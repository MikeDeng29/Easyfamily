# AGENTS.md

This file provides guidance to the AI agent when working with code in this repository.

## Build & Test Commands

### Backend (Spring Boot 3.4.2 / Java 17)
```bash
cd easyfamily-backend
mvn -s settings.xml test                          # runs against H2 in-memory (not MySQL)
mvn -s settings.xml -DskipTests package            # produces target/easyfamily-backend-0.0.1-SNAPSHOT.jar
```
The `-s settings.xml` flag is **required** — it bypasses internal mirrors and hits Maven Central directly.

### Admin (Vue 3 / Vite)
```bash
cd easyfamily-admin
npm ci && npm run build             # `npm ci`, not `npm install`
```

### Android (Kotlin / Compose / Hilt)
```bash
cd easyfamily-android
./gradlew --no-daemon assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```
`--no-daemon` is required; the daemon causes build hangs in this workspace.

## Key Architecture Notes

- **Multi-module monorepo**: `easyfamily-backend` (Spring Boot), `easyfamily-admin` (Vue 3 SPA), `easyfamily-android` (Kotlin Compose). Each builds independently.
- **Backend test DB**: Tests use H2 with MySQL compatibility mode. Do not expect a live MySQL instance for `mvn test`.
- **Android network**: Debug builds point at `http://10.0.2.2:8080` (emulator loopback to host). Release builds use `https://api.easyfamily.com`.
- **Secrets**: Sensitive values read from env vars or `easyfamily-backend/.vault.local.properties` (git-ignored). Never hardcode secrets.
- **Android DI**: Hilt (`@HiltAndroidApp`, `@HiltViewModel`). Don't use manual DI.
- **Regression**: Run `bash scripts/regression_smoke.sh` to verify all four modules before release.

## Android Bugfix Flow

When touching Android code, always: build → reinstall APK → restart app:
```bash
cd easyfamily-android && ./gradlew --no-daemon assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am force-stop com.easyfamily
adb shell am start -n "com.easyfamily/.MainActivity"
```
Report build, install, and launch results in the final message.
