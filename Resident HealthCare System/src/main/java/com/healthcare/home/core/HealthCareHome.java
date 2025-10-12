package com.healthcare.home.core;

import com.healthcare.home.audit.AuditLog;
import com.healthcare.home.exceptions.*;
import com.healthcare.home.entity.*;
import com.healthcare.home.auth.Access;
import com.healthcare.home.scheduler.Schedule;
import com.healthcare.home.scheduler.Shift;
import com.healthcare.home.staff.*;

import java.io.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Central application model for the care home.
 *
 * Notes:
 *  - Controllers call the methods below:
 *      - getResidentDetails(...)  -> Manager/Doctor/Nurse UI to view resident details
 *      - assignResidentToBed(...) -> Manager UI or Nurse UI (when rostered)
 *      - moveResidentToNewBed(...)-> Nurse UI (Move button)
 *      - writePrescription(...)   -> Doctor UI (onPrescribe)
 *      - administerMedication(...)-> Nurse UI (onAdminister)
 *      - updateAdministration(...)-> Nurse UI (edit an administration entry)
 *      - modifyPrescription(...)  -> Doctor UI (edit prescription)
 *      - dischargeResident(...)   -> Manager UI (or nurse if rostered, as per rules)
 *      - checkCompliance()        -> call after shifts are assigned or on admin action
 */
public class HealthCareHome implements Serializable {
    private static final long serialVersionUID = 1L;

    private final Map<String, Bed> bedList = new HashMap<>();
    private final Map<String, Staff> staffList = new HashMap<>();
    private final Map<String, Resident> residentList = new HashMap<>();
    private final Map<String, Prescription> prescriptionList = new HashMap<>();
    private final Schedule scheduler = new Schedule();
    private final AuditLog audit = AuditLog.get();

    public HealthCareHome() {
        bedList.put("W1-R101-B1", new Bed("W1-R101-B1"));
        bedList.put("W1-R101-B2", new Bed("W1-R101-B2"));
        bedList.put("W1-R101-B3", new Bed("W1-R101-B3"));
        bedList.put("W1-R101-B4", new Bed("W1-R101-B4"));
        bedList.put("W1-R102-B1", new Bed("W1-R102-B1"));
        bedList.put("W1-R102-B2", new Bed("W1-R102-B2"));
        bedList.put("W1-R103-B1", new Bed("W1-R103-B1"));
        bedList.put("W2-R201-B1", new Bed("W2-R201-B1"));
        bedList.put("W2-R201-B2", new Bed("W2-R201-B2"));
        bedList.put("W2-R201-B3", new Bed("W2-R201-B3"));
        bedList.put("W2-R201-B4", new Bed("W2-R201-B4"));
        bedList.put("W2-R202-B1", new Bed("W2-R202-B1"));
        bedList.put("W2-R202-B2", new Bed("W2-R202-B2"));
        bedList.put("W2-R203-B1", new Bed("W2-R203-B1"));

        // adding manager in staff (system default)
        Manager manager = new Manager("Manager", "admin", "MANAGER-PASSWORD");
        staffList.put(manager.getId(), manager);
    }

    // --------------------------
    // Staff registration helpers
    // --------------------------

    public void registerNewStaff(Staff staff) {
        staffList.put(staff.getId(), staff);
        audit.entryLog("SystemGenerated", Access.ADD_STAFF, "Registered staff with id: " + staff.getId());
//        assignDefaultShifts(staff);
    }

    public void addNewStaff(Staff manager, Staff staff) {
        requireAuthorizeManager(manager);
        staffList.put(staff.getId(), staff);
        audit.entryLog(manager.getId(), Access.ADD_STAFF, "Added staff with id: " + staff.getId());
        assignDefaultShifts(staff);
    }

    private void assignDefaultShifts(Staff staff) {
        if (staff instanceof Nurse) {
            List<Nurse> nurses = new ArrayList<>();
            for (Staff s : staffList.values()) if (s instanceof Nurse) nurses.add((Nurse) s);

            int nurseIndex = nurses.indexOf(staff);
            boolean isMorningShift = nurseIndex % 2 == 0;

            for (java.time.DayOfWeek day : java.time.DayOfWeek.values()) {
                LocalDate date = LocalDate.now().with(day);

                LocalDateTime start = isMorningShift ? date.atTime(8, 0) : date.atTime(14, 0);
                LocalDateTime end = isMorningShift ? date.atTime(16, 0) : date.atTime(22, 0);

                scheduler.assigningShiftToStaff(staff, new Shift(start, end));
            }

        } else if (staff instanceof Doctor) {
            for (java.time.DayOfWeek day : java.time.DayOfWeek.values()) {
                LocalDate date = LocalDate.now().with(day);
                LocalDateTime start = date.atTime(10, 0);
                LocalDateTime end = start.plusHours(1);
                scheduler.assigningShiftToStaff(staff, new Shift(start, end));
            }
        }
    }


