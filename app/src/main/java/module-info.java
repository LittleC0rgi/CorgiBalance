module javafx.demo {
    requires javafx.controls;
    requires javafx.fxml;
    requires static lombok;

    exports org.example;
    exports org.example.components;
    exports org.example.views;

    opens org.example to javafx.fxml;
    opens org.example.components to javafx.fxml;
    opens org.example.views to javafx.fxml;
}
