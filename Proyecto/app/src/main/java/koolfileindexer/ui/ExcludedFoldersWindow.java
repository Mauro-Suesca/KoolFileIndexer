package koolfileindexer.ui;

import java.util.Optional;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import koolfileindexer.controller.Controller;

public class ExcludedFoldersWindow extends Scene {

    private Controller controller;
    private Button addButton;
    private Button modButton;
    private Button delButton;
    private TextField itemField;
    private ObservableList<String> observableList;
    private ListView<String> listView;
    private Button applyButton;
    private ObjectProperty<Optional<Integer>> selectedItem;
    private Button cancelButton;

    public ExcludedFoldersWindow(Controller controller, Stage popupWindow) {
        // Initialization
        super(new HBox());
        this.controller = controller;

        this.observableList = FXCollections.observableArrayList(this.controller.excludedFolders);

        this.selectedItem = new SimpleObjectProperty<Optional<Integer>>();

        // Structure
        HBox root = (HBox) this.rootProperty().get();
        root.setPadding(new Insets(20));

        VBox textContiner = new VBox();
        textContiner.setPadding(new Insets(10));

        this.listView = new ListView<>(this.observableList);
        this.itemField = new TextField();
        textContiner.getChildren().addAll(this.listView, this.itemField);

        VBox buttonsContainer = new VBox();
        buttonsContainer.setPadding(new Insets(10));

        this.addButton = new Button("Add");
        this.modButton = new Button("Modify");
        this.delButton = new Button("Delete");
        this.cancelButton = new Button("Cancel");
        this.applyButton = new Button("Apply");
        buttonsContainer.getChildren().addAll(this.addButton, this.modButton, this.delButton, this.applyButton,
                this.cancelButton);

        root.getChildren().addAll(textContiner, buttonsContainer);

        // Function
        this.applyButton.setOnAction(event -> {
            System.out.println("Saving changes!");
            this.controller.setExcludedFiles(this.observableList.iterator());

            popupWindow.close();
        });
        this.listView.getSelectionModel().selectedItemProperty().addListener((observable, i, newI) -> {
            this.itemField.setText(newI);
        });
        this.listView.getSelectionModel().selectedIndexProperty().addListener((observable, i, newI) -> {
            if (newI.intValue() < 0) {
                this.selectedItem.set(Optional.empty());
                return;
            }
            this.selectedItem.set(Optional.of(newI.intValue()));
        });

        this.selectedItem.addListener((observable, i, optional) -> {
            if (optional.isPresent()) {
                this.modButton.setDisable(false);
                this.delButton.setDisable(false);
            } else {
                this.modButton.setDisable(true);
                this.delButton.setDisable(true);
            }
        });
        this.selectedItem.set(Optional.empty());

        this.addButton.setOnAction(event -> {
            String newExclude = this.itemField.getText();
            if (newExclude == null || newExclude.isEmpty()) {
                return;
            }

            this.observableList.add(newExclude);
        });

        this.modButton.setOnAction(event -> {
            String modExclude = this.itemField.getText();
            if (modExclude == null || modExclude.isEmpty()) {
                return;
            }

            Integer index = this.selectedItem.get().get(); // At this point this is already garantee to exist since the
                                                           // button is disable if this is empty
            this.observableList.set(index.intValue(), modExclude);
        });

        this.delButton.setOnAction(event -> {
            Integer index = this.selectedItem.get().get(); // Same as the previous comment
            this.observableList.remove(index.intValue());
            this.listView.getSelectionModel().clearSelection();
            this.itemField.setText("");
        });
    }

}
