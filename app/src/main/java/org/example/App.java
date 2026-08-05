package org.example;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.util.Objects;


public class App extends Application {

    private void loadFonts() {
        Font font = Font.loadFont(
                Objects.requireNonNull(
                        App.class.getResourceAsStream("/fonts/Inter_18pt-Regular.ttf")
                ),
                12
        );

        System.out.println(font.getFamily());
        System.out.println(font.getName());


        Font.loadFont(
                Objects.requireNonNull(
                        App.class.getResourceAsStream("/fonts/Inter_18pt-Light.ttf")
                ),
                12
        );

        Font.loadFont(
                Objects.requireNonNull(
                        App.class.getResourceAsStream("/fonts/Inter_18pt-Bold.ttf")
                ),
                12
        );
    }

    @Override
    public void start(Stage stage) throws Exception {
        loadFonts();
        FXMLLoader loader = new FXMLLoader(App.class.getResource("/fxml/main.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root);
        scene.getStylesheets().add(Objects.requireNonNull(App.class.getResource("/css/style.css")).toExternalForm());
        stage.setScene(scene);
        stage.setTitle("Corgi Balance");
        stage.setMaximized(true);
        stage.show();
    }
}
