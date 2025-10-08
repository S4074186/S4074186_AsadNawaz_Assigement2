
package com.healthcare.home.controller;

import com.healthcare.home.core.HealthCareHome;
import com.healthcare.home.entity.Bed;
import com.healthcare.home.entity.Gender;
import com.healthcare.home.entity.Resident;
import com.healthcare.home.staff.Doctor;
import com.healthcare.home.staff.Manager;
import com.healthcare.home.staff.Nurse;
import com.healthcare.home.staff.Staff;
import com.healthcare.home.auth.Access;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

import java.util.Optional;

public class ManagerDashboardController extends BaseDashboardController {
    private HealthCareHome home;
    private Staff staff;

    @FXML private TableView<BedRow> bedTable;
    @FXML private TableColumn<BedRow, String> colBedId;
    @FXML private TableColumn<BedRow, String> colResident;
    @FXML private Button addResidentBtn;
    @FXML private Button addStaffBtn;

    public void init(HealthCareHome home, Staff staff) {
        this.home = home; this.staff = staff;
        setupTable();
        refreshBeds();
        // role based visibility
        addResidentBtn.setVisible(staff.has(Access.ADD_RESIDENT));
        addStaffBtn.setVisible(staff.has(Access.ADD_STAFF));
    }

    private void setupTable() {
        colBedId.setCellValueFactory(c -> javafx.beans.property.SimpleStringProperty.stringExpression(c.getValue().bedId));
        colResident.setCellValueFactory(c -> javafx.beans.property.SimpleStringProperty.stringExpression(c.getValue().residentName));
        bedTable.setRowFactory(tv -> {
            TableRow<BedRow> row = new TableRow<>();
            row.setOnMouseClicked((MouseEvent event) -> {
                if (event.getClickCount() == 2 && (! row.isEmpty()) ) {
                    BedRow rowData = row.getItem();
                    showResidentDetails(String.valueOf(rowData.bedId));
                }
            });
            return row ;
        });
    }

    private void refreshBeds() {
        ObservableList<BedRow> rows = FXCollections.observableArrayList();
        for (Bed b : home.getBedList().values()) {
            String rn = b.isVacant() ? "" : b.getResident().getName();
            rows.add(new BedRow(b.getId(), rn));
        }
        bedTable.setItems(rows);
    }

    @FXML
    public void onAddResident() {
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

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField idField = new TextField();
        idField.setPromptText("Resident ID");

        TextField nameField = new TextField();
        nameField.setPromptText("Resident Name");

        // ✅ Gender dropdown — only Male/Female allowed
        ComboBox<String> genderBox = new ComboBox<>();
        genderBox.getItems().addAll("MALE", "FEMALE");
        genderBox.setPromptText("Select Gender");
        genderBox.setEditable(false);

        // ✅ Isolation dropdown — Yes/No (true/false)
        ComboBox<String> isolationBox = new ComboBox<>();
        isolationBox.getItems().addAll("YES", "NO");
        isolationBox.setPromptText("Isolation Required?");
        isolationBox.setEditable(false);

        grid.add(new Label("Resident ID:"), 0, 0);
        grid.add(idField, 1, 0);
        grid.add(new Label("Name:"), 0, 1);
        grid.add(nameField, 1, 1);
        grid.add(new Label("Gender:"), 0, 2);
        grid.add(genderBox, 1, 2);
        grid.add(new Label("Isolation:"), 0, 3);
        grid.add(isolationBox, 1, 3);

        dialog.getDialogPane().setContent(grid);
        Platform.runLater(idField::requestFocus);

        Optional<ButtonType> result = dialog.showAndWait();

        if (result.isPresent() && result.get() == addButtonType) {
            try {
                String id = idField.getText().trim();
                String name = nameField.getText().trim();
                String gender = genderBox.getValue();
                String isolation = isolationBox.getValue();

                if (id.isEmpty() || name.isEmpty() || gender == null || isolation == null) {
                    showAlert("All fields are required!");
                    return;
                }

                Resident resident = new Resident(
                        id,
                        name,
                        "MALE".equalsIgnoreCase(gender) ? Gender.MALE : Gender.FEMALE,
                        "YES".equalsIgnoreCase(isolation)
                );

                home.assignResidentToBed(staff, String.valueOf(sel.bedId), resident);
                refreshBeds();
                showAlert("Resident added successfully!");

            } catch (Exception ex) {
                showAlert("Error: " + ex.getMessage());
            }
        }
    }


    @FXML
    public void onAddStaff() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Add New Staff");
        dialog.setHeaderText("Enter staff details");

        ButtonType addButtonType = new ButtonType("Add", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField idField = new TextField();
        idField.setPromptText("Staff ID");

        TextField nameField = new TextField();
        nameField.setPromptText("Full Name");

        // ✅ ComboBox with fixed options — no typing allowed
        ComboBox<String> roleBox = new ComboBox<>();
        roleBox.getItems().addAll("MANAGER", "DOCTOR", "NURSE");
        roleBox.setPromptText("Select Role");
        roleBox.setEditable(false); // ⛔ disable manual typing

        TextField usernameField = new TextField();
        usernameField.setPromptText("Username");

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");

        grid.add(new Label("Staff ID:"), 0, 0);
        grid.add(idField, 1, 0);
        grid.add(new Label("Full Name:"), 0, 1);
        grid.add(nameField, 1, 1);
        grid.add(new Label("Role:"), 0, 2);
        grid.add(roleBox, 1, 2);
        grid.add(new Label("Username:"), 0, 3);
        grid.add(usernameField, 1, 3);
        grid.add(new Label("Password:"), 0, 4);
        grid.add(passwordField, 1, 4);

        dialog.getDialogPane().setContent(grid);
        Platform.runLater(idField::requestFocus);

        Optional<ButtonType> result = dialog.showAndWait();

        if (result.isPresent() && result.get() == addButtonType) {
            try {
                String id = idField.getText().trim();
                String name = nameField.getText().trim();
                String role = roleBox.getValue(); // ✅ fixed dropdown selection only
                String username = usernameField.getText().trim();
                String password = passwordField.getText().trim();

                if (id.isEmpty() || name.isEmpty() || role == null || username.isEmpty() || password.isEmpty()) {
                    showAlert("All fields are required!");
                    return;
                }

                Staff newStaff = switch (role) {
                    case "MANAGER" -> new Manager(id, name, username, password);
                    case "DOCTOR" -> new Doctor(id, name, username, password);
                    case "NURSE" -> new Nurse(id, name, username, password);
                    default -> null;
                };

                home.registerNewStaff(newStaff);
                refreshBeds();
                showAlert("Staff added successfully!");

            } catch (Exception ex) {
                showAlert("Error: " + ex.getMessage());
            }
        }
    }

    private void showResidentDetails(String bedId) {
        try {
            Bed b = home.getBedList().get(bedId);
            if (b==null || b.isVacant()) {
                showAlert("No resident");
                return;
            }
            Resident r = b.getResident();
            Alert a = new Alert(Alert.AlertType.INFORMATION, "Resident: " + r.getId() + " - " + r.getName());
            a.setHeaderText("Resident Details");
            a.showAndWait();
        } catch (Exception ex) { showAlert(ex.getMessage()); }
    }

    private void showAlert(String m) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, m);
        a.showAndWait();
    }

    public static class BedRow {
        public final javafx.beans.property.StringProperty bedId;
        public final javafx.beans.property.StringProperty residentName;
        public BedRow(String b, String r) {
            this.bedId = new javafx.beans.property.SimpleStringProperty(b);
            this.residentName = new javafx.beans.property.SimpleStringProperty(r);
        }
    }
}
