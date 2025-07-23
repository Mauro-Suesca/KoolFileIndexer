package koolfileindexer.ui.components;

import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.stage.Modality;
import javafx.stage.Stage;
import koolfileindexer.controller.Controller;
import koolfileindexer.ui.ExcludedFoldersWindow;

public class MenuBarComponent extends MenuBar {
    private Menu settings;
    private Menu files;

    public MenuBarComponent(Controller controller) {
        super();
        this.settings = new Menu("Settings");
        MenuItem setExcluded = new MenuItem("Set Excluded Folders");
        setExcluded.setOnAction(event -> {
            Stage popupWindow = new Stage();
            popupWindow.setTitle("Excluded Folders Settings");
            popupWindow.initModality(Modality.APPLICATION_MODAL);
            popupWindow.setScene(new ExcludedFoldersWindow(controller, popupWindow));
            popupWindow.showAndWait();
        });
        this.settings.getItems().addAll(setExcluded);

        this.files = new Menu("Files");
        MenuItem exit = new MenuItem("Exit...");
        exit.setOnAction(event -> {
            // Gracefully close the application
            javafx.application.Platform.exit();
        });
        this.files.getItems().addAll(exit);

        this.getMenus().addAll(this.files, this.settings);
    }
}
