package com.timetable.controller;

import com.timetable.model.*;
import com.timetable.service.*;
import com.timetable.util.PDFExportUtil;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.FileChooser;

import java.io.File;
import java.nio.file.Files;
import java.net.URL;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * User Portal controller — read-only timetable search and PDF export.
 */
public class UserDashboardController implements Initializable {

    @FXML private TextField classFilter;
    @FXML private ComboBox<Teacher>    teacherFilter;
    @FXML private TextField subjectFilter;
    @FXML private ComboBox<WorkingDay> dayFilter;
    @FXML private Label resultCountLabel;
    @FXML private TableView<ClassSchedule> resultsTable;

    private final ScheduleService  scheduleService = new ScheduleService();
    private final TeacherService   teacherService  = new TeacherService();
    private final WorkingDayService dayService     = new WorkingDayService();
    private TimetableApiClient apiClient;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        Optional<TimetableApiClient> remote = TimetableApiClient.fromEnvironment();
        apiClient = remote.orElse(null);

        if (apiClient != null) {
            try {
                List<ClassSchedule> available = apiClient.findSchedules(null, null, null, null);
                teacherFilter.getItems().add(null);
                teacherFilter.getItems().addAll(available.stream().map(ClassSchedule::getTeacher).distinct().toList());
                dayFilter.getItems().add(null);
                dayFilter.getItems().addAll(available.stream().map(ClassSchedule::getWorkingDay).distinct().toList());
                search();
            } catch (Exception e) {
                teacherFilter.getItems().add(null);
                dayFilter.getItems().add(null);
                resultCountLabel.setText("Shared timetable server unavailable: " + e.getMessage());
            }
            configureFilterCells();
            return;
        }

        // Load filter combos
        teacherFilter.getItems().add(null); // "All teachers" option
        teacherFilter.getItems().addAll(teacherService.findAll());
        dayFilter.getItems().add(null);
        dayFilter.getItems().addAll(dayService.findAll());
        configureFilterCells();

        // Load all on start
        search();
    }

    private void configureFilterCells() {
        teacherFilter.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(Teacher t, boolean empty) {
                super.updateItem(t, empty); setText(t == null ? "All teachers" : t.getName()); }
        });
        dayFilter.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(WorkingDay d, boolean empty) {
                super.updateItem(d, empty); setText(d == null ? "All days" : d.getDayName()); }
        });
    }

    @FXML
    public void search() {
        String cls = classFilter.getText().trim();
        Teacher teacher = teacherFilter.getValue();
        String subCode = subjectFilter.getText().trim().toLowerCase();
        WorkingDay day = dayFilter.getValue();

        List<ClassSchedule> results;
        if (apiClient != null) {
            try {
                results = apiClient.findSchedules(cls, teacher == null ? null : teacher.getName(), subCode,
                        day == null ? null : day.getDayName());
            } catch (Exception e) {
                resultCountLabel.setText("Could not load timetables: " + e.getMessage());
                resultsTable.getItems().clear();
                return;
            }
        } else {
            // Apply every supplied filter together so search results match the user's full request.
            results = scheduleService.findAll().stream()
                .filter(s -> cls.isBlank() || s.getClassName().toLowerCase().contains(cls.toLowerCase()))
                .filter(s -> teacher == null || teacher.equals(s.getTeacher()))
                .filter(s -> subCode.isBlank() || s.getSubject().getCode().toLowerCase().contains(subCode))
                .filter(s -> day == null || day.equals(s.getWorkingDay()))
                .sorted(Comparator.comparing((ClassSchedule s) -> s.getWorkingDay().getDayName())
                        .thenComparing(s -> s.getTimePeriod().getPeriodNumber())
                        .thenComparing(ClassSchedule::getClassName, String.CASE_INSENSITIVE_ORDER))
                .toList();
        }

        resultsTable.setItems(FXCollections.observableArrayList(results));
        resultCountLabel.setText("Found " + results.size() + " schedule entries.");
    }

    @FXML
    public void clearFilters() {
        classFilter.clear();
        teacherFilter.setValue(null);
        subjectFilter.clear();
        dayFilter.setValue(null);
        search();
    }

    @FXML
    public void exportPDF() {
        List<ClassSchedule> data = resultsTable.getItems();
        if (data == null || data.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING,
                    "No schedule data to export. Run a search first.", ButtonType.OK);
            alert.setHeaderText("Nothing to Export");
            alert.showAndWait();
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save Timetable PDF");
        chooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        chooser.setInitialFileName("timetable.pdf");
        File file = chooser.showSaveDialog(resultsTable.getScene().getWindow());

        if (file != null) {
            try {
                if (apiClient != null) {
                    byte[] pdf = apiClient.downloadPdf(classFilter.getText().trim(),
                            teacherFilter.getValue() == null ? null : teacherFilter.getValue().getName(),
                            subjectFilter.getText().trim(),
                            dayFilter.getValue() == null ? null : dayFilter.getValue().getDayName());
                    Files.write(file.toPath(), pdf);
                } else {
                    PDFExportUtil.export(data, file.getAbsolutePath());
                }
                Alert success = new Alert(Alert.AlertType.INFORMATION,
                        "PDF saved to:\n" + file.getAbsolutePath(), ButtonType.OK);
                success.setHeaderText("Export Successful");
                success.showAndWait();
            } catch (Exception e) {
                Alert error = new Alert(Alert.AlertType.ERROR,
                        "Failed to export PDF:\n" + e.getMessage(), ButtonType.OK);
                error.setHeaderText("Export Error");
                error.showAndWait();
            }
        }
    }
}