    /**
     * Validate roster rules:
     * - Nurses: max 8 hours per day
     * - Doctors: exactly 1 hour per day (as required)
     *
     * Where to call: after assigning shifts, during startup validation, or via Manager UI ("Validate Roster")
     */
    public void checkCompliance() {
        // NOTE: relies on Schedule exposing a way to get all shifts for staff.
        // If Schedule has getDailyRoster() or getShiftsForStaff(String staffId) use that.
        // Here we assume Schedule has a method Map<String, List<Shift>> getDailyRoster()
        Map<String, List<Shift>> roster = scheduler.getDailyRoster(); // ensure Schedule provides this method

        for (Map.Entry<String, List<Shift>> entry : roster.entrySet()) {
            String staffId = entry.getKey();
            Staff s = staffList.get(staffId);
            if (s == null) continue;

            // group by date
            Map<LocalDate, Long> hoursByDate = new HashMap<>();
            for (Shift sh : entry.getValue()) {
                LocalDate date = (LocalDate) sh.getStart().toLocalDate();
                long hours = sh.hours();
                hoursByDate.merge(date, hours, Long::sum);
            }

            if (s instanceof Nurse) {
                for (Map.Entry<LocalDate, Long> e : hoursByDate.entrySet()) {
                    if (e.getValue() > 8) {
                        throw new RosterUnfollowedException("Nurse " + s.getId() + " has more than 8 hours on " + e.getKey());
                    }
                }
            } else if (s instanceof Doctor) {
                for (Map.Entry<LocalDate, Long> e : hoursByDate.entrySet()) {
                    if (!e.getValue().equals(1L)) {
                        throw new RosterUnfollowedException("Doctor " + s.getId() + " must have 1-hour shift daily (found " + e.getValue() + " on " + e.getKey() + ")");
                    }
                }
            }
        }

        // Additional check: ensure total nurse shifts per week equals 14 across roster (2 per day * 7)
        long totalNurseShifts = roster.values().stream()
                .flatMap(List::stream)
                .filter(shift -> {
                    // find staff for shift and check if nurse
                    // we must map shift -> staffId to determine staff role; this depends on Schedule internals.
                    return true; // skip here; optional detailed check can be added if Schedule provides assignment mapping
                }).count();
        // optional: implement detailed count if schedule exposes assignments per day
    }

    // --------------------------
    // Staff / shift management
    // --------------------------

    public void assigningShift(Staff manager, Staff staff, Shift shift) {
        requireAuthorizeManager(manager);
        scheduler.assigningShiftToStaff(staff, shift);
        audit.entryLog(manager.getId(), Access.SHIFT_ASSIGNMENT, "Assigned shift to " + staff.getId());
    }

    public void updateStaffPassword(Staff manager, String staffId, String newPass) {
        requireAuthorizeManager(manager);
        Staff t = staffList.get(staffId);
        if (t == null) throw new ValidationFailedException("Staff not found with id: " + staffId);
        t.setPasswordHash(newPass);
        audit.entryLog(manager.getId(), Access.UPDATE_STAFF, "Updated password for " + staffId);
    }

    // --------------------------
    // Resident / Bed operations
    // --------------------------

    public void assignResidentToBed(Staff staff, String bedId, Resident resident) {
        Bed bed = findBed(bedId);
        if (!bed.isVacant()) throw new BedNotAvailableException("Bed occupied with bed id: " + bedId);

        if (staff instanceof Manager) {
            // manager allowed without roster check
        } else {
            requireAuthorizeRole(staff, Role.NURSE);
            requireOnDutyStaff(staff);
        }

        bed.setResident(resident);
        if (resident.getId() != null) residentList.put(resident.getId(), resident);
        audit.entryLog(staff.getId(), Access.ADD_RESIDENT, "Assigned resident " + resident.getId() + " to " + bedId);
    }

    public void moveResidentToNewBed(Staff staff, String fromBedId, String toBedId) {
        requireAuthorizeRole(staff, Role.NURSE);
        requireOnDutyStaff(staff);

        Bed from = findBed(fromBedId);
        Bed to = findBed(toBedId);

        if (from.getResident() == null) throw new ResidentNotFoundException("No resident in source bed");
        if (!to.isVacant()) throw new BedNotAvailableException("Destination occupied");

        Resident resident = from.getResident();
        from.setResident(null);
        to.setResident(resident);
        audit.entryLog(staff.getId(), Access.MOVE_RESIDENT, "Moved resident " + resident.getId() + " from " + fromBedId + " to " + toBedId);
    }

