package koolfileindexer.ui.components;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

/**
 * SearchComponent
 */
public class SearchComponent extends HBox {

    private TextField searchBar = new TextField();

    // constructor
    public SearchComponent() {
        super(10);
        Label label = new Label("Search");
        this.getChildren().add(label);

        this.searchBar.setOnAction(e -> {
                this.executeSearch();
            });
        this.getChildren().add(this.searchBar);

        Button btn = new Button("Search");
        btn.setOnAction(e -> {
            this.executeSearch();
        });
        this.getChildren().add(btn);

        HBox.setHgrow(label, Priority.NEVER);
        HBox.setHgrow(this.searchBar, Priority.ALWAYS);
        HBox.setHgrow(btn, Priority.NEVER);
    }

    private void executeSearch() {
        String search = this.searchBar.getText();
        // TODO: Run "backend"
        System.out.println("Search for " + search);
    }
}
