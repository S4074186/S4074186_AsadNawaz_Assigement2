package com.healthcare.home;

import com.healthcare.home.core.HealthCareHome;
import com.healthcare.home.entity.Bed;
import com.healthcare.home.entity.Gender;
import com.healthcare.home.entity.Prescription;
import com.healthcare.home.entity.Resident;
import com.healthcare.home.scheduler.Shift;
import com.healthcare.home.staff.Doctor;
import com.healthcare.home.staff.Manager;
import com.healthcare.home.staff.Nurse;
import com.healthcare.home.staff.Staff;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class HealthCareHomeTest {
    @Test
    void addResidentToVacantBedTest() {
        HealthCareHome home = new HealthCareHome();
        Manager manager = new Manager("M1", "Test-Manager", "test", "testManager");
        Staff nurse = new Nurse("N1", "Test-Nurse", "test-nurse", "testNurse");
        Staff doctor = new Doctor("D1", "Test-Doctor", "test-doctor", "testDoctor");
        Bed bed = home.findBed("W1-R101-B1");
        Prescription p = new Prescription(doctor.getId(), "test-medicine", "1", List.of("Morning", "Evening"));
        Resident resident = new Resident("R1", "John Doe", Gender.MALE, false, bed.getId());
        resident.setPrescription(p);
        home.assigningShift(manager, nurse, new Shift(LocalDateTime.now(), LocalDateTime.now().plusHours(8)));
        home.assignResidentToBed(nurse, bed.getId(), resident);
        assertEquals(resident, bed.getResident());
    }

}