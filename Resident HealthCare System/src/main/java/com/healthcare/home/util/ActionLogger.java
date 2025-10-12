package com.healthcare.home.util;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class ActionLogger {
    private static final String LOG_FILE = "actions.log";
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private ActionLogger() { }

    public static synchronized void log(String staffId, String action, String details) {
        try (FileWriter fw = new FileWriter(LOG_FILE, true);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter out = new PrintWriter(bw)) {
            String time = LocalDateTime.now().format(FMT);
            out.printf("%s | staff:%s | action:%s | %s%n", time, safe(staffId), safe(action), safe(details));
        } catch (Exception ex) {
            // best effort write. do not throw from logger
            ex.printStackTrace();
        }
    }

    private static String safe(String s) {
        return s == null ? "" : s.replaceAll("[\\r\\n]", " ");
    }
}
