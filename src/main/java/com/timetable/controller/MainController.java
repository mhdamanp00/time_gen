package com.timetable.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TableView;
import javafx.scene.layout.StackPane;

import java.net.URL;
import java.util.ResourceBundle;

/** Starts in the public timetable portal and gates admin tools behind authentication. */
public class MainController implements Initializable {
    @FXML private Button adminBtn;
    @FXML private Button userBtn;
    @FXML private Button logoutBtn;
    @FXML private StackPane contentPane;
    @FXML private Label statusLabel;

    private boolean administratorAuthenticated;
    private final boolean remoteUserClient = System.getenv("TIMETABLE_API_URL") != null
            && !System.getenv("TIMETABLE_API_URL").isBlank();

    @Override public void initialize(URL url, ResourceBundle bundle) { showUser(); }

    @FXML public void adminAction() {
        if (administratorAuthenticated) {
            showAdminDashboard();
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/AdminAccess.fxml"));
            Node view = loader.load();
            loader.<AdminAccessController>getController().setOnAuthenticated(() -> {
                administratorAuthenticated = true;
                showAdminDashboard();
            });
            contentPane.getChildren().setAll(view);
            configureTables(view);
            statusLabel.setText("Administrator sign-in required to manage the timetable");
            setActive(false);
        } catch (Exception e) {
            showLoadError(e);
        }
    }

    private void showAdminDashboard() {
        loadView("/fxml/AdminDashboard.fxml");
        statusLabel.setText("Administrator session active");
        setActive(false);
    }

    @FXML public void showUser() {
        loadView("/fxml/UserDashboard.fxml");
        statusLabel.setText("Search and download available timetables");
        setActive(true);
        adminBtn.setVisible(!remoteUserClient);
        adminBtn.setManaged(!remoteUserClient);
    }

    @FXML public void logout() {
        administratorAuthenticated = false;
        showUser();
    }

    private void loadView(String path) {
        try {
            Node view = FXMLLoader.load(getClass().getResource(path));
            contentPane.getChildren().setAll(view);
            configureTables(view);
        } catch (Exception e) {
            showLoadError(e);
        }
    }

    private void showLoadError(Exception e) {
        Throwable cause = e;
        while (cause.getCause() != null) cause = cause.getCause();
        String message = cause.getMessage() == null ? e.getMessage() : cause.getMessage();
        statusLabel.setText("Could not open this screen: " + message);
        e.printStackTrace();
        Alert alert = new Alert(Alert.AlertType.ERROR,
                "The screen could not be opened. Check the database configuration and application console.\n\n" + message,
                ButtonType.OK);
        alert.setHeaderText("Unable to open portal");
        alert.showAndWait();
    }

    private void setActive(boolean userPortal) {
        userBtn.getStyleClass().remove("portal-btn-active");
        adminBtn.getStyleClass().remove("portal-btn-active");
        (userPortal ? userBtn : adminBtn).getStyleClass().add("portal-btn-active");
        userBtn.setText(administratorAuthenticated ? "⌕  User portal" : "⌕  Find a timetable");
        adminBtn.setText(administratorAuthenticated ? "⚙  Admin portal" : "⚙  Admin login");
        logoutBtn.setVisible(administratorAuthenticated);
        logoutBtn.setManaged(administratorAuthenticated);
    }

    private void configureTables(Node root) {
        root.lookupAll(".table-view").forEach(node -> {
            if (node instanceof TableView<?> table)
                table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        });
    }
}
