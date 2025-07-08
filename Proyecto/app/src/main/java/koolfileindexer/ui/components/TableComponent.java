package koolfileindexer.ui.components;

import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

// TODO: Replace String for file
public class TableComponent extends TableView<String> {

    public TableComponent() {
        super();
        TableColumn<String, String> column = new TableColumn<>("data");
        column.setPrefWidth(200);
        column.setCellValueFactory(cellData -> {
            String value = cellData.getValue();
            return new SimpleStringProperty(value);
        });
        this.getColumns().add(column);
        this.getItems().addAll("element1", "element2", "element3");
    }
}
