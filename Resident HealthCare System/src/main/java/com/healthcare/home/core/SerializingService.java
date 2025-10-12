package com.healthcare.home.core;

import com.healthcare.home.scheduler.Shift;
import com.healthcare.home.staff.Doctor;
import com.healthcare.home.staff.Manager;
import com.healthcare.home.staff.Nurse;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;

public class SerializingService {

    private static final Path HEALTH_CARE_SYSTEM_FILE = Paths.get("healthCareSystem.dat");

    public static void saveRecordsInFile(HealthCareHome home) {
        try (ObjectOutputStream outputStream =
                     new ObjectOutputStream(Files.newOutputStream(HEALTH_CARE_SYSTEM_FILE))) {
            outputStream.writeObject(home);
            System.out.println("Records saved to file: " + HEALTH_CARE_SYSTEM_FILE);

            // Save the current ID counter
            saveIdCounter();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static HealthCareHome readOrCreateFile() {
        if (!Files.exists(HEALTH_CARE_SYSTEM_FILE)) {
            HealthCareHome home = new HealthCareHome();
            Manager manager = new Manager("M1", "Manager", "manager", "MANAGER-PASSWORD");
            Doctor doctor = new Doctor("D1", "Doctor", "doctor", "DOCTOR-PASSWORD");
            Nurse nurse = new Nurse("N1", "Nurse", "nurse", "NURSE-PASSWORD");

            home.registerNewStaff(manager);
            home.registerNewStaff(doctor);
            home.registerNewStaff(nurse);

            LocalDateTime now = LocalDateTime.now();
            home.assigningShift(manager, doctor, new Shift(now, now.plusHours(8)));
            home.assigningShift(manager, nurse, new Shift(now, now.plusHours(8)));

            PrescriptionIdGenerator.reset(); // reset ID counter
            return home;
        }

        try (ObjectInputStream inputStream = new ObjectInputStream(Files.newInputStream(HEALTH_CARE_SYSTEM_FILE))) {
            HealthCareHome home = (HealthCareHome) inputStream.readObject();

            // Load ID counter from file
            loadIdCounter();

            return home;
        } catch (Exception e) {
            System.err.println("Loading existing records from file "
                    + HEALTH_CARE_SYSTEM_FILE
                    + " failed, creating new one: "
                    + e.getMessage());
            PrescriptionIdGenerator.reset();
            return new HealthCareHome();
        }
    }

    public static void save(Object o) {
        try {
            Files.createDirectories(
                    HEALTH_CARE_SYSTEM_FILE.getParent() == null ? Path.of(".") : HEALTH_CARE_SYSTEM_FILE.getParent());
            try (ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(HEALTH_CARE_SYSTEM_FILE))) {
                out.writeObject(o);
            }
            saveIdCounter();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Object load() {
        try {
            if (!Files.exists(HEALTH_CARE_SYSTEM_FILE)) return null;
            try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(HEALTH_CARE_SYSTEM_FILE))) {
                Object data = in.readObject();
                loadIdCounter();
                return data;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static final Path COUNTER_FILE = Paths.get("prescriptionCounter.dat");

    private static void saveIdCounter() {
        try (ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(COUNTER_FILE))) {
            out.writeInt(PrescriptionIdGenerator.getCurrentId());
        } catch (IOException e) {
            System.err.println("Failed to save ID counter: " + e.getMessage());
        }
    }

    private static void loadIdCounter() {
        if (!Files.exists(COUNTER_FILE)) return;
        try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(COUNTER_FILE))) {
            int lastId = in.readInt();
            PrescriptionIdGenerator.setCurrentId(lastId);
        } catch (IOException e) {
            System.err.println("Failed to load ID counter: " + e.getMessage());
        }
    }

    public static class PrescriptionIdGenerator {
        private static int currentId = 1;

        public static synchronized String nextId() {
            return String.valueOf(currentId++);
        }

        public static int getCurrentId() {
            return currentId;
        }

        public static void setCurrentId(int id) {
            currentId = id;
        }

        public static void reset() {
            currentId = 1;
        }
    }

}
