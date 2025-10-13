package com.healthcare.home;

import com.healthcare.home.core.HealthCareHome;
import com.healthcare.home.core.SerializingService;
import com.healthcare.home.scheduler.Schedule;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {
    public static void main(String[] args) {
    launch(args);
    }

    private static HealthCareHome home;

    public static HealthCareHome getHome() {
        return home;
    }

    public static void setHome(HealthCareHome home) {
        Main.home = home;
    }

    @Override
    public void start(Stage stage) throws Exception {
        home = SerializingService.readOrCreateFile();
        Schedule schedule = new Schedule();
        schedule.startComplianceScheduler(home);

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/healthcare/home/view/login.fxml"));
        Scene scene = new Scene(loader.load());
        stage.setTitle("Resident Health-Care System");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();
    }

    @Override
    public void stop() {
        SerializingService.saveRecordsInFile(home);
        Schedule.stopScheduler();
    }
}
