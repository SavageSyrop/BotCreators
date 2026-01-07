package com.example.session;

import com.example.enums.UserState;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Данные сессии храним только в памяти.
 * Временные файлы создаются в /tmp и удаляются после обработки.
 */
public final class Session {
    private UserState state = UserState.WAITING_FILES;
    private Instant lastActivityAt = Instant.now();
    private final List<FileMeta> files = new ArrayList<>();

    public UserState getState() {
        return state;
    }

    public void setState(UserState state) {
        this.state = state;
        touch();
    }

    public Instant getLastActivityAt() {
        return lastActivityAt;
    }

    public void touch() {
        this.lastActivityAt = Instant.now();
    }

    public List<FileMeta> getFiles() {
        return files;
    }

    public record FileMeta(String fileId, String fileName, long fileSize) {}
}
