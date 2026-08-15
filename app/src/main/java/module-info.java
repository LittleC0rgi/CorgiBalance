module javafx.demo {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires org.xerial.sqlitejdbc;
    requires static lombok;

    exports com.corgibalance;
    exports com.corgibalance.components;
    exports com.corgibalance.components.views;
    exports com.corgibalance.components.dialogs;
    exports com.corgibalance.repositories;
    exports com.corgibalance.services;
    exports com.corgibalance.models;


    opens com.corgibalance to javafx.fxml;
    opens com.corgibalance.components to javafx.fxml;
    opens com.corgibalance.components.views to javafx.fxml;
    opens com.corgibalance.components.dialogs to javafx.fxml;
    exports com.corgibalance.controllers;
    opens com.corgibalance.controllers to javafx.fxml;
}
