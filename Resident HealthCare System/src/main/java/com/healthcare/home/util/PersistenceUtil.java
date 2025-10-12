//package com.healthcare.home.util;
//
//import com.healthcare.home.core.HealthCareHome;
//
//import java.io.FileOutputStream;
//import java.io.ObjectOutputStream;
//
///**
// Simple persistence helper that writes the HealthCareHome object to a local file.
// Replace with your own secure store if you need encryption or external access.
// */
//public final class PersistenceUtil {
//    private static final String SAVE_FILE = "healthcarehome.dat";
//    private PersistenceUtil() { }
//
//    public static void saveHome(HealthCareHome home) {
//        if (home == null) return;
//        try (FileOutputStream fos = new FileOutputStream(SAVE_FILE);
//             ObjectOutputStream oos = new ObjectOutputStream(fos)) {
//            oos.writeObject(home); // your HealthCareHome must be Serializable
//            oos.flush();
//            ActionLogger.log("SYSTEM", "SAVE_HOME", "Saved home to " + SAVE_FILE);
//        } catch (Exception ex) {
//            ActionLogger.log("SYSTEM", "SAVE_HOME_ERROR", ex.getMessage());
//            ex.printStackTrace();
//        }
//    }
//}
