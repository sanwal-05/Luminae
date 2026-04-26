package com.hiddeninpixel.ui.components;

import javafx.scene.image.Image;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import java.awt.image.BufferedImage;

public class DifferenceMapGenerator {
    
    public static Image generate(BufferedImage cover, BufferedImage stego) {
        int width = cover.getWidth();
        int height = cover.getHeight();
        
        WritableImage diffImage = new WritableImage(width, height);
        PixelWriter writer = diffImage.getPixelWriter();
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int coverRGB = cover.getRGB(x, y);
                int stegoRGB = stego.getRGB(x, y);
                
                if (coverRGB != stegoRGB) {
                    writer.setColor(x, y, Color.RED);
                } else {
                    writer.setColor(x, y, Color.BLACK);
                }
            }
        }
        
        return diffImage;
    }
}
