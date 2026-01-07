package com.example.ui;

public final class UiTexts {
    private UiTexts() {}

    public static String startText() {
        return "Привет!  \n" +
                "Я помогу Вам получить список участников Telegram чата по файлам экспорта.\n\n" +
                "1. Экспортируйте историю чата в Telegram Desktop.  \n" +
                "2. Отправьте мне от 1 до 10 файлов экспорта.  \n" +
                "3. Нажмите кнопку Начать обработку.";
    }

    public static String exportHelp() {
        return "Чтобы экспортировать чат:  \n" +
                "1. Откройте Telegram Desktop.  \n" +
                "2. Выберите чат.  \n" +
                "3. Меню — Экспорт истории чата.  \n" +
                "4. Выберите формат JSON.  \n" +
                "5. Сохраните файл и отправьте его сюда.";
    }

    public static String faq() {
        return "Как пользоваться ботом:\n\n" +
                "1. Экспортируйте чат через Telegram Desktop.\n" +
                "2. Отправьте сюда 1–10 файлов экспорта.\n" +
                "3. Нажмите Начать обработку.\n\n" +
                "Бот не хранит ваши данные.\n" +
                "В MVP поддерживается только формат JSON.";
    }
}
