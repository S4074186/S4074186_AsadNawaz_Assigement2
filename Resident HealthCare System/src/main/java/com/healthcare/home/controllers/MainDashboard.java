package com.healthcare.home.controllers;

import com.healthcare.home.core.ResidentHealthCareHome;
import com.healthcare.home.core.SerializingHandlerService;
import com.healthcare.home.entities.*;
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
import lombok.Setter;

import java.util.List;

/**
 * MainDashboard
 */
public abstract class MainDashboard {

    @FXML
    private Button logoutButton;

    // allow subclasses to set the home instance
    @Setter
    protected ResidentHealthCareHome home;

    /**
     * Shared table column setup for all dashboards
     *
     * @param columnBed
     * @param columnResident
     * @param columnPrescription
     */
    protected void setupCommonColumns(
            TableColumn<BedRow, String> columnBed,
            TableColumn<BedRow, String> columnResident,
            TableColumn<BedRow, String> columnPrescription) {

        columnBed.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().bedId.get()));

        // Resident column with gender color indicator
        columnResident.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || getTableRow() == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    var row = (BedRow) getTableRow().getItem();
                    if (row == null) return;
                    Circle circle = new Circle(6);
                    if (row.gender != null) {
                        circle.setFill(row.gender == Gender.MALE ? Color.BLUE : Color.RED);
                    } else {
                        circle.setFill(Color.GRAY);
                    }
                    Label label = new Label(row.residentName.get());
                    HBox hBox = new HBox(8, circle, label);
                    hBox.setAlignment(Pos.CENTER_LEFT);
                    setGraphic(hBox);
                    setText(null);
                }
            }
        });
        columnResident.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().residentName.get()));

        // Prescription column with expandable list (medicine, dose, time)
        columnPrescription.setCellFactory(column -> new TableCell<>() {
            private final Button expandButton = new Button("▶");
            private boolean isExpanded = false;
            private VBox expandedBox;

            {
                expandButton.setStyle("-fx-background-color: transparent; -fx-text-fill: blue; -fx-cursor: hand;");
                expandButton.setOnAction(actionEvent -> toggleExpand());
            }


            private void toggleExpand() {
                if (expandedBox == null) return;
                isExpanded = !isExpanded;
                expandedBox.setVisible(isExpanded);
                expandButton.setText(isExpanded ? "▼" : "▶");
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }

                var row = (BedRow) getTableRow().getItem();
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

                for (Prescription prescription : prescriptions) {
                    String prescriptionInfo = prescription.getMedicine() + " (" + prescription.getDose() + ") localDateTime " +
                            (prescription.getTimes() != null ? prescription.getTimes().toString() : "N/A");
                    Label label = new Label(prescriptionInfo);
                    label.setStyle("-fx-font-size: 12;");
                    expandedBox.getChildren().add(label);
                }

                VBox vBoxContainer = new VBox(5);
                vBoxContainer.getChildren().addAll(expandButton, expandedBox);
                vBoxContainer.setAlignment(Pos.CENTER_LEFT);

                setGraphic(vBoxContainer);
                setText(null);
            }
        });
    }

    /**
     * logout
     */
    @FXML
    public void onLogout() {
        try {
            // save all patient related records before logout
            try {
                if (home != null) {
                    SerializingHandlerService.saveRecordsInFile(home);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            Stage stage = (Stage) logoutButton.getScene().getWindow();
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/com/healthcare/home/view/login.fxml"));
            stage.setScene(new Scene(loader.load()));
            stage.setTitle("Login");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
