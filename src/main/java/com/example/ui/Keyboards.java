package com.example.ui;

import com.example.enums.Constants;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.List;

public final class Keyboards {
    private Keyboards() {}

    public static InlineKeyboardMarkup startMenu() {
        InlineKeyboardButton exportHelp = InlineKeyboardButton.builder()
                .text("Как экспортировать чат")
                .callbackData(Constants.CB_EXPORT_HELP)
                .build();
        InlineKeyboardButton faq = InlineKeyboardButton.builder()
                .text("FAQ")
                .callbackData(Constants.CB_FAQ)
                .build();

        return InlineKeyboardMarkup.builder()
                .keyboard(List.of(List.of(exportHelp, faq)))
                .build();
    }

    public static InlineKeyboardMarkup readyMenu() {
        InlineKeyboardButton exportHelp = InlineKeyboardButton.builder()
                .text("Как экспортировать чат")
                .callbackData(Constants.CB_EXPORT_HELP)
                .build();
        InlineKeyboardButton faq = InlineKeyboardButton.builder()
                .text("FAQ")
                .callbackData(Constants.CB_FAQ)
                .build();
        InlineKeyboardButton process = InlineKeyboardButton.builder()
                .text("Начать обработку")
                .callbackData(Constants.CB_START_PROCESS)
                .build();

        return InlineKeyboardMarkup.builder()
                .keyboard(List.of(
                        List.of(exportHelp, faq),
                        List.of(process)
                ))
                .build();
    }

    public static InlineKeyboardMarkup processOtherMenu() {
        InlineKeyboardButton processOther = InlineKeyboardButton.builder()
                .text("Обработать другой чат")
                .callbackData(Constants.CB_PROCESS_OTHER)
                .build();

        return InlineKeyboardMarkup.builder()
                .keyboard(List.of(List.of(processOther)))
                .build();
    }

    public static InlineKeyboardMarkup restartMenu() {
        InlineKeyboardButton restart = InlineKeyboardButton.builder()
                .text("Начать заново")
                .callbackData(Constants.CB_RESTART)
                .build();

        return InlineKeyboardMarkup.builder()
                .keyboard(List.of(List.of(restart)))
                .build();
    }
}
