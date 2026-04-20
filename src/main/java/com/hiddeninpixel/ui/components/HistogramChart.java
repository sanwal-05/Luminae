package com.hiddeninpixel.ui.components;

import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import java.awt.image.BufferedImage;

public class HistogramChart {
    
    public static BarChart<String, Number> generate(BufferedImage image, String title) {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Intensity");
        yAxis.setLabel("Frequency");
        
        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setTitle(title);
        chart.setLegendVisible(false);
        
        int[] histogram = new int[256];
        
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int rgb = image.getRGB(x, y);
                int red = (rgb >> 16) & 0xFF;
                histogram[red]++;
            }
        }
        
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        
        // Bin into 32 groups for readability
        for (int i = 0; i < 256; i += 8) {
            int sum = 0;
            for (int j = 0; j < 8 && (i + j) < 256; j++) {
                sum += histogram[i + j];
            }
            series.getData().add(new XYChart.Data<>(String.valueOf(i), sum));
        }
        
        chart.getData().add(series);
        return chart;
    }
}
