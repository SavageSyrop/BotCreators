# Архитектура проекта

## 1. Архитектурное решение

### 1.1 Разделение на слои
Архитектура построена как лёгкая многослойная схема:

1) **Telegram-интеграция / orchestration (Bot.java)**  
   Отвечает за:
   - обработку апдейтов (команды, документы, callback-кнопки),
   - скачивание файлов через Bot API,
   - управление жизненным циклом сессии,
   - запуск обработки (в отдельном потоке),
   - отправку результата пользователю.

2) **Сессии и состояние (session/**)  
   Нужны из-за сценария “копим файлы → жмём кнопку”.  
   Сессии хранятся **только в памяти**, без БД, потому что:
   - ТЗ не требует устойчивого хранения,
   - важнее приватность и простота,
   - бот учебный/хакатонный (минимум инфраструктуры).

3) **Core-логика (core/**)  
   - `TelegramJsonExportParser` — парсит экспорт Telegram Desktop (JSON) максимально устойчиво (через `JsonNode`, так как структура экспорта может отличаться между версиями клиента).
   - `Deduplicator` — удаление дублей и фильтрация удалённых аккаунтов.
   - `ChatExportService` — оркестратор обработки файлов (агрегация результатов).

4) **Вывод результата (export/**)  
   - `TextResultFormatter` — формирует текст строго по требованиям (раздельно “Участники” и “Упоминания”, чтобы не было перемешивания).
   - `ExcelExporter` — создаёт `.xlsx` (3 вкладки) с требуемыми колонками и “заморозкой” шапки.

5) **UI-слой (ui/**)  
   Изолирует тексты и клавиатуры, чтобы не раздувать `Bot.java` и упростить поддержку UX.

Такое разделение напрямую покрывает требование ТЗ о “чётком процессе, стабильности, отсутствии хранения данных и разных форматах результата”.

### 1.2 Почему Spring Boot (а не “plain Java”)
В проекте используется **Spring Boot** не ради REST/портов, а как **инфраструктурный контейнер**:
- Dependency Injection (удобно собирать бота, стор сессий, конфиг);
- конфиг через `application.properties` и env-переменные;
- логирование “из коробки” (через SLF4J/Logback);
- простая сборка в fat-jar для Docker.

При этом сам бот работает в режиме **long polling**, т.е. входящий HTTP-порт не обязателен.

### 1.3 Почему именно эти библиотеки
- **telegrambots** — стандартная библиотека для LongPollingBot, позволяет скачивать файлы и отправлять документы/сообщения.
- **Jackson** — быстрый и гибкий JSON-парсер; `JsonNode` даёт устойчивость к вариациям структуры экспорта.
- **Apache POI** — де-факто стандарт для XLSX в Java.
- **JUnit 5** — модульные тесты (парсер/дедуп/экспорт).
- **Lombok** — сокращение шаблонного кода (логгер, конструкторы).

---

## 2. Структура проекта (по пакетам)

- `com.example.BotApplication`  
  Точка входа: запускает Spring Boot и регистрирует Telegram-бота в `TelegramBotsApi`.

- `com.example.Bot`  
  Главный обработчик Telegram-апдейтов и orchestrator сценариев.

- `com.example.session/`  
  Сессии пользователя (на chatId), TTL, список загруженных файлов, состояние.

- `com.example.core/`  
  Основная обработка: парсинг экспорта, дедупликация, агрегация результата.

- `com.example.export/`  
  Генерация результата (текст/Excel).

- `com.example.ui/`  
  Кнопки (InlineKeyboard) и тексты меню/подсказок.

- `com.example.enums/`  
  Константы (лимиты, callback-данные), состояния.

---

## 3. Состояния сессии и как реализовано в коде

Оперирует состояниями: `IDLE`, `WAITING_FILES`, `READY_TO_PROCESS`, `PROCESSING`, `EXPIRED`.

В коде:
- **IDLE** — это **отсутствие сессии** в `SessionStore` (нет записи по chatId).
- **WAITING_FILES** — сессия создана и ожидает загрузку файлов (в коде это дефолт).
- **READY_TO_PROCESS** — есть хотя бы один файл; показываем кнопку “Начать обработку”.
- **PROCESSING** — идёт обработка; новые файлы не добавляем в текущую обработку.
- **EXPIRED** — не хранится как enum, это **логическое состояние**: `SessionStore.isExpired(session) == true`.  
  При следующем действии пользователя бот:
  - сообщает “Сессия истекла…”
  - удаляет сессию
  - предлагает “Начать заново”.

---

## 4. Общий процесс (flowchart)



```mermaid
flowchart TD
    A["Пользователь /start"] --> B["Bot: reset session, WAITING_FILES"]
    B --> C["Пользователь отправляет JSON файлы"]
    C --> D{"Файл валиден? .json"}
    D -- "нет" --> E["Сообщение об ошибке формата"]
    D -- "да" --> F["Сохраняем FileMeta в Session.files"]
    F --> G{"files.size >= 1 ?"}
    G -- "да" --> H["State = READY_TO_PROCESS<br/>Показываем кнопку «Начать обработку»"]
    H --> I["Пользователь нажимает «Начать обработку»"]
    I --> J["State = PROCESSING<br/>Сообщение: «Начинаю обработку N файлов...»"]
    J --> K["Цикл по файлам 1..N"]
    K --> L["Скачать 1 файл через Bot API"]
    L --> M["Парсинг + дедуп + сбор ResultBundle"]
    M --> N{"participants <= 50?"}
    N -- "да" --> O["Отправить текстовый результат + «Файл i/N обработан»"]
    N -- "нет" --> P["Сформировать XLSX и отправить документ + «Файл i/N обработан»"]
    O --> Q{"Последний файл?"}
    P --> Q
    Q -- "нет" --> K
    Q -- "да" --> R["Сбросить сессию (IDLE)"]
```

---

## 5. Архитектура компонентов

```mermaid
flowchart LR
    U[User / Telegram Client] --> T[Telegram Bot API]
    T -->|"Updates (long polling)"| B[Bot.java]
    B --> S[SessionStore]
    B -->|downloadFile| T
    B --> C[ChatExportService]
    C --> P[TelegramJsonExportParser]
    P --> D[Deduplicator]
    P --> RB[ResultBundle]
    RB -->|<=50| TXT[TextResultFormatter]
    RB -->|>=51| XLSX[ExcelExporter]
    TXT --> B
    XLSX --> B
    B -->|"sendMessage / sendDocument"| T
    T --> U
```

---

## 6. Диаграмма последовательности (основной сценарий)

```mermaid
sequenceDiagram
    actor User
    participant TG as Telegram Bot API
    participant Bot as Bot.java
    participant Store as SessionStore
    participant Parser as TelegramJsonExportParser
    participant Core as ChatExportService
    participant ExpTxt as TextResultFormatter
    participant ExpXls as ExcelExporter

    User->>TG: /start
    TG-->>Bot: Update(/start)
    Bot->>Store: reset(chatId), getOrCreate()
    Bot-->>TG: sendMessage(start menu)

    loop Upload 1..N files
        User->>TG: sendDocument(result.json)
        TG-->>Bot: Update(document)
        Bot->>Store: touchOrExpire()
        Bot->>Store: session.files.add(FileMeta)
        Bot-->>TG: sendMessage("Файл принят... READY_TO_PROCESS")
    end

    User->>TG: нажимает "Начать обработку"
    TG-->>Bot: Callback START_PROCESS
    Bot->>Store: touchOrExpire(), set PROCESSING
    Bot-->>TG: sendMessage("Начинаю обработку N файлов...")

    loop for each file i/N
        Bot->>TG: getFile(fileId) + downloadFile
        Bot->>Core: processJsonFiles([oneFile])
        Core->>Parser: parse(path)
        Parser-->>Core: ResultBundle
        alt participants <= 50
            Core->>ExpTxt: format(fileName, result)
            ExpTxt-->>Bot: текст
            Bot-->>TG: sendMessage("Файл i/N обработан...\n" + text)
        else participants >= 51
            Core->>ExpXls: export(result)
            ExpXls-->>Bot: participants_i.xlsx
            Bot-->>TG: sendDocument("Файл i/N обработан...", xlsx)
        end
    end

    Bot->>Store: reset(chatId)
```

---

## 7. Доменная модель (ключевые DTO и структуры данных)

Проект не использует БД и сущности JPA. Доменная модель — это простые структуры данных, которые удобно:

* агрегировать,
* дедуплицировать,
* экспортировать в текст/Excel.

Ключевые доменные классы:

* `UserEntry` — универсальная запись пользователя/упоминания/канала:

  * `username` (может быть null),
  * `displayName` (для авторов сообщений),
  * `link` (t.me/...).
* `ResultBundle` — итог по одному файлу или агрегированный результат:

  * `participants: Set<UserEntry>`
  * `mentions: Set<UserEntry>`
  * `channels: Set<UserEntry>`
* `Session` — состояние и список файлов пользователя в текущем “сеансе”:

  * `state`,
  * `lastActivityAt`,
  * `files: List<FileMeta>`.

---

## 8. Диаграмма классов (упрощённая)

```mermaid
classDiagram
    class BotApplication {
      +main(args)
    }

    class Bot {
      -botUsername: String
      -sessionStore: SessionStore
      -executor: ExecutorService
      +onUpdateReceived(update)
      -handleStart(chatId)
      -handleStop(chatId)
      -handleDocument(message)
      -startProcessing(chatId)
      -processFilesAsync(chatId, files)
    }

    class SessionStore {
      -sessions: Map~Long, Session~
      +getOrCreate(chatId) Session
      +reset(chatId)
      +isExpired(session) boolean
      +setState(chatId, state)
    }

    class Session {
      -state: UserState
      -lastActivityAt: Instant
      -files: List~FileMeta~
      +touch()
      +getFiles() List~FileMeta~
    }

    class FileMeta {
      +fileId: String
      +fileName: String
      +fileSize: long
    }

    class ChatExportService {
      +processJsonFiles(paths) ResultBundle
    }

    class TelegramJsonExportParser {
      +parse(path) ResultBundle
    }

    class Deduplicator {
      +dedup(set) Set~UserEntry~
      +isDeletedAccount(u) boolean
    }

    class ResultBundle {
      +participants() Set~UserEntry~
      +mentions() Set~UserEntry~
      +channels() Set~UserEntry~
    }

    class UserEntry {
      +username() String
      +displayName() String
      +link() String
    }

    class ExcelExporter {
      +export(result, dir) Path
    }

    class TextResultFormatter {
      +format(fileNames, result) String
    }

    class Keyboards {
      +startMenu() InlineKeyboardMarkup
      +readyMenu() InlineKeyboardMarkup
      +processOtherMenu() InlineKeyboardMarkup
      +restartMenu() InlineKeyboardMarkup
    }

    class UiTexts {
      +startText() String
      +exportHelp() String
      +faq() String
    }

    class Constants {
      +MAX_FILES: int
      +SESSION_TTL_MINUTES: int
      +TEXT_THRESHOLD_MAX: int
    }

    class UserState {
      <<enum>>
      WAITING_FILES
      READY_TO_PROCESS
      PROCESSING
    }

    BotApplication --> Bot : registers
    Bot --> SessionStore
    SessionStore --> Session
    Session --> FileMeta
    Bot --> ChatExportService
    ChatExportService --> TelegramJsonExportParser
    TelegramJsonExportParser --> Deduplicator
    TelegramJsonExportParser --> ResultBundle
    ResultBundle --> UserEntry
    Bot --> ExcelExporter
    Bot --> TextResultFormatter
    Bot --> Keyboards
    Bot --> UiTexts
    Bot --> Constants
    Session --> UserState
```

---

## 9. Развёртывание (deployment) и почему порты “не критичны”

Бот работает в режиме **long polling**:

* приложение **само** ходит в Telegram Bot API по HTTPS;
* входящие запросы на ваш контейнер/хост **не требуются** (в отличие от webhook).

```mermaid
flowchart LR
    UserDevice["Устройство пользователя<br/>Telegram Client"] --> TGCloud["Telegram Bot API (Cloud)"]
    TGCloud -->|"Updates (long polling)"| Host["Хост/VM/ПК"]
    Host -->|"docker compose"| Container["Docker container: ChatReportBot<br/>Java + Spring Boot + telegrambots"]
    Container -->|"HTTPS requests"| TGCloud
```

**Важно для Docker Compose:**

* публикация порта (`ports: 8080:8080`) **не обязательна** для работы long polling;
* однако оставленный порт может быть удобен “на будущее” (например, добавить `/actuator/health`, метрики или админ-страницу).
  Если порт не нужен — можно убрать `ports` из `docker-compose.yml`.

---

## 10. Конфиденциальность и безопасность данных

Проект реализует требования приватности:

* файлы скачиваются во временную директорию (`tempDir`) и удаляются после обработки;
* данные сессии хранятся только в памяти процесса;
* после завершения обработки вызывается `sessionStore.reset(chatId)`;
* токены/имя бота берутся из переменных окружения.

---

## 11. Обработка ошибок и устойчивость

* Невалидный формат (не `.json`) — бот отвечает сообщением и не добавляет файл в сессию.
* Если сессия истекла по TTL — бот сбрасывает сессию и предлагает начать заново.
* Если во время обработки произошла ошибка — бот сообщает об ошибке и очищает сессию.
* При многофайловой отправке результатов между сообщениями/документами добавлена маленькая пауза (throttle), чтобы снизить риск rate-limit Telegram.

---
