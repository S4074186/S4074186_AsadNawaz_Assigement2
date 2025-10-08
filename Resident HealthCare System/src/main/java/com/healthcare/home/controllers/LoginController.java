package com.healthcare.home.controllers;

import com.healthcare.home.Main;
import com.healthcare.home.core.HealthCareHome;
import com.healthcare.home.staff.Staff;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class LoginController {
    @FXML private TextField username;
    @FXML private PasswordField password;

    private final HealthCareHome home = Main.getHome();

    @FXML
    public void handleLogin(ActionEvent e) {
        Staff staff = home.authenticate(username.getText(), password.getText());
        if (staff == null) {
            username.setStyle("-fx-border-color: red;");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/healthcare/home/view/dashboard.fxml"));
            Stage stage = (Stage) username.getScene().getWindow();
            Scene scene = new Scene(loader.load());
            DashboardController controller = loader.getController();
            controller.init(home, staff);
            stage.setScene(scene);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
