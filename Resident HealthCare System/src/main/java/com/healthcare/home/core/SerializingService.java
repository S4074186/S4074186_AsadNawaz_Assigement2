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
import java.time.DayOfWeek;
import java.time.LocalDate;
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

            Manager manager = new Manager("Manager", "admin", "admin123");
            Doctor doctor1 = new Doctor("Doctor1", "doctor1", "doctor1");
            Nurse nurse1 = new Nurse("Nurse1", "nurse1", "nurse1");
            Nurse nurse2 = new Nurse("Nurse2", "nurse2", "nurse2");
            Nurse nurse3 = new Nurse("Nurse3", "nurse3", "nurse3");
            Nurse nurse4 = new Nurse("Nurse4", "nurse4", "nurse4");
            Nurse nurse5 = new Nurse("Nurse5", "nurse5", "nurse5");
            Nurse nurse6 = new Nurse("Nurse6", "nurse6", "nurse6");
            Nurse nurse7 = new Nurse("Nurse7", "nurse7", "nurse7");
            Nurse nurse8 = new Nurse("Nurse8", "nurse8", "nurse8");

            home.registerNewStaff(manager);
            home.registerNewStaff(doctor1);
            home.registerNewStaff(nurse1);
            home.registerNewStaff(nurse2);
            home.registerNewStaff(nurse3);
            home.registerNewStaff(nurse4);
            home.registerNewStaff(nurse5);
            home.registerNewStaff(nurse6);
            home.registerNewStaff(nurse7);
            home.registerNewStaff(nurse8);

            LocalDate today = LocalDate.now();

            // Assign Doctor shift: 1 hour every day of the week
            for (DayOfWeek day : DayOfWeek.values()) {
                LocalDate date = today.with(day);
                LocalDateTime start = date.atTime(10, 0);
                LocalDateTime end = start.plusHours(1);
                home.assigningShift(manager, doctor1, new Shift(start, end));
            }

            // Assign Nurse shifts: 8am-4pm and 2pm-10pm every day
            for (DayOfWeek day : DayOfWeek.values()) {
                LocalDate date = today.with(day);
                LocalDateTime morningStart = date.atTime(8, 0);
                LocalDateTime morningEnd = date.atTime(16, 0);
                LocalDateTime eveningStart = date.atTime(14, 0);
                LocalDateTime eveningEnd = date.atTime(22, 0);

                // Morning
                home.assigningShift(manager, nurse1, new Shift(morningStart, morningEnd));
                home.assigningShift(manager, nurse2, new Shift(morningStart, morningEnd));
                home.assigningShift(manager, nurse3, new Shift(morningStart, morningEnd));
                home.assigningShift(manager, nurse4, new Shift(morningStart, morningEnd));
                // Evening
                home.assigningShift(manager, nurse5, new Shift(eveningStart, eveningEnd));
                home.assigningShift(manager, nurse6, new Shift(eveningStart, eveningEnd));
                home.assigningShift(manager, nurse7, new Shift(eveningStart, eveningEnd));
                home.assigningShift(manager, nurse8, new Shift(eveningStart, eveningEnd));
            }

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
