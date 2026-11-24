package controller;

import java.io.IOException;
import java.sql.SQLException;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import dao.UserDAO;
import model.spbu.Login;
import model.spbu.User;

public class LoginController {
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;
    private final UserDAO userDAO = new UserDAO();

    @FXML
    public void handleLogin(ActionEvent event) {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        User account;

        try {
            account = userDAO.findByCredentials(username, password);
        } catch (SQLException e) {
            errorLabel.setText("Tidak dapat menghubungi database. Periksa koneksi lalu coba lagi.");
            return;
        }
        if (account != null) {
            try {
                ((Login) account).signIn();
            } catch (IllegalStateException e) {
                errorLabel.setText(e.getMessage());
                return;
            }
            try {
                // 1. Load the new FXML dashboard interface
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main_window.fxml"));
                Parent root = loader.load();

                DashboardController controller = loader.getController();
                controller.initializeApp(account.getName(), account.getUsername(), account.getId(), account.getRole());

                Stage currentStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                Stage dashboardStage = new Stage();
                dashboardStage.setTitle("POM Bensin");
                dashboardStage.setScene(new Scene(root, 1180, 760));
                dashboardStage.show();
                currentStage.close();

                dashboardStage.setOnCloseRequest(e -> {
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                    alert.setTitle("Exit Application");
                    alert.setHeaderText("Application will exit...");
                    alert.setContentText("Are you sure you want to exit and close all windows?");

                    var result = alert.showAndWait();
                    if (result.isPresent() && result.get() == ButtonType.OK) {
                        Platform.exit();
                    } else {
                        e.consume();
                    }
                });

            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            errorLabel.setText("Username atau password tidak cocok.");
        }
    }
}
