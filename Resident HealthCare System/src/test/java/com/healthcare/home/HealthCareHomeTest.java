package com.healthcare.home;

import com.healthcare.home.core.HealthCareHome;
import com.healthcare.home.entity.Bed;
import com.healthcare.home.entity.Gender;
import com.healthcare.home.entity.Resident;
import com.healthcare.home.scheduler.Shift;
import com.healthcare.home.staff.Manager;
import com.healthcare.home.staff.Nurse;
import com.healthcare.home.staff.Staff;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class HealthCareHomeTest {
    @Test
    void addResidentToVacantBedTest() {
        HealthCareHome home = new HealthCareHome();
        Manager manager = new Manager("M1", "Test-Manager", "test", "test");
        Staff staff = new Nurse("N1", "Test-Nurse", "test", "test");
        Resident resident = new Resident("R1", "John Doe", Gender.MALE, false);
        Bed bed = home.findBed("W1-R101-B1");
        home.assigningShift(manager, staff, new Shift(LocalDateTime.now(), LocalDateTime.now().plusHours(8)));
        home.assignResidentToBed(staff, bed.getId(), resident);
        assertEquals(resident, bed.getResident());
    }

}