package com.healthcare.home.controller;

import com.healthcare.home.core.HealthCareHome;
import com.healthcare.home.core.SerializingService;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.stage.Stage;

public abstract class BaseDashboardController {

    @FXML private Button logoutBtn;

    // allow subclasses to set the home instance
    protected HealthCareHome home;

    public void setHome(HealthCareHome home) {
        this.home = home;
    }

    @FXML public void onLogout() {
        try {
            // save all patient related records before logout
            try {
                if (home != null) {
                    SerializingService.saveRecordsInFile(home);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            Stage st = (Stage) logoutBtn.getScene().getWindow();
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/com/healthcare/home/view/login.fxml"));
            st.setScene(new javafx.scene.Scene(loader.load()));
            st.setTitle("Login");
        } catch (Exception ex) { ex.printStackTrace(); }
    }
}
