package dao;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class PostgreSQLConnection {
    private static final String DEFAULT_URL = "jdbc:postgresql://localhost:5432/pombensin";

    public static Connection getConnection() throws SQLException {
        Properties config = new Properties();
        Path localConfig = Path.of("database", "local.properties");
        if (Files.exists(localConfig)) {
            try (var reader = Files.newBufferedReader(localConfig)) {
                config.load(reader);
            } catch (IOException e) {
                throw new SQLException("Konfigurasi PostgreSQL lokal tidak dapat dibaca.", e);
            }
        }
        String url = config.getProperty("url", DEFAULT_URL);
        return DriverManager.getConnection(url, config.getProperty("user", "postgres"),
                config.getProperty("password", ""));
    }
}
