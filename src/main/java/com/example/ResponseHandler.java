package com.example;

import com.example.enums.Constants;
import com.example.enums.UserState;
import org.telegram.abilitybots.api.db.DBContext;
import org.telegram.abilitybots.api.sender.SilentSender;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import java.util.Map;

import static com.example.enums.UserState.WAITING_FILES;

public class ResponseHandler {
    private final SilentSender sender;
    private final Map<Long, UserState> chatStates;

    public ResponseHandler(SilentSender sender, DBContext db) {
        this.sender = sender;
        chatStates = db.getMap(Constants.CHAT_STATES);
    }

    public void replyToStart(Long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("Привет!  \n" +
                "Я помогу Вам получить список участников Telegram чата по файлам экспорта.\n" +
                "\n" +
                "1. Экспортируйте историю чата в Telegram Desktop.  \n" +
                "2. Отправьте мне от 1 до 10 файлов экспорта.  \n" +
                "3. Нажмите кнопку Начать обработку.");
        sender.execute(message);
        chatStates.put(chatId, WAITING_FILES);
    }
}