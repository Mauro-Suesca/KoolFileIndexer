package koolfileindexer;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import koolfileindexer.common.Constants;
import koolfileindexer.controller.Controller;
import koolfileindexer.ui.MainWindow;

public class App extends Application {

    Controller controller = new Controller();
    Integer unused = 0;

    public static void main(String[] args) {
        System.out.println("👋, 🌎❗");
        System.out.println(Constants.APP_NAME);
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        Scene scene = new MainWindow(controller);
        primaryStage.setScene(scene);
        System.out.println("Scene Set");
        primaryStage.setMaximized(true);
        primaryStage.setOnCloseRequest(e -> {
            System.out.println("Exiting...");
        });

        primaryStage.setHeight(600);
        primaryStage.setWidth(800);
        primaryStage.show();
    }
}
