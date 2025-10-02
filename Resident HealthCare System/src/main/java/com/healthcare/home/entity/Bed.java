package com.healthcare.home.entity;

import java.io.Serializable;

public class Bed implements Serializable {

    private final String id;
    private boolean isolated;
    private Gender gender;
    private Resident resident;

    public Bed(String id, boolean isolated, Gender gender) {
        this.id = id;
        this.isolated = isolated;
        this.gender = gender;
    }
    public Bed(String id, boolean isolated) {
        this.id = id;
        this.isolated = isolated;
    }

    public Bed(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
    public boolean isVacant() {
        return resident == null;
    }
    public void assign(Resident resident) {
        this.resident = resident;
    }
    public Gender getGender() {
        return gender;
    }
    public boolean isIsolated() {
        return isolated;
    }

    public void setGender(Gender gender) {
        this.gender = gender;
    }

    public Resident getResident() {
        return resident;
    }

    public void setResident(Resident resident) {
        this.resident = resident;
    }

    @Override public String toString() {
        return id + (isVacant() ? " [vacant]" : " [" + resident.getId() + ":" + resident.getName() + "]");
    }
}
