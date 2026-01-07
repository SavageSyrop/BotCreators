package com.example.core.parse;

import com.example.core.model.ResultBundle;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class TelegramJsonExportParserTest {

    @Test
    void parsesParticipantsMentionsAndChannelsFromFixture() throws Exception {
        Path fixture = Path.of("src/test/resources/fixtures/sample_export.json");
        ResultBundle result = TelegramJsonExportParser.parse(fixture);

        // participants: Alice (with @alice) and Bob, but "Deleted Account" must be filtered out
        assertTrue(result.participants().stream().anyMatch(u -> "alice".equalsIgnoreCase(u.username())), "Should capture @alice from 'from'");
        assertTrue(result.participants().stream().anyMatch(u -> "Bob".equalsIgnoreCase(u.displayName())), "Should capture author Bob");
        assertFalse(result.participants().stream().anyMatch(u -> u.displayName() != null && u.displayName().toLowerCase().contains("deleted")), "Deleted Account should be filtered");

        // mentions: @bob from text, @carol from structured text, @dave from entities
        assertTrue(result.mentions().stream().anyMatch(u -> "bob".equalsIgnoreCase(u.username())), "Should capture @bob");
        assertTrue(result.mentions().stream().anyMatch(u -> "carol".equalsIgnoreCase(u.username())), "Should capture @carol");
        assertTrue(result.mentions().stream().anyMatch(u -> "dave".equalsIgnoreCase(u.username())), "Should capture @dave");

        // channels: my_channel from t.me link, plus chan2 + chan3
        assertTrue(result.channels().stream().anyMatch(u -> "my_channel".equalsIgnoreCase(u.username())), "Should capture t.me/my_channel");
        assertTrue(result.channels().stream().anyMatch(u -> "chan2".equalsIgnoreCase(u.username())), "Should capture chan2");
        assertTrue(result.channels().stream().anyMatch(u -> "chan3".equalsIgnoreCase(u.username())), "Should capture chan3");
    }
}
