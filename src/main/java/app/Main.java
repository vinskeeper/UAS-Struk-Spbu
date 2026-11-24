package app;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.text.Font;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.InputStream;

public class Main extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception {
        loadApplicationFonts();
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/login.fxml"));
        primaryStage.setTitle("UAS Struk SPBU — Login");
        primaryStage.setScene(new Scene(loader.load(), 470, 570));
        primaryStage.setMinWidth(440);
        primaryStage.setMinHeight(520);
        primaryStage.show();
        primaryStage.centerOnScreen();
    }

    private void loadApplicationFonts() {
        loadFont("/fonts/CooperLtBT-Regular.ttf");
        loadFont("/fonts/CooperMdBT-Semibold.ttf");
    }

    private void loadFont(String resourcePath) {
        try (InputStream fontStream = getClass().getResourceAsStream(resourcePath)) {
            if (fontStream == null) {
                System.err.println("Resource font tidak ditemukan: " + resourcePath);
                return;
            }
            Font loadedFont = Font.loadFont(fontStream, 13);
            if (loadedFont == null) {
                System.err.println("JavaFX tidak dapat membaca file font: " + resourcePath);
            }
        } catch (Exception e) {
            System.err.println("Font aplikasi tidak dapat dimuat: " + resourcePath);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
