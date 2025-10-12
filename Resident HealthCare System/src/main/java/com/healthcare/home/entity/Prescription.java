package com.healthcare.home.entity;


import com.healthcare.home.core.SerializingService;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Prescription implements Serializable {
    private static long idCounter = 0;  // shared counter for all prescriptions
    private final String id;
    private final String doctorId;
    private final String residentId;
    private String medicine;
    private String dose;
    private final List<String> times = new ArrayList<>();
    private final List<Medication> administrations = new ArrayList<>();

    public Prescription(String doctorId, String residentId, String medicine, String dose, List<String> times) {
        this.id = SerializingService.PrescriptionIdGenerator.nextId();
        this.doctorId = doctorId;
        this.residentId = residentId;
        this.medicine = medicine;
        this.dose = dose;
        if (times != null) this.times.addAll(times);
    }


    private synchronized String generateId() {
        idCounter++;
        return String.format("PRE-%03d", idCounter);
    }

    // --- getters and setters ---
    public String getId() {
        return id;
    }

    public String getDoctorId() {
        return doctorId;
    }

    public String getResidentId() {
        return residentId;
    }

    public String getMedicine() {
        return medicine;
    }

    public void setMedicine(String medicine) {
        this.medicine = medicine;
    }

    public String getDose() {
        return dose;
    }

    public void setDose(String dose) {
        this.dose = dose;
    }

    public List<String> getTimes() {
        return times;
    }

    public List<Medication> getAdministrations() {
        return administrations;
    }

    // --- restore counter after deserialization ---
    public static void setIdCounter(long lastId) {
        idCounter = lastId;
    }

    public static long getIdCounter() {
        return idCounter;
    }
}
