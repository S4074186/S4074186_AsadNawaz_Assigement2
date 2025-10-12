package com.healthcare.home.core;

import com.healthcare.home.entity.Prescription;
import com.healthcare.home.entity.Resident;
import com.healthcare.home.scheduler.Shift;
import com.healthcare.home.staff.Doctor;
import com.healthcare.home.staff.Manager;
import com.healthcare.home.staff.Nurse;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.Collection;

public class SerializingService {

    private static final Path HEALTH_CARE_SYSTEM_FILE = Paths.get("healthCareSystem.dat");

    public static void saveRecordsInFile(HealthCareHome home) {
        try (ObjectOutputStream outputStream =
                     new ObjectOutputStream(Files.newOutputStream(HEALTH_CARE_SYSTEM_FILE))) {
            outputStream.writeObject(home);
            System.out.println("Records saved to file: " + HEALTH_CARE_SYSTEM_FILE);
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
            HealthCareHome home = (HealthCareHome) inputStream.readObject();

            // restore prescription id counter
            long maxId = 0;
            Collection<Resident> residents = home.getAllResidents().values();
            for (Resident r : residents) {
                Prescription p = r.getPrescription();
                String numPart = p.getId().replace("PRE-", "");
                long num = Long.parseLong(numPart);
                if (num > maxId) {
                    maxId = num;
                }
            }

            Prescription.setIdCounter(maxId);

            System.out.println("Loaded existing data from file. Prescription counter restored to PRE-" + maxId);
            return home;

        } catch (Exception e) {
            System.err.println("Loading existing records failed, creating new one: " + e.getMessage());
            return new HealthCareHome();
        }
    }
}
