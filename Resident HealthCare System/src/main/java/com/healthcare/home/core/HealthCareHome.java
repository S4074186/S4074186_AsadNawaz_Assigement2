package com.healthcare.home.core;

import com.healthcare.home.audit.AuditLog;
import com.healthcare.home.exceptions.*;
import com.healthcare.home.entity.*;
import com.healthcare.home.auth.Access;
import com.healthcare.home.scheduler.Schedule;
import com.healthcare.home.scheduler.Shift;
import com.healthcare.home.staff.*;

import java.io.*;
import java.time.LocalDateTime;
import java.util.*;

/**
 Updated HealthCareHome to meet GUI functional requirements and role based checks.
 Notes
 1. Manager can add resident to vacant bed.
 2. Nurse can add resident when rostered and can move resident when rostered.
 3. Doctor and nurse can view resident when rostered. Manager can view anytime.
 4. All actions are logged with audit.entryLog including staff id and timestamp.
 5. State save method saves this object to a file for restore before exit.
 6. Discharge archives resident data to a per resident archive file and logs the action.
 7. Methods throw existing exceptions for unauthorized or invalid operations.
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

        // adding manager in staff
        Manager manager = new Manager("MGR1", "Manager", "admin", "MANAGER-PASSWORD");
        staffList.put(manager.getId(), manager);
    }

    /**
     Simple register that logs system generated staff addition.
     */
    public void registerNewStaff(Staff staff) {
        staffList.put(staff.getId(), staff);
        audit.entryLog("SystemGenerated", Access.ADD_STAFF, "Registered staff with id: " + staff.getId());
    }

    /**
     Manager adds a staff member. This is a manager only operation.
     */
    public void addNewStaff(Staff manager, Staff staff) {
        requireAuthorizeManager(manager);
        staffList.put(staff.getId(), staff);
        audit.entryLog(manager.getId(), Access.ADD_STAFF, "Added staff with id: " + staff.getId());
    }

    /**
     Manager updates staff password.
     */
    public void updateStaffPassword(Staff manager, String staffId, String newPass) {
        requireAuthorizeManager(manager);
        Staff t = staffList.get(staffId);
        if (t == null) throw new ValidationFailedException("Staff not found with id: " + staffId);
        t.setPasswordHash(newPass);
        audit.entryLog(manager.getId(), Access.UPDATE_STAFF, "Updated password for " + staffId);
    }

    /**
     Manager assigns shift to staff. Only manager allowed.
     */
    public void assigningShift(Staff manager, Staff staff, Shift shift) {
        requireAuthorizeManager(manager);
        scheduler.assigningShiftToStaff(staff, shift);
        audit.entryLog(manager.getId(), Access.SHIFT_ASSIGNMENT, "Assigned shift to " + staff.getId());
    }

    /**
     Assign resident to bed.
     Rules
     - Manager can assign resident to any vacant bed without roster check.
     - Nurse can assign resident only when rostered now.
     */
    public void assignResidentToBed(Staff staff, String bedId, Resident resident) {
        Bed bed = findBed(bedId);
        if (!bed.isVacant()) throw new BedNotAvailableException("Bed occupied with bed id: " + bedId);

        if (staff instanceof Manager) {
            // manager allowed without roster check
        } else {
            // only nurse allowed and must be on duty
            requireAuthorizeRole(staff, Role.NURSE);
            requireOnDutyStaff(staff);
        }

        bed.setResident(resident);
        residentList.put(resident.getId(), resident);
        audit.entryLog(staff.getId(), Access.ADD_RESIDENT, "Assigned resident " + resident.getId() + " to " + bedId);
    }

    /**
     Nurse moves resident from one bed to another.
     Nurse must be rostered.
     */
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
     Get resident details.
     Rules
     - Manager can view anytime.
     - Doctor and nurse can view only when rostered now.
     */
    public Resident getResidentDetails(Staff staffMember, String bedId) {
        Bed bed = findBed(bedId);
        if (bed == null) throw new ValidationFailedException("Bed not found: " + bedId);

        if (staffMember instanceof Manager) {
            // allowed
        } else {
            // only medical staff allowed when rostered
            Role r = staffMember.getRole();
            if (!(Role.DOCTOR.equals(r) || Role.NURSE.equals(r))) {
                throw new UnAuthorizationException("Only medical staff or manager can view resident details");
            }
            requireOnDutyStaff(staffMember);
        }

        Resident resident = bed.getResident();
        audit.entryLog(staffMember.getId(), Access.VIEW_RESIDENT, "Viewed resident at bed " + bedId);
        return resident;
    }

    /**
     Doctor writes prescription.
     Doctor must be rostered.
     */
    public void writePrescription(Staff doctor, String bedId, Prescription prescription) {
        requireAuthorizeRole(doctor, Role.DOCTOR);
        requireOnDutyStaff(doctor);

        Bed bed = findBed(bedId);
        Resident resident = bed.getResident();
        if (resident == null) throw new ResidentNotFoundException("No resident in bed");

        prescriptionList.put(prescription.getId(), prescription);
        resident.getPrescriptions().add(prescription);
        audit.entryLog(doctor.getId(), Access.WRITE_PRESCRIPTION, "Prescription " + prescription.getId() + " added for resident " + resident.getId());
    }

    /**
     Doctor updates prescription content.
     Doctor must be rostered.
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
     Nurse administers medication.
     Nurse must be rostered.
     */
    public void administerMedication(Staff nurse, String preId, String dose) {
        requireAuthorizeRole(nurse, Role.NURSE);
        requireOnDutyStaff(nurse);

        Prescription prescription = prescriptionList.get(preId);
        if (prescription == null) throw new ValidationFailedException("Prescription not found");

        Medication medication = new Medication(prescription.getId(), nurse.getId(), LocalDateTime.now(), dose);
        prescription.getAdministrations().add(medication);
        audit.entryLog(nurse.getId(), Access.ADMINISTER_MEDICATION, "Administered " + dose + " for " + prescription.getId());
    }

    /**
     Nurse updates a previous administration record.
     Nurse must be rostered.
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

    /**
     Discharge resident.
     Rules
     - Manager can discharge anytime.
     - Nurse can discharge when rostered.
     Action
     - Archive resident full record to file for audit.
     - Remove resident from bed and resident map.
     */
    public void dischargeResident(Staff staffMember, String bedId) {
        // allow manager always
        if (staffMember instanceof Manager) {
            // allowed
        } else {
            requireAuthorizeRole(staffMember, Role.NURSE);
            requireOnDutyStaff(staffMember);
        }

        Bed bed = findBed(bedId);
        Resident resident = bed.getResident();
        if (resident == null) throw new ResidentNotFoundException("No resident");

        // archive resident to a per resident file
        try {
            archiveResident(resident);
        } catch (Exception ex) {
            // do not block discharge if archive fails, still log the error
            audit.entryLog(staffMember.getId(), Access.DISCHARGE, "Archive failed for " + resident.getId() + " error " + ex.getMessage());
            ex.printStackTrace();
        }

        // remove resident
        bed.setResident(null);
        residentList.remove(resident.getId());

        audit.entryLog(staffMember.getId(), Access.DISCHARGE, "Discharged " + resident.getId() + " from bed " + bedId);
    }

    /**
     Helper to archive resident object and related prescriptions and administrations.
     Writes an archive file named archive_{residentId}.dat in working dir
     */
    private void archiveResident(Resident resident) throws IOException {
        String filename = "archive_" + resident.getId() + ".dat";
        try (FileOutputStream fos = new FileOutputStream(filename);
             ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            // write resident object
            oos.writeObject(resident);
            // also write all prescriptions for resident for completeness
            List<Prescription> pres = resident.getPrescriptions();
            oos.writeObject(pres);
            oos.flush();
        }
    }

    /**
     Save entire state to disk for restore on next startup.
     Call this before application exit.
     */
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

    /**
     Find bed or throw validation exception.
     */
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

    /**
     Authenticate by username and password.
     Returns staff or null.
     */
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

    /**
     Manager check helper.
     */
    private void requireAuthorizeManager(Staff staff) {
        if (!(staff instanceof Manager)) throw new UnAuthorizationException("Manager authorized only");
    }

    /**
     Role check helper.
     */
    private void requireAuthorizeRole(Staff staff, Role role) {
        if (staff == null) throw new UnAuthorizationException("No staff provided");
        if (!staff.getRole().equals(role)) throw new UnAuthorizationException("Requires authorized role " + role);
    }

    /**
     On duty check using scheduler.
     */
    private void requireOnDutyStaff(Staff s) {
        if (!scheduler.isAvailableOnDuty(s, LocalDateTime.now()))
            throw new UnAuthorizationException("Staff not rostered at this time");
    }
}
