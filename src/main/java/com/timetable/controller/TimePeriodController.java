package com.timetable.controller;

import com.timetable.model.TimePeriod;
import com.timetable.service.TimePeriodService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;

import java.net.URL;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ResourceBundle;

public class TimePeriodController implements Initializable {

    @FXML private TextField periodNumberField;
    @FXML private TextField startTimeField;
    @FXML private TextField endTimeField;
    @FXML private Button saveBtn;
    @FXML private Button updateBtn;
    @FXML private Button deleteBtn;
    @FXML private Label messageLabel;
    @FXML private TableView<TimePeriod> periodTable;

    private final TimePeriodService service = new TimePeriodService();
    private TimePeriod selected;

    @Override
    public void initialize(URL url, ResourceBundle rb) { loadTable(); }

    @FXML
    public void save() {
        try {
            TimePeriod p = buildPeriod(new TimePeriod());
            service.save(p);
            showSuccess("Time period saved."); clear(); loadTable();
        } catch (Exception e) { showError(e.getMessage()); }
    }

    @FXML
    public void update() {
        if (selected == null) return;
        try {
            buildPeriod(selected);
            service.update(selected);
            showSuccess("Updated."); clear(); loadTable();
        } catch (Exception e) { showError(e.getMessage()); }
    }

    @FXML
    public void delete() {
        if (selected == null) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete Period " + selected.getPeriodNumber() + "?", ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText("Confirm Delete");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) { service.delete(selected); clear(); loadTable(); }
        });
    }

    @FXML
    public void clear() {
        periodNumberField.clear(); startTimeField.clear(); endTimeField.clear();
        selected = null;
        saveBtn.setDisable(false); updateBtn.setDisable(true); deleteBtn.setDisable(true);
        messageLabel.setVisible(false); messageLabel.setManaged(false);
    }

    @FXML
    public void onRowSelect(MouseEvent e) {
        TimePeriod p = periodTable.getSelectionModel().getSelectedItem();
        if (p == null) return;
        selected = p;
        periodNumberField.setText(String.valueOf(p.getPeriodNumber()));
        startTimeField.setText(p.getStartTime().toString());
        endTimeField.setText(p.getEndTime().toString());
        saveBtn.setDisable(true); updateBtn.setDisable(false); deleteBtn.setDisable(false);
        messageLabel.setVisible(false); messageLabel.setManaged(false);
    }

    @FXML
    public void loadTable() {
        periodTable.setItems(FXCollections.observableArrayList(service.findAll()));
    }

    private TimePeriod buildPeriod(TimePeriod p) {
        try {
            p.setPeriodNumber(Integer.parseInt(periodNumberField.getText().trim()));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Period number must be an integer.");
        }
        try {
            p.setStartTime(LocalTime.parse(startTimeField.getText().trim()));
            p.setEndTime(LocalTime.parse(endTimeField.getText().trim()));
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Time must be in HH:mm format (e.g. 09:00).");
        }
        return p;
    }

    private void showError(String msg) {
        messageLabel.setText("⚠ " + msg);
        messageLabel.setStyle("-fx-text-fill:#ef4444;");
        messageLabel.setVisible(true); messageLabel.setManaged(true);
    }

    private void showSuccess(String msg) {
        messageLabel.setText("✓ " + msg);
        messageLabel.setStyle("-fx-text-fill:#22c55e;");
        messageLabel.setVisible(true); messageLabel.setManaged(true);
    }
}
