package com.healthcare.home.controller;

import com.healthcare.home.auth.Access;
import com.healthcare.home.core.HealthCareHome;
import com.healthcare.home.entity.*;
import com.healthcare.home.exceptions.UnAuthorizationException;
import com.healthcare.home.staff.*;
import com.healthcare.home.util.ActionLogger;
import com.healthcare.home.util.AuthService;
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

public class DoctorDashboardController extends BaseDashboardController {
    private Staff staff;

    @FXML
    private TableView<com.healthcare.home.controller.ManagerDashboardController.BedRow> bedTable;
    @FXML
    private TableColumn<com.healthcare.home.controller.ManagerDashboardController.BedRow, String> colBedId;
    @FXML
    private TableColumn<com.healthcare.home.controller.ManagerDashboardController.BedRow, String> colResident;
    @FXML
    private TableColumn<com.healthcare.home.controller.ManagerDashboardController.BedRow, String> colPrescription;
    @FXML
    private Button prescribeBtn;

    public void init(HealthCareHome home, Staff staff) {
        this.staff = staff;
        setHome(home);
        setupTable();
        refreshBeds();
        prescribeBtn.setVisible(staff.has(Access.WRITE_PRESCRIPTION));
    }

    private void setupTable() {
        setupCommonColumns(colBedId, colResident, colPrescription);
    }

    private HealthCareHome getHome() {
        return (HealthCareHome) super.home;
    }

    private void refreshBeds() {
        ObservableList<ManagerDashboardController.BedRow> rows = FXCollections.observableArrayList();
        for (Bed b : getHome().getBedList().values()) {
            String rn = b.isVacant() ? "" : b.getResident().getName();
            Gender g = b.isVacant() ? null : b.getResident().getGender();
            // include residentId to allow prescription lookup
            ManagerDashboardController.BedRow br =
                    new ManagerDashboardController.BedRow(b.getId(), rn, g);
            // attach resident id on the row via a public field
            try {
                br.residentId = b.isVacant() ? "" : b.getResident().getId();
            } catch (Exception ignored) {
            }
            rows.add(br);
        }
        bedTable.setItems(rows);
    }

    @FXML
    public void onPrescribe() {
        try {
            AuthService.authorizeOrThrow(staff, Access.WRITE_PRESCRIPTION);
            getHome().requireAuthorizeRole(staff, Role.DOCTOR);
            getHome().requireOnDutyStaff(staff);
        } catch (SecurityException | UnAuthorizationException se) {
            showAlert("Not allowed. " + se.getMessage());
            return;
        }

        ManagerDashboardController.BedRow sel = bedTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            showAlert("Select a bed that has a resident first");
            return;
        }

        Resident resident = getHome().getBedList().get(sel.bedId.get()).getResident();
        if (resident == null) {
            showAlert("No resident found in this bed!");
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

        Label infoLabel = new Label("Enter details and click 'Add More' to queue multiple medicines.");
        grid.add(infoLabel, 0, 4, 2, 1);

        dialog.getDialogPane().setContent(grid);
        Platform.runLater(medField::requestFocus);

        // Keep showing dialog until doctor presses Done or Cancel
        boolean done = false;
        while (!done) {
            Optional<ButtonType> result = dialog.showAndWait();

            if (result.isEmpty() || result.get() == ButtonType.CANCEL) {
                done = true;
            } else if (result.get() == addMoreButtonType || result.get() == doneButtonType) {
                String med = medField.getText().trim();
                String dose = doseField.getText().trim();
                String times = timesField.getText().trim();

                if (med.isEmpty() || dose.isEmpty()) {
                    showAlert("Medicine and dose are required!");
                    continue;
                }

                List<String> timeList = new ArrayList<>();
                if (!times.isEmpty()) {
                    for (String t : times.split(",")) {
                        t = t.trim();
                        if (!t.isEmpty()) timeList.add(t);
                    }
                }

                Prescription prescription = new Prescription(staff.getId(), med, dose, timeList);
                newPrescriptions.add(prescription);

                // clear for next entry
                medField.clear();
                doseField.clear();
                timesField.clear();

                if (result.get() == doneButtonType) {
                    done = true;
                }
            }
        }

        if (!newPrescriptions.isEmpty()) {

            getHome().writePrescription((Doctor) staff, sel.bedId.get(), newPrescriptions);

            for (Prescription p : newPrescriptions) {
                ActionLogger.log(staff.getId(), "WRITE_PRESCRIPTION",
                        "Prescription " + p.getId() + " added for resident " + resident.getId());
            }
            refreshBeds();
            showAlert("Added " + newPrescriptions.size() + " prescriptions successfully!");

        }
    }


    private void showAlert(String m) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, m);
        a.showAndWait();
    }
}
