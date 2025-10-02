package com.healthcare.home.entity;

import java.io.Serializable;
import java.time.LocalDateTime;

public class Medication implements Serializable {
    private final String prescriptionId;
    private final String nurseId;
    private final LocalDateTime at;
    private final String dose;

    public Medication(String prescriptionId, String nurseId, LocalDateTime at, String dose) {
        this.prescriptionId = prescriptionId;
        this.nurseId = nurseId;
        this.at = at;
        this.dose = dose;
    }

    public String getPrescriptionId() {
        return prescriptionId;
    }

    public String getNurseId() {
        return nurseId;
    }

    public LocalDateTime getAt() {
        return at;
    }

    public String getDose() {
        return dose;
    }
}
