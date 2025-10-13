package com.healthcare.home.controller;

import com.healthcare.home.Main;
import com.healthcare.home.core.HealthCareHome;
import com.healthcare.home.staff.Staff;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class LoginController {
    @FXML private TextField username;
    @FXML private PasswordField password;
    @FXML private Button loginButton;

    private final HealthCareHome home = Main.getHome();

    @FXML
    public void initialize() {
        // Call login when user presses Enter
        username.setOnAction(this::handleLogin);
        password.setOnAction(this::handleLogin);
    }

    @FXML
    public void handleLogin(ActionEvent e) {
        String user = username.getText().trim();
        String pass = password.getText().trim();

        // Check if fields are empty
        if (user.isEmpty() || pass.isEmpty()) {
            showAlert("Missing Field", "Please enter both username and password.");
            highlightEmptyFields(user, pass);
            return;
        }

        try {
            Staff staff = home.authenticate(user, pass);
            if (staff == null) {
                showAlert("Login Failed", "Invalid username or password.");
                username.setStyle("-fx-border-color: red;");
                password.setStyle("-fx-border-color: red;");
                return;
            }

            // Login success
            Stage st = (Stage) username.getScene().getWindow();
            DashboardRouter.openDashboard(staff, st);
        } catch (Exception ex) {
            showAlert("Error", "Something went wrong: " + ex.getMessage());
        }
    }

    private void highlightEmptyFields(String user, String pass) {
        if (user.isEmpty()) username.setStyle("-fx-border-color: red;");
        else username.setStyle("");
        if (pass.isEmpty()) password.setStyle("-fx-border-color: red;");
        else password.setStyle("");
    }

    private void showAlert(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
