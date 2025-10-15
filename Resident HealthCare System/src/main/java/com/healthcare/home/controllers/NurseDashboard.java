package com.healthcare.home.controllers;

import com.healthcare.home.audit.AuditTrailLog;
import com.healthcare.home.auth.AuthAccess;
import com.healthcare.home.core.ResidentHealthCareHome;
import com.healthcare.home.entities.*;
import com.healthcare.home.exceptions.UnAuthorizationException;
import com.healthcare.home.staff.Nurse;
import com.healthcare.home.staff.Staff;
import com.healthcare.home.auth.AuthService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.List;

public class NurseDashboard extends MainDashboard {
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
    private Button moveButton;
    @FXML
    private Button administerButton;

    public void init(ResidentHealthCareHome home, Staff staff) {
        this.staff = staff;
        setHome(home);
        setupTable();
        refreshBeds();
        moveButton.setVisible(staff.hasAccess(AuthAccess.MOVE_RESIDENT));
        administerButton.setVisible(staff.hasAccess(AuthAccess.ADMINISTER_MEDICATION));
    }

    private ResidentHealthCareHome getHome() {
        return (ResidentHealthCareHome) super.home;
    }

    private void refreshBeds() {
        ObservableList<BedRow> rows = FXCollections.observableArrayList();
        for (Bed bed : getHome().getBedList().values()) {
            String residentName = bed.isVacant() ? "" : bed.getResident().getName();
            BedRow row = new BedRow(bed.getId(), residentName, bed.isVacant() ? null : bed.getResident().getGender());
            row.residentId = bed.isVacant() ? "" : bed.getResident().getId();
            rows.add(row);
        }
        bedTable.setItems(rows);
    }

    private void setupTable() {
        setupCommonColumns(columnBed, columnResident, columnPrescription);
    }

    // Allow nurse to move resident to another bed
    @FXML
    public void onMove() {
        try {
            AuthService.authorizeOrThrow(staff, AuthAccess.MOVE_RESIDENT);
        } catch (SecurityException ex) {
            popupAlert("Not allowed. " + ex.getMessage());
            return;
        }

        BedRow selectedItem = bedTable.getSelectionModel().getSelectedItem();
        if (selectedItem == null) {
            popupAlert("Select source bed first");
            return;
        }

        // Collect vacant beds
        var vacantBeds = getHome().getBedList().values().stream()
                .filter(Bed::isVacant)
                .map(Bed::getId)
                .toList();

        if (vacantBeds.isEmpty()) {
            popupAlert("No vacant beds available right now");
            return;
        }

        ChoiceDialog<String> dialog = new ChoiceDialog<>(vacantBeds.get(0), vacantBeds);
        dialog.setTitle("Move Resident");
        dialog.setHeaderText("Move resident from Bed " + selectedItem.bedId.get());
        dialog.setContentText("Select destination bed:");

        dialog.showAndWait().ifPresent(destination -> {
            try {
                getHome().movingResidentToNewBed((Nurse) staff, String.valueOf(selectedItem.bedId.get()), destination);
                AuditTrailLog.entryLog(staff.getId(), "MOVE_RESIDENT",
                        "Moved resident from " + selectedItem.bedId.get() + " to " + destination);
                refreshBeds();
                popupAlert("Resident moved to bed " + destination + " successfully!");
            } catch (Exception ex) {
                popupAlert("Error: " + ex.getMessage());
            }
        });
    }

    @FXML
    public void onAdminister() {
        try {
            AuthService.authorizeOrThrow(staff, AuthAccess.ADMINISTER_MEDICATION);
            getHome().requireAuthorizeRole(staff, Role.NURSE);
            getHome().requireOnDutyStaff(staff);
        } catch (SecurityException | UnAuthorizationException ex) {
            popupAlert("Not allowed. " + ex.getMessage());
            return;
        } catch (Exception ex) {
            popupAlert("Error: " + ex.getMessage());
            return;
        }

        BedRow selected = bedTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            popupAlert("Select a bed that has a resident first");
            return;
        }

        Resident resident = getHome().getResidentInBed(String.valueOf(selected.bedId.get()));
        if (resident == null) {
            popupAlert("No resident found in this bed");
            return;
        }

        if (resident.getPrescriptionList() == null || resident.getPrescriptionList().isEmpty()) {
            popupAlert("No prescription found for this resident");
            return;
        }

        // Create medicine selection dialog
        List<String> medicineNames = resident.getPrescriptionList().stream()
                .map(prescription -> prescription.getMedicine() + " (" + prescription.getDose() + ")")
                .toList();

        ChoiceDialog<String> dialog = new ChoiceDialog<>(medicineNames.get(0), medicineNames);
        dialog.setTitle("Administer Medicine");
        dialog.setHeaderText("Select medicine to administer");
        dialog.setContentText("Medicine:");

        dialog.showAndWait().ifPresent(selectedMedicine -> {
            try {
                String medicineName = selectedMedicine.split(" ")[0];
                for (Prescription prescription : resident.getPrescriptionList()) {
                    if (prescription.getMedicine().equalsIgnoreCase(medicineName)) {
                        prescription.administer(staff.getId()); // mark as administered
                        AuditTrailLog.entryLog(staff.getId(), "ADMINISTER_MEDICINE",
                                "Administered " + medicineName + " to resident " + resident.getName());
                        popupAlert("Dose administered for " + medicineName);
                        break;
                    }
                }
                getHome().saveAllStateToFile(null);
                refreshBeds();
            } catch (Exception ex) {
                popupAlert("Error: " + ex.getMessage());
            }
        });
    }

    private void popupAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, msg);
        alert.showAndWait();
    }
}
