package com.healthcare.home;

import com.healthcare.home.core.ResidentHealthCareHome;
import com.healthcare.home.core.SerializingHandlerService;
import com.healthcare.home.scheduler.Scheduler;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {
    public static void main(String[] args) {
    launch(args);
    }

    private static ResidentHealthCareHome home;

    public static ResidentHealthCareHome getHome() {
        return home;
    }

    public static void setHome(ResidentHealthCareHome home) {
        Main.home = home;
    }

    @Override
    public void start(Stage stage) throws Exception {
        home = SerializingHandlerService.readOrCreateFile();
        Scheduler scheduler = new Scheduler();
        scheduler.startComplianceScheduler(home);

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/healthcare/home/view/login.fxml"));
        Scene scene = new Scene(loader.load());
        stage.setTitle("Resident Health-Care System");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();
    }

    @Override
    public void stop() {
        SerializingHandlerService.saveRecordsInFile(home);
        Scheduler.stopScheduler();
    }
}
