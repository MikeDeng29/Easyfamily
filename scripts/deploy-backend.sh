#!/usr/bin/env bash
# Build easyfamily-backend on the production server from git and redeploy it.
#
# Usage:
#   bash scripts/deploy-backend.sh [branch]
#
# Configuration (env vars, defaults shown):
#   DEPLOY_HOST=47.102.126.67
#   DEPLOY_PORT=52521
#   DEPLOY_USER=root
#   DEPLOY_SRC_DIR=/opt/easyfamily/src/Easyfamily
#   DEPLOY_APP_DIR=/opt/easyfamily/app
#   DEPLOY_SERVICE=easyfamily-backend
#   DEPLOY_HEALTH_URL=http://127.0.0.1:8080/actuator/health
set -euo pipefail

BRANCH="${1:-main}"

DEPLOY_HOST="${DEPLOY_HOST:-47.102.126.67}"
DEPLOY_PORT="${DEPLOY_PORT:-52521}"
DEPLOY_USER="${DEPLOY_USER:-root}"
DEPLOY_SRC_DIR="${DEPLOY_SRC_DIR:-/opt/easyfamily/src/Easyfamily}"
DEPLOY_APP_DIR="${DEPLOY_APP_DIR:-/opt/easyfamily/app}"
DEPLOY_SERVICE="${DEPLOY_SERVICE:-easyfamily-backend}"
DEPLOY_HEALTH_URL="${DEPLOY_HEALTH_URL:-http://127.0.0.1:8080/actuator/health}"

SSH="ssh -p ${DEPLOY_PORT} ${DEPLOY_USER}@${DEPLOY_HOST}"

step() {
  echo
  echo "==> $1"
}

step "Pulling ${BRANCH} and building on ${DEPLOY_HOST}"
$SSH "cd '${DEPLOY_SRC_DIR}' && git fetch origin && git checkout '${BRANCH}' && git pull origin '${BRANCH}' && cd easyfamily-backend && mvn -s settings.xml -DskipTests clean package"

step "Backing up current jar"
TIMESTAMP=$(date +%Y%m%d%H%M%S)
$SSH "cp '${DEPLOY_APP_DIR}/easyfamily-backend.jar' '${DEPLOY_APP_DIR}/easyfamily-backend.jar.bak.${TIMESTAMP}'"

step "Deploying new jar and restarting ${DEPLOY_SERVICE}"
$SSH "cp '${DEPLOY_SRC_DIR}/easyfamily-backend/target/easyfamily-backend-0.0.1-SNAPSHOT.jar' '${DEPLOY_APP_DIR}/easyfamily-backend.jar' && systemctl restart ${DEPLOY_SERVICE}"

step "Waiting for service to come up"
sleep 12

if $SSH "systemctl is-active --quiet ${DEPLOY_SERVICE}" && $SSH "journalctl -u ${DEPLOY_SERVICE} -n 30 --no-pager | grep -q 'Started EasyfamilyBackendApplication'"; then
  echo "Service is active and started cleanly."
else
  step "Health check failed — rolling back to previous jar"
  $SSH "cp '${DEPLOY_APP_DIR}/easyfamily-backend.jar.bak.${TIMESTAMP}' '${DEPLOY_APP_DIR}/easyfamily-backend.jar' && systemctl restart ${DEPLOY_SERVICE}"
  echo "Rolled back. Inspect logs with: ${SSH} journalctl -u ${DEPLOY_SERVICE} -n 50 --no-pager"
  exit 1
fi

step "Deploy complete (backup: ${DEPLOY_APP_DIR}/easyfamily-backend.jar.bak.${TIMESTAMP})"
