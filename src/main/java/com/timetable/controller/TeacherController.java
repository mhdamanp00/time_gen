package com.timetable.controller;

import com.timetable.model.Subject;
import com.timetable.model.Teacher;
import com.timetable.service.SubjectService;
import com.timetable.service.TeacherService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;

import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.ResourceBundle;

public class TeacherController implements Initializable {

    @FXML private TextField nameField;
    @FXML private TextField emailField;
    @FXML private ListView<Subject> subjectListView;
    @FXML private Button saveBtn;
    @FXML private Button updateBtn;
    @FXML private Button deleteBtn;
    @FXML private Label messageLabel;
    @FXML private TableView<Teacher> teacherTable;

    private final TeacherService teacherService = new TeacherService();
    private final SubjectService subjectService = new SubjectService();
    private Teacher selected;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        subjectListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        List<Subject> subjects = subjectService.findAll();
        subjectListView.setItems(FXCollections.observableArrayList(subjects));
        loadTable();
    }

    @FXML
    public void save() {
        try {
            Teacher t = buildTeacher(new Teacher());
            teacherService.save(t);
            showSuccess("Teacher saved.");
            clear();
            loadTable();
        } catch (Exception e) {
            showError(e.getMessage());
        }
    }

    @FXML
    public void update() {
        if (selected == null) return;
        try {
            buildTeacher(selected);
            teacherService.update(selected);
            showSuccess("Teacher updated.");
            clear();
            loadTable();
        } catch (Exception e) {
            showError(e.getMessage());
        }
    }

    @FXML
    public void delete() {
        if (selected == null) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete teacher \"" + selected.getName() + "\"?", ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText("Confirm Delete");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                teacherService.delete(selected);
                clear();
                loadTable();
            }
        });
    }

    @FXML
    public void clear() {
        nameField.clear();
        emailField.clear();
        subjectListView.getSelectionModel().clearSelection();
        selected = null;
        saveBtn.setDisable(false);
        updateBtn.setDisable(true);
        deleteBtn.setDisable(true);
        messageLabel.setVisible(false);
        messageLabel.setManaged(false);
    }

    @FXML
    public void onRowSelect(MouseEvent e) {
        Teacher t = teacherTable.getSelectionModel().getSelectedItem();
        if (t == null) return;
        selected = t;
        nameField.setText(t.getName());
        emailField.setText(t.getEmail());
        // Restore subject selections
        subjectListView.getSelectionModel().clearSelection();
        subjectListView.getItems().forEach(subj -> {
            if (t.getSubjects().contains(subj)) {
                subjectListView.getSelectionModel().select(subj);
            }
        });
        saveBtn.setDisable(true);
        updateBtn.setDisable(false);
        deleteBtn.setDisable(false);
        messageLabel.setVisible(false);
        messageLabel.setManaged(false);
    }

    @FXML
    public void loadTable() {
        teacherTable.setItems(FXCollections.observableArrayList(teacherService.findAll()));
    }

    // ── Helpers ────────────────────────────────────────

    private Teacher buildTeacher(Teacher t) {
        t.setName(nameField.getText().trim());
        t.setEmail(emailField.getText().trim());
        t.setSubjects(new HashSet<>(subjectListView.getSelectionModel().getSelectedItems()));
        return t;
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
