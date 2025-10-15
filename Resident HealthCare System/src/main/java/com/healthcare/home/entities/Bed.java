package com.healthcare.home.entities;

import lombok.Data;

import java.io.Serializable;

@Data
public class Bed implements Serializable {

    private final String id;
    private boolean isolated;
    private Gender gender;
    private Resident resident;

    public boolean isVacant() {
        return resident == null;
    }

}
