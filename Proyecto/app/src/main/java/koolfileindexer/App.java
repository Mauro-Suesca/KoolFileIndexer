package koolfileindexer;
import java.sql.Connection;
import java.sql.SQLException;

import koolfileindexer.db.ConectorBasedeDatos;

public class App {

  public static void main(String[] args) throws SQLException {
    try (Connection conexion_base_de_datos = ConectorBasedeDatos.obtenerConexion()) {
		if (conexion_base_de_datos != null) {
			System.out.println("Conexión exitosa a PostgreSQL");
		}
	} catch (SQLException e) {
		System.err.println("Error de conexión: " + e.getMessage());
	} finally {
		ConectorBasedeDatos.terminarConexion();
	}
  }
}
