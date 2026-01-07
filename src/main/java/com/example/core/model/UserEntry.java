package com.example.core.model;

/**
 * Универсальная сущность для вывода в текст/Excel.
 */
public record UserEntry(
        String username,     // without @ (may be null)
        String displayName,  // may be null
        String link          // may be null
) {}
