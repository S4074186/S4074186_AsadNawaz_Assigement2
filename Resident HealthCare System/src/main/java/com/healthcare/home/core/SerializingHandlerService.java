package com.healthcare.home.core;

import com.healthcare.home.entities.Prescription;
import com.healthcare.home.entities.Resident;
import com.healthcare.home.entities.Bed;
import com.healthcare.home.entities.Shift;
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

/**
 * SerializingHandlerService
 */
public class SerializingHandlerService {

    private static final Path HEALTH_CARE_SYSTEM_FILE = Paths.get("healthCareSystem.dat");

    /**
     * saveRecordsInFile method to serialization to the file
     *
     * @param home
     */
    public static void saveRecordsInFile(ResidentHealthCareHome home) {
        try {
            Path parent = HEALTH_CARE_SYSTEM_FILE.getParent();
            Files.createDirectories(parent == null ? Path.of(".") : parent);
            try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(Files.newOutputStream(HEALTH_CARE_SYSTEM_FILE))) {
                objectOutputStream.writeObject(home);
            }
            System.out.println("Records saved to file: " + HEALTH_CARE_SYSTEM_FILE);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * readOrCreateFile method to deserialization the object from the serialized file
     *
     * @return
     */
    public static ResidentHealthCareHome readOrCreateFile() {
        if (!Files.exists(HEALTH_CARE_SYSTEM_FILE)) {
            // create new system with default staff
            ResidentHealthCareHome home = new ResidentHealthCareHome();

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

            home.registeringNewStaff(manager);
            home.registeringNewStaff(doctor1);
            home.registeringNewStaff(nurse1);
            home.registeringNewStaff(nurse2);
            home.registeringNewStaff(nurse3);
            home.registeringNewStaff(nurse4);
            home.registeringNewStaff(nurse5);
            home.registeringNewStaff(nurse6);
            home.registeringNewStaff(nurse7);
            home.registeringNewStaff(nurse8);

            LocalDate today = LocalDate.now();

            // Assign Doctor shift: 1 hour every day of the week
            for (DayOfWeek dayOfWeek : DayOfWeek.values()) {
                LocalDate date = today.with(dayOfWeek);
                LocalDateTime startTime = date.atTime(10, 0);
                LocalDateTime endTime = startTime.plusHours(1);
                home.assigningShift(manager, doctor1, new Shift(startTime, endTime));
            }

            // Assign Nurse shifts: 8am-4pm and 2pm-10pm every day
            for (DayOfWeek dayOfWeek : DayOfWeek.values()) {
                LocalDate date = today.with(dayOfWeek);
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
            home.checkingCompliance();

            return home;
        }

        try (ObjectInputStream objectInputStream = new ObjectInputStream(Files.newInputStream(HEALTH_CARE_SYSTEM_FILE))) {
            ResidentHealthCareHome home = (ResidentHealthCareHome) objectInputStream.readObject();

            // Restore staff id counter
            int maxStaff = 0;
            for (Staff staff : home.getStaffList().values()) {
                String staffId = staff.getId();
                if (staffId != null && staffId.startsWith("STF")) {
                    try {
                        int num = Integer.parseInt(staffId.substring(5));
                        if (num > maxStaff) maxStaff = num;
                    } catch (Exception ignored) {
                    }
                }
            }
            Staff.setIdCounter(maxStaff);

            // Restore resident and prescription counters
            int maxResidents = 0;
            long maxPrescriptions = 0;

            for (Bed bed : home.getBedList().values()) {
                Resident resident = bed.getResident();
                if (resident == null) continue;

                // Resident ID counter
                String residentId = resident.getId();
                if (residentId != null && residentId.startsWith("RES")) {
                    try {
                        int num = Integer.parseInt(residentId.substring(3));
                        if (num > maxResidents) maxResidents = num;
                    } catch (Exception ignored) {
                    }
                }

                // Prescription ID counter
                List<Prescription> prescriptionList = resident.getPrescriptionList();
                if (prescriptionList != null) {
                    for (Prescription prescription : prescriptionList) {
                        if (prescription == null || prescription.getId() == null) continue;
                        String prescriptionId = prescription.getId();
                        if (prescriptionId.startsWith("PRE")) {
                            try {
                                long num = Long.parseLong(prescriptionId.substring(3));
                                if (num > maxPrescriptions) maxPrescriptions = num;
                            } catch (Exception ignored) {
                            }
                        }
                    }
                }
            }

            Resident.setIdCounter(maxResidents);
            Prescription.setIdCounter(maxPrescriptions);

            System.out.println("Loaded data. Counters restored -> Staff: " + maxStaff + " Resident: " + maxResidents + " Prescription: " + maxPrescriptions);
            return home;

        } catch (Exception ex) {
            System.err.println("Loading existing records failed, creating new one: " + ex.getMessage());
            return new ResidentHealthCareHome();
        }
    }
}
