module javafx.demo {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires org.xerial.sqlitejdbc;
    requires static lombok;

    exports org.example;
    exports org.example.components;
    exports org.example.components.views;

    opens org.example to javafx.fxml;
    opens org.example.components to javafx.fxml;
    opens org.example.components.views to javafx.fxml;
    exports org.example.controllers;
    opens org.example.controllers to javafx.fxml;
}
