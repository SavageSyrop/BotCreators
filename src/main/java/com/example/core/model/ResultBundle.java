package com.example.core.model;

import java.util.Set;

public record ResultBundle(
        Set<UserEntry> participants,
        Set<UserEntry> mentions,
        Set<UserEntry> channels
) {
    public int uniqueUsersCount() {
        // "participants" + "mentions" (каналы не считаем пользователями для порога)
        return unionSize(participants, mentions);
    }

    private static int unionSize(Set<UserEntry> a, Set<UserEntry> b) {
        java.util.HashSet<UserEntry> s = new java.util.HashSet<>(a);
        s.addAll(b);
        return s.size();
    }
}
