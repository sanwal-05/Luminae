package com.hiddeninpixel.model;

public class AnalyticsRecord {
    private int id;
    private String algorithmName;
    private String imageResolution;
    private double payloadSizeKB;
    private double changeRatio;
    private long timestamp;
    
    // Getters and setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public String getAlgorithmName() { return algorithmName; }
    public void setAlgorithmName(String algorithmName) { this.algorithmName = algorithmName; }
    
    public String getImageResolution() { return imageResolution; }
    public void setImageResolution(String imageResolution) { this.imageResolution = imageResolution; }
    
    public double getPayloadSizeKB() { return payloadSizeKB; }
    public void setPayloadSizeKB(double payloadSizeKB) { this.payloadSizeKB = payloadSizeKB; }
    
    public double getChangeRatio() { return changeRatio; }
    public void setChangeRatio(double changeRatio) { this.changeRatio = changeRatio; }
    
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
