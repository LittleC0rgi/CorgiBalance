package org.example;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;


public class App extends Application {

    private static final Logger logger = Logger.getLogger(App.class.getName());

    private Connection connection;

    private void connectDB() {
        try {
            Path dbDir = Path.of(System.getProperty("user.home"), ".corgibalance");
            Files.createDirectories(dbDir);
            Path dbPath = dbDir.resolve("corgibalance.db");
            logger.info("Connecting to database: " + dbPath.toAbsolutePath());
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
            initDatabase();
        } catch (SQLException | IOException e) {
            logger.log(Level.SEVERE, "Failed to connect to the database", e);
            throw new RuntimeException("Failed to connect to the database", e);
        }
    }

    private void initDatabase() throws SQLException {
        try (InputStream input = App.class.getResourceAsStream("/db/init.sql")) {
            if (input == null) {
                throw new SQLException("Database initialization script /db/init.sql not found");
            }
            String script = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            logger.info("Applying database initialization script (" + script.length() + " chars)");
            try (Statement statement = connection.createStatement()) {
                for (String sql : script.split(";\\s*")) {
                    if (sql.trim().isEmpty()) {
                        continue;
                    }
                    statement.execute(sql);
                }
            }
            logger.info("Database initialization completed");
        } catch (IOException e) {
            throw new SQLException("Failed to read database initialization script", e);
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
