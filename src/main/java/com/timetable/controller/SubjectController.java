package com.timetable.controller;

import com.timetable.model.Subject;
import com.timetable.service.SubjectService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.net.URL;
import java.util.ResourceBundle;

public class SubjectController implements Initializable {

    @FXML private Label formTitle;
    @FXML private TextField nameField;
    @FXML private TextField codeField;
    @FXML private TextField creditsField;
    @FXML private Button saveBtn;
    @FXML private Button updateBtn;
    @FXML private Button deleteBtn;
    @FXML private Label messageLabel;
    @FXML private TableView<Subject> subjectTable;
    @FXML private TableColumn<Subject, Long> colId;

    private final SubjectService service = new SubjectService();
    private Subject selected;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Show a clean, sequential row number without changing database primary keys.
        colId.setText("No.");
        colId.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Long id, boolean empty) {
                super.updateItem(id, empty);
                setText(empty ? null : Integer.toString(getIndex() + 1));
            }
        });
        subjectTable.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> showSelectedSubject(newValue));
        loadTable();
    }

    @FXML
    public void save() {
        try {
            Subject s = new Subject(
                nameField.getText().trim(),
                codeField.getText().trim(),
                parseCredits()
            );
            service.save(s);
            clear();
            showSuccess("Subject added successfully.");
            loadTable();
        } catch (Exception e) {
            showError(e.getMessage());
        }
    }

    @FXML
    public void update() {
        if (selected == null) return;
        try {
            selected.setName(nameField.getText().trim());
            selected.setCode(codeField.getText().trim());
            selected.setCredits(parseCredits());
            service.update(selected);
            clear();
            showSuccess("Subject updated successfully.");
            loadTable();
        } catch (Exception e) {
            showError(e.getMessage());
        }
    }

    @FXML
    public void delete() {
        if (selected == null) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete subject \"" + selected.getName() + "\"?", ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText("Confirm Delete");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                service.delete(selected);
                clear();
                loadTable();
            }
        });
    }

    @FXML
    public void clear() {
        nameField.clear();
        codeField.clear();
        creditsField.clear();
        selected = null;
        subjectTable.getSelectionModel().clearSelection();
        formTitle.setText("Add a subject");
        saveBtn.setDisable(false);
        updateBtn.setDisable(true);
        deleteBtn.setDisable(true);
        messageLabel.setVisible(false);
        messageLabel.setManaged(false);
    }

    @FXML
    private void showSelectedSubject(Subject s) {
        if (s == null) {
            selected = null;
            formTitle.setText("Add a subject");
            saveBtn.setDisable(false);
            updateBtn.setDisable(true);
            deleteBtn.setDisable(true);
            return;
        }
        selected = s;
        formTitle.setText("Edit subject");
        nameField.setText(s.getName());
        codeField.setText(s.getCode());
        creditsField.setText(String.valueOf(s.getCredits()));
        saveBtn.setDisable(true);
        updateBtn.setDisable(false);
        deleteBtn.setDisable(false);
        messageLabel.setVisible(false);
        messageLabel.setManaged(false);
    }

    public void onRowSelect() {
        showSelectedSubject(subjectTable.getSelectionModel().getSelectedItem());
    }

    @FXML
    public void loadTable() {
        subjectTable.setItems(FXCollections.observableArrayList(service.findAll()));
    }

    // ── Helpers ────────────────────────────────────────

    private int parseCredits() {
        try {
            return Integer.parseInt(creditsField.getText().trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Credits must be a valid integer.");
        }
    }

    private void showError(String msg) {
        messageLabel.setText("⚠ " + msg);
        messageLabel.setStyle("-fx-text-fill:#ef4444;");
        messageLabel.setVisible(true);
        messageLabel.setManaged(true);
    }

    private void showSuccess(String msg) {
        messageLabel.setText("✓ " + msg);
        messageLabel.setStyle("-fx-text-fill:#22c55e;");
        messageLabel.setVisible(true);
        messageLabel.setManaged(true);
    }
}
