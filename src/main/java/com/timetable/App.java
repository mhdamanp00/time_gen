package com.timetable;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import com.timetable.dao.HibernateUtil;
import com.timetable.service.TimetableApiServer;

/**
 * Main JavaFX Application entry point.
 * Loads the MainView and applies the dark-theme stylesheet.
 */
public class App extends Application {

    private static Stage primaryStage;
    private TimetableApiServer apiServer;

    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;

        if ("true".equalsIgnoreCase(System.getenv("TIMETABLE_API_SERVER"))) {
            int port = Integer.parseInt(System.getenv().getOrDefault("TIMETABLE_API_PORT", "8080"));
            apiServer = new TimetableApiServer(port);
            apiServer.start();
            System.out.println("Read-only timetable service listening on port " + port);
        }

        Parent root = FXMLLoader.load(getClass().getResource("/fxml/MainView.fxml"));
        Scene scene = new Scene(root, 1280, 800);
        scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());

        stage.setTitle("Automatic Class Timetable and Schedule Generator");
        stage.setScene(scene);
        stage.setMinWidth(1024);
        stage.setMinHeight(700);
        stage.setOnCloseRequest(event -> {
            if (apiServer != null) apiServer.close();
            HibernateUtil.shutdown();
        });
        stage.show();
    }

    /**
     * Returns the primary stage (useful for dialogs and file choosers).
     */
    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
