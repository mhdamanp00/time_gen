package com.timetable.controller;

import com.timetable.service.AdminAuthService;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.ResourceBundle;

/** First-run setup and subsequent sign-in for the protected administrator area. */
public class AdminAccessController implements Initializable {
    @FXML private VBox setupForm;
    @FXML private VBox loginForm;
    @FXML private Label heading;
    @FXML private Label description;
    @FXML private Label messageLabel;
    @FXML private TextField setupUsername;
    @FXML private PasswordField setupPassword;
    @FXML private PasswordField confirmPassword;
    @FXML private TextField loginUsername;
    @FXML private PasswordField loginPassword;

    private final AdminAuthService authService = new AdminAuthService();
    private Runnable onAuthenticated = () -> {};

    @Override public void initialize(URL url, ResourceBundle bundle) {
        boolean configured = authService.isConfigured();
        setupForm.setVisible(!configured);
        setupForm.setManaged(!configured);
        loginForm.setVisible(configured);
        loginForm.setManaged(configured);
        heading.setText(configured ? "Admin sign in" : "Set up your admin account");
        description.setText(configured
                ? "Sign in to manage teachers, classes, and timetable generation."
                : "Create the first administrator account. This account controls timetable setup and schedule management.");
    }

    public void setOnAuthenticated(Runnable callback) {
        onAuthenticated = callback == null ? () -> {} : callback;
    }

    @FXML public void createAdmin() {
        char[] password = setupPassword.getText().toCharArray();
        try {
            if (!setupPassword.getText().equals(confirmPassword.getText())) {
                showMessage("The passwords do not match.", true);
                return;
            }
            authService.createInitialAdmin(setupUsername.getText(), password);
            clearPasswords();
            onAuthenticated.run();
        } catch (Exception e) {
            showMessage(e.getMessage() == null ? "Could not create the admin account." : e.getMessage(), true);
        } finally {
            java.util.Arrays.fill(password, '\0');
        }
    }

    @FXML public void login() {
        char[] password = loginPassword.getText().toCharArray();
        try {
            if (authService.authenticate(loginUsername.getText(), password)) {
                clearPasswords();
                onAuthenticated.run();
            } else {
                loginPassword.clear();
                showMessage("Username or password is incorrect.", true);
            }
        } catch (Exception e) {
            loginPassword.clear();
            showMessage("Could not sign in. Check the database connection and try again.", true);
        } finally {
            java.util.Arrays.fill(password, '\0');
        }
    }

    private void clearPasswords() {
        setupPassword.clear(); confirmPassword.clear(); loginPassword.clear();
    }
    private void showMessage(String text, boolean error) {
        messageLabel.setText(text);
        messageLabel.setStyle("-fx-text-fill:" + (error ? "#f87171" : "#34d399") + ";");
        messageLabel.setVisible(true);
        messageLabel.setManaged(true);
    }
}