    /**
     * Return resident details for display.
     * Controllers: call this to populate resident details view.
     */
    public Resident getResidentDetails(Staff staffMember, String bedId) {
        Bed bed = findBed(bedId);
        if (bed == null) throw new ValidationFailedException("Bed not found: " + bedId);

        // manager can always view
        if (!(staffMember instanceof Manager)) {
            Role r = staffMember.getRole();
            if (!(Role.DOCTOR.equals(r) || Role.NURSE.equals(r))) {
                throw new UnAuthorizationException("Only medical staff or manager can view resident details");
            }
            requireOnDutyStaff(staffMember); // nurse or doctor must be on duty
        }

        Resident resident = bed.getResident();
        audit.entryLog(staffMember.getId(), Access.VIEW_RESIDENT, "Viewed resident at bed " + bedId);
        return resident;
    }

    public Resident getResidentInBed(String bedId) {
        Bed bed = bedList.get(bedId);
        if (bed == null) return null;
        return bed.getResident();
    }

    // --------------------------
    // Prescription / Medication
    // --------------------------

    /**
     * Add one or more prescriptions for resident in bed.
     * Controllers: call this from Doctor Dashboard when saving prescriptions.
     */
    public void writePrescription(Staff doctor, String bedId, List<Prescription> newPrescriptions) {
        requireAuthorizeRole(doctor, Role.DOCTOR);
        requireOnDutyStaff(doctor);

        Bed bed = findBed(bedId);
        Resident resident = bed.getResident();
        if (resident == null) throw new ResidentNotFoundException("No resident in bed");

        List<Prescription> existing = resident.getPrescriptionList();
        if (existing == null) existing = new ArrayList<>();

        existing.addAll(newPrescriptions);
        resident.setPrescriptionList(existing);

        // register each prescription in system-wide map for direct lookup by id
        for (Prescription p : newPrescriptions) {
            if (p.getId() != null) prescriptionList.put(p.getId(), p);
            audit.entryLog(doctor.getId(), Access.WRITE_PRESCRIPTION, "Prescription " + p.getId() + " added for resident " + resident.getId());
        }
    }

    /**
     * Doctor modifies a prescription's med/dose text.
     * Controllers: call this from a Doctor edit prescription UI.
     */
    public void modifyPrescription(Staff doctor, String preId, String medicine, String dose) {
        requireAuthorizeRole(doctor, Role.DOCTOR);
        requireOnDutyStaff(doctor);

        Prescription prescription = prescriptionList.get(preId);
        if (prescription == null) throw new ValidationFailedException("Prescription not found");

        prescription.setMedicine(medicine);
        prescription.setDose(dose);
        audit.entryLog(doctor.getId(), Access.UPDATE_PRESCRIPTION, "Updated prescription " + preId);
    }

    /**
     * Nurse administers medication for a prescription id.
     * Controllers: NurseDashboardController.onAdminister() should call this (no manual prescription id input required
     * if you determine prescription from selected bed; pass that id here).
     */
    public void administerMedication(Staff nurse, String preId, String doseGiven) {
        requireAuthorizeRole(nurse, Role.NURSE);
        requireOnDutyStaff(nurse);

        Prescription prescription = prescriptionList.get(preId);
        if (prescription == null) throw new ValidationFailedException("Prescription not found");

        Medication medication = new Medication(prescription.getId(), nurse.getId(), LocalDateTime.now(), doseGiven);
        prescription.getAdministrations().add(medication);

        audit.entryLog(nurse.getId(), Access.ADMINISTER_MEDICATION, "Administered " + doseGiven + " for " + prescription.getId());
    }

    /**
     * Nurse updates a previous administration entry for a prescription.
     * Controllers: call this from a UI that allows editing an administration record (index must be known).
     */
    public void updateAdministration(Staff nurse, String preId, int index, String updatedDose) {
        requireAuthorizeRole(nurse, Role.NURSE);
        requireOnDutyStaff(nurse);

        Prescription prescription = prescriptionList.get(preId);
        if (prescription == null) throw new ValidationFailedException("Prescription not found");

        if (index < 0 || index >= prescription.getAdministrations().size())
            throw new ValidationFailedException("Administration entry not found");

        Medication old = prescription.getAdministrations().get(index);
        Medication updatedMedication = new Medication(old.getPrescriptionId(), nurse.getId(), old.getAt(), updatedDose);
        prescription.getAdministrations().set(index, updatedMedication);

        audit.entryLog(nurse.getId(), Access.UPDATE_PRESCRIPTION, "Updated administration for " + preId + " index " + index);
    }

