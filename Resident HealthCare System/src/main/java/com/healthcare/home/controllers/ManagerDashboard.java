package com.healthcare.home.controllers;

import com.healthcare.home.audit.AuditTrailLog;
import com.healthcare.home.auth.AuthAccess;
import com.healthcare.home.core.ResidentHealthCareHome;
import com.healthcare.home.entities.Bed;
import com.healthcare.home.entities.BedRow;
import com.healthcare.home.entities.Resident;
import com.healthcare.home.entities.Gender;
import com.healthcare.home.staff.*;
import com.healthcare.home.auth.AuthService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.Optional;

/**
 * ManagerDashboard
 */
public class ManagerDashboard extends MainDashboard {
    private Staff staff;

    @FXML
    private TableView<BedRow> bedTable;
    @FXML
    private TableColumn<BedRow, String> columnBedId;
    @FXML
    private TableColumn<BedRow, String> columnResident;
    @FXML
    private TableColumn<BedRow, String> columnPrescription;
    @FXML
    private Button addResidentButton;
    @FXML
    private Button addStaffButton;
    @FXML
    private Button dischargeButton;

    /**
     * init
     *
     * @param home
     * @param staff
     */
    public void init(ResidentHealthCareHome home, Staff staff) {
        this.staff = staff;
        setHome(home);
        setupTable();
        refreshBeds();
        addResidentButton.setVisible(staff.hasAccess(AuthAccess.ADD_RESIDENT));
        addStaffButton.setVisible(staff.hasAccess(AuthAccess.ADD_STAFF));
        dischargeButton.setVisible(staff.hasAccess(AuthAccess.DISCHARGE_RESIDENT));
    }

    /**
     * refreshBeds method to refresh the bed list with updated items and values
     */
    private void refreshBeds() {
        ObservableList<BedRow> bedRows = FXCollections.observableArrayList();
        for (Bed bed : home.getBedList().values()) {
            String residentName = bed.isVacant() ? "" : bed.getResident().getName();
            Gender gender = bed.isVacant() ? null : bed.getResident().getGender();
            bedRows.add(new BedRow(bed.getId(), residentName, gender));
        }
        bedTable.setItems(bedRows);
    }

    /**
     * onAddResident
     */
    @FXML
    public void onAddResident() {
        try {
            AuthService.authorizeOrThrow(staff, AuthAccess.ADD_RESIDENT);
        } catch (SecurityException ex) {
            popupAlert("Not allowed. " + ex.getMessage());
            return;
        }

        BedRow selectedItem = bedTable.getSelectionModel().getSelectedItem();
        if (selectedItem == null) {
            popupAlert("Select a bed first");
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Add New Resident");
        dialog.setHeaderText("Enter Resident Details");
        ButtonType addButtonType = new ButtonType("Add", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);

        TextField nameField = new TextField();
        nameField.setPromptText("Resident Name");
        ComboBox<String> genderBox = new ComboBox<>();
        genderBox.getItems().addAll("MALE", "FEMALE");
        genderBox.setPromptText("Select Gender");
        ComboBox<String> isolationBox = new ComboBox<>();
        isolationBox.getItems().addAll("YES", "NO");
        isolationBox.setPromptText("Isolation Required?");

        var grid = new javafx.scene.layout.GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20, 150, 10, 10));
        grid.add(new Label("Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Gender:"), 0, 1);
        grid.add(genderBox, 1, 1);
        grid.add(new Label("Isolation:"), 0, 2);
        grid.add(isolationBox, 1, 2);

