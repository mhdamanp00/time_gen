package com.timetable.controller;

import com.timetable.model.*;
import com.timetable.service.*;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Admin schedule controller — create/update/delete ClassSchedule entries.
 * Calls ScheduleService which performs conflict detection before persisting.
 */
public class ScheduleController implements Initializable {

    @FXML private TextField classNameField;
    @FXML private ComboBox<Subject>     subjectCombo;
    @FXML private ComboBox<Teacher>     teacherCombo;
    @FXML private ComboBox<Classroom>   classroomCombo;
    @FXML private ComboBox<WorkingDay>  dayCombo;
    @FXML private ComboBox<TimePeriod>  periodCombo;
    @FXML private Button saveBtn;
    @FXML private Button updateBtn;
    @FXML private Button deleteBtn;
    @FXML private Label messageLabel;
    @FXML private TableView<ClassSchedule> scheduleTable;

    private final ScheduleService scheduleService   = new ScheduleService();
    private final SubjectService  subjectService    = new SubjectService();
    private final TeacherService  teacherService    = new TeacherService();
    private final ClassroomService classroomService = new ClassroomService();
    private final WorkingDayService dayService      = new WorkingDayService();
    private final TimePeriodService periodService   = new TimePeriodService();

    private ClassSchedule selected;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        loadCombos();
        loadTable();
    }

    @FXML
    public void save() {
        try {
            ClassSchedule cs = buildSchedule(new ClassSchedule());
            scheduleService.save(cs);
            showSuccess("Schedule entry saved.");
            clear(); loadTable();
        } catch (ConflictException e) {
            showConflictDialog(e.getMessage());
        } catch (Exception e) {
            showError(e.getMessage());
        }
    }

    @FXML
    public void update() {
        if (selected == null) return;
        try {
            buildSchedule(selected);
            scheduleService.update(selected);
            showSuccess("Schedule entry updated.");
            clear(); loadTable();
        } catch (ConflictException e) {
            showConflictDialog(e.getMessage());
        } catch (Exception e) {
            showError(e.getMessage());
        }
    }

    @FXML
    public void delete() {
        if (selected == null) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete this schedule entry?", ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText("Confirm Delete");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                scheduleService.delete(selected);
                clear(); loadTable();
            }
        });
    }

    @FXML
    public void clear() {
        classNameField.clear();
        subjectCombo.setValue(null);
        teacherCombo.setValue(null);
        classroomCombo.setValue(null);
        dayCombo.setValue(null);
        periodCombo.setValue(null);
        selected = null;
        saveBtn.setDisable(false);
        updateBtn.setDisable(true);
        deleteBtn.setDisable(true);
        messageLabel.setVisible(false);
        messageLabel.setManaged(false);
    }

    @FXML
    public void onRowSelect(MouseEvent e) {
        ClassSchedule cs = scheduleTable.getSelectionModel().getSelectedItem();
        if (cs == null) return;
        selected = cs;
        classNameField.setText(cs.getClassName());
        subjectCombo.setValue(cs.getSubject());
        teacherCombo.setValue(cs.getTeacher());
        classroomCombo.setValue(cs.getClassroom());
        dayCombo.setValue(cs.getWorkingDay());
        periodCombo.setValue(cs.getTimePeriod());
        saveBtn.setDisable(true);
        updateBtn.setDisable(false);
        deleteBtn.setDisable(false);
        messageLabel.setVisible(false);
        messageLabel.setManaged(false);
    }

    @FXML
    public void loadTable() {
        scheduleTable.setItems(FXCollections.observableArrayList(scheduleService.findAll()));
    }

    // ── Helpers ────────────────────────────────────────

    private void loadCombos() {
        subjectCombo.setItems(FXCollections.observableArrayList(subjectService.findAll()));
        teacherCombo.setItems(FXCollections.observableArrayList(teacherService.findAll()));
        classroomCombo.setItems(FXCollections.observableArrayList(classroomService.findAll()));
        dayCombo.setItems(FXCollections.observableArrayList(dayService.findAll()));
        periodCombo.setItems(FXCollections.observableArrayList(periodService.findAll()));
    }

    private ClassSchedule buildSchedule(ClassSchedule cs) {
        cs.setClassName(classNameField.getText().trim());
        cs.setSubject(subjectCombo.getValue());
        cs.setTeacher(teacherCombo.getValue());
        cs.setClassroom(classroomCombo.getValue());
        cs.setWorkingDay(dayCombo.getValue());
        cs.setTimePeriod(periodCombo.getValue());
        return cs;
    }

    private void showConflictDialog(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Scheduling Conflict Detected");
        alert.setHeaderText("Cannot save — conflict found!");
        alert.setContentText(msg);
        alert.showAndWait();
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
