package com.hiddeninpixel.ui.controllers;

import com.hiddeninpixel.core.*;
import com.hiddeninpixel.service.*;
import com.hiddeninpixel.exception.*;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import java.io.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

public class MainController {
    
    @FXML private ComboBox<String> algorithmComboBox;
    @FXML private ImageView coverImageView;
    @FXML private ImageView stegoImageView;
    @FXML private TextArea messageTextArea;
    @FXML private PasswordField keyPasswordField;
    @FXML private Label statusLabel;
    @FXML private ProgressBar progressBar;
    
    private final ImageProcessor imageProcessor = new ImageProcessor();
    private final AnalyticsService analyticsService = new AnalyticsService();
    private BufferedImage coverImage;
    private BufferedImage stegoImage;
    
    @FXML
    public void initialize() {
        algorithmComboBox.getItems().addAll(
            "Sequential LSB", "XOR-LSB", "Stochastic LSB", "Metadata Channel"
        );
        algorithmComboBox.setValue("Sequential LSB");
    }
    
    @FXML
    private void handleLoadImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Cover Image");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("PNG Images", "*.png")
        );
        
        File file = fileChooser.showOpenDialog(null);
        if (file != null) {
            try {
                coverImage = imageProcessor.loadImage(file);
                coverImageView.setImage(convertToFXImage(coverImage));
                statusLabel.setText("Image loaded: " + file.getName());
            } catch (StegoException e) {
                showError(e.getMessage());
            }
        }
    }
    
    @FXML
    private void handleHide() {
        if (coverImage == null) {
            showError("Please load a cover image first");
            return;
        }
        
        String message = messageTextArea.getText();
        if (message.isEmpty()) {
            showError("Please enter a message to hide");
            return;
        }
        
        String key = keyPasswordField.getText();
        String algoName = algorithmComboBox.getValue();
        
        try {
            StegoAlgorithm algorithm = getAlgorithm(algoName);
            byte[] payload = message.getBytes("UTF-8");
            
            stegoImage = algorithm.hide(coverImage, payload, key);
            stegoImageView.setImage(convertToFXImage(stegoImage));
            
            analyticsService.logOperation(algoName, coverImage, stegoImage, payload.length);
            
            statusLabel.setText("Message hidden successfully using " + algoName);
            
        } catch (StegoException | UnsupportedEncodingException e) {
            showError(e.getMessage());
        }
    }
    
    @FXML
    private void handleExtract() {
        if (stegoImage == null) {
            showError("No stego image available");
            return;
        }
        
        String key = keyPasswordField.getText();
        String algoName = algorithmComboBox.getValue();
        
        try {
            StegoAlgorithm algorithm = getAlgorithm(algoName);
            byte[] extracted = algorithm.extract(stegoImage, key);
            
            messageTextArea.setText(new String(extracted, "UTF-8"));
            statusLabel.setText("Message extracted successfully");
            
        } catch (StegoException | UnsupportedEncodingException e) {
            showError(e.getMessage());
        }
    }
    
    @FXML
    private void handleSaveStego() {
        if (stegoImage == null) {
            showError("No stego image to save");
            return;
        }
        
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Stego Image");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("PNG Images", "*.png")
        );
        
        File file = fileChooser.showSaveDialog(null);
        if (file != null) {
            try {
                imageProcessor.saveImage(stegoImage, file);
                statusLabel.setText("Stego image saved: " + file.getName());
            } catch (StegoException e) {
                showError(e.getMessage());
            }
        }
    }
    
    private StegoAlgorithm getAlgorithm(String name) {
        return switch (name) {
            case "Sequential LSB" -> new SequentialLSB();
            case "XOR-LSB" -> new XorLSB();
            case "Stochastic LSB" -> new StochasticLSB();
            case "Metadata Channel" -> new MetadataChannel();
            default -> new SequentialLSB();
        };
    }
    
    private Image convertToFXImage(BufferedImage bImage) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(bImage, "png", baos);
            return new Image(new ByteArrayInputStream(baos.toByteArray()));
        } catch (IOException e) {
            return null;
        }
    }
    
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
