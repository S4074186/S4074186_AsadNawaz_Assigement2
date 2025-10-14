package com.healthcare.home;

import com.healthcare.home.core.HealthCareHome;
import com.healthcare.home.entity.Bed;
import com.healthcare.home.entity.Gender;
import com.healthcare.home.entity.Prescription;
import com.healthcare.home.entity.Resident;
import com.healthcare.home.exceptions.RosterUnfollowedException;
import com.healthcare.home.scheduler.Shift;
import com.healthcare.home.staff.Doctor;
import com.healthcare.home.staff.Manager;
import com.healthcare.home.staff.Nurse;
import com.healthcare.home.staff.Staff;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class HealthCareHomeTest {
    @Test
    void addResidentToVacantBedTest() {
        HealthCareHome home = new HealthCareHome();
        Manager manager = new Manager("Test-Manager", "test", "testManager");
        Staff nurse = new Nurse("Test-Nurse", "test-nurse", "testNurse");
        Staff doctor = new Doctor("Test-Doctor", "test-doctor", "testDoctor");
        Bed bed = home.findBed("W1-R101-B1");
        Prescription p = new Prescription(doctor.getId(), "test-medicine", "1", List.of("Morning", "Evening"));
        Resident resident = new Resident("John Doe", Gender.MALE, false, bed.getId());
        resident.setPrescriptionList(List.of(p));
        home.assigningShift(manager, nurse, new Shift(LocalDateTime.now(), LocalDateTime.now().plusHours(8)));
        home.assignResidentToBed(nurse, bed.getId(), resident);
        assertEquals(resident, bed.getResident());
    }

    @Test
    void addResidentToOccupiedBedShouldThrowException() {
        HealthCareHome home = new HealthCareHome();
        Manager manager = new Manager("M1", "Manager", "manager");
        Nurse nurse = new Nurse("N1", "Nurse", "nurse");
        Doctor doctor = new Doctor("D1", "Doctor", "doctor");

        Bed bed = home.findBed("W1-R101-B1");
        Resident r1 = new Resident("Alice", Gender.FEMALE, false, bed.getId());
        Resident r2 = new Resident("Bob", Gender.MALE, false, bed.getId());

        home.assigningShift(manager, nurse, new Shift(LocalDateTime.now(), LocalDateTime.now().plusHours(8)));
        home.assignResidentToBed(nurse, bed.getId(), r1);

        assertThrows(RuntimeException.class, () -> {
            home.assignResidentToBed(nurse, bed.getId(), r2);
        });
    }

    @Test
    void nurseShiftExceeds8HoursShouldFailCompliance() {
        HealthCareHome home = new HealthCareHome();
        Manager manager = new Manager("M2", "Manager", "manager2");
        Nurse nurse = new Nurse("N2", "Nurse", "nurse2");

        home.assigningShift(manager, nurse, new Shift(LocalDateTime.now(), LocalDateTime.now().plusHours(10)));

        assertThrows(RuntimeException.class, home::checkCompliance);
    }

    @Test
    void doctorShiftLongerThanOneHourShouldFailCompliance() {
        HealthCareHome home = new HealthCareHome();
        Manager manager = new Manager("M3", "Manager", "manager3");
        Doctor doctor = new Doctor("D3", "Doctor", "doctor3");

        home.assigningShift(manager, doctor, new Shift(LocalDateTime.now(), LocalDateTime.now().plusHours(2)));

        assertThrows(RuntimeException.class, home::checkCompliance);
    }

    @Test
    void nurseNotRosteredShouldNotAssignResident() {
        HealthCareHome home = new HealthCareHome();
        Nurse nurse = new Nurse("N4", "Nurse", "nurse4");
        Bed bed = home.findBed("W1-R101-B2");
        Resident resident = new Resident("Tom", Gender.MALE, false, bed.getId());

        assertThrows(RuntimeException.class, () -> {
            home.assignResidentToBed(nurse, bed.getId(), resident);
        });
    }

    @Test
    void managerCanAddStaffSuccessfully() {
        HealthCareHome home = new HealthCareHome();
        Manager manager = new Manager("M5", "Manager", "manager5");
        Nurse newNurse = new Nurse("N5", "New Nurse", "newNurse");

        assertDoesNotThrow(() -> home.addNewStaff(manager, newNurse));
    }

    @Test
    void nurseCannotAddStaffShouldThrowException() {
        HealthCareHome home = new HealthCareHome();
        Nurse nurse = new Nurse("N6", "Nurse", "nurse6");
        Nurse anotherNurse = new Nurse("N7", "AnotherNurse", "nurse7");

        assertThrows(RuntimeException.class, () -> {
            home.addNewStaff(nurse, anotherNurse);
        });
    }

    @Test
    void dischargeResidentShouldVacateBed() {
        HealthCareHome home = new HealthCareHome();
        Manager manager = new Manager("M7", "Manager", "manager7");
        Nurse nurse = new Nurse("N8", "Nurse", "nurse8");
        Bed bed = home.findBed("W1-R101-B3");
        Resident res = new Resident("Chris", Gender.MALE, false, bed.getId());

        home.assigningShift(manager, nurse, new Shift(LocalDateTime.now(), LocalDateTime.now().plusHours(8)));
        home.assignResidentToBed(nurse, bed.getId(), res);
        home.dischargeResident(manager, bed.getId());

        assertNull(bed.getResident());
    }

    private Shift shiftFor(LocalDate day, int startHour, int endHour) {
        return new Shift(day.atTime(startHour, 0), day.atTime(endHour, 0));
    }

    @Test
    void testComplianceSuccess() {
        HealthCareHome home = new HealthCareHome();
        Manager manager = new Manager("Manger X", "mgr", "pwd");
        home.registerNewStaff(manager);

        // 1 doctor + 2 nurses (we will assign enough nurses to cover both shifts)
        Doctor d = new Doctor("Doc", "doc", "pwd");
        Nurse n1 = new Nurse("N1","n1","pwd");
        Nurse n2 = new Nurse("N2","n2","pwd");
        home.registerNewStaff(d);
        home.registerNewStaff(n1);
        home.registerNewStaff(n2);

        LocalDate today = LocalDate.now().with(DayOfWeek.MONDAY); // base day
        // For each day of week assign:
        for (DayOfWeek dow : DayOfWeek.values()) {
            LocalDate day = today.with(dow);
            // doctor 1 hr at 10:00
            home.assigningShift(manager, d, shiftFor(day, 10, 11));
            // nurse morning (08-16) by n1
            home.assigningShift(manager, n1, shiftFor(day, 8, 16));
            // nurse evening (14-22) by n2
            home.assigningShift(manager, n2, shiftFor(day, 14, 22));
        }

        // should not throw
        assertDoesNotThrow(home::checkCompliance);
    }

    @Test
    void testNurseOverworked() {
        HealthCareHome home = new HealthCareHome();
        Manager manager = new Manager("M","mgr","pwd");
        home.registerNewStaff(manager);
        Nurse n = new Nurse("N","n","pwd");
        home.registerNewStaff(n);
        // assign same nurse two shifts in same day (8-16 and 14-22) → total 16 hours
        LocalDate day = LocalDate.now().with(DayOfWeek.MONDAY);
        home.assigningShift(manager, n, shiftFor(day, 8, 16));
        home.assigningShift(manager, n, shiftFor(day, 14, 22));

        RosterUnfollowedException ex = assertThrows(RosterUnfollowedException.class, home::checkCompliance);
        assertTrue(ex.getMessage().toLowerCase().contains("compliance failure"));
    }

    @Test
    void testMissingCoverage() {
        HealthCareHome home = new HealthCareHome();
        Manager manager = new Manager("M","mgr","pwd");
        home.registerNewStaff(manager);
        Nurse n1 = new Nurse("N1","n1","pwd");
        Doctor d = new Doctor("doc","doc","pwd");
        home.registerNewStaff(n1);
        home.registerNewStaff(d);

        // Only assign morning nurse for every day, no evening nurse
        LocalDate base = LocalDate.now().with(DayOfWeek.MONDAY);
        for (DayOfWeek dow : DayOfWeek.values()) {
            LocalDate day = base.with(dow);
            home.assigningShift(manager, n1, shiftFor(day, 8, 16));
            home.assigningShift(manager, d, shiftFor(day, 10, 11));
        }

        RosterUnfollowedException ex = assertThrows(RosterUnfollowedException.class, home::checkCompliance);
        assertTrue(ex.getMessage().toLowerCase().contains("evening") || ex.getMessage().toLowerCase().contains("no nurse"));
    }
}