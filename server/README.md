# Сервер LuminaGPT

Сервер — это обычный `llama-server` из [llama.cpp](https://github.com/ggml-org/llama.cpp) с твоим `.gguf`.
Он отдаёт OpenAI-совместимый API (`/v1/chat/completions` со стримингом), с которым и работает приложение.

## 1. Какую модель качать

Нужен файл **`.gguf`** в квантовании **Q4_K_M** (лучший баланс размера и качества).
Выбирай по оперативной памяти сервера (файл + ~1–2 ГБ запаса):

| RAM сервера | Модель (Instruct) | Файл ≈ |
|---|---|---|
| 2–4 ГБ | Qwen2.5-1.5B-Instruct | 1 ГБ |
| 4–8 ГБ | Qwen2.5-3B-Instruct | 2 ГБ |
| 8–16 ГБ | **Qwen2.5-7B-Instruct** (рекомендую) | 4.7 ГБ |
| 16+ ГБ | Qwen2.5-14B-Instruct | 9 ГБ |

Качай с Hugging Face, репозитории вида `bartowski/Qwen2.5-7B-Instruct-GGUF`,
файл `Qwen2.5-7B-Instruct-Q4_K_M.gguf`. Qwen хорошо знает русский.

## 2. Запуск

### Вариант А — Docker
```bash
mkdir -p models
# положи файл сюда и назови model.gguf:  models/model.gguf
echo "API_KEY=придумай_секрет" > .env     # необязательно
docker compose up -d
```

### Вариант Б — без Docker
Скачай готовый архив `llama-bXXXX-bin-ubuntu-x64.zip` со страницы
[релизов llama.cpp](https://github.com/ggml-org/llama.cpp/releases), распакуй так, чтобы `llama-server` лежал рядом с `start.sh`, потом:
```bash
chmod +x start.sh
MODEL=./models/model.gguf API_KEY=секрет PORT=8080 ./start.sh
```

Проверка: `curl http://IP:8080/health` должен ответить `{"status":"ok"}`.

## 3. В приложении

⚙ → «Адрес сервера» `http://IP:8080` (и API-ключ, если задавал).

> Для доступа из интернета лучше поставить перед сервером HTTPS (Caddy/nginx) и обязательно задать `API_KEY`.
