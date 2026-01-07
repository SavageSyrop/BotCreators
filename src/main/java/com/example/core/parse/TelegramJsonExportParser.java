package com.example.core.parse;

import com.example.core.dedup.Deduplicator;
import com.example.core.model.ResultBundle;
import com.example.core.model.UserEntry;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Парсер Telegram Desktop export (JSON).
 * Структура экспорта может отличаться между версиями клиента, поэтому читаем через JsonNode.
 */
public final class TelegramJsonExportParser {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final Pattern MENTION_RE = Pattern.compile("(?<!\\w)@([A-Za-z0-9_]{3,})");
    private static final Pattern TME_RE = Pattern.compile("(?i)(?:https?://)?t\\.me/(?:s/)?([A-Za-z0-9_]{3,})(?:\\b|/|\\?)");

    private TelegramJsonExportParser() {}

    public static ResultBundle parse(Path jsonFile) throws IOException {
        JsonNode root;
        try (var in = Files.newInputStream(jsonFile)) {
            root = MAPPER.readTree(in);
        }

        Set<UserEntry> participants = new LinkedHashSet<>();
        Set<UserEntry> mentions = new LinkedHashSet<>();
        Set<UserEntry> channels = new LinkedHashSet<>();

        JsonNode messages = root.get("messages");
        if (messages != null && messages.isArray()) {
            for (JsonNode msg : messages) {
                extractAuthor(msg, participants);
                extractMentions(msg, mentions, channels);
            }
        }

        // Дедуп + фильтр deleted
        participants = Deduplicator.dedup(participants).stream()
                .filter(u -> !Deduplicator.isDeletedAccount(u))
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        mentions = Deduplicator.dedup(mentions).stream()
                .filter(u -> !Deduplicator.isDeletedAccount(u))
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        channels = Deduplicator.dedup(channels).stream()
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));

        return new ResultBundle(participants, mentions, channels);
    }

    private static void extractAuthor(JsonNode msg, Set<UserEntry> participants) {
        if (msg == null || !msg.isObject()) return;
        // Telegram export commonly uses "from" as display name
        String from = asText(msg.get("from"));
        if (from == null || from.isBlank()) return;
        // Sometimes "from" may contain @username; try to parse it
        String username = null;
        Matcher m = MENTION_RE.matcher(from);
        if (m.find()) {
            username = m.group(1);
        }
        String link = username != null ? "https://t.me/" + username : null;
        participants.add(new UserEntry(username, from, link));
    }

    private static void extractMentions(JsonNode msg, Set<UserEntry> mentions, Set<UserEntry> channels) {
        // 1) structured entities (text_entities)
        JsonNode entities = msg.get("text_entities");
        if (entities != null && entities.isArray()) {
            for (JsonNode ent : entities) {
                String type = asText(ent.get("type"));
                if (type == null) continue;
                if ("mention".equals(type)) {
                    String t = asText(ent.get("text"));
                    addMentionFromText(t, mentions);
                } else if ("text_link".equals(type)) {
                    String href = asText(ent.get("href"));
                    addTmeFromUrl(href, channels);
                }
            }
        }

        // 2) text field can be string or array
        JsonNode text = msg.get("text");
        if (text == null) return;
        if (text.isTextual()) {
            extractByRegex(text.asText(""), mentions, channels);
        } else if (text.isArray()) {
            StringBuilder combined = new StringBuilder();
            for (JsonNode part : text) {
                if (part.isTextual()) {
                    combined.append(part.asText("")).append(' ');
                } else if (part.isObject()) {
                    String type = asText(part.get("type"));
                    String t = asText(part.get("text"));
                    if (t != null) combined.append(t).append(' ');

                    if ("mention".equals(type)) {
                        addMentionFromText(t, mentions);
                    } else if ("text_link".equals(type)) {
                        String href = asText(part.get("href"));
                        addTmeFromUrl(href, channels);
                    }
                }
            }
            extractByRegex(combined.toString(), mentions, channels);
        }
    }

    private static void extractByRegex(String text, Set<UserEntry> mentions, Set<UserEntry> channels) {
        if (text == null || text.isBlank()) return;
        Matcher m = MENTION_RE.matcher(text);
        while (m.find()) {
            String username = m.group(1);
            mentions.add(new UserEntry(username, null, "https://t.me/" + username));
        }
        Matcher t = TME_RE.matcher(text);
        while (t.find()) {
            String name = t.group(1);
            channels.add(new UserEntry(name, null, "https://t.me/" + name));
        }
    }

    private static void addMentionFromText(String t, Set<UserEntry> mentions) {
        if (t == null) return;
        Matcher m = MENTION_RE.matcher(t);
        if (m.find()) {
            String username = m.group(1);
            mentions.add(new UserEntry(username, null, "https://t.me/" + username));
        }
    }

    private static void addTmeFromUrl(String url, Set<UserEntry> channels) {
        if (url == null) return;
        Matcher m = TME_RE.matcher(url);
        if (m.find()) {
            String name = m.group(1);
            channels.add(new UserEntry(name, null, "https://t.me/" + name));
        }
    }

    private static String asText(JsonNode node) {
        if (node == null || node.isNull()) return null;
        return node.asText(null);
    }
}
