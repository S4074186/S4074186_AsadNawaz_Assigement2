package com.healthcare.home.controller;

import com.healthcare.home.auth.Access;
import com.healthcare.home.core.HealthCareHome;
import com.healthcare.home.entity.Bed;
import com.healthcare.home.entity.Resident;
import com.healthcare.home.entity.Gender;
import com.healthcare.home.staff.*;
import com.healthcare.home.util.ActionLogger;
import com.healthcare.home.util.AuthService;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.Optional;

public class ManagerDashboardController extends BaseDashboardController {
    private Staff staff;

    @FXML private TableView<BedRow> bedTable;
    @FXML private TableColumn<BedRow, String> colBedId;
    @FXML private TableColumn<BedRow, String> colResident;
    @FXML private TableColumn<BedRow, String> colPrescription;
    @FXML private Button addResidentBtn;
    @FXML private Button addStaffBtn;
    @FXML private Button dischargeBtn;

    public void init(HealthCareHome home, Staff staff) {
        this.staff = staff;
        setHome(home);
        setupTable();
        refreshBeds();
        addResidentBtn.setVisible(staff.has(Access.ADD_RESIDENT));
        addStaffBtn.setVisible(staff.has(Access.ADD_STAFF));
        dischargeBtn.setVisible(staff.has(Access.DISCHARGE_RESIDENT));
    }

    private void refreshBeds() {
        ObservableList<BedRow> rows = FXCollections.observableArrayList();
        for (Bed b : home.getBedList().values()) {
            String rn = b.isVacant() ? "" : b.getResident().getName();
            Gender g = b.isVacant() ? null : b.getResident().getGender();
            rows.add(new BedRow(b.getId(), rn, g));
        }
        bedTable.setItems(rows);
    }

    @FXML
    public void onAddResident() {
        try {
            AuthService.authorizeOrThrow(staff, Access.ADD_RESIDENT);
        } catch (SecurityException se) {
            showAlert("Not allowed. " + se.getMessage());
            return;
        }

        BedRow sel = bedTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            showAlert("Select a bed first");
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
                    showAlert("All fields are required!");
                    return;
                }
                Resident resident = new Resident(name,
                        "MALE".equalsIgnoreCase(gender) ? Gender.MALE : Gender.FEMALE,
                        "YES".equalsIgnoreCase(isolation), sel.getBedId());
                home.assignResidentToBed(staff, String.valueOf(sel.bedId.get()), resident);
                ActionLogger.log(staff.getId(), "ADD_RESIDENT", "Assigned resident " + resident.getId() + " to bed " + sel.bedId.get());
                refreshBeds();
                showAlert("Resident added successfully!");
            } catch (Exception ex) {
                showAlert("Error: " + ex.getMessage());
            }
        }
    }

    @FXML
    public void onAddStaff() {
        try {
            AuthService.authorizeOrThrow(staff, Access.ADD_STAFF);
        } catch (SecurityException se) {
            showAlert("Not allowed. " + se.getMessage());
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
                    showAlert("All fields are required!");
                    return;
                }

                Staff newStaff = switch (role) {
                    case "MANAGER" -> new Manager(name, username, password);
                    case "DOCTOR" -> new Doctor(name, username, password);
                    case "NURSE" -> new Nurse(name, username, password);
                    default -> null;
                };
                home.addNewStaff(staff, newStaff);
                ActionLogger.log(staff.getId(), "ADD_STAFF", "Added staff " + newStaff.getId() + " role " + role);
                refreshBeds();
                showAlert("Staff added successfully!");
            } catch (Exception ex) {
                showAlert("Error: " + ex.getMessage());
            }
        }
    }

    @FXML
    public void onDischarge() {
        try {
            AuthService.authorizeOrThrow(staff, Access.DISCHARGE_RESIDENT);
        } catch (SecurityException se) {
            showAlert("Not allowed. " + se.getMessage());
            return;
        }

        BedRow sel = bedTable.getSelectionModel().getSelectedItem();
        if (sel == null || sel.getResidentName() == null || sel.getResidentName().isEmpty()) {
            showAlert("Select a bed with a resident to discharge");
            return;
        }

        Bed bed = home.getBedList().get(sel.getBedId());
        if (bed == null || bed.isVacant()) {
            showAlert("This bed is already vacant");
            return;
        }

        Resident r = bed.getResident();
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Discharge");
        confirm.setHeaderText("Discharge Resident");
        confirm.setContentText("Are you sure you want to discharge " + r.getName() + "?");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                home.dischargeResident(staff, r.getBedId());
                ActionLogger.log(staff.getId(), "DISCHARGE_RESIDENT", "Discharged resident " + r.getId());
                refreshBeds();
                showAlert("Resident " + r.getName() + " has been discharged successfully.");
            } catch (Exception ex) {
                showAlert("Error discharging resident: " + ex.getMessage());
            }
        }
    }

    private void showResidentDetails(String bedId) {
        try {
            Bed b = home.getBedList().get(bedId);
            if (b == null || b.isVacant()) {
                showAlert("No resident");
                return;
            }
            Resident r = b.getResident();
            Alert a = new Alert(Alert.AlertType.INFORMATION, "Resident: " + r.getId() + " - " + r.getName());
            a.setHeaderText("Resident Details");
            a.showAndWait();
            ActionLogger.log(staff.getId(), "VIEW_RESIDENT", "Viewed resident " + r.getId() + " from bed " + bedId);
        } catch (Exception ex) {
            showAlert(ex.getMessage());
        }
    }

    private void showAlert(String m) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, m);
        a.showAndWait();
    }

    public static class BedRow {
        public final StringProperty bedId;
        public final StringProperty residentName;
        public String residentId;
        public final Gender gender;

        public BedRow(String b, String r, Gender g) {
            this.bedId = new SimpleStringProperty(b);
            this.residentName = new SimpleStringProperty(r);
            this.gender = g;
        }

        public String getBedId() {
            return bedId.get();
        }

        public String getResidentName() {
            return residentName.get();
        }
    }

    private void setupTable() {
        setupCommonColumns(colBedId, colResident, colPrescription);
        bedTable.setRowFactory(tv -> {
            TableRow<BedRow> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    BedRow rowData = row.getItem();
                    showResidentDetails(String.valueOf(rowData.bedId.get()));
                }
            });
            return row;
        });
    }
}
