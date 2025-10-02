package com.healthcare.home.entity;

import java.io.Serializable;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class Prescription implements Serializable {
    private final String id;
    private final String doctorId;
    private final String residentId;
    private String medicine;
    private String dose;
    private final List<LocalTime> times = new ArrayList<>();
    private final List<Medication> administrations = new ArrayList<>();

    public Prescription(String id, String doctorId, String residentId, String medicine, String dose, List<LocalTime> times) {
        this.id = id;
        this.doctorId = doctorId;
        this.residentId = residentId;
        this.medicine = medicine;
        this.dose = dose;
        if (times != null) this.times.addAll(times);
    }

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

    public void setMedicine(String m) {
        this.medicine = m;
    }

    public String getDose() {
        return dose;
    }

    public void setDose(String d) {
        this.dose = d;
    }

    public List<LocalTime> getTimes() {
        return times;
    }

    public List<Medication> getAdministrations() {
        return administrations;
    }
}