    // --------------------------
    // Discharge
    // --------------------------

    /**
     * Discharge resident.
     * - Manager can always discharge.
     * - Nurse can discharge only when on duty (business rule).
     * Controllers: call this from Manager dashboard discharge action (or Nurse UI if you allow).
     */
    public void dischargeResident(Staff staffMember, String bedId) {
        if (!(staffMember instanceof Manager)) {
            // nurses may be allowed if rostered
            requireAuthorizeRole(staffMember, Role.NURSE);
            requireOnDutyStaff(staffMember);
        }

        Bed bed = findBed(bedId);
        Resident resident = bed.getResident();
        if (resident == null) throw new ResidentNotFoundException("No resident");

        // archive resident to a per resident file (archive_{id}.dat)
        try {
            archiveResident(resident);
        } catch (Exception ex) {
            audit.entryLog(staffMember.getId(), Access.DISCHARGE_RESIDENT, "Archive failed for " + resident.getId() + " error " + ex.getMessage());
            ex.printStackTrace();
        }

        // clear bed and maps
        bed.setResident(null);
        if (resident.getId() != null) residentList.remove(resident.getId());

        // also remove associated prescriptions from system map
        List<Prescription> pres = resident.getPrescriptionList();
        if (pres != null) {
            for (Prescription p : pres) {
                if (p != null && p.getId() != null) prescriptionList.remove(p.getId());
            }
        }

        audit.entryLog(staffMember.getId(), Access.DISCHARGE_RESIDENT, "Discharged " + resident.getId() + " from bed " + bedId);
    }

    private void archiveResident(Resident resident) throws IOException {
        String filename = "archive_" + resident.getId() + ".dat";
        try (FileOutputStream fos = new FileOutputStream(filename);
             ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            oos.writeObject(resident);
            List<Prescription> pres = resident.getPrescriptionList();
            oos.writeObject(pres);
            oos.flush();
        }
    }

    // --------------------------
    // Persistence / helpers
    // --------------------------

    public void saveAllStateToFile(String path) {
        if (path == null || path.trim().isEmpty()) path = "healthcarehome_state.dat";
        try (FileOutputStream fos = new FileOutputStream(path);
             ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            oos.writeObject(this);
            oos.flush();
            audit.entryLog("System", Access.ADD_RESIDENT, "Saved system state to " + path);
        } catch (Exception ex) {
            audit.entryLog("System", Access.UPDATE_STAFF, "Failed to save system state " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    public Bed findBed(String id) {
        Bed bed = bedList.get(id);
        if (bed == null) throw new ValidationFailedException("Bed not found: " + id);
        return bed;
    }

    public Map<String, Bed> getBedList() {
        return Map.copyOf(bedList);
    }

    public Map<String, Staff> getStaffList() {
        return Map.copyOf(staffList);
    }

    public Map<String, Resident> getAllResidents() {
        return Map.copyOf(residentList);
    }

    public Map<String, Prescription> getPrescriptionList() {
        return Map.copyOf(prescriptionList);
    }

    public Schedule getScheduler() {
        return scheduler;
    }

    public AuditLog getAudit() {
        return audit;
    }

    public Staff authenticate(String username, String password) {
        Collection<Staff> staffList = getStaffList().values();
        for (Staff staff : staffList) {
            if (staff.getUsername().equals(username) && staff.getPasswordHash().equals(password)) {
                audit.entryLog(staff.getId(), Access.VIEW_RESIDENT, "Authenticated");
                return staff;
            }
        }
        return null;
    }

    // --------------------------
    // Role checks
    // --------------------------

    private void requireAuthorizeManager(Staff staff) {
        if (!(staff instanceof Manager)) throw new UnAuthorizationException("Manager authorized only");
    }

    private void requireAuthorizeRole(Staff staff, Role role) {
        if (staff == null) throw new UnAuthorizationException("No staff provided");
        if (!staff.getRole().equals(role)) throw new UnAuthorizationException("Requires authorized role " + role);
    }

    private void requireOnDutyStaff(Staff s) {
        if (!scheduler.isAvailableOnDuty(s, LocalDateTime.now()))
            throw new UnAuthorizationException("Staff not rostered at this time");
    }
}
