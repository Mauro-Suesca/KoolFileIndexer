package koolfileindexer.ui.components;

import java.util.Collection;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import koolfileindexer.controller.File;

public class TableComponent extends TableView<File> {

    public TableComponent() {
        super();
        TableColumn<File, String> nameColumn = new TableColumn<>("Filename");
        nameColumn.setPrefWidth(150);
        nameColumn.setCellValueFactory(cellData -> {
            File file = cellData.getValue();
            return new SimpleStringProperty(
                file.getName() + "." + file.getExtension()
            );
        });
        TableColumn<File, Integer> sizeColumn = new TableColumn<>("Size");
        sizeColumn.setPrefWidth(50);
        sizeColumn.setCellValueFactory(new PropertyValueFactory<>("size"));
        TableColumn<File, String> pathColumn = new TableColumn<>("Path");
        pathColumn.setPrefWidth(300);
        pathColumn.setCellValueFactory(new PropertyValueFactory<>("path"));

        this.getColumns().add(nameColumn);
        this.getColumns().add(sizeColumn);
        this.getColumns().add(pathColumn);
    }

    public void populateTable(File... files) {
        this.getItems().setAll(files);
    }

    public void populateTable(Collection<? extends File> files) {
        this.getItems().setAll(files);
    }
}
