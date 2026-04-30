// This class manages the connection to our database.
// A database is like a filing cabinet: it stores data permanently so it survives
// even after the app is closed and reopened.
// We use H2, which is a tiny database that runs inside the app itself.
// No need to install a separate database server.
//
// This class uses the "Singleton" design pattern.
// Singleton means: only one instance of this class is ever created.
// Think of it like having just one key to the filing cabinet for the whole app.
// That way we don't accidentally open two connections at the same time and confuse things.
package com.hiddeninpixel.db;

// Java's built-in SQL library lets us talk to databases using standard SQL commands.
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseConnection {

    // "instance" holds the one and only DatabaseConnection object.
    // It starts as null, meaning the connection hasn't been created yet.
    private static DatabaseConnection instance;

    // "connection" is the actual live link to the database file on disk.
    private Connection connection;

    // The URL that tells Java where the database file lives.
    // "jdbc:h2:~/luminae" means: use H2, and store the file as "luminae.mv.db" in the home directory.
    private static final String DB_URL = "jdbc:h2:~/luminae";

    // The constructor is private so that nobody outside this class can call "new DatabaseConnection()".
    // This forces everyone to use "getInstance()" instead, which enforces the singleton rule.
    private DatabaseConnection() {
        try {
            // Tell Java which database driver to use. H2 is the brand of our database.
            Class.forName("org.h2.Driver");

            // Open the actual connection to the database file.
            connection = DriverManager.getConnection(DB_URL);

            // Create the tables we need if they don't already exist.
            initializeSchema();

        } catch (ClassNotFoundException | SQLException e) {
            // If anything goes wrong here, the whole app cannot function, so we crash loudly.
            throw new RuntimeException("Database initialization failed", e);
        }
    }

    // This is how the rest of the app gets the single shared connection.
    // "synchronized" means: if two things try to call this at the exact same moment,
    // only one runs at a time. This prevents accidentally creating two instances.
    public static synchronized DatabaseConnection getInstance() {
        // If no connection exists yet, create one.
        if (instance == null) {
            instance = new DatabaseConnection();
        }
        // Return the one shared instance.
        return instance;
    }

    // Gives other classes access to the raw SQL connection object.
    public Connection getConnection() {
        return connection;
    }

    // This method creates the database tables when the app first runs.
    // "IF NOT EXISTS" means: if the table already exists from a previous run, skip creating it.
    // This is safe to run every single time the app starts.
    private void initializeSchema() throws SQLException {
        // This is a SQL statement that creates our table.
        // Think of it like designing a spreadsheet with column names and types.
        String createTable = """
            CREATE TABLE IF NOT EXISTS stego_analytics (
                id INT AUTO_INCREMENT PRIMARY KEY,
                algorithm_name VARCHAR(50) NOT NULL,
                image_resolution VARCHAR(20),
                payload_size_kb DOUBLE,
                change_ratio DOUBLE,
                timestamp BIGINT,
                details VARCHAR(1000)
            )
        """;

        // Execute the SQL. "try-with-resources" automatically closes the Statement when done.
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createTable);
        }

        // Also run an ALTER TABLE in case the database was created by an older version
        // of this app that did not have the "details" column yet.
        // If the column already exists, H2 will throw an error which we just ignore.
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("ALTER TABLE stego_analytics ADD COLUMN IF NOT EXISTS details VARCHAR(1000)");
        } catch (SQLException ignored) {
            // The column already exists, which is fine. We just move on.
        }
    }

    // Closes the database connection cleanly when the app is shutting down.
    // This is important so no data gets corrupted.
    public void close() {
        try {
            // Only try to close if the connection was actually opened.
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
