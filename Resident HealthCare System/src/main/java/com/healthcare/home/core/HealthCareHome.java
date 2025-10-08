package com.healthcare.home.core;

import com.healthcare.home.audit.AuditLog;
import com.healthcare.home.exceptions.*;
import com.healthcare.home.entity.*;
import com.healthcare.home.auth.Access;
import com.healthcare.home.scheduler.Schedule;
import com.healthcare.home.scheduler.Shift;
import com.healthcare.home.staff.*;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.*;

public class HealthCareHome implements Serializable {
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

    public void registerNewStaff(Staff staff) {
        staffList.put(staff.getId(), staff);
        audit.entryLog("System-Generated", Access.ADD_STAFF, "Registered staff with id: " + staff.getId());
    }

    public void addNewStaff(Manager manager, Staff staff) {
        requireAuthorizeManager(manager);
        staffList.put(staff.getId(), staff);
        audit.entryLog(manager.getId(), Access.ADD_STAFF, "Added staff with id: " + staff.getId());
    }

    public void updateStaffPassword(Manager manager, String staffId, String newPass) {
        requireAuthorizeManager(manager);
        Staff t = staffList.get(staffId);
        if (t == null) throw new ValidationFailedException("Staff not found with id: "+ staffId);
        t.setPasswordHash(newPass);
        audit.entryLog(manager.getId(), Access.UPDATE_STAFF, "Updated password for " + staffId);
    }

    public void assigningShift(Manager manager, Staff staff, Shift shift) {
        requireAuthorizeManager(manager);
        scheduler.assigningShiftToStaff(staff, shift);
        audit.entryLog(manager.getId(), Access.SHIFT_ASSIGNMENT, "Assigned shift to " + staff.getId());
    }

    public void assignResidentToBed(Staff staff, String bedId, Resident resident) {
        requireAuthorizeRole(staff, Role.NURSE);
        requireOnDutyStaff(staff);
        Bed bed = findBed(bedId);
        if (!bed.isVacant()) throw new BedNotAvailableException("Bed occupied with bed id: " + bedId);
        bed.setResident(resident);
        residentList.put(resident.getId(), resident);
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

    public Resident getResidentDetails(Staff staffMember, String bedId) {
        requireOnDutyStaff(staffMember);
        Bed bed = findBed(bedId);
        return bed.getResident();
    }

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

    public void modifyPrescription(Doctor doctor, String preId, String medicine, String dose) {
        requireAuthorizeRole(doctor, Role.DOCTOR);
        requireOnDutyStaff(doctor);
        Prescription prescription = prescriptionList.get(preId);
        if (prescription == null) throw new ValidationFailedException("Prescription not found");
        prescription.setMedicine(medicine);
        prescription.setDose(dose);
        audit.entryLog(doctor.getId(), Access.UPDATE_PRESCRIPTION, "Updated prescription " + preId);
    }

    public void administerMedication(Nurse nurse, String preId, String dose) {
        requireAuthorizeRole(nurse, Role.NURSE);
        requireOnDutyStaff(nurse);
        Prescription prescription = prescriptionList.get(preId);
        if (prescription == null) throw new ValidationFailedException("Prescription not found");
        Medication medication = new Medication(prescription.getId(), nurse.getId(), LocalDateTime.now(), dose);
        prescription.getAdministrations().add(medication);
        audit.entryLog(nurse.getId(), Access.ADMINISTER_MEDICATION, "Administered " + dose + " for " + prescription.getId());
    }

    public void updateAdministration(Nurse nurse, String preId, int index, String updatedDose) {
        requireAuthorizeRole(nurse, Role.NURSE);
        requireOnDutyStaff(nurse);
        Prescription prescription = prescriptionList.get(preId);
        if (prescription == null) throw new ValidationFailedException("Prescription not found");
        if (index < 0 || index >= prescription.getAdministrations().size())
            throw new ValidationFailedException("Administration entry not found");
        Medication old = prescription.getAdministrations().get(index);
        Medication updatedMedication = new Medication(old.getPrescriptionId(), nurse.getId(), old.getAt(), updatedDose);
        prescription.getAdministrations().set(index, updatedMedication);
        audit.entryLog(nurse.getId(), Access.UPDATE_PRESCRIPTION, "Updated administration for " + preId);
    }

    public void dischargeResident(Staff staffMember, String bedId) {
        requireOnDutyStaff(staffMember);
        Bed bed = findBed(bedId);
        Resident resident = bed.getResident();
        if (resident == null) throw new ResidentNotFoundException("No resident");
        SerializingService.save(resident);
        bed.setResident(null);
        residentList.remove(resident.getId());
        audit.entryLog(staffMember.getId(), Access.DISCHARGE, "Discharged " + resident.getId());
    }

    private void requireAuthorizeManager(Staff staff) {
        if (!(staff instanceof Manager)) throw new UnAuthorizationException("Manager authorized only");
    }

    private void requireAuthorizeRole(Staff staff, Role role) {
        if (!staff.getRole().equals(role)) throw new UnAuthorizationException("Requires authorized role " + role);
    }

    private void requireOnDutyStaff(Staff s) {
        if (!scheduler.isAvailableOnDuty(s, LocalDateTime.now()))
            throw new UnAuthorizationException("Staff not rostered at this time");
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

    public Staff authenticate(String username, String password) {

        Map<String, Staff> staffMap = getStaffList();
        for (int i = 0; i < staffMap.size(); i++) {
            Staff staff = staffMap.get(i);
            if (staff.getUsername().equals(username) && staff.getPasswordHash().equals(password)) {
                return staff;
            }
        }
        return null;
    }
}
