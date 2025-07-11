package koolfileindexer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class SQLConnector {
    private static final String URL = "jdbc:postgresql://localhost:5432/KoolFileIndexer";
    private static final String USER = "kool_user";
    private static final String PASSWORD = "koolpass";

    private static Connection instance;

     public static synchronized Connection getConnection() throws SQLException {
        try {
            if (instance == null || instance.isClosed()) {
                instance = DriverManager.getConnection(URL, USER, PASSWORD);
            }
        } catch (SQLException e) {
            throw new SQLException("Error al obtener la conexión", e);
        }
        return instance;
    }
}
