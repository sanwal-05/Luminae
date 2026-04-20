package com.hiddeninpixel.db;

import com.hiddeninpixel.model.AnalyticsRecord;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AnalyticsDAO {
    
    private final Connection connection;
    
    public AnalyticsDAO() {
        this.connection = DatabaseConnection.getInstance().getConnection();
    }
    
    public void insert(AnalyticsRecord record) {
        String sql = """
            INSERT INTO stego_analytics 
            (algorithm_name, image_resolution, payload_size_kb, change_ratio, timestamp)
            VALUES (?, ?, ?, ?, ?)
        """;
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, record.getAlgorithmName());
            pstmt.setString(2, record.getImageResolution());
            pstmt.setDouble(3, record.getPayloadSizeKB());
            pstmt.setDouble(4, record.getChangeRatio());
            pstmt.setLong(5, record.getTimestamp());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    public List<AnalyticsRecord> getAll() {
        List<AnalyticsRecord> records = new ArrayList<>();
        String sql = "SELECT * FROM stego_analytics ORDER BY timestamp DESC";
        
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                AnalyticsRecord record = new AnalyticsRecord();
                record.setId(rs.getInt("id"));
                record.setAlgorithmName(rs.getString("algorithm_name"));
                record.setImageResolution(rs.getString("image_resolution"));
                record.setPayloadSizeKB(rs.getDouble("payload_size_kb"));
                record.setChangeRatio(rs.getDouble("change_ratio"));
                record.setTimestamp(rs.getLong("timestamp"));
                records.add(record);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return records;
    }
}
