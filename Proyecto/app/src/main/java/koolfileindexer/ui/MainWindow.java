package koolfileindexer.ui;

import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import koolfileindexer.ui.components.SearchComponent;
import koolfileindexer.ui.components.TableComponent;

public class MainWindow extends Scene {

    private static Parent getPane() {
        // A grid panel with of 4 by 4
        VBox p = new VBox(10);
        p.setPadding(new Insets(10));
        HBox searchBar = new SearchComponent();
        p.getChildren().add(searchBar);
        TableComponent table = new TableComponent();
        p.getChildren().add(table);

        VBox.setVgrow(searchBar, Priority.NEVER);
        VBox.setVgrow(table, Priority.ALWAYS);
        return p;
    }

    public MainWindow() {
        super(MainWindow.getPane(), 600, 600);
    }
}
