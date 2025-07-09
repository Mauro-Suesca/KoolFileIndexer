package koolfileindexer.ui.components;

import java.util.function.Consumer;
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
    private Consumer<String> searchCB;

    // constructor
    public SearchComponent(Consumer<String> searchCB) {
        super(10);
        this.searchCB = searchCB;

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
        this.searchCB.accept(search);
    }
}
