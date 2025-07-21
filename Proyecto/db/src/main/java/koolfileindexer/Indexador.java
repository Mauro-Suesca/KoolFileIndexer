package koolfileindexer;

import java.sql.Connection;
import java.sql.SQLException;
import koolfileindexer.db.ConectorBasedeDatos;

public class Indexador {

    public static void main(String[] args) throws SQLException {
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
}
