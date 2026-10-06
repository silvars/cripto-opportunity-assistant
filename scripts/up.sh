#!/usr/bin/env bash
# Sobe toda a stack (build + up -d). Uso: scripts/up.sh [BACKFILL_PERIOD]
# Exemplo pra teste rápido sem esperar 12 meses reais: scripts/up.sh 2-days
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR/.."

if [ -n "${1:-}" ]; then
  export BACKFILL_PERIOD="$1"
  echo "==> Subindo docker compose com BACKFILL_PERIOD=$BACKFILL_PERIOD..."
else
  echo "==> Subindo docker compose (período de backfill default: 12-months)..."
fi

docker compose up --build -d
echo "==> Status:"
docker compose ps
