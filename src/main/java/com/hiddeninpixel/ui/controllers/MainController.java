// MainController is the largest and most important controller in the app.
// It manages the Encode tab and the Decode tab of the main window.
// "Controller" means: it listens to what the user does (clicks, typing) and
// responds by calling the right algorithm, updating labels, and showing errors.
//
// JavaFX connects this class to main.fxml automatically because of the
// fx:controller="...MainController" attribute in that file.
package com.hiddeninpixel.ui.controllers;

// Import all steganography algorithm classes from the core package.
import com.hiddeninpixel.core.*;

// Service classes that handle image I/O and analytics logging.
import com.hiddeninpixel.service.ImageProcessor;
import com.hiddeninpixel.service.AnalyticsService;

// JavaFX imports for threading, observable collections, annotations, and UI components.
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

// Java standard library imports.
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class MainController {

    // --- ENCODE TAB UI ELEMENTS ---
    // These are all linked to fx:id attributes in main.fxml. JavaFX fills them in automatically.

    @FXML private StackPane encodeDropZone;         // the drag-and-drop area where the user drops an image
    @FXML private ImageView encodeImageView;        // shows a preview of the loaded image
    @FXML private Button clearEncodeBtn;            // clears the loaded image and resets the form
    @FXML private ComboBox<String> encodeAlgorithmCombo; // dropdown to choose the hiding algorithm
    @FXML private PasswordField encodePasswordField;     // text box for the password (shows dots)
    @FXML private TextArea encodeMessageArea;       // where the user types the secret message
    @FXML private ProgressBar encodeProgressBar;   // animated bar that shows encoding progress
    @FXML private Label encodeStatusLabel;          // text below the progress bar describing current state
    @FXML private Label encodeCharCountLabel;       // shows "Characters: X / Y" live as user types
    @FXML private Label encodeWarningLabel;         // red warning shown when message is too long
    @FXML private Button encodeBtn;                 // the main "Hide Message & Save" button

    // --- DECODE TAB UI ELEMENTS ---
    @FXML private StackPane decodeDropZone;          // drag-and-drop area for the stego image
    @FXML private ImageView decodeImageView;         // preview of the stego image
    @FXML private Button clearDecodeBtn;             // clears the decode form
    @FXML private ComboBox<String> decodeAlgorithmCombo; // dropdown to choose which algorithm to use when extracting
    @FXML private PasswordField decodePasswordField;      // password field for decoding
    @FXML private TextArea decodeMessageArea;        // shows the extracted secret message
    @FXML private ProgressBar decodeProgressBar;    // progress bar for decoding
    @FXML private Label decodeStatusLabel;           // status text for decoding

    // A Map is like a dictionary. The key is the name shown in the dropdown.
    // The value is the actual algorithm object. This lets us look up any algorithm by name.
    private final Map<String, StegoAlgorithm> algorithms = new HashMap<>();

    // Handles reading PNG files from disk and saving PNG files to disk.
    private final ImageProcessor imageProcessor = new ImageProcessor();

    // Logs each operation to the database after it completes.
    private final AnalyticsService analyticsService = new AnalyticsService();

    // These remember which files the user has loaded, so we can process them later.
    private File currentEncodeFile;
    private File currentDecodeFile;

    // The in-memory image object for the encode tab, used to calculate capacity.
    private BufferedImage currentCoverImage;

    // JavaFX calls "initialize" automatically after loading main.fxml.
    // This is where we wire everything up.
    @FXML
    public void initialize() {
        // Register all four algorithms with human-readable names.
        // When the user picks "Sequential LSB" in the dropdown, algorithms.get("Sequential LSB")
        // gives us the SequentialLSB object to call.
        algorithms.put("Sequential LSB", new SequentialLSB());
        algorithms.put("XOR-LSB (Secure)", new XorLSB());
        algorithms.put("Stochastic LSB", new StochasticLSB());
        algorithms.put("Metadata (EOF Append)", new MetadataChannel());

        // Fill the encode tab's algorithm dropdown with all algorithm names.
        encodeAlgorithmCombo.setItems(FXCollections.observableArrayList(algorithms.keySet()));
        // Select the first item by default so the dropdown is never empty.
        encodeAlgorithmCombo.getSelectionModel().selectFirst();

        // Do the same for the decode tab's dropdown.
        decodeAlgorithmCombo.setItems(FXCollections.observableArrayList(algorithms.keySet()));
        decodeAlgorithmCombo.getSelectionModel().selectFirst();

        // Attach drag-and-drop behavior to both drop zones.
        // "true" means it is the encode drop zone; "false" means decode.
        setupDragAndDrop(encodeDropZone, true);
        setupDragAndDrop(decodeDropZone, false);

        // Listen for changes to the message text area.
        // Every time the user types or deletes a character, update the "Characters: X / Y" label.
        encodeMessageArea.textProperty().addListener((observable, oldValue, newValue) -> {
            updateCapacityDisplay();
        });

        // Also update the capacity label when the user changes the algorithm dropdown.
        // Different algorithms have slightly different capacities.
        encodeAlgorithmCombo.valueProperty().addListener((observable, oldValue, newValue) -> {
            updateCapacityDisplay();
        });

        // Run the capacity display update once at startup to initialize the label.
        updateCapacityDisplay();
    }

    // Sets up drag-and-drop behavior for a drop zone panel.
    // "isEncode" tells us whether this is the encode tab (true) or decode tab (false).
    private void setupDragAndDrop(StackPane dropZone, boolean isEncode) {
        // "setOnDragOver" fires continuously while the user is dragging something over the zone.
        dropZone.setOnDragOver(event -> {
            Dragboard db = event.getDragboard();
            // Only accept the drag if the dragged item is an image file we can use.
            if (db.hasFiles() && isImageFile(db.getFiles().get(0))) {
                // Accept a COPY transfer (we are reading the file, not moving it).
                event.acceptTransferModes(TransferMode.COPY);
            }
            // "consume" means we handled this event and no parent element should also handle it.
            event.consume();
        });

        // "setOnDragDropped" fires once when the user releases the mouse and drops the file.
        dropZone.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.hasFiles() && isImageFile(db.getFiles().get(0))) {
                File file = db.getFiles().get(0);
                // Load the file into the correct tab based on which drop zone received it.
                if (isEncode) {
                    loadEncodeImage(file);
                } else {
                    loadDecodeImage(file);
                }
                success = true;
            }
            // Tell the drag-drop system whether we successfully handled the drop.
            event.setDropCompleted(success);
            event.consume();
        });
    }

    // Returns true if the given file is an image format we support.
    private boolean isImageFile(File file) {
        String name = file.getName().toLowerCase();
        return name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg");
    }

    // --- ENCODE TAB LOGIC ---

    // Called when the user clicks the "Browse File" button on the encode tab.
    @FXML
    public void handleLoadEncodeImage() {
        // Open a file picker dialog filtered to image files.
        FileChooser fileChooser = getStandardImageFileChooser();
        File file = fileChooser.showOpenDialog(encodeDropZone.getScene().getWindow());
        // If the user picked a file (and did not cancel), load it.
        if (file != null) loadEncodeImage(file);
    }

    // Updates the "Characters: X / Y" label below the message text area.
    // Also shows or hides the warning label and enables or disables the Encode button.
    private void updateCapacityDisplay() {
        // If no image is loaded yet, reset to defaults and return early.
        if (currentCoverImage == null) {
            if (encodeCharCountLabel != null) encodeCharCountLabel.setText("Characters: 0 / 0");
            if (encodeWarningLabel != null) {
                encodeWarningLabel.setVisible(false);
                encodeWarningLabel.setManaged(false);
            }
            if (encodeBtn != null) encodeBtn.setDisable(false);
            return;
        }

        // Look up which algorithm is currently selected.
        String algoName = encodeAlgorithmCombo.getValue();
        StegoAlgorithm algorithm = algorithms.get(algoName);

        // Ask the algorithm how many bytes this specific image can hold.
        int maxCapacity = 0;
        if (algorithm != null) {
            maxCapacity = algorithm.getCapacity(currentCoverImage);
        }

        // How many characters has the user typed so far?
        int currentLength = encodeMessageArea.getText() != null ? encodeMessageArea.getText().length() : 0;

        // Update the label to show current vs. maximum.
        if (encodeCharCountLabel != null) {
            encodeCharCountLabel.setText("Characters: " + currentLength + " / " + maxCapacity);
        }

        // If the message is too long, show the warning and disable the encode button.
        if (currentLength > maxCapacity && maxCapacity > 0) {
            if (encodeWarningLabel != null) {
                encodeWarningLabel.setVisible(true);
                encodeWarningLabel.setManaged(true);
            }
            if (encodeBtn != null) encodeBtn.setDisable(true);
        } else {
            // Message fits, so hide the warning and re-enable the button.
            if (encodeWarningLabel != null) {
                encodeWarningLabel.setVisible(false);
                encodeWarningLabel.setManaged(false);
            }
            if (encodeBtn != null) encodeBtn.setDisable(false);
        }
    }

    // Loads an image file for the encode tab. Updates the UI to show the image preview.
    private void loadEncodeImage(File file) {
        currentEncodeFile = file;
        // Also load the image into memory so we can calculate capacity.
        try {
            currentCoverImage = imageProcessor.loadImage(file);
        } catch (Exception e) {
            currentCoverImage = null;
        }
        // Show the image preview in the ImageView.
        encodeImageView.setImage(new Image(file.toURI().toString()));
        encodeImageView.setVisible(true);
        // Show the clear button so the user can reset.
        clearEncodeBtn.setVisible(true);
        // Update the status label with the filename.
        encodeStatusLabel.setText("Loaded: " + file.getName());
        // Hide the instruction text (the "Drag & Drop..." VBox is the first child).
        encodeDropZone.getChildren().get(0).setVisible(false);
        // Refresh the capacity display now that we have an image.
        updateCapacityDisplay();
    }

    // Resets the encode tab back to its empty starting state.
    @FXML
    public void handleClearEncodeImage() {
        currentEncodeFile = null;
        currentCoverImage = null;
        encodeImageView.setImage(null);
        encodeImageView.setVisible(false);
        clearEncodeBtn.setVisible(false);
        encodeMessageArea.clear();
        encodePasswordField.clear();
        encodeStatusLabel.setText("Ready to Encode");
        encodeProgressBar.setProgress(0);
        // Show the instruction text again.
        encodeDropZone.getChildren().get(0).setVisible(true);
        updateCapacityDisplay();
    }

    // Called when the user clicks "Hide Message & Save".
    // Validates input, opens a save dialog, then runs the encoding on a background thread.
    @FXML
    public void handleEncode() {
        // Make sure an image is loaded before proceeding.
        if (currentEncodeFile == null) {
            showAlert("Error", "Please load a cover image first.");
            return;
        }

        // Make sure the user has actually typed something to hide.
        String message = encodeMessageArea.getText();
        if (message == null || message.isEmpty()) {
            showAlert("Error", "Please enter a secret message.");
            return;
        }

        // Get the selected algorithm object and the password.
        String algoName = encodeAlgorithmCombo.getValue();
        StegoAlgorithm algorithm = algorithms.get(algoName);
        String key = encodePasswordField.getText();

        // Open a "Save File" dialog so the user can choose where to save the output.
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Stego Image");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG Files", "*.png"));
        File outputFile = fileChooser.showSaveDialog(encodeDropZone.getScene().getWindow());

        // If the user cancelled the save dialog, do nothing.
        if (outputFile == null) return;

        // Show an indeterminate (spinning) progress bar while we work.
        encodeProgressBar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
        encodeStatusLabel.setText("Encoding in progress...");

        // Run the encoding on a BACKGROUND THREAD using CompletableFuture.
        // This is very important: image processing can be slow.
        // If we ran it on the main thread, the window would freeze and become unresponsive.
        // CompletableFuture.runAsync puts the work on a separate thread automatically.
        CompletableFuture.runAsync(() -> {
            try {
                if (algorithm instanceof MetadataChannel) {
                    // MetadataChannel works with files directly, not BufferedImage objects.
                    ((MetadataChannel) algorithm).embedIntoFile(currentEncodeFile, message.getBytes("UTF-8"), outputFile);
                    // Log the operation. No before/after images for metadata approach.
                    analyticsService.logOperation(algoName, null, null, message.length());
                } else {
                    // For all other pixel-based algorithms:
                    BufferedImage cover = imageProcessor.loadImage(currentEncodeFile);
                    // Convert the message string to bytes using UTF-8 encoding.
                    byte[] payload = message.getBytes("UTF-8");
                    // Run the hiding algorithm.
                    BufferedImage stego = algorithm.hide(cover, payload, key);
                    // Save the stego image to the file the user chose.
                    imageProcessor.saveImage(stego, outputFile);
                    // Log the operation with both images so change ratio can be calculated.
                    analyticsService.logOperation(algoName, cover, stego, payload.length);
                }

                // "Platform.runLater" is required to update the UI from a background thread.
                // JavaFX UI can only be updated from the main (JavaFX Application) thread.
                Platform.runLater(() -> {
                    encodeProgressBar.setProgress(1.0);
                    encodeStatusLabel.setText("Success! Saved to " + outputFile.getName());
                    showAlert("Success", "Message hidden successfully!");
                    // Reset the form so the user can do another operation.
                    handleClearEncodeImage();
                });

            } catch (Exception e) {
                // If anything went wrong, update the UI with an error message.
                Platform.runLater(() -> {
                    encodeProgressBar.setProgress(0);
                    encodeStatusLabel.setText("Error: " + e.getMessage());
                    showAlert("Encoding Error", e.getMessage());
                });
            }
        });
    }

    // --- DECODE TAB LOGIC ---

    // Called when the user clicks "Browse File" on the decode tab.
    @FXML
    public void handleLoadDecodeImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG Files", "*.png"));
        File file = fileChooser.showOpenDialog(decodeDropZone.getScene().getWindow());
        if (file != null) loadDecodeImage(file);
    }

    // Loads an image into the decode tab and shows its preview.
    private void loadDecodeImage(File file) {
        currentDecodeFile = file;
        decodeImageView.setImage(new Image(file.toURI().toString()));
        decodeImageView.setVisible(true);
        clearDecodeBtn.setVisible(true);
        decodeStatusLabel.setText("Loaded: " + file.getName());
        // Hide the instruction text.
        decodeDropZone.getChildren().get(0).setVisible(false);
    }

    // Resets the decode tab to its empty starting state.
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
        // Show the instruction text again.
        decodeDropZone.getChildren().get(0).setVisible(true);
    }

    // Called when the user clicks "Extract Message".
    // Runs decoding on a background thread just like encoding.
    @FXML
    public void handleDecode() {
        // Cannot decode without an image.
        if (currentDecodeFile == null) {
            showAlert("Error", "Please load a stego image first.");
            return;
        }

        String algoName = decodeAlgorithmCombo.getValue();
        StegoAlgorithm algorithm = algorithms.get(algoName);
        String key = decodePasswordField.getText();

        decodeProgressBar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
        decodeStatusLabel.setText("Decoding in progress...");

        // Run extraction on a background thread.
        CompletableFuture.runAsync(() -> {
            try {
                byte[] extracted;

                if (algorithm instanceof MetadataChannel) {
                    // MetadataChannel reads from the file directly.
                    extracted = ((MetadataChannel) algorithm).extractFromFile(currentDecodeFile);
                } else {
                    // All other algorithms work on the in-memory pixel data.
                    BufferedImage stego = imageProcessor.loadImage(currentDecodeFile);
                    extracted = algorithm.extract(stego, key);
                }

                // Convert the extracted bytes back into a human-readable string.
                String message = new String(extracted, "UTF-8");

                // Update the UI on the main thread.
                Platform.runLater(() -> {
                    decodeProgressBar.setProgress(1.0);
                    decodeStatusLabel.setText("Decoding successful.");
                    // Display the recovered message in the text area.
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

    // Creates a standard FileChooser that filters for common image formats.
    private FileChooser getStandardImageFileChooser() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg")
        );
        return fileChooser;
    }

    // Shows a simple pop-up dialog with a title and a message.
    // Used for both success notifications and error messages.
    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);  // null removes the secondary header so only the content shows
        alert.setContentText(content);
        alert.showAndWait();        // blocks until the user clicks OK
    }
}
