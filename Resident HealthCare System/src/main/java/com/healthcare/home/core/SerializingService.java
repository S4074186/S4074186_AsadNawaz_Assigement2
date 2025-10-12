package com.healthcare.home.core;

import com.healthcare.home.entity.Prescription;
import com.healthcare.home.entity.Resident;
import com.healthcare.home.entity.Bed;
import com.healthcare.home.scheduler.Shift;
import com.healthcare.home.staff.Doctor;
import com.healthcare.home.staff.Manager;
import com.healthcare.home.staff.Nurse;
import com.healthcare.home.staff.Staff;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.List;

public class SerializingService {

    private static final Path HEALTH_CARE_SYSTEM_FILE = Paths.get("healthCareSystem.dat");

    public static void saveRecordsInFile(HealthCareHome home) {
        try {
            Path parent = HEALTH_CARE_SYSTEM_FILE.getParent();
            Files.createDirectories(parent == null ? Path.of(".") : parent);
            try (ObjectOutputStream outputStream = new ObjectOutputStream(Files.newOutputStream(HEALTH_CARE_SYSTEM_FILE))) {
                outputStream.writeObject(home);
            }
            System.out.println("Records saved to file: " + HEALTH_CARE_SYSTEM_FILE);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static HealthCareHome readOrCreateFile() {
        if (!Files.exists(HEALTH_CARE_SYSTEM_FILE)) {
            // create new system with default staff
            HealthCareHome home = new HealthCareHome();

            Manager manager = new Manager("Manager", "manager", "MANAGER-PASSWORD");
            Doctor doctor = new Doctor("Doctor", "doctor", "DOCTOR-PASSWORD");
            Nurse nurse = new Nurse("Nurse", "nurse", "NURSE-PASSWORD");

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

            // Restore staff id counter
            int maxStaff = 0;
            for (Staff s : home.getStaffList().values()) {
                String id = s.getId();
                if (id != null && id.startsWith("STAFF")) {
                    try {
                        int num = Integer.parseInt(id.substring(5));
                        if (num > maxStaff) maxStaff = num;
                    } catch (Exception ignored) {}
                }
            }
            Staff.setIdCounter(maxStaff);

            // Restore resident and prescription counters
            int maxRes = 0;
            long maxPre = 0;

            for (Bed b : home.getBedList().values()) {
                Resident r = b.getResident();
                if (r == null) continue;

                // Resident ID counter
                String rid = r.getId();
                if (rid != null && rid.startsWith("RES")) {
                    try {
                        int num = Integer.parseInt(rid.substring(3));
                        if (num > maxRes) maxRes = num;
                    } catch (Exception ignored) {}
                }

                // Prescription ID counter
                List<Prescription> prescriptionList = r.getPrescriptionList();
                if (prescriptionList != null) {
                    for (Prescription p : prescriptionList) {
                        if (p == null || p.getId() == null) continue;
                        String pid = p.getId();
                        if (pid.startsWith("PRE")) {
                            try {
                                long num = Long.parseLong(pid.substring(3));
                                if (num > maxPre) maxPre = num;
                            } catch (Exception ignored) {}
                        }
                    }
                }
            }

            Resident.setIdCounter(maxRes);
            Prescription.setIdCounter(maxPre);

            System.out.println("Loaded data. Counters restored -> Staff: " + maxStaff + " Res: " + maxRes + " Pre: " + maxPre);
            return home;

        } catch (Exception e) {
            System.err.println("Loading existing records failed, creating new one: " + e.getMessage());
            return new HealthCareHome();
        }
    }
}
