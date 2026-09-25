package com.timetable.controller;

import com.timetable.model.*;
import com.timetable.service.*;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.net.URL;
import java.util.*;

/**
 * Auto-generate controller — parses text-area assignments and runs TimetableGenerator.
 */
public class AutoGenerateController implements Initializable {

    @FXML private TextArea assignmentsArea;
    @FXML private CheckBox clearFirstCheck;
    @FXML private Label statsLabel;
    @FXML private Label messageLabel;
    @FXML private TableView<ClassSchedule> resultTable;

    private final SubjectService  subjectService  = new SubjectService();
    private final TeacherService  teacherService  = new TeacherService();
    private final ClassroomService classroomService = new ClassroomService();
    private final WorkingDayService dayService    = new WorkingDayService();
    private final TimePeriodService periodService = new TimePeriodService();
    private final TimetableGenerator generator    = new TimetableGenerator();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        refreshStats();
    }

    @FXML
    public void refreshStats() {
        int subjects  = subjectService.findAll().size();
        int teachers  = teacherService.findAll().size();
        int rooms     = classroomService.findAll().size();
        int days      = dayService.findAll().size();
        int periods   = periodService.findAll().size();
        statsLabel.setText(
            subjects + " subjects · " + teachers + " teachers · " + rooms + " rooms\n" +
            days + " days · " + periods + " periods/day = " +
            (days * periods) + " total slots"
        );
    }

    @FXML
    public void generate() {
        String text = assignmentsArea.getText();
        if (text == null || text.isBlank()) {
            showError("Please enter at least one assignment row.");
            return;
        }

        // Parse assignment rows: ClassName,SubjectCode,TeacherEmail
        List<TimetableGenerator.Assignment> assignments = new ArrayList<>();
        List<String> parseErrors = new ArrayList<>();

        Map<String, Subject>  subjectMap  = new HashMap<>();
        Map<String, Teacher>  teacherMap  = new HashMap<>();
        subjectService.findAll().forEach(s -> subjectMap.put(s.getCode().toLowerCase(), s));
        teacherService.findAll().forEach(t -> teacherMap.put(t.getEmail().toLowerCase(), t));

        String[] lines = text.trim().split("\\n");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            String[] parts = line.split(",");
            if (parts.length < 3) {
                parseErrors.add("Line " + (i + 1) + ": expected format ClassName,SubjectCode,TeacherEmail");
                continue;
            }
            String className   = parts[0].trim();
            String subjectCode = parts[1].trim().toLowerCase();
            String teacherEmail = parts[2].trim().toLowerCase();

            Subject subject = subjectMap.get(subjectCode);
            Teacher teacher = teacherMap.get(teacherEmail);

            if (subject == null) {
                parseErrors.add("Line " + (i + 1) + ": subject code \"" + parts[1].trim() + "\" not found.");
                continue;
            }
            if (teacher == null) {
                parseErrors.add("Line " + (i + 1) + ": teacher email \"" + parts[2].trim() + "\" not found.");
                continue;
            }
            assignments.add(new TimetableGenerator.Assignment(className, subject, teacher));
        }

        if (!parseErrors.isEmpty()) {
            showError("Parse errors:\n" + String.join("\n", parseErrors));
            return;
        }

        if (assignments.isEmpty()) {
            showError("No valid assignments found.");
            return;
        }

        List<Classroom>   classrooms = classroomService.findAll();
        List<WorkingDay>  days       = dayService.findAll();
        List<TimePeriod>  periods    = periodService.findAll();

        if (classrooms.isEmpty() || days.isEmpty() || periods.isEmpty()) {
            showError("Please add classrooms, working days, and time periods first.");
            return;
        }

        TimetableGenerator.GenerationResult result =
            generator.generate(assignments, classrooms, days, periods, clearFirstCheck.isSelected());

        resultTable.setItems(FXCollections.observableArrayList(result.created));

        StringBuilder sb = new StringBuilder();
        sb.append("✓ ").append(result.created.size()).append(" entries scheduled.");
        if (!result.failures.isEmpty()) {
            sb.append("\n⚠ ").append(result.failures.size()).append(" could not be scheduled:\n");
            result.failures.forEach(f -> sb.append("  • ").append(f).append("\n"));
            messageLabel.setStyle("-fx-text-fill:#d97706;");
        } else {
            messageLabel.setStyle("-fx-text-fill:#22c55e;");
        }
        messageLabel.setText(sb.toString());
        messageLabel.setVisible(true);
        messageLabel.setManaged(true);
    }

    private void showError(String msg) {
        messageLabel.setText("⚠ " + msg);
        messageLabel.setStyle("-fx-text-fill:#ef4444;");
        messageLabel.setVisible(true);
        messageLabel.setManaged(true);
    }
}
