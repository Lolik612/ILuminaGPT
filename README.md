# LuminaGPT

Своя нейросеть: Android-приложение (чат со стримингом ответа) + сервер на llama.cpp, который запускает твой `.gguf`.

- `app/` — Android-клиент (Kotlin, Jetpack Compose, тёмная тема)
- `server/` — запуск модели, см. [server/README.md](server/README.md)
- `.github/workflows/build.yml` — GitHub Actions собирает APK при каждом пуше в `main`

## Где взять APK
Вкладка **Releases** репозитория (файл `LuminaGPT.apk`) или **Actions → Build APK → Artifacts**.
APK подписан одним постоянным ключом, поэтому новая версия ставится поверх старой без удаления.

## Как пользоваться
1. Подними сервер с моделью (`server/README.md`).
2. Поставь APK, открой ⚙ и впиши адрес сервера (`http://IP:8080`) и ключ, если задан.
3. Пиши сообщения. ■ останавливает генерацию, 🗑 очищает чат.
