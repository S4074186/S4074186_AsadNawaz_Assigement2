package com.healthcare.home;

import com.healthcare.home.core.HealthCareHome;
import com.healthcare.home.core.SerializingService;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {
    public static void main(String[] args) {
//        HealthCareHome home = SerializingService.readOrCreateFile();
//        // By-Default added staff
//        Manager manager = new Manager("M1", "Manager", "admin", "MANAGER-PASSWORD");
//        home.registerNewStaff(manager);
//        Doctor doctor = new Doctor("D1", "Doctor", "doctor", "DOCTOR-PASSWORD");
//        home.registerNewStaff(doctor);
//        Nurse nurse = new Nurse("N1", "Nurse", "nurse", "NURSE-PASSWORD");
//        home.registerNewStaff(nurse);
//
//        // By-Default assigning shifts
//        LocalDateTime now = LocalDateTime.now();
//        home.assigningShift(manager, doctor, new Shift(now, now.plusHours(1)));
//        home.assigningShift(manager, nurse, new Shift(now, now.plusHours(8)));
//        Scanner sc = new Scanner(System.in);
//        while (true) {
//            System.out.println("\n--- RMIT Care Home ---");
//            System.out.println("1) List Beds");
//            System.out.println("2) Add Resident to Bed (Nurse)");
//            System.out.println("3) Move Resident (Nurse)");
//            System.out.println("4) Add Staff (Manager)");
//            System.out.println("5) Assign Shift (Manager)");
//            System.out.println("6) Doctor: Add Prescription");
//            System.out.println("7) Nurse: Administer Medication");
//            System.out.println("8) Show Audit Log");
//            System.out.println("9) Discharge Resident");
//            System.out.println("0) Save & Exit");
//            System.out.print("> ");
//            String c = sc.nextLine().trim();
//            try {
//                switch (c) {
//                    case "1":
//                        home.getBedList().values().forEach(System.out::println);
//                        break;
//                    case "2": {
//                        System.out.print("Nurse ID: ");
//                        String nurseId = sc.nextLine();
//                        System.out.print("Bed ID: ");
//                        String bedId = sc.nextLine();
//                        System.out.print("Resident ID: ");
//                        String residentId = sc.nextLine();
//                        System.out.print("Resident Name: ");
//                        String residentName = sc.nextLine();
//                        System.out.print("Enter Gender: ");
//                        String gender = sc.nextLine();
//                        System.out.print("Is Isolation Required: (Y/N)");
//                        String isolation = sc.nextLine();
//                        Staff staff = home.getStaffList().get(nurseId);
//                        home.assignResidentToBed(staff, bedId,
//                                new Resident(residentId, residentName,
//                                "M".equalsIgnoreCase(gender) ? Gender.MALE : Gender.FEMALE,
//                                "Y".equalsIgnoreCase(isolation)));
//                        System.out.println("Added");
//                    }
//                    break;
//                    case "3": {
//                        System.out.print("Nurse ID: ");
//                        String nurseId = sc.nextLine();
//                        System.out.print("From Bed: ");
//                        String from = sc.nextLine();
//                        System.out.print("To Bed: ");
//                        String to = sc.nextLine();
//                        Staff staff = home.getStaffList().get(nurseId);
//                        home.moveResidentToNewBed(staff, from, to);
//                        System.out.println("Moved");
//                    }
//                    break;
//                    case "4": {
//                        System.out.print("Manager ID: ");
//                        String managerId = sc.nextLine();
//                        System.out.print("New staff id: ");
//                        String staffId = sc.nextLine();
//                        System.out.print("Name: ");
//                        String staffName = sc.nextLine();
//                        System.out.print("Role (DOCTOR/NURSE): ");
//                        String role = sc.nextLine().trim().toUpperCase();
//                        Staff man = home.getStaffList().get(managerId);
//                        Staff newStaff = role.equals("DOCTOR") ? new Doctor(staffId, staffName, "pass", "DOCTOR-PASSWORD") : new Nurse(staffId, staffName, "pass", "NURSE-PASSWORD");
//                        home.addNewStaff((Manager) man, newStaff);
//                        System.out.println("Staff added");
//                    }
//                    break;
//                    case "5": {
//                        System.out.print("Manager ID: ");
//                        String managerId = sc.nextLine();
//                        System.out.print("Staff ID: ");
//                        String staffId = sc.nextLine();
//                        System.out.print("Shift start (yyyy-MM-ddTHH:mm): ");
//                        String shiftStart = sc.nextLine();
//                        System.out.print("Shift end (yyyy-MM-ddTHH:mm): ");
//                        String shiftEnd = sc.nextLine();
//                        Staff man = home.getStaffList().get(managerId);
//                        Staff staff = home.getStaffList().get(staffId);
//                        Shift shift = new Shift(LocalDateTime.parse(shiftStart), LocalDateTime.parse(shiftEnd));
//                        home.assigningShift((Manager) man, staff, shift);
//                        System.out.println("Assigned");
//                    }
//                    break;
//                    case "6": {
//                        System.out.print("Doctor ID: ");
//                        String doctorId = sc.nextLine();
//                        System.out.print("Bed ID: ");
//                        String bedId = sc.nextLine();
//                        System.out.print("Prescription ID: ");
//                        String prescriptionId = sc.nextLine();
//                        System.out.print("Medicine name: ");
//                        String medicineName = sc.nextLine();
//                        System.out.print("Dose text: ");
//                        String dose = sc.nextLine();
//                        System.out.print("Times (comma HH:mm): ");
//                        String times = sc.nextLine();
//                        List<java.time.LocalTime> timelist = new ArrayList<>();
//                        for (String time : times.split(","))
//                            if (!time.isBlank()) timelist.add(java.time.LocalTime.parse(time.trim()));
//                        Doctor doc = (Doctor) home.getStaffList().get(doctorId);
//                        Bed bed = home.getBedList().get(bedId);
//                        if (bed == null || bed.getResident() == null) System.out.println("No resident in bed");
//                        else {
//                            Prescription prescription = new Prescription(prescriptionId, doc.getId(), bed.getResident().getId(), medicineName, dose, timelist);
//                            home.writePrescription(doc, bedId, prescription);
//                            System.out.println("Prescription added");
//                        }
//                    }
//                    break;
//                    case "7": {
//                        System.out.print("Nurse ID: ");
//                        String nurseId = sc.nextLine();
//                        System.out.print("Prescription ID: ");
//                        String prescriptionId = sc.nextLine();
//                        System.out.print("Dose: ");
//                        String dose = sc.nextLine();
//                        home.administerMedication((Nurse) home.getStaffList().get(nurseId), prescriptionId, dose);
//                        System.out.println("Administered");
//                    }
//                    break;
//                    case "8": {
//                        List<com.healthcare.home.audit.AuditLog.Entry> entries = com.healthcare.home.audit.AuditLog.get().getEntries();
//                        entries.forEach(e -> System.out.println(e.at() + " | " + e.staffId() + " | " + e.action() + " | " + e.message()));
//                    }
//                    break;
//                    case "9": {
//                        System.out.print("Actor staff id: ");
//                        String staffId = sc.nextLine();
//                        System.out.print("Bed id to discharge: ");
//                        String bedId = sc.nextLine();
//                        home.dischargeResident(home.getStaffList().get(staffId), bedId);
//                        System.out.println("Discharged");
//                    }
//                    break;
//                    case "0": {
//                        SerializingService.save(home);
//                        System.out.println("All records saved in external file");
//                        return;
//                    }
//                    default:
//                        System.out.println("Unknown");
//                }
//            } catch (Exception ex) {
//                System.err.println("Error: " + ex.getMessage());
//            }
//        }
//    }

    launch(args);
    }

    private static HealthCareHome home;

    public static HealthCareHome getHome() {
        return home;
    }

    public static void setHome(HealthCareHome home) {
        Main.home = home;
    }

    @Override
    public void start (Stage stage) throws Exception {
        home = SerializingService.readOrCreateFile();
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/healthcare/home/view/login.fxml"));
        Scene scene = new Scene(loader.load());
        stage.setTitle("Resident Health-Care System");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();
    }

    @Override
    public void stop () {
        SerializingService.saveRecordsInFile(home);
    }

}
