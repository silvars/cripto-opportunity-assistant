#!/usr/bin/env bash
# Compila o backend (main + test), sem rodar os testes. Uso: scripts/build.sh
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/_env.sh"
cd "$SCRIPT_DIR/../backend"
echo "==> Compilando backend..."
mvn -q test-compile "$@"
echo "==> OK."
