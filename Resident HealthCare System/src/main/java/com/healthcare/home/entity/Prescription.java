package com.healthcare.home.entity;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Prescription implements Serializable {
    private static long idCounter = 0;
    private final String id;
    private final String doctorId;
    private String medicine;
    private String dose;
    private final List<String> times = new ArrayList<>();
    private final List<Medication> administrations = new ArrayList<>();

    public Prescription(String doctorId, String medicine, String dose, List<String> times) {
        this.id = generateId();
        this.doctorId = doctorId;
        this.medicine = medicine;
        this.dose = dose;
        if (times != null) {
            this.times.addAll(times);
        }
    }

    private synchronized String generateId() {
        idCounter++;
        return String.format("PRE-%03d", idCounter);
    }

    public static void setIdCounter(long lastId) {
        idCounter = lastId;
    }

    public static long getIdCounter() {
        return idCounter;
    }

    public String getId() {
        return id;
    }

    public String getDoctorId() {
        return doctorId;
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

    /**
     * Called when a nurse administers a medicine.
     * Records the administration in the administrations list.
     */
    /**
     * Called when a nurse administers a medicine.
     * Records the administration in the administrations list.
     */
    public void administer(String nurseId) {
        Medication med = new Medication(
                this.id,
                nurseId,
                LocalDateTime.now(),
                this.dose
        );
        administrations.add(med);
    }


    @Override
    public String toString() {
        return "Prescription{" +
                "id='" + id + '\'' +
                ", doctorId='" + doctorId + '\'' +
                ", medicine='" + medicine + '\'' +
                ", dose='" + dose + '\'' +
                ", times=" + times +
                ", administrations=" + administrations +
                '}';
    }
}
