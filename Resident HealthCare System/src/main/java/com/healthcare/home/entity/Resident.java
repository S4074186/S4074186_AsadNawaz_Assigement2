package com.healthcare.home.entity;

import java.io.Serializable;
import java.util.List;

public class Resident implements Serializable {
    private static long idCounter = 0;
    private String id;
    private String name;
    private Gender gender;
    private boolean isolation;
    private String bedId;
    private List<Prescription> prescriptionList;

    public Resident(String name, Gender gender, boolean isolation, String bedId) {
        this.id = generateId();;
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

    public List<Prescription> getPrescriptionList() {
        return prescriptionList;
    }

    public void setPrescriptionList(List<Prescription> prescriptionList) {
        this.prescriptionList = prescriptionList;
    }

    private synchronized String generateId() {
        idCounter++;
        return String.format("RES-%03d", idCounter);
    }

    public static void setIdCounter(long lastId) {
        idCounter = lastId;
    }

    public static long getIdCounter() {
        return idCounter;
    }
}
