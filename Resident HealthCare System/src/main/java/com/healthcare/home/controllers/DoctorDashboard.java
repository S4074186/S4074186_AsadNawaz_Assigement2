package com.healthcare.home.controllers;

import com.healthcare.home.audit.AuditTrailLog;
import com.healthcare.home.auth.AuthAccess;
import com.healthcare.home.core.ResidentHealthCareHome;
import com.healthcare.home.entities.*;
import com.healthcare.home.exceptions.UnAuthorizationException;
import com.healthcare.home.staff.*;
import com.healthcare.home.auth.AuthService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.geometry.Insets;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DoctorDashboard extends MainDashboard {
    private Staff staff;

    @FXML
    private TableView<BedRow> bedTable;
    @FXML
    private TableColumn<BedRow, String> columnBed;
    @FXML
    private TableColumn<BedRow, String> columnResident;
    @FXML
    private TableColumn<BedRow, String> columnPrescription;
    @FXML
    private Button prescribeButton;

    public void init(ResidentHealthCareHome home, Staff staff) {
        this.staff = staff;
        setHome(home);
        setupTable();
        refreshBeds();
        prescribeButton.setVisible(staff.hasAccess(AuthAccess.WRITE_PRESCRIPTION));
    }

    private void setupTable() {
        setupCommonColumns(columnBed, columnResident, columnPrescription);
    }

    private ResidentHealthCareHome getHome() {
        return (ResidentHealthCareHome) super.home;
    }

    private void refreshBeds() {
        ObservableList<BedRow> rows = FXCollections.observableArrayList();
        for (Bed bed : getHome().getBedList().values()) {
            String residentName = bed.isVacant() ? "" : bed.getResident().getName();
            Gender gender = bed.isVacant() ? null : bed.getResident().getGender();
            // include residentId to allow prescription lookup
            BedRow bedRow = new BedRow(bed.getId(), residentName, gender);
            // attach resident id on the row via a public field
            try {
                bedRow.residentId = bed.isVacant() ? "" : bed.getResident().getId();
            } catch (Exception ignored) {
            }
            rows.add(bedRow);
        }
        bedTable.setItems(rows);
    }

    @FXML
    public void onPrescribe() {
        try {
            AuthService.authorizeOrThrow(staff, AuthAccess.WRITE_PRESCRIPTION);
            getHome().requireAuthorizeRole(staff, Role.DOCTOR);
            getHome().requireOnDutyStaff(staff);
        } catch (SecurityException | UnAuthorizationException ex) {
            popupAlert("Not allowed. " + ex.getMessage());
            return;
        }

        BedRow selectedItem = bedTable.getSelectionModel().getSelectedItem();
        if (selectedItem == null) {
            popupAlert("Select a bed that has a resident first");
            return;
        }

        Resident resident = getHome().getBedList().get(selectedItem.bedId.get()).getResident();
        if (resident == null) {
            popupAlert("No resident found in this bed!");
            return;
        }

        List<Prescription> newPrescriptions = new ArrayList<>();

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Write Prescription");
        dialog.setHeaderText("Enter Prescription Details");

        ButtonType addMoreButtonType = new ButtonType("Add More", ButtonBar.ButtonData.APPLY);
        ButtonType doneButtonType = new ButtonType("Done", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addMoreButtonType, doneButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField medicineField = new TextField();
        medicineField.setPromptText("Medicine Name");
        TextField doseField = new TextField();
        doseField.setPromptText("Dose Description (e.g., 1 tablet)");
        TextField timesField = new TextField();
        timesField.setPromptText("Times (comma separated, HH:mm optional)");

        grid.add(new Label("Medicine:"), 0, 1);
        grid.add(medicineField, 1, 1);
        grid.add(new Label("Dose:"), 0, 2);
        grid.add(doseField, 1, 2);
        grid.add(new Label("Times:"), 0, 3);
        grid.add(timesField, 1, 3);

        Label infoLabel = new Label("Enter details and click 'Add More' to queue multiple medicines.");
        grid.add(infoLabel, 0, 4, 2, 1);

        dialog.getDialogPane().setContent(grid);
        Platform.runLater(medicineField::requestFocus);

        // Keep showing dialog until doctor presses Done or Cancel
        boolean done = false;
        while (!done) {
            Optional<ButtonType> result = dialog.showAndWait();

            if (result.isEmpty() || result.get() == ButtonType.CANCEL) {
                done = true;
            } else if (result.get() == addMoreButtonType || result.get() == doneButtonType) {
                String medicine = medicineField.getText().trim();
                String dose = doseField.getText().trim();
                String times = timesField.getText().trim();

                if (medicine.isEmpty() || dose.isEmpty()) {
                    popupAlert("Medicine and dose are required!");
                    continue;
                }

                List<String> timeList = new ArrayList<>();
                if (!times.isEmpty()) {
                    for (String time : times.split(",")) {
                        time = time.trim();
                        if (!time.isEmpty()) timeList.add(time);
                    }
                }

                Prescription prescription = new Prescription(staff.getId(), medicine, dose, timeList);
                newPrescriptions.add(prescription);

                // clear for next entry
                medicineField.clear();
                doseField.clear();
                timesField.clear();

                if (result.get() == doneButtonType) {
                    done = true;
                }
            }
        }

        if (!newPrescriptions.isEmpty()) {

            getHome().writingPrescription((Doctor) staff, selectedItem.bedId.get(), newPrescriptions);

            for (Prescription newPrescription : newPrescriptions) {
                AuditTrailLog.entryLog(staff.getId(), "WRITE_PRESCRIPTION",
                        "Prescription " + newPrescription.getId() + " added for resident " + resident.getId());
            }
            refreshBeds();
            popupAlert("Added " + newPrescriptions.size() + " prescriptions successfully!");

        }
    }

    private void popupAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, msg);
        alert.showAndWait();
    }
}
