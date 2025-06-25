package koolfileindexer.ui;

import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import koolfileindexer.ui.components.SearchComponent;

public class MainWindow extends Scene {

    private static Parent getPane() {
        // A grid panel with of 4 by 4
        VBox p = new VBox(10);
        p.setPadding(new Insets(10));
        p.getChildren().add(new SearchComponent());
        return p;
    }

    public MainWindow() {
        super(MainWindow.getPane(), 600, 600);
    }
}
