#!/usr/bin/env bash
# Checagem rápida de status após subir a stack. Uso: scripts/status.sh [SYMBOL]
set -euo pipefail
SYMBOL="${1:-BTCUSDT}"
curl -s "http://localhost:8080/api/v1/market/${SYMBOL}/status" | python3 -m json.tool
