
package com.healthcare.home.controllers;

import com.healthcare.home.Main;
import com.healthcare.home.core.ResidentHealthCareHome;
import com.healthcare.home.entities.Role;
import com.healthcare.home.staff.Staff;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * DashboardRouter class to route the user towards correct dashboard through its role
 */
public class DashboardRouter {

    /**
     * showDashboard method to open and route the correct dashboard
     *
     * @param staff
     * @param stage
     * @throws Exception
     */
    public static void showDashboard(Staff staff, Stage stage) throws Exception {
        ResidentHealthCareHome home = Main.getHome();
        if (staff.getRole() == Role.MANAGER) {
            FXMLLoader loader = new FXMLLoader(DashboardRouter.class.getResource("/com/healthcare/home/view/manager_dashboard.fxml"));
            Parent root = loader.load();
            ManagerDashboard controller = loader.getController();
            controller.init(home, staff);
            stage.setScene(new Scene(root));
            stage.setTitle("Manager Dashboard");
        } else if (staff.getRole() == Role.DOCTOR) {
            FXMLLoader loader = new FXMLLoader(DashboardRouter.class.getResource("/com/healthcare/home/view/doctor_dashboard.fxml"));
            Parent root = loader.load();
            DoctorDashboard controller = loader.getController();
            controller.init(home, staff);
            stage.setScene(new Scene(root));
            stage.setTitle("Doctor Dashboard");
        } else if (staff.getRole() == Role.NURSE) {
            FXMLLoader loader = new FXMLLoader(DashboardRouter.class.getResource("/com/healthcare/home/view/nurse_dashboard.fxml"));
            Parent root = loader.load();
            NurseDashboard controller = loader.getController();
            controller.init(home, staff);
            stage.setScene(new Scene(root));
            stage.setTitle("Nurse Dashboard");
        }
        stage.setResizable(false);
        stage.show();
    }
}
