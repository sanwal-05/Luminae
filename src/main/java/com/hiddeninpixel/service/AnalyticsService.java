package com.hiddeninpixel.service;

import com.hiddeninpixel.db.AnalyticsDAO;
import com.hiddeninpixel.model.AnalyticsRecord;
import java.awt.image.BufferedImage;

/**
 * SRP: Business logic for analytics.
 */
public class AnalyticsService {
    
    private final AnalyticsDAO dao;
    
    public AnalyticsService() {
        this.dao = new AnalyticsDAO();
    }
    
    public void logOperation(String algorithm, BufferedImage cover, 
                             BufferedImage stego, int payloadSize) {
        
        AnalyticsRecord record = new AnalyticsRecord();
        record.setAlgorithmName(algorithm);
        record.setImageResolution(cover.getWidth() + "x" + cover.getHeight());
        record.setPayloadSizeKB(payloadSize / 1024.0);
        record.setChangeRatio(calculateChangeRatio(cover, stego));
        record.setTimestamp(System.currentTimeMillis());
        
        dao.insert(record);
    }
    
    private double calculateChangeRatio(BufferedImage cover, BufferedImage stego) {
        int totalPixels = cover.getWidth() * cover.getHeight();
        int changedPixels = 0;
        
        for (int y = 0; y < cover.getHeight(); y++) {
            for (int x = 0; x < cover.getWidth(); x++) {
                if (cover.getRGB(x, y) != stego.getRGB(x, y)) {
                    changedPixels++;
                }
            }
        }
        
        return (changedPixels * 100.0) / totalPixels;
    }
}
