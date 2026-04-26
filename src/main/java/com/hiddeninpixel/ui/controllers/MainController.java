package com.hiddeninpixel.ui.controllers;

import com.hiddeninpixel.core.*;
import com.hiddeninpixel.service.ImageProcessor;
import com.hiddeninpixel.service.AnalyticsService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class MainController {

    // --- ENCODE TAB CONTROLS ---
    @FXML private StackPane encodeDropZone;
    @FXML private ImageView encodeImageView;
    @FXML private Button clearEncodeBtn;
    @FXML private ComboBox<String> encodeAlgorithmCombo;
    @FXML private PasswordField encodePasswordField;
    @FXML private TextArea encodeMessageArea;
    @FXML private ProgressBar encodeProgressBar;
    @FXML private Label encodeStatusLabel;

    // --- DECODE TAB CONTROLS ---
    @FXML private StackPane decodeDropZone;
    @FXML private ImageView decodeImageView;
    @FXML private Button clearDecodeBtn;
    @FXML private ComboBox<String> decodeAlgorithmCombo;
    @FXML private PasswordField decodePasswordField;
    @FXML private TextArea decodeMessageArea;
    @FXML private ProgressBar decodeProgressBar;
    @FXML private Label decodeStatusLabel;

    private final Map<String, StegoAlgorithm> algorithms = new HashMap<>();
    private final ImageProcessor imageProcessor = new ImageProcessor();
    private final AnalyticsService analyticsService = new AnalyticsService();

    private File currentEncodeFile;
    private File currentDecodeFile;

    @FXML
    public void initialize() {
        // Initialize Algorithms
        algorithms.put("Sequential LSB", new SequentialLSB());
        algorithms.put("XOR-LSB (Secure)", new XorLSB());
        algorithms.put("Stochastic LSB", new StochasticLSB());
        algorithms.put("Metadata (EOF Append)", new MetadataChannel());

        // Setup ComboBoxes
        encodeAlgorithmCombo.setItems(FXCollections.observableArrayList(algorithms.keySet()));
        encodeAlgorithmCombo.getSelectionModel().selectFirst();
        
        decodeAlgorithmCombo.setItems(FXCollections.observableArrayList(algorithms.keySet()));
        decodeAlgorithmCombo.getSelectionModel().selectFirst();

        // Setup Drag and Drop
        setupDragAndDrop(encodeDropZone, true);
        setupDragAndDrop(decodeDropZone, false);
    }

    private void setupDragAndDrop(StackPane dropZone, boolean isEncode) {
        dropZone.setOnDragOver(event -> {
            Dragboard db = event.getDragboard();
            if (db.hasFiles() && isImageFile(db.getFiles().get(0))) {
                event.acceptTransferModes(TransferMode.COPY);
            }
            event.consume();
        });

        dropZone.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.hasFiles() && isImageFile(db.getFiles().get(0))) {
                File file = db.getFiles().get(0);
                if (isEncode) {
                    loadEncodeImage(file);
                } else {
                    loadDecodeImage(file);
                }
                success = true;
            }
            event.setDropCompleted(success);
            event.consume();
        });
    }

    private boolean isImageFile(File file) {
        String name = file.getName().toLowerCase();
        return name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg");
    }

    // --- ENCODE LOGIC ---

    @FXML
    public void handleLoadEncodeImage() {
        FileChooser fileChooser = getStandardImageFileChooser();
        File file = fileChooser.showOpenDialog(encodeDropZone.getScene().getWindow());
        if (file != null) loadEncodeImage(file);
    }

    private void loadEncodeImage(File file) {
        currentEncodeFile = file;
        encodeImageView.setImage(new Image(file.toURI().toString()));
        encodeImageView.setVisible(true);
        clearEncodeBtn.setVisible(true);
        encodeStatusLabel.setText("Loaded: " + file.getName());
        encodeDropZone.getChildren().get(0).setVisible(false); // Hide instructional VBox
    }

    @FXML
    public void handleClearEncodeImage() {
        currentEncodeFile = null;
        encodeImageView.setImage(null);
        encodeImageView.setVisible(false);
        clearEncodeBtn.setVisible(false);
        encodeMessageArea.clear();
        encodePasswordField.clear();
        encodeStatusLabel.setText("Ready to Encode");
        encodeProgressBar.setProgress(0);
        encodeDropZone.getChildren().get(0).setVisible(true); // Show instructional VBox
    }

    @FXML
    public void handleEncode() {
        if (currentEncodeFile == null) {
            showAlert("Error", "Please load a cover image first.");
            return;
        }

        String message = encodeMessageArea.getText();
        if (message == null || message.isEmpty()) {
            showAlert("Error", "Please enter a secret message.");
            return;
        }

        String algoName = encodeAlgorithmCombo.getValue();
        StegoAlgorithm algorithm = algorithms.get(algoName);
        String key = encodePasswordField.getText();

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Stego Image");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG Files", "*.png"));
        File outputFile = fileChooser.showSaveDialog(encodeDropZone.getScene().getWindow());

        if (outputFile == null) return;

        encodeProgressBar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
        encodeStatusLabel.setText("Encoding in progress...");

        CompletableFuture.runAsync(() -> {
            try {
                if (algorithm instanceof MetadataChannel) {
                    ((MetadataChannel) algorithm).embedIntoFile(currentEncodeFile, message.getBytes("UTF-8"), outputFile);
                    // Standard log
                    analyticsService.logOperation(algoName, null, null, message.length());
                } else {
                    BufferedImage cover = imageProcessor.loadImage(currentEncodeFile);
                    byte[] payload = message.getBytes("UTF-8");
                    BufferedImage stego = algorithm.hide(cover, payload, key);
                    imageProcessor.saveImage(stego, outputFile);
                    analyticsService.logOperation(algoName, cover, stego, payload.length);
                }

                Platform.runLater(() -> {
                    encodeProgressBar.setProgress(1.0);
                    encodeStatusLabel.setText("Success! Saved to " + outputFile.getName());
                    showAlert("Success", "Message hidden successfully!");
                    handleClearEncodeImage(); // Reset UI state after success
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    encodeProgressBar.setProgress(0);
                    encodeStatusLabel.setText("Error: " + e.getMessage());
                    showAlert("Encoding Error", e.getMessage());
                });
            }
        });
    }

    // --- DECODE LOGIC ---

    @FXML
    public void handleLoadDecodeImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG Files", "*.png"));
        File file = fileChooser.showOpenDialog(decodeDropZone.getScene().getWindow());
        if (file != null) loadDecodeImage(file);
    }

    private void loadDecodeImage(File file) {
        currentDecodeFile = file;
        decodeImageView.setImage(new Image(file.toURI().toString()));
        decodeImageView.setVisible(true);
        clearDecodeBtn.setVisible(true);
        decodeStatusLabel.setText("Loaded: " + file.getName());
        decodeDropZone.getChildren().get(0).setVisible(false);
    }

    @FXML
    public void handleClearDecodeImage() {
        currentDecodeFile = null;
        decodeImageView.setImage(null);
        decodeImageView.setVisible(false);
        clearDecodeBtn.setVisible(false);
        decodeMessageArea.clear();
        decodePasswordField.clear();
        decodeStatusLabel.setText("Ready to Decode");
        decodeProgressBar.setProgress(0);
        decodeDropZone.getChildren().get(0).setVisible(true);
    }

    @FXML
    public void handleDecode() {
        if (currentDecodeFile == null) {
            showAlert("Error", "Please load a stego image first.");
            return;
        }

        String algoName = decodeAlgorithmCombo.getValue();
        StegoAlgorithm algorithm = algorithms.get(algoName);
        String key = decodePasswordField.getText();

        decodeProgressBar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
        decodeStatusLabel.setText("Decoding in progress...");

        CompletableFuture.runAsync(() -> {
            try {
                byte[] extracted;
                if (algorithm instanceof MetadataChannel) {
                    extracted = ((MetadataChannel) algorithm).extractFromFile(currentDecodeFile);
                } else {
                    BufferedImage stego = imageProcessor.loadImage(currentDecodeFile);
                    extracted = algorithm.extract(stego, key);
                }

                String message = new String(extracted, "UTF-8");

                Platform.runLater(() -> {
                    decodeProgressBar.setProgress(1.0);
                    decodeStatusLabel.setText("Decoding successful.");
                    decodeMessageArea.setText(message);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    decodeProgressBar.setProgress(0);
                    decodeStatusLabel.setText("Error: " + e.getMessage());
                    showAlert("Decoding Error", "Failed to extract (Wrong Password or File Corrupted).");
                });
            }
        });
    }

    private FileChooser getStandardImageFileChooser() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg")
        );
        return fileChooser;
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
