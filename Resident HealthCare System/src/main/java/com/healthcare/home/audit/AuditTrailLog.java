package com.healthcare.home.audit;

import com.healthcare.home.auth.AuthAccess;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 *
 */
public final class AuditTrailLog implements Serializable {

    private static final String ACTIONS_LOG = "actions.log";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final AuditTrailLog AUDIT_TRAIL_LOG = new AuditTrailLog();

    // Represents a single log entry
    public static record EntryRecord(LocalDateTime localDateTime, String staffId, String action, String message) implements Serializable {
    }

    private static final List<EntryRecord> ENTRY_RECORD_LIST = new ArrayList<>();

    private AuditTrailLog() {
    }

    public static AuditTrailLog get() {
        return AUDIT_TRAIL_LOG;
    }

    /**
     * Logs an event with text action
     */
    public static synchronized void entryLog(String staffId, String action, String message) {
        LocalDateTime now = LocalDateTime.now();
        EntryRecord entryRecord = new EntryRecord(now, validate(staffId), validate(action), validate(message));
        ENTRY_RECORD_LIST.add(entryRecord);
        writeLogsToFile(entryRecord);
    }

    /**
     * Logs an event with AuthAccess enum
     */
    public synchronized void entryLog(String staffId, AuthAccess action, String message) {
        entryLog(staffId, action == null ? "" : action.name(), message);
    }

    private static void writeLogsToFile(EntryRecord entryRecord) {
        try (FileWriter fileWriter = new FileWriter(ACTIONS_LOG, true);
             BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
             PrintWriter printWriter = new PrintWriter(bufferedWriter)) {
            String time = entryRecord.localDateTime.format(DATE_TIME_FORMATTER);
            printWriter.printf("%s | staff:%s | action:%s | %s%n",
                    time, entryRecord.staffId, entryRecord.action, entryRecord.message);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public List<EntryRecord> getEntries() {
        return List.copyOf(ENTRY_RECORD_LIST);
    }

    private static String validate(String s) {
        return s == null ? "" : s.replaceAll("[\\r\\n]", " ");
    }
}
