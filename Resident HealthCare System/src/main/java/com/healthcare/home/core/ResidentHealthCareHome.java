package com.healthcare.home.core;

import com.healthcare.home.audit.AuditTrailLog;
import com.healthcare.home.exceptions.*;
import com.healthcare.home.entities.*;
import com.healthcare.home.auth.AuthAccess;
import com.healthcare.home.scheduler.Scheduler;
import com.healthcare.home.entities.Shift;
import com.healthcare.home.staff.*;
import lombok.Data;

import java.io.*;
import java.time.*;
import java.util.*;

/**
 * ResidentHealthCareHome
 */
@Data
public class ResidentHealthCareHome implements Serializable {
    private static final long serialVersionUID = 1L;

    private final Map<String, Bed> bedList = new HashMap<>();
    private final Map<String, Staff> staffList = new HashMap<>();
    private final Map<String, Resident> residentList = new HashMap<>();
    private final Map<String, Prescription> prescriptionList = new HashMap<>();
    private final Scheduler scheduler = new Scheduler();
    private final AuditTrailLog auditTrailLog = AuditTrailLog.get();

    /**
     * ResidentHealthCareHome Constructor
     */
    public ResidentHealthCareHome() {
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

        Manager manager = new Manager("Manager", "admin", "MANAGER-PASSWORD");
        staffList.put(manager.getId(), manager);
    }

    /**
     * registeringNewStaff
     *
     * @param staff
     */
    public void registeringNewStaff(Staff staff) {
        staffList.put(staff.getId(), staff);
        auditTrailLog.entryLog("SystemGenerated", AuthAccess.ADD_STAFF, "Registered staff with id: " + staff.getId());
    }

    /**
     * addingNewStaff
     *
     * @param manager
     * @param staff
     */
    public void addingNewStaff(Staff manager, Staff staff) {
        requireAuthorizeManager(manager);
        staffList.put(staff.getId(), staff);
        auditTrailLog.entryLog(manager.getId(), AuthAccess.ADD_STAFF, "Added staff with id: " + staff.getId());
        assigningDefaultShifts(staff);
    }

    /**
     * assigningDefaultShifts
     *
     * @param staff
     */
    private void assigningDefaultShifts(Staff staff) {
        if (staff instanceof Nurse) {
            List<Nurse> nurses = new ArrayList<>();
            for (Staff stf : staffList.values()) if (stf instanceof Nurse) nurses.add((Nurse) stf);

            int nurseIndex = nurses.indexOf(staff);
            boolean isMorningShift = nurseIndex % 2 == 0;

            for (DayOfWeek day : DayOfWeek.values()) {
                LocalDate date = LocalDate.now().with(day);

                LocalDateTime startTime = isMorningShift ? date.atTime(8, 0) : date.atTime(14, 0);
                LocalDateTime endTime = isMorningShift ? date.atTime(16, 0) : date.atTime(22, 0);

                scheduler.assigningShiftToStaff(staff, new Shift(startTime, endTime));
            }

        } else if (staff instanceof Doctor) {
            for (DayOfWeek day : DayOfWeek.values()) {
                LocalDate date = LocalDate.now().with(day);
                LocalDateTime startTime = date.atTime(10, 0);
                LocalDateTime endTime = startTime.plusHours(1);
                scheduler.assigningShiftToStaff(staff, new Shift(startTime, endTime));
            }
        }
    }

