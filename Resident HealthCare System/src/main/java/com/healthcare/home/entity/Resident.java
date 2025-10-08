package com.healthcare.home.entity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Resident implements Serializable {

    private final String id;
    private final String name;
    private Gender gender;
    private boolean requiresIsolation;
    private final List<Prescription> prescriptions = new ArrayList<>();

    public Resident(String id, String name, Gender gender, boolean requiresIsolation) {
        this.id = id;
        this.name = name;
        this.gender = gender;
        this.requiresIsolation = requiresIsolation;
    }

    public Resident(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() {
        return id;
    }
    public String getName() {
        return name;
    }
    public Gender getGender() {
        return gender;
    }
    public boolean requiresIsolation() {
        return requiresIsolation;
    }

    public boolean isRequiresIsolation() {
        return requiresIsolation;
    }

    public List<Prescription> getPrescriptions() {
        return prescriptions;
    }
}
