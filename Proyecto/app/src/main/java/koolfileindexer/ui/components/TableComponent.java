package koolfileindexer.ui.components;

import java.util.Collection;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import koolfileindexer.common.model.File;
import koolfileindexer.controller.Controller;

public class TableComponent extends TableView<File> {

    public TableComponent(Controller controller) {
        super();
        TableColumn<File, String> nameColumn = new TableColumn<>("Filename");
        nameColumn.setPrefWidth(150);
        nameColumn.setCellValueFactory(cellData -> {
            File file = cellData.getValue();
            return new SimpleStringProperty(
                    file.getName());
        });
        TableColumn<File, Integer> sizeColumn = new TableColumn<>("Size");
        sizeColumn.setPrefWidth(50);
        sizeColumn.setCellValueFactory(new PropertyValueFactory<>("size"));
        TableColumn<File, String> pathColumn = new TableColumn<>("Path");
        pathColumn.setPrefWidth(300);
        pathColumn.setCellValueFactory(new PropertyValueFactory<>("path"));
        TableColumn<File, String> modificationDateColumn = new TableColumn<>(
                "Modification Date");
        modificationDateColumn.setPrefWidth(300);
        modificationDateColumn.setCellValueFactory(
                new PropertyValueFactory<>("modifiedDate"));

        ContextMenu menu = new ContextMenu();

        MenuItem addKeyword = new MenuItem("Add Keyword");
        MenuItem addTag = new MenuItem("Add Tag");

        menu.getItems().addAll(addKeyword, addTag);
        this.setContextMenu(menu);

        addKeyword.setOnAction(event -> {
            File file = this.getSelectionModel().getSelectedItem();
            // Create a popup window to get the keyword value
            javafx.scene.control.TextInputDialog dialog = new javafx.scene.control.TextInputDialog();
            dialog.setTitle("Add Keyword");
            dialog.setHeaderText("Enter a keyword for the selected file:");
            dialog.setContentText("Keyword:");
            java.util.Optional<String> result = dialog.showAndWait();
            result.ifPresent(keyword -> {
                if (file != null && !keyword.trim().isEmpty()) {
                    controller.setKeyword(file, keyword.trim());
                }
            });
        });
        addTag.setOnAction(event -> {
            File file = this.getSelectionModel().getSelectedItem();
            // Create a popup window to get the tag value
            javafx.scene.control.TextInputDialog dialog = new javafx.scene.control.TextInputDialog();
            dialog.setTitle("Add Tag");
            dialog.setHeaderText("Enter a tag for the selected file:");
            dialog.setContentText("Tag:");
            java.util.Optional<String> result = dialog.showAndWait();
            result.ifPresent(tag -> {
                if (file != null && !tag.trim().isEmpty()) {
                    controller.setTag(file, tag.trim());
                }
            });
        });

        this.getColumns().add(nameColumn);
        this.getColumns().add(sizeColumn);
        this.getColumns().add(pathColumn);
        this.getColumns().add(modificationDateColumn);
    }

    public void populateTable(File... files) {
        this.getItems().setAll(files);
    }

    public void populateTable(Collection<? extends File> files) {
        this.getItems().setAll(files);
    }
}
