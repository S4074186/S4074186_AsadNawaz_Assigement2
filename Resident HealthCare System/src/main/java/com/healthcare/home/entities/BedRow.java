package com.healthcare.home.entities;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class BedRow {
    public final StringProperty bedId;
    public final StringProperty residentName;
    public String residentId;
    public final Gender gender;

    public BedRow(String b, String r, Gender g) {
        this.bedId = new SimpleStringProperty(b);
        this.residentName = new SimpleStringProperty(r);
        this.gender = g;
    }

    public String getBedId() {
        return bedId.get();
    }

    public String getResidentName() {
        return residentName.get();
    }
}