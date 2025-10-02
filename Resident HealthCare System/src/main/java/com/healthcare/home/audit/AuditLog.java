package com.healthcare.home.audit;

import com.healthcare.home.auth.Access;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AuditLog implements Serializable {
    public static record Entry(LocalDateTime at, String staffId, Access action,
                               String message) implements Serializable {
    }

    private static final AuditLog INSTANCE = new AuditLog();
    private final List<Entry> entries = new ArrayList<>();

    private AuditLog() {
    }

    public static AuditLog get() {
        return INSTANCE;
    }

    public void entryLog(String staffId, Access action, String message) {
        entries.add(new Entry(LocalDateTime.now(), staffId, action, message));
    }

    public List<Entry> getEntries() {
        return List.copyOf(entries);
    }
}
