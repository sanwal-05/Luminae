// AnalyticsService sits between the UI and the database.
// Its job is to take raw data from a completed operation and turn it into a
// neat AnalyticsRecord object, then hand that record to the DAO to be saved.
// This way the UI controller does not need to know anything about databases.
package com.hiddeninpixel.service;

// Import the DAO so we can call its insert method.
import com.hiddeninpixel.db.AnalyticsDAO;

// Import the data container that represents one database row.
import com.hiddeninpixel.model.AnalyticsRecord;

// BufferedImage is Java's class for holding an image in memory (a grid of pixels).
import java.awt.image.BufferedImage;

public class AnalyticsService {

    // The DAO object we will use to actually write to the database.
    // "final" means this reference never changes after the constructor sets it.
    private final AnalyticsDAO dao;

    // Constructor: create the DAO when this service is created.
    public AnalyticsService() {
        this.dao = new AnalyticsDAO();
    }

    // Called by the UI after each successful encode or decode operation.
    // "algorithm" is the name shown in the dropdown (e.g. "Sequential LSB").
    // "cover" is the original image before hiding; "stego" is the output image with the secret.
    // "payloadSize" is the number of bytes (characters) that were hidden.
    public void logOperation(String algorithm, BufferedImage cover,
                             BufferedImage stego, int payloadSize) {

        // Create a new blank record and fill in all the fields.
        AnalyticsRecord record = new AnalyticsRecord();

        // Store which algorithm was used.
        record.setAlgorithmName(algorithm);

        // If we have both images, calculate the resolution and how many pixels were changed.
        if (cover != null && stego != null) {
            // Store the image size as a readable string like "1280x720".
            record.setImageResolution(cover.getWidth() + "x" + cover.getHeight());

            // Calculate what percentage of pixels are different between original and output.
            record.setChangeRatio(calculateChangeRatio(cover, stego));

        } else {
            // Metadata channel operations do not give us two images to compare,
            // so we use placeholder values instead.
            record.setImageResolution("Unknown");
            record.setChangeRatio(0.0);
        }

        // Convert bytes to kilobytes (1 KB = 1024 bytes) and store it.
        record.setPayloadSizeKB(payloadSize / 1024.0);

        // Record the current time in milliseconds. This is a Unix timestamp.
        // (Milliseconds that have passed since January 1st, 1970)
        record.setTimestamp(System.currentTimeMillis());

        // Details start empty; the user can add notes later from the analytics table.
        record.setDetails(null);

        // Tell the DAO to write this record to the database.
        dao.insert(record);
    }

    // Counts how many pixels are different between the original and the stego image.
    // Returns the result as a percentage of the total number of pixels.
    private double calculateChangeRatio(BufferedImage cover, BufferedImage stego) {
        // Total number of pixels in the image (width times height).
        int totalPixels = cover.getWidth() * cover.getHeight();

        // Counter for pixels that changed.
        int changedPixels = 0;

        // Loop over every row (y) and every column (x) in the image.
        for (int y = 0; y < cover.getHeight(); y++) {
            for (int x = 0; x < cover.getWidth(); x++) {
                // getRGB returns one integer that encodes the Red, Green, Blue values of a pixel.
                // If the cover pixel and stego pixel are not identical, that pixel was changed.
                if (cover.getRGB(x, y) != stego.getRGB(x, y)) {
                    changedPixels++;
                }
            }
        }

        // Divide changed pixels by total pixels and multiply by 100 to get a percentage.
        return (changedPixels * 100.0) / totalPixels;
    }
}
