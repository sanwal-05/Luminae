// This is the main entry point for the Luminae app.
// Every Java application needs one class with a "main" method, this is it.
// This class extends "Application" which is JavaFX's way of saying "this is a desktop app".
package com.hiddeninpixel;

// JavaFX imports needed to create the window and load the UI layout from the FXML file.
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

// We need the database connection here so we can close it properly when the app exits.
import com.hiddeninpixel.db.DatabaseConnection;

public class HiddenInPixelApp extends Application {

    // JavaFX calls this "start" method automatically once the app launches.
    // "primaryStage" is the main window of the app, like the frame of a painting.
    @Override
    public void start(Stage primaryStage) throws Exception {

        // FXMLLoader reads our main.fxml file which describes the layout of all the buttons,
        // tabs, and panels. It's like reading a blueprint before building a house.
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/main.fxml"));

        // "root" is the top-level container that holds the entire UI layout.
        Parent root = loader.load();

        // A Scene is the canvas that sits inside the Stage (window).
        // We set the window size here: 1200 pixels wide and 800 pixels tall.
        Scene scene = new Scene(root, 1200, 800);

        // Load our CSS stylesheet so the app looks dark and styled, not plain grey.
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());

        // Set the title that appears in the top bar of the window.
        primaryStage.setTitle("Luminae | Steganography Suite");

        // Attach the scene (all our UI) to the stage (the window frame).
        primaryStage.setScene(scene);

        // When the user clicks the red X to close the window, we close the database
        // connection cleanly so no data is lost or corrupted.
        primaryStage.setOnCloseRequest(e -> DatabaseConnection.getInstance().close());

        // Finally, make the window visible on screen.
        primaryStage.show();
    }

    // The "main" method is the very first thing Java runs.
    // "launch" is a JavaFX method that sets up the environment and then calls "start" above.
    public static void main(String[] args) {
        launch(args);
    }
}
