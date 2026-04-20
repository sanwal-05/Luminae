package com.hiddeninpixel.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Singleton pattern for database connection management.
 */
public class DatabaseConnection {
    
    private static DatabaseConnection instance;
    private Connection connection;
    private static final String DB_URL = "jdbc:h2:~/hiddeninpixel";
    
    private DatabaseConnection() {
        try {
            Class.forName("org.h2.Driver");
            connection = DriverManager.getConnection(DB_URL);
            initializeSchema();
        } catch (ClassNotFoundException | SQLException e) {
            throw new RuntimeException("Database initialization failed", e);
        }
    }
    
    public static synchronized DatabaseConnection getInstance() {
        if (instance == null) {
            instance = new DatabaseConnection();
        }
        return instance;
    }
    
    public Connection getConnection() {
        return connection;
    }
    
    private void initializeSchema() throws SQLException {
        String createTable = """
            CREATE TABLE IF NOT EXISTS stego_analytics (
                id INT AUTO_INCREMENT PRIMARY KEY,
                algorithm_name VARCHAR(50) NOT NULL,
                image_resolution VARCHAR(20),
                payload_size_kb DOUBLE,
                change_ratio DOUBLE,
                timestamp BIGINT
            )
        """;
        
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createTable);
        }
    }
    
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
