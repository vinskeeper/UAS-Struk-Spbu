package controller;

import dao.UserDAO;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import model.spbu.User;
import java.sql.SQLException;

public class UserController {
    @FXML private TextField nameField, usernameField;
    @FXML private PasswordField passwordField;
    @FXML private ComboBox<String> genderBox;
    @FXML private ComboBox<String> roleBox;
    @FXML private DatePicker birthdayPicker;
    @FXML private TableView<User> petugasTable;
    @FXML private TableColumn<User,String> nameColumn, usernameColumn, roleColumn, genderColumn, birthdayColumn, statusColumn;
    @FXML private Label selectionLabel;
    @FXML private Button saveButton;
    private final UserDAO dao = new UserDAO();
    private String activeUsername = "";

    @FXML private void initialize() {
        genderBox.setItems(FXCollections.observableArrayList("Laki-laki", "Perempuan"));
        roleBox.setItems(FXCollections.observableArrayList("ADMIN", "PETUGAS"));
        roleBox.setValue("PETUGAS");
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        usernameColumn.setCellValueFactory(new PropertyValueFactory<>("username"));
        roleColumn.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
                "ADMIN".equals(c.getValue().getRole()) ? "Admin" : "Petugas"));
        genderColumn.setCellValueFactory(new PropertyValueFactory<>("gender"));
        birthdayColumn.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
                c.getValue().getBirthday() == null ? "-" : c.getValue().getBirthday().toString()));
        statusColumn.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
                c.getValue().isActive() ? "Aktif" : "Nonaktif"));
        petugasTable.getSelectionModel().selectedItemProperty().addListener((o,a,p) -> select(p));
        refresh();
    }
    public void setActiveUsername(String username) { activeUsername = username == null ? "" : username; }
    private void select(User p) {
        if (p == null) return;
        nameField.setText(p.getName()); usernameField.setText(p.getUsername());
        genderBox.setValue("Male".equals(p.getGender()) ? "Laki-laki" : "Perempuan");
        roleBox.setValue(p.getRole());
        birthdayPicker.setValue(p.getBirthday()); passwordField.clear();
        passwordField.setPromptText("Kosongkan jika tetap"); saveButton.setText("Simpan Perubahan");
        selectionLabel.setText("Mengubah akun: " + p.getUsername());
    }
    @FXML private void save() {
        String gender = "Laki-laki".equals(genderBox.getValue()) ? "Male" : "Perempuan".equals(genderBox.getValue()) ? "Female" : null;
        User p = petugasTable.getSelectionModel().getSelectedItem();
        try {
            if (p == null) dao.create(nameField.getText(), gender, birthdayPicker.getValue(), usernameField.getText(), passwordField.getText(), roleBox.getValue());
            else dao.update(p.getId(), nameField.getText(), gender, birthdayPicker.getValue(), usernameField.getText(), passwordField.getText(), roleBox.getValue());
            clear(); refresh();
        } catch (SQLException e) { error("Akun user gagal disimpan", e.getMessage()); }
    }
    @FXML private void clear() {
        petugasTable.getSelectionModel().clearSelection(); nameField.clear(); usernameField.clear();
        passwordField.clear(); passwordField.setPromptText("Password akun baru"); genderBox.setValue(null); roleBox.setValue("PETUGAS"); birthdayPicker.setValue(null);
        saveButton.setText("Tambah Akun"); selectionLabel.setText("Akun baru.");
    }
    @FXML private void deactivate() { setStatus(false); }
    @FXML private void activate() { setStatus(true); }
    private void setStatus(boolean active) {
        User p=petugasTable.getSelectionModel().getSelectedItem(); if(p==null){warn("Pilih akun user terlebih dahulu.");return;}
        if(!active && p.getUsername().equalsIgnoreCase(activeUsername)){warn("Akun yang sedang digunakan tidak bisa dinonaktifkan.");return;}
        try{dao.setActive(p.getId(),active);refresh();}catch(SQLException e){error("Status akun gagal diubah",e.getMessage());}
    }
    @FXML private void delete() {
        User p=petugasTable.getSelectionModel().getSelectedItem(); if(p==null){warn("Pilih akun yang ingin dihapus.");return;}
        if(p.getUsername().equalsIgnoreCase(activeUsername)){warn("Akun yang sedang digunakan tidak bisa dihapus.");return;}
        Alert a=new Alert(Alert.AlertType.CONFIRMATION,"Hapus akun "+p.getUsername()+" secara permanen?");
        if(a.showAndWait().filter(ButtonType.OK::equals).isEmpty())return;
        try{dao.delete(p.getId());refresh();clear();}catch(SQLException e){error("Akun gagal dihapus",e.getMessage());}
    }
    private void refresh(){try{petugasTable.setItems(FXCollections.observableArrayList(dao.getAll()));}catch(SQLException e){error("Data user tidak dapat dimuat",e.getMessage());}}
    private void warn(String s){new Alert(Alert.AlertType.WARNING,s).showAndWait();}
    private void error(String t,String s){Alert a=new Alert(Alert.AlertType.ERROR,s);a.setTitle(t);a.showAndWait();}
}
