package com.corgibalance;

import com.corgibalance.services.Database;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


public class App extends Application {

    private static final String MAIN_CLASS = App.class.getName();

    public static void main(String[] args) {
        launch(args);
    }

    public static void restart() {
        List<String> command = new ArrayList<>();
        command.add(Path.of(System.getProperty("java.home"), "bin", isWindows() ? "java.exe" : "java").toString());
        command.addAll(ManagementFactory.getRuntimeMXBean().getInputArguments());
        String modulePath = System.getProperty("jdk.module.path");
        Module thisModule = App.class.getModule();

        if (modulePath != null && !modulePath.isBlank() && thisModule.isNamed()) {
            command.add("--module-path");
            command.add(modulePath);
            command.add("--module");
            command.add(thisModule.getName() + "/" + MAIN_CLASS);
        } else {
            command.add("-cp");
            command.add(System.getProperty("java.class.path"));
            command.add(MAIN_CLASS);
        }

        Process newProcess;
        try {
            newProcess = new ProcessBuilder(command)
                    .directory(Path.of(System.getProperty("user.dir")).toFile())
                    .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                    .redirectError(ProcessBuilder.Redirect.INHERIT)
                    .start();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to restart application", e);
        }

        try {
            if (newProcess.waitFor(1, java.util.concurrent.TimeUnit.SECONDS) && newProcess.exitValue() != 0) {
                throw new IllegalStateException(
                        "New process exited immediately with code " + newProcess.exitValue());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        try {
            Database.getInstance().close();
        } catch (SQLException _) {
        }

        Platform.exit();
        System.exit(0);
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    private void loadFonts() {
        Font.loadFont(Objects.requireNonNull(App.class.getResourceAsStream("/fonts/Inter_18pt-Regular.ttf")), 12);
        Font.loadFont(Objects.requireNonNull(App.class.getResourceAsStream("/fonts/Inter_18pt-Light.ttf")), 12);
        Font.loadFont(Objects.requireNonNull(App.class.getResourceAsStream("/fonts/Inter_18pt-Bold.ttf")), 12);
    }

    private void loadStyles(Scene scene) {
        scene.getStylesheets().add(Objects.requireNonNull(App.class.getResource("/css/base.css")).toExternalForm());
        scene.getStylesheets().add(Objects.requireNonNull(App.class.getResource("/css/sidebar.css")).toExternalForm());
        scene.getStylesheets().add(Objects.requireNonNull(App.class.getResource("/css/overview.css")).toExternalForm());
        scene.getStylesheets().add(Objects.requireNonNull(App.class.getResource("/css/table.css")).toExternalForm());
        scene.getStylesheets().add(Objects.requireNonNull(App.class.getResource("/css/calendar.css")).toExternalForm());
        scene.getStylesheets().add(Objects.requireNonNull(App.class.getResource("/css/analytics.css")).toExternalForm());
    }

    @Override
    public void start(Stage stage) throws Exception {
        loadFonts();
        Database.getInstance().getConnection();
        FXMLLoader loader = new FXMLLoader(App.class.getResource("/fxml/main.fxml"));
        Scene scene = new Scene(loader.load());
        loadStyles(scene);
        stage.setScene(scene);
        stage.setTitle("Corgi Balance");
        stage.setMinWidth(500);
        stage.setMaximized(true);
        stage.show();
    }

    @Override
    public void stop() throws SQLException {
        Database.getInstance().close();
    }
}
