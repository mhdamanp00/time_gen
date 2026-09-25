package com.timetable.controller;

import com.timetable.model.Classroom;
import com.timetable.service.ClassroomService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;

import java.net.URL;
import java.util.ResourceBundle;

public class ClassroomController implements Initializable {

    @FXML private TextField roomNumberField;
    @FXML private TextField capacityField;
    @FXML private Button saveBtn;
    @FXML private Button updateBtn;
    @FXML private Button deleteBtn;
    @FXML private Label messageLabel;
    @FXML private TableView<Classroom> classroomTable;

    private final ClassroomService service = new ClassroomService();
    private Classroom selected;

    @Override
    public void initialize(URL url, ResourceBundle rb) { loadTable(); }

    @FXML
    public void save() {
        try {
            Classroom c = new Classroom(roomNumberField.getText().trim(), parseCapacity());
            service.save(c);
            showSuccess("Classroom saved.");
            clear(); loadTable();
        } catch (Exception e) { showError(e.getMessage()); }
    }

    @FXML
    public void update() {
        if (selected == null) return;
        try {
            selected.setRoomNumber(roomNumberField.getText().trim());
            selected.setCapacity(parseCapacity());
            service.update(selected);
            showSuccess("Classroom updated.");
            clear(); loadTable();
        } catch (Exception e) { showError(e.getMessage()); }
    }

    @FXML
    public void delete() {
        if (selected == null) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete room \"" + selected.getRoomNumber() + "\"?", ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText("Confirm Delete");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) { service.delete(selected); clear(); loadTable(); }
        });
    }

    @FXML
    public void clear() {
        roomNumberField.clear(); capacityField.clear();
        selected = null;
        saveBtn.setDisable(false); updateBtn.setDisable(true); deleteBtn.setDisable(true);
        messageLabel.setVisible(false); messageLabel.setManaged(false);
    }

    @FXML
    public void onRowSelect(MouseEvent e) {
        Classroom c = classroomTable.getSelectionModel().getSelectedItem();
        if (c == null) return;
        selected = c;
        roomNumberField.setText(c.getRoomNumber());
        capacityField.setText(String.valueOf(c.getCapacity()));
        saveBtn.setDisable(true); updateBtn.setDisable(false); deleteBtn.setDisable(false);
        messageLabel.setVisible(false); messageLabel.setManaged(false);
    }

    @FXML
    public void loadTable() {
        classroomTable.setItems(FXCollections.observableArrayList(service.findAll()));
    }

    private int parseCapacity() {
        try { return Integer.parseInt(capacityField.getText().trim()); }
        catch (NumberFormatException e) { throw new IllegalArgumentException("Capacity must be a valid integer."); }
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
