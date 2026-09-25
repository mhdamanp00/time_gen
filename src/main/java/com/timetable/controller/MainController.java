package com.timetable.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * Root controller: manages portal switching (Admin ↔ User).
 */
public class MainController implements Initializable {

    @FXML private Button adminBtn;
    @FXML private Button userBtn;
    @FXML private StackPane contentPane;
    @FXML private Label statusLabel;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        showAdmin();
    }

    @FXML
    public void showAdmin() {
        loadView("/fxml/AdminDashboard.fxml");
        adminBtn.getStyleClass().add("portal-btn-active");
        userBtn.getStyleClass().remove("portal-btn-active");
    }

    @FXML
    public void showUser() {
        loadView("/fxml/UserDashboard.fxml");
        userBtn.getStyleClass().add("portal-btn-active");
        adminBtn.getStyleClass().remove("portal-btn-active");
    }

    private void loadView(String fxmlPath) {
        try {
            Node view = FXMLLoader.load(getClass().getResource(fxmlPath));
            contentPane.getChildren().setAll(view);
        } catch (IOException e) {
            statusLabel.setText("Error loading view: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
