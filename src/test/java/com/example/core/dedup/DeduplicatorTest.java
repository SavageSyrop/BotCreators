package com.example.core.dedup;

import com.example.core.model.UserEntry;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class DeduplicatorTest {

    @Test
    void dedupByUsernameKeepsSingleEntry() {
        Set<UserEntry> input = new LinkedHashSet<>();
        input.add(new UserEntry("Alice", "Alice", "https://t.me/Alice"));
        input.add(new UserEntry("alice", "Alice 2", "https://t.me/alice"));

        Set<UserEntry> out = Deduplicator.dedup(input);
        assertEquals(1, out.size(), "Username dedup should be case-insensitive");
    }

    @Test
    void dedupFallsBackToLinkWhenNoUsername() {
        Set<UserEntry> input = new LinkedHashSet<>();
        input.add(new UserEntry(null, "User One", "https://t.me/some"));
        input.add(new UserEntry(null, "User Two", "https://t.me/some"));

        Set<UserEntry> out = Deduplicator.dedup(input);
        assertEquals(1, out.size(), "Link dedup should collapse identical links");
    }

    @Test
    void detectsDeletedAccountInEnglishAndRussian() {
        assertTrue(Deduplicator.isDeletedAccount(new UserEntry(null, "Deleted Account", null)));
        assertTrue(Deduplicator.isDeletedAccount(new UserEntry(null, "Удаленный аккаунт", null)));
        assertTrue(Deduplicator.isDeletedAccount(new UserEntry(null, "Удалённый аккаунт", null)));
        assertFalse(Deduplicator.isDeletedAccount(new UserEntry("bob", "Bob", null)));
    }
}
