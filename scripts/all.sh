#!/usr/bin/env bash
# Pipeline completo: compila, roda a suíte de testes, sobe a stack e aguarda ficar healthy.
# Uso: scripts/all.sh [BACKFILL_PERIOD]
# Exemplo pra validar rápido sem esperar 12 meses reais: scripts/all.sh 2-days
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "=== 1/4 Compilando ==="
"$SCRIPT_DIR/build.sh"

echo "=== 2/4 Rodando testes (unit + integration/Testcontainers) ==="
"$SCRIPT_DIR/test.sh"

echo "=== 3/4 Subindo a stack (docker compose up --build) ==="
"$SCRIPT_DIR/up.sh" "${1:-}"

echo "=== 4/4 Aguardando a API ficar healthy ==="
for i in $(seq 1 30); do
  if curl -sf http://localhost:8080/actuator/health > /dev/null 2>&1; then
    echo "API healthy."
    break
  fi
  if [ "$i" -eq 30 ]; then
    echo "Timeout esperando a API ficar healthy. Veja: scripts/logs.sh"
    exit 1
  fi
  sleep 2
done

echo "=== Status do mercado (BTCUSDT) ==="
"$SCRIPT_DIR/status.sh" BTCUSDT || true

echo
echo "Pronto. Logs: scripts/logs.sh | Derrubar: scripts/down.sh"
