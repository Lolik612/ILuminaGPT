#!/usr/bin/env bash
# Запуск сервера LuminaGPT: llama.cpp (llama-server) + твой .gguf файл.
# Использование:
#   MODEL=./models/model.gguf API_KEY=секрет PORT=8080 ./start.sh
set -euo pipefail

MODEL="${MODEL:-./models/model.gguf}"
PORT="${PORT:-8080}"
CTX="${CTX:-4096}"          # длина контекста; больше = больше RAM
THREADS="${THREADS:-$(nproc)}"
API_KEY="${API_KEY:-}"

if [ ! -f "$MODEL" ]; then
  echo "Файл модели не найден: $MODEL"
  echo "Положи .gguf в server/models/ или укажи путь: MODEL=/путь/к/model.gguf ./start.sh"
  exit 1
fi

ARGS=(-m "$MODEL" --host 0.0.0.0 --port "$PORT" -c "$CTX" -t "$THREADS")
if [ -n "$API_KEY" ]; then
  ARGS+=(--api-key "$API_KEY")
fi

if command -v llama-server >/dev/null 2>&1; then
  exec llama-server "${ARGS[@]}"
elif [ -x "./llama-server" ]; then
  exec ./llama-server "${ARGS[@]}"
else
  echo "llama-server не найден. Скачай готовый билд llama.cpp (см. server/README.md)"
  echo "или запусти через Docker: docker compose up -d"
  exit 1
fi
