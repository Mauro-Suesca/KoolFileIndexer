package koolfileindexer;
import java.sql.Connection;
import java.sql.SQLException;

public class App {

  public static void main(String[] args) throws SQLException {
    try (Connection conn = SQLConnector.getConnection()) {
            if (conn != null) {
                System.out.println("Conexión exitosa a PostgreSQL");
            }
        } catch (SQLException e) {
            System.err.println("Error de conexión: " + e.getMessage());
        }
  }
}
