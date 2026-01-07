package com.example.export;

import com.example.core.model.ResultBundle;
import com.example.core.model.UserEntry;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.LinkedHashSet;

import static org.junit.jupiter.api.Assertions.*;

class TextResultFormatterTest {

    @Test
    void formatsSectionsAndCountsAndSort() {
        var participants = new LinkedHashSet<UserEntry>();
        participants.add(new UserEntry("bob", null, "https://t.me/bob"));
        participants.add(new UserEntry("alice", null, "https://t.me/alice"));

        var mentions = new LinkedHashSet<UserEntry>();
        mentions.add(new UserEntry("carol", null, "https://t.me/carol"));

        var channels = new LinkedHashSet<UserEntry>();
        channels.add(new UserEntry("my_channel", null, "https://t.me/my_channel"));

        String out = TextResultFormatter.format(List.of("chat1.json"), new ResultBundle(participants, mentions, channels));

        assertTrue(out.contains("Файл: chat1.json"));
        assertTrue(out.contains("Количество участников: 2"));
        assertTrue(out.contains("Количество упоминаний: 1"));

        // Participants section should be sorted
        int idxAlice = out.indexOf("- @alice");
        int idxBob = out.indexOf("- @bob");
        assertTrue(idxAlice >= 0 && idxBob >= 0);
        assertTrue(idxAlice < idxBob, "Expected alphabetical sort");

        // Mentions should include @username
        assertTrue(out.contains("Упоминания:"));
        assertTrue(out.contains("- @carol"));

        // Channels note
        assertTrue(out.contains("Каналы/ссылки"));
    }
}
