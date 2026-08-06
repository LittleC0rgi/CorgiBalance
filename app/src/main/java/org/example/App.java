package org.example;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Objects;


public class App extends Application {

    private Connection connection;
    
    private void connectDB() {
        try {
            Path dbDir = Path.of(System.getProperty("user.home"), ".corgibalance");
            Files.createDirectories(dbDir);
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbDir.resolve("corgibalance.db"));
        } catch (SQLException | IOException e) {
            throw new RuntimeException("Failed to connect to the database", e);
        }
    }

    private void loadFonts() {
        Font.loadFont(Objects.requireNonNull(App.class.getResourceAsStream("/fonts/Inter_18pt-Regular.ttf")), 12);
        Font.loadFont(Objects.requireNonNull(App.class.getResourceAsStream("/fonts/Inter_18pt-Light.ttf")), 12);
        Font.loadFont(Objects.requireNonNull(App.class.getResourceAsStream("/fonts/Inter_18pt-Bold.ttf")), 12);
    }

    private void loadStyles(Scene scene) {
        scene.getStylesheets().add(Objects.requireNonNull(App.class.getResource("/css/base.css")).toExternalForm());
        scene.getStylesheets().add(Objects.requireNonNull(App.class.getResource("/css/sidebar.css")).toExternalForm());
        scene.getStylesheets().add(Objects.requireNonNull(App.class.getResource("/css/views.css")).toExternalForm());
    }

    @Override
    public void start(Stage stage) throws Exception {
        loadFonts();
        connectDB();
        FXMLLoader loader = new FXMLLoader(App.class.getResource("/fxml/main.fxml"));
        Scene scene = new Scene(loader.load());
        loadStyles(scene);
        stage.setScene(scene);
        stage.setTitle("Corgi Balance");
        stage.setMaximized(true);
        stage.show();
    }

    @Override
    public void stop() throws SQLException {
        if (connection != null) {
            connection.close();
        }
    }
}
