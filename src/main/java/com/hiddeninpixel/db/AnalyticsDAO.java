// DAO stands for Data Access Object.
// This class is the ONLY place in the whole app that talks to the database directly.
// It knows how to save a record, load all records, update a record, and delete a record.
// Every other class just calls these methods without needing to know any SQL.
// This separation is a good design practice: the rest of the app stays clean.
package com.hiddeninpixel.db;

// Import the data container that represents one row in our analytics table.
import com.hiddeninpixel.model.AnalyticsRecord;

// Java SQL imports for connecting, querying, and reading results from the database.
import java.sql.*;

// We use List and ArrayList to return multiple records at once.
import java.util.ArrayList;
import java.util.List;

public class AnalyticsDAO {

    // The connection object we use to send SQL commands to the database.
    // We get this from the DatabaseConnection singleton rather than creating a new one.
    private final Connection connection;

    // When this DAO is created, it grabs the shared database connection immediately.
    public AnalyticsDAO() {
        this.connection = DatabaseConnection.getInstance().getConnection();
    }

    // Inserts a new record into the database.
    // This is called every time the user finishes a hide or extract operation.
    public void insert(AnalyticsRecord record) {
        // The SQL INSERT statement. The "?" marks are placeholders.
        // We fill them in below using setString, setDouble, etc.
        // Using placeholders (called prepared statements) protects against SQL injection attacks.
        String sql = """
            INSERT INTO stego_analytics 
            (algorithm_name, image_resolution, payload_size_kb, change_ratio, timestamp, details)
            VALUES (?, ?, ?, ?, ?, ?)
        """;

        // "try-with-resources" automatically closes the PreparedStatement when done,
        // even if an error happens. This prevents resource leaks.
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            // Fill in each "?" placeholder with the real value from the record object.
            pstmt.setString(1, record.getAlgorithmName());   // slot 1: algorithm name
            pstmt.setString(2, record.getImageResolution()); // slot 2: image size like "1920x1080"
            pstmt.setDouble(3, record.getPayloadSizeKB());   // slot 3: how many KB were hidden
            pstmt.setDouble(4, record.getChangeRatio());     // slot 4: percentage of pixels changed
            pstmt.setLong(5, record.getTimestamp());         // slot 5: when it happened
            pstmt.setString(6, record.getDetails());         // slot 6: optional user notes

            // Actually run the INSERT and write the data to disk.
            pstmt.executeUpdate();

        } catch (SQLException e) {
            // If inserting fails, print what went wrong for debugging.
            e.printStackTrace();
        }
    }

    // Fetches every record from the database, newest first.
    // Returns them as a List (a resizable ordered collection).
    public List<AnalyticsRecord> getAll() {
        // A List to collect all the rows we find.
        List<AnalyticsRecord> records = new ArrayList<>();

        // This SQL reads all rows and sorts them so the most recent is at the top.
        String sql = "SELECT * FROM stego_analytics ORDER BY timestamp DESC";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            // "rs.next()" moves to the next row. We loop until there are no more rows.
            while (rs.next()) {
                // Create a new empty record and fill it with the data from the current row.
                AnalyticsRecord record = new AnalyticsRecord();

                record.setId(rs.getInt("id"));                        // read the auto-generated ID
                record.setAlgorithmName(rs.getString("algorithm_name")); // read algorithm name
                record.setImageResolution(rs.getString("image_resolution")); // read image size
                record.setPayloadSizeKB(rs.getDouble("payload_size_kb")); // read KB value
                record.setChangeRatio(rs.getDouble("change_ratio"));   // read % changed
                record.setTimestamp(rs.getLong("timestamp"));          // read when it happened
                record.setDetails(rs.getString("details"));            // read optional user notes

                // Add this finished record to our collection.
                records.add(record);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        // Return the full collection of records to whoever called this method.
        return records;
    }

    // Deletes one specific record from the database by its unique ID.
    // This is called when the user clicks the Delete button on a row.
    public void delete(int id) {
        // SQL DELETE statement: remove the row where the id column matches.
        String sql = "DELETE FROM stego_analytics WHERE id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            // Fill in the ID placeholder with the actual ID we want to delete.
            pstmt.setInt(1, id);

            // Execute the delete. The row is gone from the database permanently.
            pstmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Updates only the "details" text field for one specific record.
    // This is called when the user types into the Details column and saves.
    public void updateDetails(int id, String details) {
        // SQL UPDATE statement: change only the "details" column for the matching row.
        String sql = "UPDATE stego_analytics SET details = ? WHERE id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            // Slot 1 is the new details text.
            pstmt.setString(1, details);

            // Slot 2 is which row to update (matched by ID).
            pstmt.setInt(2, id);

            // Execute the update. The row is changed on disk immediately.
            pstmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
