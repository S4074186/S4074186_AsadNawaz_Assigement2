
package com.healthcare.home.controller;

import com.healthcare.home.core.HealthCareHome;
import com.healthcare.home.entity.Bed;
import com.healthcare.home.entity.Prescription;
import com.healthcare.home.entity.Resident;
import com.healthcare.home.staff.Doctor;
import com.healthcare.home.staff.Staff;
import com.healthcare.home.auth.Access;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DoctorDashboardController extends BaseDashboardController {
    private HealthCareHome home;
    private Staff staff;

    @FXML private TableView<com.healthcare.home.controller.ManagerDashboardController.BedRow> bedTable;
    @FXML private TableColumn<com.healthcare.home.controller.ManagerDashboardController.BedRow, String> colBedId;
    @FXML private TableColumn<com.healthcare.home.controller.ManagerDashboardController.BedRow, String> colResident;
    @FXML private Button prescribeBtn;

    public void init(HealthCareHome home, Staff staff) {
        this.home = home; this.staff = staff;
        setupTable(); refreshBeds();
        prescribeBtn.setVisible(staff.has(Access.WRITE_PRESCRIPTION));
    }

    private void setupTable() {
        colBedId.setCellValueFactory(c -> javafx.beans.property.SimpleStringProperty.stringExpression(c.getValue().bedId));
        colResident.setCellValueFactory(c -> javafx.beans.property.SimpleStringProperty.stringExpression(c.getValue().residentName));
    }

    private void refreshBeds() {
        ObservableList<ManagerDashboardController.BedRow> rows = FXCollections.observableArrayList();
        for (Bed b : home.getBedList().values()) {
            String rn = b.isVacant() ? "" : b.getResident().getName();
            rows.add(new ManagerDashboardController.BedRow(b.getId(), rn));
        }
        bedTable.setItems(rows);
    }

    @FXML
    public void onPrescribe() {
        ManagerDashboardController.BedRow sel = bedTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            showAlert("Select a bed that has a resident first");
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Write Prescription");
        dialog.setHeaderText("Enter Prescription Details");

        ButtonType addButtonType = new ButtonType("Add", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField idField = new TextField();
        idField.setPromptText("Prescription ID");

        TextField medField = new TextField();
        medField.setPromptText("Medicine Name");

        TextField doseField = new TextField();
        doseField.setPromptText("Dose Description (e.g., 1 tablet)");

        TextField timesField = new TextField();
        timesField.setPromptText("Times (comma separated, HH:mm — optional)");

        grid.add(new Label("Prescription ID:"), 0, 0);
        grid.add(idField, 1, 0);
        grid.add(new Label("Medicine:"), 0, 1);
        grid.add(medField, 1, 1);
        grid.add(new Label("Dose:"), 0, 2);
        grid.add(doseField, 1, 2);
        grid.add(new Label("Times:"), 0, 3);
        grid.add(timesField, 1, 3);

        dialog.getDialogPane().setContent(grid);
        Platform.runLater(idField::requestFocus);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == addButtonType) {
            try {
                String pid = idField.getText().trim();
                String med = medField.getText().trim();
                String dose = doseField.getText().trim();
                String times = timesField.getText().trim();

                if (pid.isEmpty() || med.isEmpty() || dose.isEmpty()) {
                    showAlert("Prescription ID, Medicine, and Dose are required!");
                    return;
                }

                // Optional: Parse times if entered
                List<LocalTime> timeList = new ArrayList<>();
                if (!times.isEmpty()) {
                    for (String t : times.split(",")) {
                        t = t.trim();
                        if (!t.isEmpty()) timeList.add(LocalTime.parse(t));
                    }
                }

                Resident resident = home.getBedList().get(sel.bedId).getResident();
                if (resident == null) {
                    showAlert("No resident found in this bed!");
                    return;
                }

                Prescription prescription = new Prescription(
                        pid,
                        staff.getId(),
                        resident.getId(),
                        med,
                        dose,
                        timeList
                );

                home.writePrescription((Doctor) staff, String.valueOf(sel.bedId), prescription);
                showAlert("Prescription added successfully!");

            } catch (Exception ex) {
                showAlert("Error: " + ex.getMessage());
            }
        }
    }

    private void showAlert(String m) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, m);
        a.showAndWait();
    }
}
