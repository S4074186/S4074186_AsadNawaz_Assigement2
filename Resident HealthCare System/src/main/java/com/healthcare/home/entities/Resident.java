package com.healthcare.home.entities;

import lombok.Data;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

@Data
public class Resident implements Serializable {
    @Setter
    private static long idCounter = 0;
    private String id;
    private String name;
    private Gender gender;
    private boolean isolation;
    private String bedId;
    private List<Prescription> prescriptionList;

    /**
     * Resident Constructor
     *
     * @param name
     * @param gender
     * @param isolation
     * @param bedId
     */
    public Resident(String name, Gender gender, boolean isolation, String bedId) {
        this.id = generateId();
        ;
        this.name = name;
        this.gender = gender;
        this.isolation = isolation;
        this.bedId = bedId;
    }

    /**
     * generateId
     *
     * @return
     */
    private synchronized String generateId() {
        idCounter++;
        return String.format("RES-%03d", idCounter);
    }

}
