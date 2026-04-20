package com.hiddeninpixel.ui.controllers;

import com.hiddeninpixel.db.AnalyticsDAO;
import com.hiddeninpixel.model.AnalyticsRecord;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

public class AnalyticsController {
    
    @FXML private TableView<AnalyticsRecord> analyticsTable;
    @FXML private TableColumn<AnalyticsRecord, Integer> idColumn;
    @FXML private TableColumn<AnalyticsRecord, String> algorithmColumn;
    @FXML private TableColumn<AnalyticsRecord, String> resolutionColumn;
    @FXML private TableColumn<AnalyticsRecord, Double> sizeColumn;
    @FXML private TableColumn<AnalyticsRecord, Double> ratioColumn;
    @FXML private TableColumn<AnalyticsRecord, Long> timestampColumn;
    
    private final AnalyticsDAO dao = new AnalyticsDAO();
    
    @FXML
    public void initialize() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        algorithmColumn.setCellValueFactory(new PropertyValueFactory<>("algorithmName"));
        resolutionColumn.setCellValueFactory(new PropertyValueFactory<>("imageResolution"));
        sizeColumn.setCellValueFactory(new PropertyValueFactory<>("payloadSizeKB"));
        ratioColumn.setCellValueFactory(new PropertyValueFactory<>("changeRatio"));
        timestampColumn.setCellValueFactory(new PropertyValueFactory<>("timestamp"));
        
        refreshData();
    }
    
    @FXML
    public void refreshData() {
        ObservableList<AnalyticsRecord> data = FXCollections.observableArrayList(dao.getAll());
        analyticsTable.setItems(data);
    }
}
