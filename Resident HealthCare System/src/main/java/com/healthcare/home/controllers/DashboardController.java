package com.healthcare.home.controllers;

import com.healthcare.home.core.HealthCareHome;
import com.healthcare.home.entity.Bed;
import com.healthcare.home.entity.Gender;
import com.healthcare.home.staff.Staff;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.Map;

public class DashboardController {
    @FXML private Label welcomeLabel;
    @FXML private GridPane bedGrid;

    private HealthCareHome home;
    private Staff staff;

    public void init(HealthCareHome home, Staff staff) {
        this.home = home;
        this.staff = staff;
        welcomeLabel.setText("Welcome, " + staff.getName() + " (" + staff.getRole() + ")");
        renderBeds();
    }

    private void renderBeds() {
        bedGrid.getChildren().clear();
        int row = 0, col = 0;

        Map<String, Bed> bedMap = home.getBedList();

        for (int i = 0; i < bedMap.size(); i++) {
            Bed bed = bedMap.get(i);
            Button btn = new Button(bed.getId());
            btn.setPrefSize(80, 40);
            String color = !bed.isVacant()
                    ? (bed.getResident().getGender().equals(Gender.MALE) ? "lightblue" : "lightcoral")
                    : "lightgray";
            btn.setStyle("-fx-background-color:" + color + ";");
            btn.setOnAction(e -> openBedDetails(bed.getId()));
            bedGrid.add(btn, col++, row);
            if (col == 6) { col = 0; row++; }
        }
    }

    private void openBedDetails(String bedId) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/healthcare/home/view/bed_details.fxml"));
            Stage stage = new Stage();
            stage.setScene(new Scene(loader.load()));
            BedDetailsController ctrl = loader.getController();
            ctrl.init(home, staff, bedId);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
