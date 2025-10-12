package com.healthcare.home.entity;

import java.io.Serializable;

public class Resident implements Serializable {
    private String id;
    private String name;
    private Gender gender;
    private boolean isolation;
    private String bedId;
    private Prescription prescription;

    public Resident(String id, String name, Gender gender, boolean isolation, String bedId) {
        this.id = id;
        this.name = name;
        this.gender = gender;
        this.isolation = isolation;
        this.bedId = bedId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Gender getGender() {
        return gender;
    }

    public void setGender(Gender gender) {
        this.gender = gender;
    }

    public boolean isIsolation() {
        return isolation;
    }

    public void setIsolation(boolean isolation) {
        this.isolation = isolation;
    }

    public String getBedId() {
        return bedId;
    }

    public void setBedId(String bedId) {
        this.bedId = bedId;
    }

    public Prescription getPrescription() {
        return prescription;
    }

    public void setPrescription(Prescription prescription) {
        this.prescription = prescription;
    }
}
