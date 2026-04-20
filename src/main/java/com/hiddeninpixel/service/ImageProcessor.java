package com.hiddeninpixel.service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import com.hiddeninpixel.exception.*;

/**
 * SRP: Image I/O operations and format validation.
 */
public class ImageProcessor {
    
    public BufferedImage loadImage(File file) throws StegoException {
        if (!file.getName().toLowerCase().endsWith(".png")) {
            throw new UnsupportedImageFormatException("Only PNG format is supported");
        }
        
        try {
            BufferedImage img = ImageIO.read(file);
            if (img == null) {
                throw new StegoException("Failed to read image file");
            }
            
            // Convert to RGB if needed
            if (img.getType() != BufferedImage.TYPE_INT_RGB) {
                BufferedImage rgb = new BufferedImage(
                    img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_RGB
                );
                rgb.getGraphics().drawImage(img, 0, 0, null);
                return rgb;
            }
            
            return img;
            
        } catch (IOException e) {
            throw new StegoException("Image loading failed: " + e.getMessage());
        }
    }
    
    public void saveImage(BufferedImage image, File file) throws StegoException {
        try {
            if (!ImageIO.write(image, "PNG", file)) {
                throw new StegoException("PNG encoding failed");
            }
        } catch (IOException e) {
            throw new StegoException("Image saving failed: " + e.getMessage());
        }
    }
    
    /**
     * Generates pixel-wise difference map for forensic analysis.
     */
    public BufferedImage generateDifferenceMap(BufferedImage cover, BufferedImage stego) {
        int width = cover.getWidth();
        int height = cover.getHeight();
        
        BufferedImage diff = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int coverRGB = cover.getRGB(x, y);
                int stegoRGB = stego.getRGB(x, y);
                
                int rDiff = Math.abs(((coverRGB >> 16) & 0xFF) - ((stegoRGB >> 16) & 0xFF));
                int gDiff = Math.abs(((coverRGB >> 8) & 0xFF) - ((stegoRGB >> 8) & 0xFF));
                int bDiff = Math.abs((coverRGB & 0xFF) - (stegoRGB & 0xFF));
                
                // Amplify differences (scale by 100 for visibility)
                int diffRGB = ((Math.min(rDiff * 100, 255)) << 16) |
                              ((Math.min(gDiff * 100, 255)) << 8) |
                              (Math.min(bDiff * 100, 255));
                
                diff.setRGB(x, y, diffRGB);
            }
        }
        
        return diff;
    }
    
    /**
     * Calculates PSNR (Peak Signal-to-Noise Ratio) for quality assessment.
     */
    public double calculatePSNR(BufferedImage cover, BufferedImage stego) {
        int width = cover.getWidth();
        int height = cover.getHeight();
        
        double mse = 0.0;
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int coverRGB = cover.getRGB(x, y);
                int stegoRGB = stego.getRGB(x, y);
                
                int rDiff = ((coverRGB >> 16) & 0xFF) - ((stegoRGB >> 16) & 0xFF);
                int gDiff = ((coverRGB >> 8) & 0xFF) - ((stegoRGB >> 8) & 0xFF);
                int bDiff = (coverRGB & 0xFF) - (stegoRGB & 0xFF);
                
                mse += (rDiff * rDiff + gDiff * gDiff + bDiff * bDiff);
            }
        }
        
        mse /= (width * height * 3.0);
        
        if (mse == 0) return Double.POSITIVE_INFINITY;
        
        double maxPixel = 255.0;
        return 20 * Math.log10(maxPixel / Math.sqrt(mse));
    }
}
