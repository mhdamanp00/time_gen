package com.timetable.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Admin sidebar controller — loads sub-views into the right content pane.
 */
public class AdminDashboardController implements Initializable {

    @FXML private Button btnSubject;
    @FXML private Button btnTeacher;
    @FXML private Button btnClassroom;
    @FXML private Button btnWorkingDay;
    @FXML private Button btnTimePeriod;
    @FXML private Button btnSchedule;
    @FXML private Button btnAutoGenerate;
    @FXML private StackPane adminContentPane;

    private List<Button> sidebarButtons;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        sidebarButtons = List.of(btnSubject, btnTeacher, btnClassroom,
                                  btnWorkingDay, btnTimePeriod, btnSchedule, btnAutoGenerate);
        showSubjects();
    }

    @FXML public void showSubjects()      { load("/fxml/SubjectView.fxml", btnSubject); }
    @FXML public void showTeachers()      { load("/fxml/TeacherView.fxml", btnTeacher); }
    @FXML public void showClassrooms()    { load("/fxml/ClassroomView.fxml", btnClassroom); }
    @FXML public void showWorkingDays()   { load("/fxml/WorkingDayView.fxml", btnWorkingDay); }
    @FXML public void showTimePeriods()   { load("/fxml/TimePeriodView.fxml", btnTimePeriod); }
    @FXML public void showSchedule()      { load("/fxml/ScheduleView.fxml", btnSchedule); }
    @FXML public void showAutoGenerate()  { load("/fxml/AutoGenerateView.fxml", btnAutoGenerate); }

    private void load(String path, Button activeBtn) {
        try {
            Node view = FXMLLoader.load(getClass().getResource(path));
            adminContentPane.getChildren().setAll(view);
            // Update active sidebar button style
            sidebarButtons.forEach(b -> b.getStyleClass().remove("sidebar-btn-active"));
            activeBtn.getStyleClass().add("sidebar-btn-active");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