        dialog.getDialogPane().setContent(grid);
        javafx.application.Platform.runLater(nameField::requestFocus);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == addButtonType) {
            try {
                String name = nameField.getText().trim();
                String gender = genderBox.getValue();
                String isolation = isolationBox.getValue();
                if (name.isEmpty() || gender == null || isolation == null) {
                    popupAlert("All fields are required!");
                    return;
                }
                Resident resident = new Resident(name,
                        "MALE".equalsIgnoreCase(gender) ? Gender.MALE : Gender.FEMALE,
                        "YES".equalsIgnoreCase(isolation), selectedItem.getBedId());
                home.assigningResidentToBed(staff, String.valueOf(selectedItem.bedId.get()), resident);
                AuditTrailLog.entryLog(staff.getId(), "ADD_RESIDENT", "Assigned resident " + resident.getId() + " to bed " + selectedItem.bedId.get());
                refreshBeds();
                popupAlert("Resident added successfully!");
            } catch (Exception ex) {
                popupAlert("Error: " + ex.getMessage());
            }
        }
    }

    /**
     * onAddStaff
     */
    @FXML
    public void onAddStaff() {
        try {
            AuthService.authorizeOrThrow(staff, AuthAccess.ADD_STAFF);
        } catch (SecurityException se) {
            popupAlert("Not allowed. " + se.getMessage());
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Add New Staff");
        dialog.setHeaderText("Enter staff details");
        ButtonType addButtonType = new ButtonType("Add", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);

        TextField nameField = new TextField();
        nameField.setPromptText("Full Name");
        ComboBox<String> roleBox = new ComboBox<>();
        roleBox.getItems().addAll("MANAGER", "DOCTOR", "NURSE");
        roleBox.setPromptText("Select Role");
        TextField usernameField = new TextField();
        usernameField.setPromptText("Username");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");

        var grid = new javafx.scene.layout.GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20, 150, 10, 10));
        grid.add(new Label("Full Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Role:"), 0, 1);
        grid.add(roleBox, 1, 1);
        grid.add(new Label("Username:"), 0, 2);
        grid.add(usernameField, 1, 2);
        grid.add(new Label("Password:"), 0, 3);
        grid.add(passwordField, 1, 3);

        dialog.getDialogPane().setContent(grid);
        javafx.application.Platform.runLater(nameField::requestFocus);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == addButtonType) {
            try {
                String name = nameField.getText().trim();
                String role = roleBox.getValue();
                String username = usernameField.getText().trim();
                String password = passwordField.getText().trim();
                if (name.isEmpty() || role == null || username.isEmpty() || password.isEmpty()) {
                    popupAlert("All fields are required!");
                    return;
                }

                Staff newStaff = switch (role) {
                    case "MANAGER" -> new Manager(name, username, password);
                    case "DOCTOR" -> new Doctor(name, username, password);
                    case "NURSE" -> new Nurse(name, username, password);
                    default -> null;
                };
                home.addingNewStaff(staff, newStaff);
                AuditTrailLog.entryLog(staff.getId(), "ADD_STAFF", "Added staff " + newStaff.getId() + " role " + role);
                refreshBeds();
                popupAlert("Staff added successfully!");
            } catch (Exception ex) {
                popupAlert("Error: " + ex.getMessage());
            }
        }
    }

    /**
     * onDischarge
     */
    @FXML
    public void onDischarge() {
        try {
            AuthService.authorizeOrThrow(staff, AuthAccess.DISCHARGE_RESIDENT);
        } catch (SecurityException ex) {
            popupAlert("Not allowed. " + ex.getMessage());
            return;
        }

        BedRow selectedItem = bedTable.getSelectionModel().getSelectedItem();
        if (selectedItem == null || selectedItem.getResidentName() == null || selectedItem.getResidentName().isEmpty()) {
            popupAlert("Select a bed with a resident to discharge");
            return;
        }

        Bed bed = home.getBedList().get(selectedItem.getBedId());
        if (bed == null || bed.isVacant()) {
            popupAlert("This bed is already vacant");
            return;
        }

        Resident resident = bed.getResident();
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirm Discharge");
        confirmAlert.setHeaderText("Discharge Resident");
        confirmAlert.setContentText("Are you sure you want to discharge " + resident.getName() + "?");

        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                home.dischargingResident(staff, resident.getBedId());
                AuditTrailLog.entryLog(staff.getId(), "DISCHARGE_RESIDENT", "Discharged resident " + resident.getId());
                refreshBeds();
                popupAlert("Resident " + resident.getName() + " has been discharged successfully.");
            } catch (Exception ex) {
                popupAlert("Error discharging resident: " + ex.getMessage());
            }
        }
    }

    /**
     * showResidentDetails
     *
     * @param bedId
     */
    private void showResidentDetails(String bedId) {
        try {
            Bed bed = home.getBedList().get(bedId);
            if (bed == null || bed.isVacant()) {
                popupAlert("No resident");
                return;
            }
            Resident resident = bed.getResident();
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "Resident: " + resident.getId() + " - " + resident.getName());
            alert.setHeaderText("Resident Details");
            alert.showAndWait();
            AuditTrailLog.entryLog(staff.getId(), "VIEW_RESIDENT", "Viewed resident " + resident.getId() + " from bed " + bedId);
        } catch (Exception ex) {
            popupAlert(ex.getMessage());
        }
    }

    /**
     * popupAlert
     *
     * @param message
     */
    private void popupAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message);
        alert.showAndWait();
    }

    /**
     * setupTable
     */
    private void setupTable() {
        setupCommonColumns(columnBedId, columnResident, columnPrescription);
        bedTable.setRowFactory(tableView -> {
            TableRow<BedRow> tableRow = new TableRow<>();
            tableRow.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !tableRow.isEmpty()) {
                    BedRow rowData = tableRow.getItem();
                    showResidentDetails(String.valueOf(rowData.bedId.get()));
                }
            });
            return tableRow;
        });
    }
}
