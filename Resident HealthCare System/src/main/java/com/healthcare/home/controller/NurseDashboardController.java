package com.healthcare.home.controller;

import com.healthcare.home.auth.Access;
import com.healthcare.home.core.HealthCareHome;
import com.healthcare.home.entity.Bed;
import com.healthcare.home.staff.*;
import com.healthcare.home.util.ActionLogger;
import com.healthcare.home.util.AuthService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.geometry.Insets;
import javafx.application.Platform;

import java.util.Optional;

public class NurseDashboardController extends BaseDashboardController {
    private Staff staff;

    @FXML private TableView<com.healthcare.home.controller.ManagerDashboardController.BedRow> bedTable;
    @FXML private TableColumn<com.healthcare.home.controller.ManagerDashboardController.BedRow, String> colBedId;
    @FXML private TableColumn<com.healthcare.home.controller.ManagerDashboardController.BedRow, String> colResident;
    @FXML private Button moveBtn;
    @FXML private Button administerBtn;

    public void init(HealthCareHome home, Staff staff) {
        this.staff = staff;
        setHome(home);
        setupTable(); refreshBeds();
        moveBtn.setVisible(staff.has(Access.MOVE_RESIDENT));
        administerBtn.setVisible(staff.has(Access.ADMINISTER_MEDICATION));
    }

    private void setupTable() {
        colBedId.setCellValueFactory(c -> javafx.beans.property.SimpleStringProperty.stringExpression(c.getValue().bedId));
        colResident.setCellValueFactory(c -> javafx.beans.property.SimpleStringProperty.stringExpression(c.getValue().residentName));
    }

    private void refreshBeds() {
        ObservableList<ManagerDashboardController.BedRow> rows = FXCollections.observableArrayList();
        for (Bed b : home.getBedList().values()) {
            String rn = b.isVacant() ? "" : b.getResident().getName();
            rows.add(new com.healthcare.home.controller.ManagerDashboardController.BedRow(b.getId(), rn, b.isVacant() ? null : b.getResident().getGender()));
        }
        bedTable.setItems(rows);
    }

    @FXML public void onMove() {
        try {
            AuthService.authorizeOrThrow(staff, Access.MOVE_RESIDENT);
        } catch (SecurityException se) {
            showAlert("Not allowed. " + se.getMessage());
            return;
        }

        ManagerDashboardController.BedRow sel = bedTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            showAlert("Select source bed");
            return;
        }
        TextInputDialog dlg = new TextInputDialog();
        dlg.setTitle("Move Resident");
        dlg.setHeaderText("Enter destination bed id");
        dlg.showAndWait().ifPresent(dest -> {
            try {
                home.moveResidentToNewBed((Nurse) staff, String.valueOf(sel.bedId.get()), dest.trim());
                ActionLogger.log(staff.getId(), "MOVE_RESIDENT", "Moved resident from " + sel.bedId.get() + " to " + dest.trim());
                refreshBeds();
                showAlert("Moved");
            } catch (Exception ex) { showAlert(ex.getMessage()); }
        });
    }

    @FXML
    public void onAdminister() {
        try {
            AuthService.authorizeOrThrow(staff, Access.ADMINISTER_MEDICATION);
        } catch (SecurityException se) {
            showAlert("Not allowed. " + se.getMessage());
            return;
        }

        ManagerDashboardController.BedRow sel = bedTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            showAlert("Select a bed with a resident first");
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Administer Medication");
        dialog.setHeaderText("Enter Medication Administration Details");

        ButtonType adminButtonType = new ButtonType("Administer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(adminButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField prescriptionIdField = new TextField();
        prescriptionIdField.setPromptText("Prescription ID");

        TextField doseField = new TextField();
        doseField.setPromptText("Dose Given (e.g., 1 tablet)");

        grid.add(new Label("Prescription ID:"), 0, 0);
        grid.add(prescriptionIdField, 1, 0);
        grid.add(new Label("Dose:"), 0, 1);
        grid.add(doseField, 1, 1);

        dialog.getDialogPane().setContent(grid);
        Platform.runLater(prescriptionIdField::requestFocus);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == adminButtonType) {
            try {
                String prescriptionId = prescriptionIdField.getText().trim();
                String dose = doseField.getText().trim();

                if (prescriptionId.isEmpty() || dose.isEmpty()) {
                    showAlert("Both fields are required!");
                    return;
                }

                home.administerMedication((Nurse) staff, prescriptionId, dose);
                ActionLogger.log(staff.getId(), "ADMINISTER_MEDICATION", "Administered " + dose + " for prescription " + prescriptionId);
                showAlert("Medication administered successfully!");

            } catch (Exception ex) {
                showAlert("Error: " + ex.getMessage());
            }
        }
    }

    private void showAlert(String m) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, m); a.showAndWait();
    }
}
