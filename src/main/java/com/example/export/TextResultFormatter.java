package com.example.export;

import com.example.core.model.ResultBundle;
import com.example.core.model.UserEntry;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Формирует человекочитаемый текстовый результат для Telegram.
 *
 * Важно: "participants" и "mentions" выводятся раздельно (по ТЗ).
 */
public final class TextResultFormatter {
    private TextResultFormatter() {}

    /**
     * Backward-compatible overload (без отображения имён файлов).
     */
    public static String format(ResultBundle result) {
        return format(List.of(), result);
    }

    /**
     * Формат для ответа бота (текстом), когда участников <= порога.
     */
    public static String format(List<String> fileNames, ResultBundle result) {
        List<UserEntry> participants = new ArrayList<>(result.participants());
        List<UserEntry> mentions = new ArrayList<>(result.mentions());

        participants.sort(Comparator.comparing(TextResultFormatter::participantKey, String.CASE_INSENSITIVE_ORDER));
        mentions.sort(Comparator.comparing(TextResultFormatter::mentionKey, String.CASE_INSENSITIVE_ORDER));

        StringBuilder sb = new StringBuilder();

        // Header
        if (fileNames != null && !fileNames.isEmpty()) {
            if (fileNames.size() == 1) {
                sb.append("Файл: ").append(fileNames.get(0)).append("\n");
            } else {
                sb.append("Файлы: ").append(String.join(", ", fileNames)).append("\n");
            }
        }

        sb.append("Количество участников: ").append(participants.size()).append("\n");
        sb.append("Количество упоминаний: ").append(mentions.size()).append("\n\n");

        // Participants
        sb.append("Участники:\n");
        if (participants.isEmpty()) {
            sb.append("- (не найдено)\n");
        } else {
            for (UserEntry u : participants) {
                sb.append("- ").append(renderParticipant(u)).append("\n");
            }
        }

        // Mentions
        sb.append("\nУпоминания:\n");
        if (mentions.isEmpty()) {
            sb.append("- (не найдено)\n");
        } else {
            for (UserEntry u : mentions) {
                sb.append("- ").append(renderMention(u)).append("\n");
            }
        }

        // Channels
        if (!result.channels().isEmpty()) {
            sb.append("\nКаналы/ссылки: ")
                    .append(result.channels().size())
                    .append(" (в Excel будут отдельной вкладкой)\n");
        }

        return sb.toString();
    }

    private static String renderParticipant(UserEntry u) {
        // Для участников приоритет — отображаемое имя (как в экспорте Telegram)
        if (u.displayName() != null && !u.displayName().isBlank()) {
            return u.displayName();
        }
        if (u.username() != null && !u.username().isBlank()) {
            return "@" + u.username();
        }
        if (u.link() != null && !u.link().isBlank()) {
            return u.link();
        }
        return "(unknown)";
    }

    private static String renderMention(UserEntry u) {
        // Для упоминаний логично показывать именно @username
        if (u.username() != null && !u.username().isBlank()) {
            return "@" + u.username();
        }
        // Фоллбек: если почему-то нет username
        if (u.displayName() != null && !u.displayName().isBlank()) {
            return u.displayName();
        }
        if (u.link() != null && !u.link().isBlank()) {
            return u.link();
        }
        return "(unknown)";
    }

    private static String participantKey(UserEntry u) {
        if (u.displayName() != null && !u.displayName().isBlank()) return u.displayName();
        if (u.username() != null && !u.username().isBlank()) return u.username();
        if (u.link() != null && !u.link().isBlank()) return u.link();
        return "";
    }

    private static String mentionKey(UserEntry u) {
        if (u.username() != null && !u.username().isBlank()) return u.username();
        if (u.displayName() != null && !u.displayName().isBlank()) return u.displayName();
        if (u.link() != null && !u.link().isBlank()) return u.link();
        return "";
    }
}
