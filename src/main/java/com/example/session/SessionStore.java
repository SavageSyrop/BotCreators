package com.example.session;

import com.example.enums.Constants;
import com.example.enums.UserState;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SessionStore {
    private final Map<Long, Session> sessions = new ConcurrentHashMap<>();

    public Session getOrCreate(long chatId) {
        return sessions.computeIfAbsent(chatId, id -> new Session());
    }

    public void reset(long chatId) {
        sessions.remove(chatId);
    }

    public boolean isExpired(Session s) {
        Instant last = s.getLastActivityAt();
        return Duration.between(last, Instant.now()).toMinutes() >= Constants.SESSION_TTL_MINUTES;
    }

    public void setState(long chatId, UserState state) {
        Session s = getOrCreate(chatId);
        synchronized (s) {
            s.setState(state);
        }
    }
}
