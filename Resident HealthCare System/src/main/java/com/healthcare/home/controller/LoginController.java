
package com.healthcare.home.controller;

import com.healthcare.home.Main;
import com.healthcare.home.core.HealthCareHome;
import com.healthcare.home.staff.Staff;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class LoginController {
    public Button loginButton;
    @FXML private TextField username;
    @FXML private PasswordField password;

    private final HealthCareHome home = Main.getHome();

    @FXML
    public void handleLogin(ActionEvent e) {
        try {
            String user = username.getText();
            String pass = password.getText();
            Staff staff = home.authenticate(user, pass);
            if (staff == null) {
                username.setStyle("-fx-border-color: red;");
                return;
            }
            Stage st = (Stage) username.getScene().getWindow();
            DashboardRouter.openDashboard(staff, st);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
