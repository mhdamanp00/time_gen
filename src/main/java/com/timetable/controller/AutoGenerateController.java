package com.timetable.controller;

import com.timetable.model.*;
import com.timetable.service.*;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.net.URL;
import java.util.*;

/** Validates generation requests, previews the proposed timetable, then applies on admin confirmation. */
public class AutoGenerateController implements Initializable {
    @FXML private TextArea assignmentsArea;
    @FXML private CheckBox clearFirstCheck;
    @FXML private Button applyButton;
    @FXML private Label statsLabel;
    @FXML private Label messageLabel;
    @FXML private TableView<ClassSchedule> resultTable;

    private final SubjectService subjectService = new SubjectService();
    private final TeacherService teacherService = new TeacherService();
    private final ClassroomService classroomService = new ClassroomService();
    private final WorkingDayService dayService = new WorkingDayService();
    private final TimePeriodService periodService = new TimePeriodService();
    private final TimetableGenerator generator = new TimetableGenerator();
    private TimetableGenerator.GenerationResult pendingProposal;
    private boolean pendingReplace;

    @Override public void initialize(URL url, ResourceBundle rb) {
        applyButton.setDisable(true);
        refreshStats();
    }

    @FXML public void refreshStats() {
        int subjects = subjectService.findAll().size();
        int teachers = teacherService.findAll().size();
        int rooms = classroomService.findAll().size();
        int days = dayService.findAll().size();
        int periods = periodService.findAll().size();
        statsLabel.setText(subjects + " subjects · " + teachers + " teachers · " + rooms + " rooms\n" +
                days + " days · " + periods + " periods/day = " + (days * periods) + " weekly slots per class");
    }

    @FXML public void generate() {
        pendingProposal = null;
        applyButton.setDisable(true);
        String text = assignmentsArea.getText();
        if (text == null || text.isBlank()) { showError("Enter at least one assignment row."); return; }

        Map<String, Subject> subjects = new HashMap<>();
        Map<String, Teacher> teachers = new HashMap<>();
        subjectService.findAll().forEach(s -> subjects.put(s.getCode().trim().toLowerCase(), s));
        teacherService.findAll().forEach(t -> teachers.put(t.getEmail().trim().toLowerCase(), t));
        List<TimetableGenerator.Assignment> assignments = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        String[] lines = text.split("\\R");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            String[] parts = line.split(",", -1);
            if (parts.length < 3 || parts.length > 4) {
                errors.add("Line " + (i + 1) + ": use ClassName,SubjectCode,TeacherEmail[,LessonsPerWeek].");
                continue;
            }
            String className = parts[0].trim();
            Subject subject = subjects.get(parts[1].trim().toLowerCase());
            Teacher teacher = teachers.get(parts[2].trim().toLowerCase());
            int count = 1;
            if (className.isEmpty() || className.length() > 50) {
                errors.add("Line " + (i + 1) + ": class/section name must be 1–50 characters."); continue;
            }
            if (subject == null) { errors.add("Line " + (i + 1) + ": subject code '" + parts[1].trim() + "' was not found."); continue; }
            if (teacher == null) { errors.add("Line " + (i + 1) + ": teacher email '" + parts[2].trim() + "' was not found."); continue; }
            if (!teacher.getSubjects().contains(subject)) {
                errors.add("Line " + (i + 1) + ": " + teacher.getName() + " is not assigned to teach " + subject.getCode() + "."); continue;
            }
            if (parts.length == 4) {
                try { count = Integer.parseInt(parts[3].trim()); }
                catch (NumberFormatException e) { errors.add("Line " + (i + 1) + ": lessons per week must be a whole number."); continue; }
            }
            if (count < 1 || count > 100) { errors.add("Line " + (i + 1) + ": lessons per week must be between 1 and 100."); continue; }
            assignments.add(new TimetableGenerator.Assignment(className, subject, teacher, count));
        }
        if (!errors.isEmpty()) { showError("Fix these rows before previewing:\n" + String.join("\n", errors)); return; }
        if (assignments.isEmpty()) { showError("No valid assignment rows found."); return; }

        List<Classroom> rooms = classroomService.findAll();
        List<WorkingDay> days = dayService.findAll();
        List<TimePeriod> periods = periodService.findAll();
        if (rooms.isEmpty() || days.isEmpty() || periods.isEmpty()) {
            showError("Add at least one classroom, working day, and time period first."); return;
        }

        pendingReplace = clearFirstCheck.isSelected();
        pendingProposal = generator.preview(assignments, rooms, days, periods, pendingReplace);
        resultTable.setItems(FXCollections.observableArrayList(pendingProposal.created));
        applyButton.setDisable(pendingProposal.created.isEmpty());
        String summary = pendingProposal.created.size() + " lesson(s) fit. Review the proposed rows below, then apply.";
        if (!pendingProposal.failures.isEmpty()) summary += "\n" + pendingProposal.failures.size() + " lesson(s) could not be placed:\n• " + String.join("\n• ", pendingProposal.failures);
        showMessage(summary, pendingProposal.failures.isEmpty() ? "#22c55e" : "#d97706");
    }

    @FXML public void applyProposal() {
        if (pendingProposal == null || pendingProposal.created.isEmpty()) return;
        String action = pendingReplace
                ? "Replace the current timetable with these " + pendingProposal.created.size() + " proposed lessons?"
                : "Add these " + pendingProposal.created.size() + " proposed lessons to the current timetable?";
        if (!pendingProposal.failures.isEmpty()) action += "\n\nSome requested lessons could not be placed.";
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, action, ButtonType.CANCEL, ButtonType.OK);
        confirm.setHeaderText("Apply timetable proposal");
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;
        try {
            generator.apply(pendingProposal, pendingReplace);
            int saved = pendingProposal.created.size();
            pendingProposal = null;
            applyButton.setDisable(true);
            showMessage("Applied successfully: " + saved + " lesson(s) saved. The user portal can now view them.", "#22c55e");
        } catch (Exception e) {
            showError("Could not apply the proposal. The existing timetable was preserved. " + rootMessage(e));
        }
    }

    private String rootMessage(Exception e) {
        Throwable cause = e;
        while (cause.getCause() != null) cause = cause.getCause();
        return cause.getMessage() == null ? e.getMessage() : cause.getMessage();
    }

    private void showError(String message) { showMessage("⚠ " + message, "#ef4444"); }
    private void showMessage(String message, String color) {
        messageLabel.setText(message);
        messageLabel.setStyle("-fx-text-fill:" + color + ";");
        messageLabel.setVisible(true);
        messageLabel.setManaged(true);
    }
}
