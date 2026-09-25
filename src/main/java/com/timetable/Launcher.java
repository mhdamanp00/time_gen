package com.timetable;

/**
 * Launcher class for running the application from IDEs (e.g., IntelliJ IDEA)
 * without explicit JavaFX module-path / VM arguments.
 *
 * When running via Maven, use: mvn javafx:run
 * When running from IDE, set this class as the main class.
 */
public class Launcher {
    public static void main(String[] args) {
        App.main(args);
    }
}
