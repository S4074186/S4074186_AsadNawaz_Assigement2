package com.healthcare.home.controllers;

import com.healthcare.home.Main;
import com.healthcare.home.core.ResidentHealthCareHome;
import com.healthcare.home.staff.Staff;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

/**
 * LoginHandler Class responsible to authenticate and login the user
 */
public class LoginHandler {
    @FXML
    private TextField username;
    @FXML
    private PasswordField password;
    @FXML
    private Button loginButton;

    private final ResidentHealthCareHome home = Main.getHome();

    /**
     * initialize user
     */
    @FXML
    public void initialize() {
        // Call login when user presses Enter
        username.setOnAction(this::processLogin);
        password.setOnAction(this::processLogin);
    }

    /**
     * processLogin with an event
     *
     * @param event
     */
    @FXML
    public void processLogin(ActionEvent event) {
        String username = this.username.getText().trim();
        String password = this.password.getText().trim();

        // Check if fields are empty
        if (username.isEmpty() || password.isEmpty()) {
            popupAlert("Missing Field", "Please enter both username and password.");
            highlightEmptyFields(username, password);
            return;
        }

        try {
            Staff staff = home.authenticate(username, password);
            if (staff == null) {
                popupAlert("Login Failed", "Invalid username or password.");
                this.username.setStyle("-fx-border-color: red;");
                this.password.setStyle("-fx-border-color: red;");
                return;
            }

            // Login success
            Stage st = (Stage) this.username.getScene().getWindow();
            DashboardRouter.showDashboard(staff, st);
        } catch (Exception ex) {
            popupAlert("Error", "Something went wrong: " + ex.getMessage());
        }
    }

    /**
     * highlightEmptyFields
     *
     * @param username
     * @param password
     */
    private void highlightEmptyFields(String username, String password) {
        if (username.isEmpty()) this.username.setStyle("-fx-border-color: red;");
        else this.username.setStyle("");
        if (password.isEmpty()) this.password.setStyle("-fx-border-color: red;");
        else this.password.setStyle("");
    }

    /**
     * popupAlert
     *
     * @param title
     * @param msg
     */
    private void popupAlert(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
