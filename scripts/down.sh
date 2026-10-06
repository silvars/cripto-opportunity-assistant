#!/usr/bin/env bash
# Derruba a stack. Uso: scripts/down.sh [-v]  (-v também apaga o volume do Postgres)
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR/.."
echo "==> Derrubando docker compose..."
docker compose down "$@"
