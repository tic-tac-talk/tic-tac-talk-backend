#!/usr/bin/env bash
# docker-compose의 connect-init 원샷 컨테이너에서 실행됨.
# Kafka Connect가 뜨길 기다렸다가, 이 디렉터리의 *-connector.json을 전부 REST API로 등록한다.
set -euo pipefail

CONNECT_URL="${CONNECT_URL:-http://connect:8083}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "[register-connectors] Kafka Connect(${CONNECT_URL}) 기동 대기 중..."
until curl -s -f -o /dev/null "${CONNECT_URL}/connectors"; do
  echo "[register-connectors]   아직 준비 안 됨, 2초 후 재시도"
  sleep 2
done
echo "[register-connectors] Kafka Connect 준비 완료"

register() {
  local file="$1"
  local name
  name=$(grep -o '"name"[[:space:]]*:[[:space:]]*"[^"]*"' "$file" | head -1 | sed -E 's/.*:[[:space:]]*"([^"]+)"/\1/')

  echo "[register-connectors] ${name} 등록 중 (${file})"
  local status
  status=$(curl -s -o /tmp/register-response.json -w '%{http_code}' \
    -X POST "${CONNECT_URL}/connectors" \
    -H "Content-Type: application/json" \
    -d @"${file}")

  if [[ "${status}" == "201" ]]; then
    echo "[register-connectors] ${name} 등록 성공 (201)"
  elif [[ "${status}" == "409" ]]; then
    echo "[register-connectors] ${name} 이미 등록되어 있음 (409, 스킵)"
  else
  echo "[register-connectors] ${name} 등록 실패 (HTTP ${status})"
  cat /tmp/register-response.json
  exit 1
  fi
}

for connector_file in "${SCRIPT_DIR}"/*-connector.json; do
  register "${connector_file}"
done

echo "[register-connectors] 전체 커넥터 상태:"
curl -s "${CONNECT_URL}/connectors"
echo
