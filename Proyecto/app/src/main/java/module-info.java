module app {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;

    requires common;

    exports koolfileindexer to javafx.graphics;
}
