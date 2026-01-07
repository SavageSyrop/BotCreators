package com.example.core;

import com.example.core.model.ResultBundle;
import com.example.core.model.UserEntry;
import com.example.core.parse.TelegramJsonExportParser;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class ChatExportService {
    private ChatExportService() {}

    public static ResultBundle processJsonFiles(List<Path> jsonFiles) throws IOException {
        Set<UserEntry> participants = new LinkedHashSet<>();
        Set<UserEntry> mentions = new LinkedHashSet<>();
        Set<UserEntry> channels = new LinkedHashSet<>();

        for (Path f : jsonFiles) {
            ResultBundle r = TelegramJsonExportParser.parse(f);
            participants.addAll(r.participants());
            mentions.addAll(r.mentions());
            channels.addAll(r.channels());
        }

        return new ResultBundle(participants, mentions, channels);
    }
}
