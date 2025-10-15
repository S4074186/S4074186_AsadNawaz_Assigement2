package com.healthcare.home.entities;

import lombok.Data;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class Prescription implements Serializable {
    @Setter
    private static long idCounter = 0;
    private final String id;
    private final String doctorId;
    private String medicine;
    private String dose;
    private final List<String> times = new ArrayList<>();
    private final List<Medication> administrations = new ArrayList<>();

    /**
     * Prescription Constructor
     *
     * @param doctorId
     * @param medicine
     * @param dose
     * @param times
     */
    public Prescription(String doctorId, String medicine, String dose, List<String> times) {
        this.id = generateId();
        this.doctorId = doctorId;
        this.medicine = medicine;
        this.dose = dose;
        if (times != null) {
            this.times.addAll(times);
        }
    }

    /**
     * generateId
     *
     * @return
     */
    private synchronized String generateId() {
        idCounter++;
        return String.format("PRE-%03d", idCounter);
    }

    /**
     * administer
     *
     * @param nurseId
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

}
