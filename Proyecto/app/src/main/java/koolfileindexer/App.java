package koolfileindexer;

import java.sql.Connection;
import java.sql.SQLException;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import koolfileindexer.db.ConectorBasedeDatos;
import koolfileindexer.ui.MainWindow;

public class App extends Application {

    // TODO: Move this to it own program
    public static void dbMain(String[] args) throws SQLException {
        ConectorBasedeDatos conector = ConectorBasedeDatos.obtenerInstancia();
        try (Connection conexion = conector.obtenerConexion()) {
            if (conexion != null) {
                System.out.println("Conexión exitosa a PostgreSQL");
            }
        } catch (SQLException e) {
            System.err.println("Error de conexión: " + e.getMessage());
        } finally {
            conector.terminarConexion();
        }
    }

    public static void main(String[] args) {
        System.out.println("👋, 🌎❗");
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        Scene scene = new MainWindow();
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
