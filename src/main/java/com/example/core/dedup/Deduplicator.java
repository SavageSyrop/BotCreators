package com.example.core.dedup;

import com.example.core.model.UserEntry;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class Deduplicator {
    private Deduplicator() {}

    public static Set<UserEntry> dedup(Set<UserEntry> input) {
        Map<String, UserEntry> map = new LinkedHashMap<>();
        for (UserEntry u : input) {
            String key = key(u);
            if (key == null) continue;
            map.putIfAbsent(key, u);
        }
        return map.values().stream().collect(Collectors.toCollection(java.util.LinkedHashSet::new));
    }

    public static boolean isDeletedAccount(UserEntry u) {
        String name = (u.displayName() == null) ? "" : u.displayName().trim().toLowerCase();
        return name.equals("deleted account") || name.equals("удаленный аккаунт") || name.equals("удалённый аккаунт");
    }

    private static String key(UserEntry u) {
        if (u == null) return null;
        if (u.username() != null && !u.username().isBlank()) {
            return "@" + u.username().trim().toLowerCase();
        }
        if (u.link() != null && !u.link().isBlank()) {
            return "link:" + u.link().trim().toLowerCase();
        }
        if (u.displayName() != null && !u.displayName().isBlank()) {
            return "name:" + u.displayName().trim().toLowerCase();
        }
        return null;
    }
}
