package com.hiddeninpixel.ui.controllers;

import com.hiddeninpixel.db.AnalyticsDAO;
import com.hiddeninpixel.model.AnalyticsRecord;
import com.hiddeninpixel.service.ImageProcessor;
import com.hiddeninpixel.ui.components.DifferenceMapGenerator;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AnalyticsController {

    @FXML private TableView<AnalyticsRecord> analyticsTable;
    @FXML private TableColumn<AnalyticsRecord, Integer> idColumn;
    @FXML private TableColumn<AnalyticsRecord, String> algorithmColumn;
    @FXML private TableColumn<AnalyticsRecord, String> resolutionColumn;
    @FXML private TableColumn<AnalyticsRecord, Double> sizeColumn;
    @FXML private TableColumn<AnalyticsRecord, Double> ratioColumn;
    @FXML private TableColumn<AnalyticsRecord, String> timestampColumn;

    @FXML private PieChart algorithmUsageChart;
    @FXML private BarChart<String, Number> metricsChart;

    // --- FORENSICS CONTROLS ---
    @FXML private StackPane coverForensicDropZone;
    @FXML private StackPane stegoForensicDropZone;
    @FXML private ImageView coverForensicView;
    @FXML private ImageView stegoForensicView;
    @FXML private ImageView diffMapView;

    private final AnalyticsDAO dao = new AnalyticsDAO();
    private final ImageProcessor imageProcessor = new ImageProcessor();
    private File coverFile;
    private File stegoFile;

    @FXML
    public void initialize() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        algorithmColumn.setCellValueFactory(new PropertyValueFactory<>("algorithmName"));
        resolutionColumn.setCellValueFactory(new PropertyValueFactory<>("imageResolution"));
        sizeColumn.setCellValueFactory(new PropertyValueFactory<>("payloadSizeKB"));
        ratioColumn.setCellValueFactory(new PropertyValueFactory<>("changeRatio"));
        timestampColumn.setCellValueFactory(new PropertyValueFactory<>("timestamp"));

        refreshData();
        setupForensicDropZones();
    }

    private void setupForensicDropZones() {
        setupDropZone(coverForensicDropZone, true);
        setupDropZone(stegoForensicDropZone, false);
    }

    private void setupDropZone(StackPane zone, boolean isCover) {
        if (zone == null) return;
        zone.setOnDragOver(event -> {
            if (event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY);
            }
            event.consume();
        });
        zone.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            if (db.hasFiles()) {
                File file = db.getFiles().get(0);
                if (isCover) loadCover(file); else loadStego(file);
            }
            event.setDropCompleted(true);
            event.consume();
        });
    }

    @FXML
    public void handleLoadCoverForensic() {
        File file = showFileChooser();
        if (file != null) loadCover(file);
    }

    @FXML
    public void handleLoadStegoForensic() {
        File file = showFileChooser();
        if (file != null) loadStego(file);
    }

    private void loadCover(File file) {
        this.coverFile = file;
        coverForensicView.setImage(new Image(file.toURI().toString()));
        coverForensicView.setVisible(true);
        coverForensicDropZone.getChildren().get(0).setVisible(false);
    }

    private void loadStego(File file) {
        this.stegoFile = file;
        stegoForensicView.setImage(new Image(file.toURI().toString()));
        stegoForensicView.setVisible(true);
        stegoForensicDropZone.getChildren().get(0).setVisible(false);
    }

    private File showFileChooser() {
        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg"));
        return chooser.showOpenDialog(analyticsTable.getScene().getWindow());
    }

    @FXML
    public void handleGenerateDiffMap() {
        if (coverFile == null || stegoFile == null) return;
        try {
            BufferedImage cover = imageProcessor.loadImage(coverFile);
            BufferedImage stego = imageProcessor.loadImage(stegoFile);
            Image diffMap = DifferenceMapGenerator.generate(cover, stego);
            diffMapView.setImage(diffMap);
            diffMapView.setVisible(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void refreshData() {
        List<AnalyticsRecord> records = dao.getAll();
        ObservableList<AnalyticsRecord> data = FXCollections.observableArrayList(records);
        analyticsTable.setItems(data);

        updateCharts(records);
    }

    private void updateCharts(List<AnalyticsRecord> records) {
        // Aggregators for Charts
        Map<String, Integer> usageCount = new HashMap<>();
        Map<String, Double> totalPayload = new HashMap<>();

        for (AnalyticsRecord record : records) {
            String algo = record.getAlgorithmName();
            usageCount.put(algo, usageCount.getOrDefault(algo, 0) + 1);
            totalPayload.put(algo, totalPayload.getOrDefault(algo, 0.0) + record.getPayloadSizeKB());
        }

        // Populate Pie Chart
        ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList();
        for (Map.Entry<String, Integer> entry : usageCount.entrySet()) {
            pieChartData.add(new PieChart.Data(entry.getKey() + " (" + entry.getValue() + ")", entry.getValue()));
        }
        algorithmUsageChart.setData(pieChartData);

        // Populate Bar Chart
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Average KB Embedded");
        for (Map.Entry<String, Double> entry : totalPayload.entrySet()) {
            int count = usageCount.get(entry.getKey());
            double average = entry.getValue() / count;
            series.getData().add(new XYChart.Data<>(entry.getKey(), average));
        }
        
        metricsChart.getData().clear();
        metricsChart.getData().add(series);
    }
}
