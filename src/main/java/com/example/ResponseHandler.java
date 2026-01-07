package com.example;

import com.example.session.SessionStore;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import static com.example.enums.UserState.WAITING_FILES;

public class ResponseHandler {
    /**
     * В проекте используется TelegramLongPollingBot, поэтому вместо AbilityBots SilentSender
     * используем стандартный AbsSender (bot.execute(...)).
     */
    private final AbsSender sender;
    private final SessionStore sessionStore;

    public ResponseHandler(AbsSender sender, SessionStore sessionStore) {
        this.sender = sender;
        this.sessionStore = sessionStore;
    }

    public void replyToStart(Long chatId) {
        SendMessage message = new SendMessage(String.valueOf(chatId), "Привет!  \n" +
                "Я помогу Вам получить список участников Telegram чата по файлам экспорта.\n" +
                "\n" +
                "1. Экспортируйте историю чата в Telegram Desktop.  \n" +
                "2. Отправьте мне от 1 до 10 файлов экспорта.  \n" +
                "3. Нажмите кнопку Начать обработку.");
        try {
            sender.execute(message);
        } catch (TelegramApiException e) {
            // Этот класс может быть неиспользуемым в текущем потоке (логика в Bot.java),
            // но оставим безопасную обработку исключения.
        }
        sessionStore.setState(chatId, WAITING_FILES);
    }
}