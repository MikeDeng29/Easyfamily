#!/usr/bin/env bash
set -euo pipefail

WITH_DEVICE=0
for arg in "$@"; do
  case "$arg" in
    --with-device) WITH_DEVICE=1 ;;
    *)
      echo "Unknown argument: $arg"
      echo "Usage: $0 [--with-device]"
      exit 1
      ;;
  esac
done

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

step() {
  echo
  echo "==> $1"
}

run_backend_maven() {
  if [ -x "$ROOT_DIR/easyfamily-backend/mvnw" ]; then
    (cd "$ROOT_DIR/easyfamily-backend" && ./mvnw "$@")
  else
    (cd "$ROOT_DIR/easyfamily-backend" && mvn -s settings.xml "$@")
  fi
}

step "Backend tests"
run_backend_maven -q test

step "Backend package"
run_backend_maven -q -DskipTests package

step "Admin build"
(
  cd "$ROOT_DIR/easyfamily-admin"
  npm ci
  npm run build
)

step "Android assembleDebug (if SDK available)"
if [ -f "$ROOT_DIR/easyfamily-android/local.properties" ] || [ -n "${ANDROID_HOME:-}" ] || [ -n "${ANDROID_SDK_ROOT:-}" ]; then
  (
    cd "$ROOT_DIR/easyfamily-android"
    ./gradlew --no-daemon assembleDebug
  )
else
  echo "Skip Android build: no SDK configuration found (local.properties/ANDROID_HOME/ANDROID_SDK_ROOT)."
fi

if [ "$WITH_DEVICE" -eq 1 ]; then
  step "Android device smoke"
  APK="$ROOT_DIR/easyfamily-android/app/build/outputs/apk/debug/app-debug.apk"
  if [ ! -f "$APK" ]; then
    echo "Missing APK: $APK"
    echo "Run Android assembleDebug first."
    exit 1
  fi

  if ! command -v adb >/dev/null 2>&1; then
    echo "adb not found in PATH."
    exit 1
  fi

  DEVICE_COUNT="$(adb devices | awk 'NR>1 && $2=="device" {count++} END {print count+0}')"
  if [ "$DEVICE_COUNT" -lt 1 ]; then
    echo "No connected device/emulator. Use adb devices to verify."
    exit 1
  fi

  adb install -r "$APK"
  adb shell am start -n "com.easyfamily/.MainActivity"
  adb shell monkey -p com.easyfamily --pct-syskeys 0 --ignore-crashes --ignore-timeouts --monitor-native-crashes -v 50
fi

echo
echo "Regression smoke finished."
