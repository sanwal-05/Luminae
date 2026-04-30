// This model class represents a single row of data in our analytics table.
// Think of it like one row in a spreadsheet: it holds all the facts about one operation
// that the user performed (hiding or extracting a message).
// This type of class is called a POJO, which stands for Plain Old Java Object.
// It has no logic inside it, it just stores data and lets other parts of the code read it.
package com.hiddeninpixel.model;

public class AnalyticsRecord {

    // The unique number that the database assigns to this row automatically.
    // Every new row gets the next number: 1, 2, 3, and so on.
    private int id;

    // Which algorithm was used for this operation (e.g. "Sequential LSB").
    private String algorithmName;

    // The width and height of the image used, stored as a string like "1920x1080".
    private String imageResolution;

    // How many kilobytes of secret data were hidden in the image.
    private double payloadSizeKB;

    // What percentage of the pixels in the image were changed during hiding.
    private double changeRatio;

    // The exact moment this operation happened, stored as a Unix timestamp (milliseconds since 1970).
    private long timestamp;

    // A free-text notes field where the user can write anything they want about this record.
    // It starts out empty (null) until the user types something.
    private String details;

    // Getters and setters below.
    // A "getter" lets other classes read the value stored in a private field.
    // A "setter" lets other classes change the value stored in a private field.
    // We keep the fields private so that only controlled code can access them.

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

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
}
