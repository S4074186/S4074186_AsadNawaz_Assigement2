package com.healthcare.home.controllers;

import com.healthcare.home.core.HealthCareHome;
import com.healthcare.home.entity.Bed;
import com.healthcare.home.entity.Resident;
import com.healthcare.home.staff.Staff;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;

public class BedDetailsController {
    @FXML private Label bedIdLabel;
    @FXML private Label residentLabel;
    @FXML private TextArea details;

    private HealthCareHome home;
    private Staff staff;
    private Bed bed;

    public void init(HealthCareHome home, Staff staff, String bedId) {
        this.home = home;
        this.staff = staff;
        this.bed = home.findBed(bedId);
        bedIdLabel.setText(bed.getId());
        if (!bed.isVacant()) {
            Resident r = bed.getResident();
            residentLabel.setText(r.getName() + " (" + r.getGender() + ")");
            details.setText(home.getResidentDetails(staff, r.getId()).getName());
        } else {
            residentLabel.setText("Vacant");
        }
    }
}
