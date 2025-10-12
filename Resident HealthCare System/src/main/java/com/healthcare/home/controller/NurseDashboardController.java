package com.healthcare.home.controller;

import com.healthcare.home.auth.Access;
import com.healthcare.home.core.HealthCareHome;
import com.healthcare.home.entity.Bed;
import com.healthcare.home.entity.Prescription;
import com.healthcare.home.entity.Resident;
import com.healthcare.home.staff.Nurse;
import com.healthcare.home.staff.Staff;
import com.healthcare.home.util.ActionLogger;
import com.healthcare.home.util.AuthService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class NurseDashboardController extends BaseDashboardController {
    private Staff staff;

    @FXML
    private TableView<ManagerDashboardController.BedRow> bedTable;
    @FXML
    private TableColumn<ManagerDashboardController.BedRow, String> colBedId;
    @FXML
    private TableColumn<ManagerDashboardController.BedRow, String> colResident;
    @FXML
    private TableColumn<ManagerDashboardController.BedRow, String> colPrescription;
    @FXML
    private Button moveBtn;
    @FXML
    private Button administerBtn;

    public void init(HealthCareHome home, Staff staff) {
        this.staff = staff;
        setHome(home);
        setupTable();
        refreshBeds();
        moveBtn.setVisible(staff.has(Access.MOVE_RESIDENT));
        administerBtn.setVisible(staff.has(Access.ADMINISTER_MEDICATION));
    }

    private HealthCareHome getHome() {
        return (HealthCareHome) super.home;
    }

    private void refreshBeds() {
        ObservableList<ManagerDashboardController.BedRow> rows = FXCollections.observableArrayList();
        for (Bed b : getHome().getBedList().values()) {
            String rn = b.isVacant() ? "" : b.getResident().getName();
            ManagerDashboardController.BedRow row =
                    new ManagerDashboardController.BedRow(b.getId(), rn, b.isVacant() ? null : b.getResident().getGender());
            row.residentId = b.isVacant() ? "" : b.getResident().getId();
            rows.add(row);
        }
        bedTable.setItems(rows);
    }

    private void setupTable() {
        setupCommonColumns(colBedId, colResident, colPrescription);
    }

    // Allow nurse to move resident to another bed
    @FXML
    public void onMove() {
        try {
            AuthService.authorizeOrThrow(staff, Access.MOVE_RESIDENT);
        } catch (SecurityException se) {
            showAlert("Not allowed. " + se.getMessage());
            return;
        }

        ManagerDashboardController.BedRow sel = bedTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            showAlert("Select source bed first");
            return;
        }

        // Collect vacant beds
        var vacantBeds = getHome().getBedList().values().stream()
                .filter(Bed::isVacant)
                .map(Bed::getId)
                .toList();

        if (vacantBeds.isEmpty()) {
            showAlert("No vacant beds available right now");
            return;
        }

        ChoiceDialog<String> dlg = new ChoiceDialog<>(vacantBeds.get(0), vacantBeds);
        dlg.setTitle("Move Resident");
        dlg.setHeaderText("Move resident from Bed " + sel.bedId.get());
        dlg.setContentText("Select destination bed:");

        dlg.showAndWait().ifPresent(dest -> {
            try {
                getHome().moveResidentToNewBed((Nurse) staff, String.valueOf(sel.bedId.get()), dest);
                ActionLogger.log(staff.getId(), "MOVE_RESIDENT",
                        "Moved resident from " + sel.bedId.get() + " to " + dest);
                refreshBeds();
                showAlert("Resident moved to bed " + dest + " successfully!");
            } catch (Exception ex) {
                showAlert("Error: " + ex.getMessage());
            }
        });
    }


    // Nurse administers medication (no need to manually enter prescriptionId)
    @FXML
    public void onAdminister() {
        try {
            AuthService.authorizeOrThrow(staff, Access.ADMINISTER_MEDICATION);
        } catch (SecurityException se) {
            showAlert("Not allowed. " + se.getMessage());
            return;
        }

        ManagerDashboardController.BedRow selected = bedTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Select a bed that has a resident first");
            return;
        }

        Resident resident = getHome().getResidentInBed(String.valueOf(selected.bedId.get()));
        if (resident == null) {
            showAlert("No resident found in this bed");
            return;
        }

        if (resident.getPrescriptionList() == null || resident.getPrescriptionList().isEmpty()) {
            showAlert("No prescription found for this resident");
            return;
        }

        // Create medicine selection dialog
        List<String> medNames = resident.getPrescriptionList().stream()
                .map(p -> p.getMedicine() + " (" + p.getDose() + ")")
                .toList();

        ChoiceDialog<String> dialog = new ChoiceDialog<>(medNames.get(0), medNames);
        dialog.setTitle("Administer Medicine");
        dialog.setHeaderText("Select medicine to administer");
        dialog.setContentText("Medicine:");
        dialog.showAndWait().ifPresent(selectedMed -> {
            String medName = selectedMed.split(" ")[0];
            for (Prescription p : resident.getPrescriptionList()) {
                if (p.getMedicine().equalsIgnoreCase(medName)) {
                    p.administer(staff.getId()); // mark as administered
                    ActionLogger.log(staff.getId(), "ADMINISTER_MEDICINE",
                            "Administered " + medName + " to resident " + resident.getName());
                    showAlert("Dose administered for " + medName);
                    break;
                }
            }
            getHome().saveAllStateToFile(null);
            refreshBeds();
        });
    }



    private void showAlert(String m) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, m);
        a.showAndWait();
    }
}
