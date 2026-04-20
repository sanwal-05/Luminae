package com.hiddeninpixel;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import com.hiddeninpixel.db.DatabaseConnection;

public class HiddenInPixelApp extends Application {
    
    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/main.fxml"));
        Parent root = loader.load();
        
        Scene scene = new Scene(root, 1200, 800);
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        
        primaryStage.setTitle("Hidden-In-Pixel | Steganography Suite");
        primaryStage.setScene(scene);
        primaryStage.setOnCloseRequest(e -> DatabaseConnection.getInstance().close());
        primaryStage.show();
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}
