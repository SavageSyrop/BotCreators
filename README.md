# Telegram Export Participants Bot (MVP: JSON)

Бот принимает JSON экспорт истории чата (Telegram Desktop), извлекает уникальных участников (авторы сообщений + упоминания) и выдаёт результат:

- **<= 50** участников — текстовый список в чат
- **>= 51** участников — Excel **XLSX** с 3 вкладками: `participants / mentions / channels`

Данные не сохраняются: файлы скачиваются во временную директорию и удаляются после обработки.

## Локальный запуск

1) Создайте переменные окружения:

```bash
set BOT_TOKEN=...   # Windows PowerShell: $env:BOT_TOKEN="..."
set BOT_NAME=@your_bot
```

2) Запустите приложение:

```bash
mvn spring-boot:run
```

> Если вы запускаете через IDE — добавьте env `BOT_TOKEN` и `BOT_NAME` в конфигурацию запуска.

## Docker Compose

1) Создайте файл `.env` рядом с `docker-compose.yml` (можно скопировать из `.env.example`).

2) Запуск:

```bash
docker compose up --build
```

## Тесты

Запуск всех тестов:

```bash
mvn test
```

## Использование

1) Откройте чат в **Telegram Desktop** → **Export chat history** → выберите **JSON**.
2) Отправьте боту 1–10 JSON файлов.
3) Нажмите **Начать обработку**.
