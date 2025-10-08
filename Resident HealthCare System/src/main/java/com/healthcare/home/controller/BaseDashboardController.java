package com.healthcare.home.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.stage.Stage;

public abstract class BaseDashboardController {

    @FXML private Button logoutBtn;

    @FXML public void onLogout() {
        try {
            Stage st = (Stage) logoutBtn.getScene().getWindow();
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/com/healthcare/home/view/login.fxml"));
            st.setScene(new javafx.scene.Scene(loader.load()));
            st.setTitle("Login");
        } catch (Exception ex) { ex.printStackTrace(); }
    }
}
