// ImageProcessor handles reading and writing image files and some analysis tasks.
// It uses Java's built-in ImageIO library which knows how to handle common image formats.
// By putting all image I/O in one place, all other classes stay simpler.
package com.hiddeninpixel.service;

// ImageIO is Java's image reading and writing toolkit.
import javax.imageio.ImageIO;

// BufferedImage is how Java stores an image in memory as a grid of pixel values.
import java.awt.image.BufferedImage;

// File represents a path to a file on disk.
import java.io.File;

// IOException is thrown when reading or writing a file fails (e.g. file not found).
import java.io.IOException;

// Our custom error types.
import com.hiddeninpixel.exception.*;

public class ImageProcessor {

    // Reads an image file from disk into memory as a BufferedImage.
    // Throws an error if the file is not a PNG or if reading fails.
    public BufferedImage loadImage(File file) throws StegoException {
        // We only support PNG format because PNG is lossless.
        // JPEG is lossy: it slightly changes pixel values when saving, which would
        // destroy any hidden data we embedded. PNG keeps pixels exactly as-is.
        if (!file.getName().toLowerCase().endsWith(".png")) {
            throw new UnsupportedImageFormatException("Only PNG format is supported");
        }

        try {
            // Ask ImageIO to read the file and give us a BufferedImage object.
            BufferedImage img = ImageIO.read(file);

            // If ImageIO returned null, the file exists but could not be decoded.
            if (img == null) {
                throw new StegoException("Failed to read image file");
            }

            // Some PNGs are stored with an alpha channel (RGBA, or transparency data).
            // Our algorithms only work with pure RGB. So if the image is not already RGB,
            // we redraw it onto a fresh RGB canvas to convert it.
            if (img.getType() != BufferedImage.TYPE_INT_RGB) {
                // Create a blank RGB image with the same dimensions.
                BufferedImage rgb = new BufferedImage(
                    img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_RGB
                );
                // Draw the original image onto the RGB canvas (alpha is dropped here).
                rgb.getGraphics().drawImage(img, 0, 0, null);
                return rgb;
            }

            return img;

        } catch (IOException e) {
            throw new StegoException("Image loading failed: " + e.getMessage());
        }
    }

    // Saves a BufferedImage from memory to a file on disk in PNG format.
    // Called after hiding a message so the stego image can be saved.
    public void saveImage(BufferedImage image, File file) throws StegoException {
        try {
            // ImageIO.write returns false if it cannot find a writer for the format.
            if (!ImageIO.write(image, "PNG", file)) {
                throw new StegoException("PNG encoding failed");
            }
        } catch (IOException e) {
            throw new StegoException("Image saving failed: " + e.getMessage());
        }
    }

    // Creates a "difference map" by comparing two images pixel by pixel.
    // Any pixel that changed gets amplified (multiplied by 100) so the tiny LSB changes
    // become visible as bright spots. This is a forensic tool to visualize where data was hidden.
    public BufferedImage generateDifferenceMap(BufferedImage cover, BufferedImage stego) {
        int width = cover.getWidth();
        int height = cover.getHeight();

        // Create a blank image of the same size to draw the differences onto.
        BufferedImage diff = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        // Loop over every pixel position in the image.
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                // Get the packed RGB integer for each pixel in both images.
                int coverRGB = cover.getRGB(x, y);
                int stegoRGB = stego.getRGB(x, y);

                // Extract and compare each channel separately.
                // Math.abs gives the absolute difference (always positive).
                int rDiff = Math.abs(((coverRGB >> 16) & 0xFF) - ((stegoRGB >> 16) & 0xFF));
                int gDiff = Math.abs(((coverRGB >> 8) & 0xFF) - ((stegoRGB >> 8) & 0xFF));
                int bDiff = Math.abs((coverRGB & 0xFF) - (stegoRGB & 0xFF));

                // Multiply differences by 100 so a change of 1 becomes 100 (visible).
                // Math.min clamps the value at 255 so it stays within a valid color range.
                int diffRGB = ((Math.min(rDiff * 100, 255)) << 16) |
                              ((Math.min(gDiff * 100, 255)) << 8) |
                              (Math.min(bDiff * 100, 255));

                // Write the amplified difference pixel into our diff image.
                diff.setRGB(x, y, diffRGB);
            }
        }

        return diff;
    }

    // Calculates PSNR: Peak Signal-to-Noise Ratio.
    // This is a standard engineering measurement of image quality.
    // Higher PSNR means less visible distortion was introduced.
    // Values above 40 dB are generally considered imperceptible to the human eye.
    public double calculatePSNR(BufferedImage cover, BufferedImage stego) {
        int width = cover.getWidth();
        int height = cover.getHeight();

        // MSE = Mean Squared Error: the average squared difference per channel value.
        double mse = 0.0;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int coverRGB = cover.getRGB(x, y);
                int stegoRGB = stego.getRGB(x, y);

                // Calculate the difference in each channel, then square it.
                // Squaring penalizes large differences more than small ones.
                int rDiff = ((coverRGB >> 16) & 0xFF) - ((stegoRGB >> 16) & 0xFF);
                int gDiff = ((coverRGB >> 8) & 0xFF) - ((stegoRGB >> 8) & 0xFF);
                int bDiff = (coverRGB & 0xFF) - (stegoRGB & 0xFF);

                mse += (rDiff * rDiff + gDiff * gDiff + bDiff * bDiff);
            }
        }

        // Divide by the total number of channel samples (3 channels per pixel).
        mse /= (width * height * 3.0);

        // If MSE is zero, the images are identical, so PSNR is infinite (perfect quality).
        if (mse == 0) return Double.POSITIVE_INFINITY;

        // PSNR formula: 20 * log10(MAX / sqrt(MSE)), where MAX = 255 (max pixel value).
        double maxPixel = 255.0;
        return 20 * Math.log10(maxPixel / Math.sqrt(mse));
    }
}
