#!/usr/bin/env bash
# Roda a suíte completa de testes (unit + integration/Testcontainers).
# Precisa do Docker rodando (os testes de integração sobem Postgres real).
# Uso: scripts/test.sh [args extras do maven, ex.: -Dtest=CandleValidatorTest]
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/_env.sh"
cd "$SCRIPT_DIR/../backend"
echo "==> Rodando testes (unit + integration/Testcontainers)..."
mvn test "$@"
echo "==> Resumo:"
grep -h "Tests run" target/surefire-reports/*.txt 2>/dev/null || true
