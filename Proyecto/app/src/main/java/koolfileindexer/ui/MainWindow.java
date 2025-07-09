package koolfileindexer.ui;

import java.util.List;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import koolfileindexer.controller.Controller;
import koolfileindexer.controller.File;
import koolfileindexer.controller.UninitializedServerException;
import koolfileindexer.ui.components.SearchComponent;
import koolfileindexer.ui.components.TableComponent;

public class MainWindow extends Scene {

    private SearchComponent searchBar;
    private TableComponent table;
    private Controller controller;

    public MainWindow(Controller controller) {
        super(new VBox(10), 600, 600);
        this.controller = controller;

        VBox root = (VBox) this.rootProperty().get();
        root.setPadding(new Insets(10));

        this.searchBar = new SearchComponent(search -> {
            try {
                List<File> files = this.controller.searchFiles(search);
                this.table.populateTable(files);
            } catch (UninitializedServerException e) {
                System.out.println("Error found: " + e.getMessage());
            }
        });
        root.getChildren().add(this.searchBar);

        this.table = new TableComponent();

        root.getChildren().add(this.table);

        VBox.setVgrow(this.searchBar, Priority.NEVER);
        VBox.setVgrow(this.table, Priority.ALWAYS);
    }
}
