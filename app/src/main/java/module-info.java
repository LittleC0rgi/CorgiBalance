module javafx.demo {
    requires javafx.controls;
    requires javafx.fxml;
    requires static lombok;

    exports org.example;

    opens org.example to javafx.fxml;
}