    /**
     * checkingCompliance
     */
    public void checkingCompliance() {
        // copy of roster for inspection
        Map<String, List<Shift>> roster = scheduler.getDailyRoster();

        // Build mapping staffId -> Staff (for role checks)
        // staffList is a map in HealthCareHome
        // For safety use unmodifiable copy
        Map<String, Staff> staffMap = this.getStaffList();

        // For each day of week, it checks coverage
        for (DayOfWeek dayOfWeek : DayOfWeek.values()) {
            boolean morningCovered = false;   // 08:00-16:00
            boolean eveningCovered = false;   // 14:00-22:00
            boolean doctorOneHourPresent = false;

            // now, iterate through roster entries for each staff
            for (Map.Entry<String, List<Shift>> entry : roster.entrySet()) {
                String staffId = entry.getKey();
                Staff staff = staffMap.get(staffId);
                if (staff == null) continue;
                List<Shift> shifts = entry.getValue();

                for (Shift shift : shifts) {
                    LocalDateTime startTime = (LocalDateTime) shift.start();
                    LocalDateTime endTime = (LocalDateTime) shift.end();

                    if (!startTime.getDayOfWeek().equals(dayOfWeek)) continue;

                    LocalDate dayDate = startTime.toLocalDate();
                    LocalDateTime morningStart = LocalDateTime.of(dayDate, LocalTime.of(8, 0));
                    LocalDateTime morningEnd = LocalDateTime.of(dayDate, LocalTime.of(16, 0));
                    if (!startTime.isAfter(morningStart) && !endTime.isBefore(morningEnd)) {
                        if (staff instanceof Nurse) morningCovered = true;
                    }

                    // evening coverage if shift fully contains 14:00-22:00 on that day
                    LocalDateTime eveningStart = LocalDateTime.of(dayDate, LocalTime.of(14, 0));
                    LocalDateTime eveningEnd = LocalDateTime.of(dayDate, LocalTime.of(22, 0));
                    if (!startTime.isAfter(eveningStart) && !endTime.isBefore(eveningEnd)) {
                        if (staff instanceof Nurse) eveningCovered = true;
                    }

                    // doctor 1-hour check - if staff is doctor and shift duration == 1 hour
                    if (staff instanceof Doctor) {
                        long hours = Duration.between(startTime, endTime).toHours();
                        if (hours == 1) doctorOneHourPresent = true;
                    }
                }
            }

            if (!morningCovered) {
                throw new RosterUnfollowedException("Compliance failure: no nurse assigned for morning (08:00-16:00) on " + dayOfWeek);
            }
            if (!eveningCovered) {
                throw new RosterUnfollowedException("Compliance failure: no nurse assigned for evening (14:00-22:00) on " + dayOfWeek);
            }
            if (!doctorOneHourPresent) {
                throw new RosterUnfollowedException("Compliance failure: no doctor 1-hour shift on " + dayOfWeek);
            }
        }

        for (Map.Entry<String, List<Shift>> roasterEntry : roster.entrySet()) {
            String staffId = roasterEntry.getKey();
            Staff staff = staffMap.get(staffId);
            if (!(staff instanceof Nurse)) continue;

            Map<LocalDate, Long> hoursPerDay = new HashMap<>();
            for (Shift shift : roasterEntry.getValue()) {
                LocalDateTime shiftStart = (LocalDateTime) shift.start();
                LocalDateTime shiftEnd = (LocalDateTime) shift.end();
                // assuming shifts do not span more than 24 hours and belong to a single calendar day
                LocalDate localDate = shiftStart.toLocalDate();
                long hours = Duration.between(shiftStart, shiftEnd).toHours();
                hoursPerDay.merge(localDate, hours, Long::sum);
            }

            for (Map.Entry<LocalDate, Long> entry : hoursPerDay.entrySet()) {
                if (entry.getValue() > 8) {
                    throw new RosterUnfollowedException("Compliance failure: nurse " + staffId +
                            " assigned " + entry.getValue() + " hours on " + entry.getKey() + " (max 8)");
                }
            }
        }
    }

    /**
     * assigningShift
     *
     * @param manager
     * @param staff
     * @param shift
     */
    public void assigningShift(Staff manager, Staff staff, Shift shift) {
        requireAuthorizeManager(manager);
        scheduler.assigningShiftToStaff(staff, shift);
        auditTrailLog.entryLog(manager.getId(), AuthAccess.SHIFT_ASSIGNMENT, "Assigned shift to " + staff.getId());
    }

    /**
     * assigningResidentToBed
     *
     * @param staff
     * @param bedId
     * @param resident
     */
    public void assigningResidentToBed(Staff staff, String bedId, Resident resident) {
        Bed bed = findBed(bedId);
        if (!bed.isVacant()) throw new BedNotAvailableException("Bed occupied with bed id: " + bedId);

        requireAuthorizeRole(staff, Role.NURSE);
        requireOnDutyStaff(staff);

        bed.setResident(resident);
        if (resident.getId() != null) residentList.put(resident.getId(), resident);
        auditTrailLog.entryLog(staff.getId(), AuthAccess.ADD_RESIDENT, "Assigned resident " + resident.getId() + " to " + bedId);
    }

    /**
     * movingResidentToNewBed
     *
     * @param staff
     * @param fromBedId
     * @param toBedId
     */
    public void movingResidentToNewBed(Staff staff, String fromBedId, String toBedId) {
        requireAuthorizeRole(staff, Role.NURSE);
        requireOnDutyStaff(staff);

        Bed fromBed = findBed(fromBedId);
        Bed toBed = findBed(toBedId);

        if (fromBed.getResident() == null)
            throw new ResidentNotFoundException("No resident in source bed: " + fromBedId);
        if (!toBed.isVacant()) throw new BedNotAvailableException("Destination already occupied: " + toBedId);

        Resident resident = fromBed.getResident();
        fromBed.setResident(null);
        toBed.setResident(resident);
        auditTrailLog.entryLog(staff.getId(), AuthAccess.MOVE_RESIDENT, "Moved resident " + resident.getId() + " from " + fromBedId + " to " + toBedId);
    }

    /**
     * getResidentInBed
     *
     * @param bedId
     * @return
     */
    public Resident getResidentInBed(String bedId) {
        Bed bed = bedList.get(bedId);
        if (bed == null) return null;
        return bed.getResident();
    }

