#!/usr/bin/env bash
# Acompanha os logs da API em tempo real. Uso: scripts/logs.sh
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR/.."
docker compose logs -f api
