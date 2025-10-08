
package com.healthcare.home.controller;

import com.healthcare.home.Main;
import com.healthcare.home.core.HealthCareHome;
import com.healthcare.home.entity.Role;
import com.healthcare.home.staff.Staff;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class DashboardRouter {
    public static void openDashboard(Staff staff, Stage stage) throws Exception {
        HealthCareHome home = Main.getHome();
        if (staff.getRole() == Role.MANAGER) {
            FXMLLoader loader = new FXMLLoader(DashboardRouter.class.getResource("/com/healthcare/home/view/manager_dashboard.fxml"));
            Parent root = loader.load();
            ManagerDashboardController ctrl = loader.getController();
            ctrl.init(home, staff);
            stage.setScene(new Scene(root));
            stage.setTitle("Manager Dashboard");
        } else if (staff.getRole() == Role.DOCTOR) {
            FXMLLoader loader = new FXMLLoader(DashboardRouter.class.getResource("/com/healthcare/home/view/doctor_dashboard.fxml"));
            Parent root = loader.load();
            DoctorDashboardController ctrl = loader.getController();
            ctrl.init(home, staff);
            stage.setScene(new Scene(root));
            stage.setTitle("Doctor Dashboard");
        } else {
            FXMLLoader loader = new FXMLLoader(DashboardRouter.class.getResource("/com/healthcare/home/view/nurse_dashboard.fxml"));
            Parent root = loader.load();
            NurseDashboardController ctrl = loader.getController();
            ctrl.init(home, staff);
            stage.setScene(new Scene(root));
            stage.setTitle("Nurse Dashboard");
        }
        stage.setResizable(false);
        stage.show();
    }
}
