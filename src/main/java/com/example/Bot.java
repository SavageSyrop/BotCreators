package com.example;

import com.example.core.ChatExportService;
import com.example.core.model.ResultBundle;
import com.example.enums.Constants;
import com.example.enums.UserState;
import com.example.export.ExcelExporter;
import com.example.export.TextResultFormatter;
import com.example.session.Session;
import com.example.session.SessionStore;
import com.example.ui.Keyboards;
import com.example.ui.UiTexts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Document;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Component("bot")
public class Bot extends TelegramLongPollingBot {

    private final String botUsername;
    private final SessionStore sessionStore;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public Bot(@Value("${telegram-bot.token}") String botToken,
               @Value("${telegram-bot.name}") String botUsername,
               SessionStore sessionStore) {
        // Since telegrambots 6.x, the no-arg TelegramLongPollingBot() constructor is deprecated.
        // Use the token constructor instead.
        super(botToken);
        this.botUsername = botUsername;
        this.sessionStore = sessionStore;
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public void onUpdateReceived(Update update) {
        try {
            if (update.hasCallbackQuery()) {
                handleCallback(update.getCallbackQuery());
                return;
            }
            if (update.hasMessage()) {
                Message msg = update.getMessage();
                if (msg.hasText()) {
                    String txt = msg.getText().trim();
                    if ("/start".equals(txt)) {
                        handleStart(msg.getChatId());
                        return;
                    }
                    // /stop или /stop@<botUsername> — сбрасываем текущую сессию
                    if ("/stop".equals(txt) || ("/stop@" + botUsername).equalsIgnoreCase(txt)) {
                        handleStop(msg.getChatId());
                        return;
                    }
                }
                if (msg.hasDocument()) {
                    handleDocument(msg);
                    return;
                }
                // любое другое сообщение — игнорируем, но обновляем TTL
                touchOrExpire(msg.getChatId());
            }
        } catch (Exception e) {
            log.error("Unhandled update error", e);
        }
    }

    private void handleStart(long chatId) throws TelegramApiException {
        sessionStore.reset(chatId);
        Session s = sessionStore.getOrCreate(chatId);
        s.setState(UserState.WAITING_FILES);

        SendMessage m = new SendMessage(String.valueOf(chatId), UiTexts.startText());
        m.setReplyMarkup(Keyboards.startMenu());
        execute(m);
    }

    private void handleStop(long chatId) throws TelegramApiException {
        // /stop — это "остановка" сценария: удаляем сессию и показываем стартовое меню.
        sessionStore.reset(chatId);

        SendMessage m = new SendMessage(
                String.valueOf(chatId),
                "Ок, остановил текущую сессию.\n\n" +
                "Чтобы начать заново — нажмите /start."
        );
        m.setReplyMarkup(Keyboards.startMenu());
        execute(m);
    }



    private void handleDocument(Message message) throws TelegramApiException {
        long chatId = message.getChatId();
        Session s = touchOrExpire(chatId);
        if (s == null) return;

        if (s.getState() == UserState.PROCESSING) {
            sendText(chatId, "Обработка уже выполняется.  \nЭтот файл не войдет в текущий отчет.", null);
            return;
        }

        Document doc = message.getDocument();
        String fileName = doc.getFileName() != null ? doc.getFileName() : "export.json";
        if (!fileName.toLowerCase().endsWith(".json")) {
            sendText(chatId, "Неподдерживаемый формат файла.  \nПоддерживается только JSON.", null);
            return;
        }

        if (s.getFiles().size() >= Constants.MAX_FILES) {
            // по ТЗ — предлагаем новую сессию
            SendMessage m = new SendMessage(String.valueOf(chatId),
                    "Можно загрузить не более 10 файлов за одну обработку.  \nНачните новую сессию.");
            m.setReplyMarkup(Keyboards.processOtherMenu());
            execute(m);
            return;
        }

        s.getFiles().add(new Session.FileMeta(doc.getFileId(), fileName, doc.getFileSize() == null ? 0L : doc.getFileSize()));
        s.setState(UserState.READY_TO_PROCESS);

        SendMessage m = new SendMessage(String.valueOf(chatId),
                "Файл принят.  \n" +
                "Сейчас загружено: " + s.getFiles().size() + " файл(ов).  \n\n" +
                "Можете отправить еще файлы или нажмите Начать обработку.");
        m.setReplyMarkup(Keyboards.readyMenu());
        execute(m);
    }

    private void handleCallback(CallbackQuery cq) throws TelegramApiException {
        long chatId = cq.getMessage().getChatId();
        String data = cq.getData();

        // UX: быстро убираем "часики" у кнопки
        AnswerCallbackQuery ack = new AnswerCallbackQuery();
        ack.setCallbackQueryId(cq.getId());
        execute(ack);

        Session s = touchOrExpire(chatId);
        if (s == null && !Constants.CB_RESTART.equals(data)) {
            return;
        }

        switch (data) {
            case Constants.CB_EXPORT_HELP -> sendText(chatId, UiTexts.exportHelp(), Keyboards.startMenu());
            case Constants.CB_FAQ -> sendText(chatId, UiTexts.faq(), Keyboards.startMenu());
            case Constants.CB_PROCESS_OTHER -> handleStart(chatId);
            case Constants.CB_RESTART -> handleStart(chatId);
            case Constants.CB_START_PROCESS -> startProcessing(chatId);
            default -> sendText(chatId, "Неизвестная команда.", null);
        }
    }

    private void startProcessing(long chatId) throws TelegramApiException {
        Session s = touchOrExpire(chatId);
        if (s == null) return;

        if (s.getFiles().isEmpty()) {
            sendText(chatId, "Сначала загрузите хотя бы один файл экспорта.", Keyboards.startMenu());
            return;
        }

        s.setState(UserState.PROCESSING);
        int total = s.getFiles().size();
        sendText(chatId,
                "Начинаю обработку " + total + " файл(ов).  \nПожалуйста, подождите.",
                null);

        // Обрабатываем асинхронно, чтобы не блокировать polling
        List<Session.FileMeta> filesSnapshot = new ArrayList<>(s.getFiles());
        executor.submit(() -> processFilesAsync(chatId, filesSnapshot));
    }

    private void processFilesAsync(long chatId, List<Session.FileMeta> files) {
        Path tmpDir = null;
        try {
            tmpDir = Files.createTempDirectory("tg-export-" + chatId + "-");
            int total = files.size();
            for (int i = 0; i < total; i++) {
                Session.FileMeta meta = files.get(i);
                boolean isLast = (i == total - 1);

                // 1) Скачиваем ОДИН файл
                Path p = downloadFileTo(tmpDir, meta, i + 1);

                // 2) Обрабатываем ОДИН файл
                ResultBundle result = ChatExportService.processJsonFiles(List.of(p));
                int participantsCount = result.participants().size();
                int mentionsCount = result.mentions().size();

                // 3) Отправляем отдельный результат
                String progressLine = "Файл " + (i + 1) + "/" + total + " обработан.";
                String fileName = meta.fileName();

                // Порог по ТЗ: считаем именно участников
                if (participantsCount <= Constants.TEXT_THRESHOLD_MAX) {
                    String text = progressLine + "\n\n" + TextResultFormatter.format(List.of(fileName), result);
                    SendMessage m = new SendMessage(String.valueOf(chatId), text);
                    if (isLast) {
                        m.setReplyMarkup(Keyboards.processOtherMenu());
                    }
                    execute(m);
                } else {
                    Path xlsx = ExcelExporter.export(result, tmpDir);

                    // ExcelExporter всегда пишет participants.xlsx — переименуем, чтобы не перетирать при нескольких файлах
                    Path uniqueXlsx = tmpDir.resolve(String.format("participants_%02d.xlsx", i + 1));
                    Files.move(xlsx, uniqueXlsx, StandardCopyOption.REPLACE_EXISTING);

                    SendDocument doc = new SendDocument();
                    doc.setChatId(String.valueOf(chatId));
                    doc.setCaption(
                            progressLine + "\n" +
                                    "Готово.\n" +
                                    "Файл: " + fileName + "\n" +
                                    "Количество участников: " + participantsCount + "\n" +
                                    "Количество упоминаний: " + mentionsCount + "\n" +
                                    "Я сформировал Excel файл."
                    );
                    if (isLast) {
                        doc.setReplyMarkup(Keyboards.processOtherMenu());
                    }
                    doc.setDocument(new InputFile(uniqueXlsx.toFile(), uniqueXlsx.getFileName().toString()));
                    execute(doc);
                }

                // Небольшая пауза: если файлов много (до 10), Telegram может ограничивать скорость отправки.
                if (!isLast) {
                    try {
                        Thread.sleep(250);
                    } catch (InterruptedException ignored) {
                    }
                }
            }

            // После результата — сессию удаляем
            sessionStore.reset(chatId);
        } catch (Exception e) {
            log.error("Processing error", e);
            try {
                SendMessage m = new SendMessage(String.valueOf(chatId),
                        "Произошла ошибка при обработке файлов.  \nПроверьте формат экспорта и попробуйте снова.");
                m.setReplyMarkup(Keyboards.processOtherMenu());
                execute(m);
            } catch (TelegramApiException ignored) {
            }
            sessionStore.reset(chatId);
        } finally {
            if (tmpDir != null) {
                deleteRecursively(tmpDir);
            }
        }
    }

    private Path downloadFileTo(Path dir, Session.FileMeta meta, int index) throws TelegramApiException, IOException {
        GetFile gf = new GetFile(meta.fileId());
        org.telegram.telegrambots.meta.api.objects.File file = execute(gf);
        java.io.File downloaded = downloadFile(file);

        // В Telegram Desktop export часто все части называются одинаково (например, result.json).
        // Если сохранять по оригинальному имени, то второй файл перезапишет первый.
        String originalName = meta.fileName() == null ? "export.json" : meta.fileName();
        String safeName = safeFilename(originalName);
        String uniqueName = String.format("%02d_%s", index, safeName);

        Path target = dir.resolve(uniqueName);
        Files.move(downloaded.toPath(), target, StandardCopyOption.REPLACE_EXISTING);
        return target;
    }

    /**
     * Приводит имя файла к безопасному виду для Windows/Linux (убирает запрещённые символы и лишние пробелы).
     * Нужен, потому что Telegram Desktop export иногда даёт имена с символами, которые нельзя создать как файл.
     */
    private static String safeFilename(String name) {
        if (name == null || name.isBlank()) return "export.json";
        String trimmed = name.trim();
        // Запрещённые символы в Windows: \ / : * ? " < > |
        String cleaned = trimmed.replaceAll("[\\\\/:*?\"<>|]", "_");
        // Чтобы не было путей вроде ".." и т.п.
        cleaned = cleaned.replace("..", "_");
        return cleaned.isBlank() ? "export.json" : cleaned;
    }

    private Session touchOrExpire(long chatId) throws TelegramApiException {
        Session s = sessionStore.getOrCreate(chatId);
        synchronized (s) {
            if (sessionStore.isExpired(s)) {
                sessionStore.reset(chatId);
                SendMessage m = new SendMessage(String.valueOf(chatId),
                        "Сессия истекла.  \nПожалуйста, начните заново.");
                m.setReplyMarkup(Keyboards.restartMenu());
                execute(m);
                return null;
            }
            s.touch();
            return s;
        }
    }

    private void sendText(long chatId, String text, org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard markup) throws TelegramApiException {
        SendMessage m = new SendMessage(String.valueOf(chatId), text);
        if (markup != null) {
            m.setReplyMarkup(markup);
        }
        execute(m);
    }

    private static void deleteRecursively(Path path) {
        try {
            if (!Files.exists(path)) return;
            Files.walk(path)
                    .sorted((a, b) -> b.compareTo(a))
                    .forEach(p -> {
                        try {
                            Files.deleteIfExists(p);
                        } catch (IOException ignored) {
                        }
                    });
        } catch (IOException ignored) {
        }
    }
}
