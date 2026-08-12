package com.corgibalance;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import com.corgibalance.services.Database;

import java.sql.SQLException;
import java.util.Objects;


public class App extends Application {

    private void loadFonts() {
        Font.loadFont(Objects.requireNonNull(App.class.getResourceAsStream("/fonts/Inter_18pt-Regular.ttf")), 12);
        Font.loadFont(Objects.requireNonNull(App.class.getResourceAsStream("/fonts/Inter_18pt-Light.ttf")), 12);
        Font.loadFont(Objects.requireNonNull(App.class.getResourceAsStream("/fonts/Inter_18pt-Bold.ttf")), 12);
    }

    private void loadStyles(Scene scene) {

        scene.getStylesheets().add(Objects.requireNonNull(App.class.getResource("/css/base.css")).toExternalForm());
        scene.getStylesheets().add(Objects.requireNonNull(App.class.getResource("/css/sidebar.css")).toExternalForm());
        scene.getStylesheets().add(Objects.requireNonNull(App.class.getResource("/css/views.css")).toExternalForm());
        scene.getStylesheets().add(Objects.requireNonNull(App.class.getResource("/css/table.css")).toExternalForm());
        scene.getStylesheets().add(Objects.requireNonNull(App.class.getResource("/css/overview.css")).toExternalForm());
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
        stage.setMaximized(true);
        stage.show();
    }

    @Override
    public void stop() throws SQLException {
        Database.getInstance().close();
    }
}
