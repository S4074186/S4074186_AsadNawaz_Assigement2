package com.healthcare.home.controller;

import com.healthcare.home.auth.Access;
import com.healthcare.home.core.HealthCareHome;
import com.healthcare.home.entity.Prescription;
import com.healthcare.home.entity.Resident;
import com.healthcare.home.staff.*;
import com.healthcare.home.util.ActionLogger;
import com.healthcare.home.util.AuthService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.geometry.Insets;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DoctorDashboardController extends BaseDashboardController {
    private Staff staff;

    @FXML private TableView<com.healthcare.home.controller.ManagerDashboardController.BedRow> bedTable;
    @FXML private TableColumn<com.healthcare.home.controller.ManagerDashboardController.BedRow, String> colBedId;
    @FXML private TableColumn<com.healthcare.home.controller.ManagerDashboardController.BedRow, String> colResident;
    @FXML private Button prescribeBtn;

    public void init(HealthCareHome home, Staff staff) {
        this.staff = staff;
        setHome(home);
        setupTable(); refreshBeds();
        prescribeBtn.setVisible(staff.has(Access.WRITE_PRESCRIPTION));
    }

    private void setupTable() {
        colBedId.setCellValueFactory(c -> javafx.beans.property.SimpleStringProperty.stringExpression(c.getValue().bedId));
        colResident.setCellValueFactory(c -> javafx.beans.property.SimpleStringProperty.stringExpression(c.getValue().residentName));
    }

    private void refreshBeds() {
        ObservableList<com.healthcare.home.controller.ManagerDashboardController.BedRow> rows = FXCollections.observableArrayList();
        for (com.healthcare.home.entity.Bed b : home.getBedList().values()) {
            String rn = b.isVacant() ? "" : b.getResident().getName();
            rows.add(new com.healthcare.home.controller.ManagerDashboardController.BedRow(b.getId(), rn, b.isVacant() ? null : b.getResident().getGender()));
        }
        bedTable.setItems(rows);
    }

    @FXML
    public void onPrescribe() {
        try {
            AuthService.authorizeOrThrow(staff, Access.WRITE_PRESCRIPTION);
        } catch (SecurityException se) {
            showAlert("Not allowed. " + se.getMessage());
            return;
        }

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

        TextField medField = new TextField();
        medField.setPromptText("Medicine Name");

        TextField doseField = new TextField();
        doseField.setPromptText("Dose Description (e.g., 1 tablet)");

        TextField timesField = new TextField();
        timesField.setPromptText("Times (comma separated, HH:mm optional)");

        grid.add(new Label("Medicine:"), 0, 1);
        grid.add(medField, 1, 1);
        grid.add(new Label("Dose:"), 0, 2);
        grid.add(doseField, 1, 2);
        grid.add(new Label("Times:"), 0, 3);
        grid.add(timesField, 1, 3);

        dialog.getDialogPane().setContent(grid);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == addButtonType) {
            try {
                String med = medField.getText().trim();
                String dose = doseField.getText().trim();
                String times = timesField.getText().trim();

                if (med.isEmpty() || dose.isEmpty()) {
                    showAlert("Prescription ID, Medicine, and Dose are required!");
                    return;
                }

                List<String> timeList = new ArrayList<>();
                if (!times.isEmpty()) {
                    for (String t : times.split(",")) {
                        t = t.trim();
                        if (!t.isEmpty()) timeList.add(t);
                    }
                }

                Resident resident = home.getBedList().get(sel.bedId.get()).getResident();
                if (resident == null) {
                    showAlert("No resident found in this bed!");
                    return;
                }

                Prescription prescription = new Prescription(
                        staff.getId(),
                        resident.getId(),
                        med,
                        dose,
                        timeList
                );

                home.writePrescription((Doctor) staff, String.valueOf(sel.bedId.get()), prescription);
                ActionLogger.log(staff.getId(), "WRITE_PRESCRIPTION", "Prescription " + prescription.getId() + " for resident " + resident.getId());
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
