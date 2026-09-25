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
import java.net.URL;
import java.util.List;
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

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Load filter combos
        teacherFilter.getItems().add(null); // "All teachers" option
        teacherFilter.getItems().addAll(teacherService.findAll());
        teacherFilter.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(Teacher t, boolean empty) {
                super.updateItem(t, empty); setText(t == null ? "All teachers" : t.getName()); }
        });

        dayFilter.getItems().add(null);
        dayFilter.getItems().addAll(dayService.findAll());
        dayFilter.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(WorkingDay d, boolean empty) {
                super.updateItem(d, empty); setText(d == null ? "All days" : d.getDayName()); }
        });

        // Load all on start
        search();
    }

    @FXML
    public void search() {
        List<ClassSchedule> results;

        String cls     = classFilter.getText().trim();
        Teacher teacher = teacherFilter.getValue();
        String subCode  = subjectFilter.getText().trim();
        WorkingDay day  = dayFilter.getValue();

        // Priority: if class name provided, start from that
        if (!cls.isBlank()) {
            results = scheduleService.findByClassName(cls);
        } else if (teacher != null) {
            results = scheduleService.findByTeacher(teacher);
        } else if (day != null) {
            results = scheduleService.findByDay(day);
        } else if (!subCode.isBlank()) {
            results = scheduleService.findBySubjectCode(subCode);
        } else {
            results = scheduleService.findAll();
        }

        // Post-filter by additional criteria
        if (!cls.isBlank() && teacher != null) {
            results = results.stream().filter(s -> s.getTeacher().equals(teacher)).toList();
        }
        if (!cls.isBlank() && day != null) {
            results = results.stream().filter(s -> s.getWorkingDay().equals(day)).toList();
        }
        if (!subCode.isBlank() && !results.isEmpty()) {
            results = results.stream()
                .filter(s -> s.getSubject().getCode().toLowerCase().contains(subCode.toLowerCase()))
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
                PDFExportUtil.export(data, file.getAbsolutePath());
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
