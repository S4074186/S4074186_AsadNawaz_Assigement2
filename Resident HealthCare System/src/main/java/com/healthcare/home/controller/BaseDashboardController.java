package com.healthcare.home.controller;

import com.healthcare.home.core.HealthCareHome;
import com.healthcare.home.core.SerializingService;
import com.healthcare.home.entity.Bed;
import com.healthcare.home.entity.Gender;
import com.healthcare.home.entity.Prescription;
import com.healthcare.home.entity.Resident;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

import java.util.List;

public abstract class BaseDashboardController {

    @FXML private Button logoutBtn;

    // allow subclasses to set the home instance
    protected HealthCareHome home;

    public void setHome(HealthCareHome home) {
        this.home = home;
    }

    /** Shared table column setup for all dashboards */
    protected void setupCommonColumns(
            TableColumn<ManagerDashboardController.BedRow, String> colBedId,
            TableColumn<ManagerDashboardController.BedRow, String> colResident,
            TableColumn<ManagerDashboardController.BedRow, String> colPrescription) {

        colBedId.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().bedId.get()));

        // Resident column with gender color indicator
        colResident.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || getTableRow() == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    var row = (ManagerDashboardController.BedRow) getTableRow().getItem();
                    if (row == null) return;
                    Circle c = new Circle(6);
                    if (row.gender != null) {
                        c.setFill(row.gender == Gender.MALE ? Color.BLUE : Color.RED);
                    } else {
                        c.setFill(Color.GRAY);
                    }
                    Label lbl = new Label(row.residentName.get());
                    HBox h = new HBox(8, c, lbl);
                    h.setAlignment(Pos.CENTER_LEFT);
                    setGraphic(h);
                    setText(null);
                }
            }
        });
        colResident.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().residentName.get()));

        // Prescription column with expandable list (medicine, dose, time)
        colPrescription.setCellFactory(col -> new TableCell<>() {
            private final Button expandBtn = new Button("▶");
            private boolean expanded = false;
            private VBox expandedBox;

            {
                expandBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: blue; -fx-cursor: hand;");
                expandBtn.setOnAction(e -> toggleExpand());
            }

            private void toggleExpand() {
                if (expandedBox == null) return;
                expanded = !expanded;
                expandedBox.setVisible(expanded);
                expandBtn.setText(expanded ? "▼" : "▶");
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }

                var row = (ManagerDashboardController.BedRow) getTableRow().getItem();
                if (row == null) {
                    setGraphic(null);
                    return;
                }

                Bed bed = home.getBedList().get(row.bedId.get());
                if (bed == null || bed.isVacant()) {
                    setGraphic(new Label(""));
                    return;
                }

                Resident resident = bed.getResident();
                if (resident == null || resident.getPrescriptionList() == null || resident.getPrescriptionList().isEmpty()) {
                    setGraphic(new Label("No prescription"));
                    return;
                }

                List<Prescription> prescriptions = resident.getPrescriptionList();

                expandedBox = new VBox(4);
                expandedBox.setVisible(false);
                expandedBox.setAlignment(Pos.CENTER_LEFT);
                expandedBox.setStyle("-fx-padding: 4 0 0 18;");

                for (Prescription p : prescriptions) {
                    String presInfo = p.getMedicine() + " (" + p.getDose() + ") at " +
                            (p.getTimes() != null ? p.getTimes().toString() : "N/A");
                    Label label = new Label(presInfo);
                    label.setStyle("-fx-font-size: 12;");
                    expandedBox.getChildren().add(label);
                }

                VBox container = new VBox(5);
                container.getChildren().addAll(expandBtn, expandedBox);
                container.setAlignment(Pos.CENTER_LEFT);

                setGraphic(container);
                setText(null);
            }
        });
    }

    @FXML public void onLogout() {
        try {
            // save all patient related records before logout
            try {
                if (home != null) {
                    SerializingService.saveRecordsInFile(home);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            Stage st = (Stage) logoutBtn.getScene().getWindow();
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/com/healthcare/home/view/login.fxml"));
            st.setScene(new Scene(loader.load()));
            st.setTitle("Login");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
