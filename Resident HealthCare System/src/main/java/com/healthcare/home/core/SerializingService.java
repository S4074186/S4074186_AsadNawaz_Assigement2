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
            System.out.println("Records saved to file: "
                    + HEALTH_CARE_SYSTEM_FILE);
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
            return home;
        }
        try (ObjectInputStream inputStream = new ObjectInputStream(Files.newInputStream(HEALTH_CARE_SYSTEM_FILE))) {
            return (HealthCareHome) inputStream.readObject();
        } catch (Exception e) {
            System.err.println("Loading existing records from file "
                    + HEALTH_CARE_SYSTEM_FILE
                    + " failed, creating new one: "
                    + e.getMessage());
            return new HealthCareHome();
        }
    }

    public static void save(Object o) {
        try {
            Files.createDirectories(HEALTH_CARE_SYSTEM_FILE.getParent() == null ? Path.of(".") : HEALTH_CARE_SYSTEM_FILE.getParent());
            try (ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(HEALTH_CARE_SYSTEM_FILE))) {
                out.writeObject(o);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Object load() {
        try {
            if (!Files.exists(HEALTH_CARE_SYSTEM_FILE)) return null;
            try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(HEALTH_CARE_SYSTEM_FILE))) {
                return in.readObject();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