    /**
     * writingPrescription
     *
     * @param doctor
     * @param bedId
     * @param newPrescriptions
     */
    public void writingPrescription(Staff doctor, String bedId, List<Prescription> newPrescriptions) {
        requireAuthorizeRole(doctor, Role.DOCTOR);
        requireOnDutyStaff(doctor);

        Bed bed = findBed(bedId);
        Resident resident = bed.getResident();
        if (resident == null) throw new ResidentNotFoundException("No resident in bed: " + bedId);

        List<Prescription> existing = resident.getPrescriptionList();
        if (existing == null) existing = new ArrayList<>();

        existing.addAll(newPrescriptions);
        resident.setPrescriptionList(existing);

        for (Prescription newPrescription : newPrescriptions) {
            if (newPrescription.getId() != null) prescriptionList.put(newPrescription.getId(), newPrescription);
            auditTrailLog.entryLog(doctor.getId(), AuthAccess.WRITE_PRESCRIPTION, "Prescription " + newPrescription.getId() + " added for resident " + resident.getId());
        }
    }

    /**
     * dischargingResident
     *
     * @param staffMember
     * @param bedId
     */
    public void dischargingResident(Staff staffMember, String bedId) {
        if (!(staffMember instanceof Manager)) {
            // nurses may be allowed if rostered
            requireAuthorizeRole(staffMember, Role.NURSE);
            requireOnDutyStaff(staffMember);
        }

        Bed bed = findBed(bedId);
        Resident resident = bed.getResident();
        if (resident == null) throw new ResidentNotFoundException("No resident: " + bedId);

        // archive resident to a per resident file (archive_{id}.dat)
        try {
            archivingResident(resident);
        } catch (Exception ex) {
            auditTrailLog.entryLog(staffMember.getId(), AuthAccess.DISCHARGE_RESIDENT, "Archive failed for " + resident.getId() + " error " + ex.getMessage());
            ex.printStackTrace();
        }

        // clear bed and maps
        bed.setResident(null);
        if (resident.getId() != null) residentList.remove(resident.getId());

        // also remove associated prescriptions from system map
        List<Prescription> prescriptionList = resident.getPrescriptionList();
        if (prescriptionList != null) {
            for (Prescription prescription : prescriptionList) {
                if (prescription != null && prescription.getId() != null)
                    this.prescriptionList.remove(prescription.getId());
            }
        }

        auditTrailLog.entryLog(staffMember.getId(), AuthAccess.DISCHARGE_RESIDENT, "Discharged " + resident.getId() + " from bed " + bedId);
    }

    /**
     * archivingResident
     *
     * @param resident
     * @throws IOException
     */
    private void archivingResident(Resident resident) throws IOException {
        String filename = "archive_" + resident.getId() + ".dat";
        try (FileOutputStream fileOutputStream = new FileOutputStream(filename);
             ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream)) {
            objectOutputStream.writeObject(resident);
            List<Prescription> prescriptionList = resident.getPrescriptionList();
            objectOutputStream.writeObject(prescriptionList);
            objectOutputStream.flush();
        }
    }

    /**
     * saveAllStateToFile
     *
     * @param path
     */
    public void saveAllStateToFile(String path) {
        if (path == null || path.trim().isEmpty()) path = "healthcarehome_state.dat";
        try (FileOutputStream fileOutputStream = new FileOutputStream(path);
             ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream)) {
            objectOutputStream.writeObject(this);
            objectOutputStream.flush();
            auditTrailLog.entryLog("System", AuthAccess.ADD_RESIDENT, "Saved system state to " + path);
        } catch (Exception ex) {
            auditTrailLog.entryLog("System", AuthAccess.UPDATE_STAFF, "Failed to save system state " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    /**
     * findBed
     *
     * @param bedId
     * @return
     */
    public Bed findBed(String bedId) {
        Bed bed = bedList.get(bedId);
        if (bed == null) throw new ValidationFailedException("Bed not found: " + bedId);
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

    /**
     * authenticate
     *
     * @param username
     * @param password
     * @return
     */
    public Staff authenticate(String username, String password) {
        Collection<Staff> staffList = getStaffList().values();
        for (Staff staff : staffList) {
            if (staff.getUsername().equals(username) && staff.getPassword().equals(password)) {
                auditTrailLog.entryLog(staff.getId(), AuthAccess.VIEW_RESIDENT, "Authenticated");
                return staff;
            }
        }
        return null;
    }

    /**
     * requireAuthorizeManager
     *
     * @param staff
     */
    private void requireAuthorizeManager(Staff staff) {
        if (!(staff instanceof Manager)) throw new UnAuthorizationException("Manager authorized only");
    }

    /**
     * requireAuthorizeRole
     *
     * @param staff
     * @param role
     */
    public void requireAuthorizeRole(Staff staff, Role role) {
        if (staff == null) throw new UnAuthorizationException("No staff provided");
        if (!staff.getRole().equals(role)) throw new UnAuthorizationException("Requires authorized role " + role);
    }

    /**
     * requireOnDutyStaff
     *
     * @param staff
     */
    public void requireOnDutyStaff(Staff staff) {
        if (!scheduler.isAvailableOnDuty(staff, LocalDateTime.now()))
            throw new UnAuthorizationException("Staff not rostered localDateTime this time");
    }
}
