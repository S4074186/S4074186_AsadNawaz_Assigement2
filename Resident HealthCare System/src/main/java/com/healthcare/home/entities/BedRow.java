package com.healthcare.home.entities;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class BedRow {
    public final StringProperty bedId;
    public final StringProperty residentName;
    public String residentId;
    public final Gender gender;

    /**
     * BedRow Constructor
     *
     * @param bedId
     * @param residentName
     * @param gender
     */
    public BedRow(String bedId, String residentName, Gender gender) {
        this.bedId = new SimpleStringProperty(bedId);
        this.residentName = new SimpleStringProperty(residentName);
        this.gender = gender;
    }

    public String getBedId() {
        return bedId.get();
    }

    public String getResidentName() {
        return residentName.get();
    }
}