package com.timetable.controller;

import com.timetable.model.WorkingDay;
import com.timetable.service.WorkingDayService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;

import java.net.URL;
import java.util.ResourceBundle;

public class WorkingDayController implements Initializable {

    @FXML private TextField dayNameField;
    @FXML private Button saveBtn;
    @FXML private Button updateBtn;
    @FXML private Button deleteBtn;
    @FXML private Label messageLabel;
    @FXML private TableView<WorkingDay> dayTable;

    private final WorkingDayService service = new WorkingDayService();
    private WorkingDay selected;

    @Override
    public void initialize(URL url, ResourceBundle rb) { loadTable(); }

    @FXML
    public void save() {
        try {
            service.save(new WorkingDay(dayNameField.getText().trim()));
            showSuccess("Working day saved."); clear(); loadTable();
        } catch (Exception e) { showError(e.getMessage()); }
    }

    @FXML
    public void update() {
        if (selected == null) return;
        try {
            selected.setDayName(dayNameField.getText().trim());
            service.update(selected);
            showSuccess("Updated."); clear(); loadTable();
        } catch (Exception e) { showError(e.getMessage()); }
    }

    @FXML
    public void delete() {
        if (selected == null) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete \"" + selected.getDayName() + "\"?", ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText("Confirm Delete");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) { service.delete(selected); clear(); loadTable(); }
        });
    }

    @FXML
    public void clear() {
        dayNameField.clear(); selected = null;
        saveBtn.setDisable(false); updateBtn.setDisable(true); deleteBtn.setDisable(true);
        messageLabel.setVisible(false); messageLabel.setManaged(false);
    }

    @FXML
    public void onRowSelect(MouseEvent e) {
        WorkingDay d = dayTable.getSelectionModel().getSelectedItem();
        if (d == null) return;
        selected = d;
        dayNameField.setText(d.getDayName());
        saveBtn.setDisable(true); updateBtn.setDisable(false); deleteBtn.setDisable(false);
        messageLabel.setVisible(false); messageLabel.setManaged(false);
    }

    @FXML
    public void loadTable() {
        dayTable.setItems(FXCollections.observableArrayList(service.findAll()));
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
