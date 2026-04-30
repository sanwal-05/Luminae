// AnalyticsController manages the "Analytics & Forensics" tab.
// Its job is to fetch all past operations from the database and display them
// in a table, a pie chart (which algorithm was used most), and a bar chart
// (average payload size per algorithm).
//
// It also adds a Delete button to each row, and an editable Details column
// so the user can write notes about any operation.
package com.hiddeninpixel.ui.controllers;

// The DAO talks directly to the database for us.
import com.hiddeninpixel.db.AnalyticsDAO;

// The model object that represents one row of analytics data.
import com.hiddeninpixel.model.AnalyticsRecord;

// JavaFX collections and UI imports.
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.util.Callback;
import javafx.scene.layout.HBox;
import javafx.geometry.Pos;

// Standard Java utilities.
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AnalyticsController {

    // "@FXML" tells JavaFX to automatically connect this Java variable
    // to the matching fx:id element in the analytics.fxml file.

    // The main table showing all past operations.
    @FXML
    private TableView<AnalyticsRecord> analyticsTable;

    // Individual columns in the table.
    @FXML private TableColumn<AnalyticsRecord, Integer> idColumn;
    @FXML private TableColumn<AnalyticsRecord, String> algorithmColumn;
    @FXML private TableColumn<AnalyticsRecord, String> resolutionColumn;
    @FXML private TableColumn<AnalyticsRecord, Double> sizeColumn;
    @FXML private TableColumn<AnalyticsRecord, Double> ratioColumn;
    @FXML private TableColumn<AnalyticsRecord, String> timestampColumn;

    // The editable "Details" column where the user can type notes.
    @FXML private TableColumn<AnalyticsRecord, String> detailsColumn;

    // The "Delete" button column (built entirely in code, not FXML).
    @FXML private TableColumn<AnalyticsRecord, Void> deleteColumn;

    // The pie chart showing which algorithms were used most.
    @FXML private PieChart algorithmUsageChart;

    // The bar chart showing average payload size per algorithm.
    @FXML private BarChart<String, Number> metricsChart;

    // The DAO that handles all database read/write/delete operations.
    private final AnalyticsDAO dao = new AnalyticsDAO();

    // "initialize" is called automatically by JavaFX right after the FXML file is loaded.
    // This is where we configure every column and load the initial data.
    @FXML
    public void initialize() {
        // Tell each column which field from AnalyticsRecord it should display.
        // PropertyValueFactory("id") looks for the method "getId()" on AnalyticsRecord.
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        algorithmColumn.setCellValueFactory(new PropertyValueFactory<>("algorithmName"));
        resolutionColumn.setCellValueFactory(new PropertyValueFactory<>("imageResolution"));
        sizeColumn.setCellValueFactory(new PropertyValueFactory<>("payloadSizeKB"));
        ratioColumn.setCellValueFactory(new PropertyValueFactory<>("changeRatio"));
        timestampColumn.setCellValueFactory(new PropertyValueFactory<>("timestamp"));

        // Set up the Details column so it shows and edits the "details" field.
        detailsColumn.setCellValueFactory(new PropertyValueFactory<>("details"));

        // Allow the table to be edited (required for editable cells).
        analyticsTable.setEditable(true);

        // TextFieldTableCell makes the details cell turn into a text box when double-clicked.
        detailsColumn.setCellFactory(TextFieldTableCell.forTableColumn());

        // When the user finishes editing a details cell and presses Enter, save the new value.
        detailsColumn.setOnEditCommit(event -> {
            // Get the record that was edited.
            AnalyticsRecord record = event.getRowValue();

            // Update the in-memory object with the new text.
            record.setDetails(event.getNewValue());

            // Save the change to the database so it persists after the app is closed.
            dao.updateDetails(record.getId(), event.getNewValue());
        });

        // Set up the Delete column with a red "X" button in every row.
        setupDeleteColumn();

        // Load and display all records from the database.
        refreshData();
    }

    // Builds the Delete column. Each row gets its own button that deletes that row.
    private void setupDeleteColumn() {
        // A Callback defines how to create a TableCell for each row.
        // We use an anonymous class to build a custom cell containing a button.
        Callback<TableColumn<AnalyticsRecord, Void>, TableCell<AnalyticsRecord, Void>> cellFactory =
            new Callback<>() {
                @Override
                public TableCell<AnalyticsRecord, Void> call(TableColumn<AnalyticsRecord, Void> param) {
                    return new TableCell<>() {
                        // Create the delete button once per cell.
                        private final Button deleteBtn = new Button("X");

                        // This block runs when the cell is first created.
                        {
                            // Style the button red to indicate a destructive action.
                            deleteBtn.setStyle(
                                "-fx-background-color: #ef4444; " +
                                "-fx-text-fill: white; " +
                                "-fx-font-weight: bold; " +
                                "-fx-padding: 3 8 3 8; " +
                                "-fx-background-radius: 4;"
                            );

                            // When the button is clicked, delete this row's record.
                            deleteBtn.setOnAction(event -> {
                                // getTableView().getItems().get(getIndex()) gives us the record
                                // for the row this button belongs to.
                                AnalyticsRecord record = getTableView().getItems().get(getIndex());

                                // Delete from the database.
                                dao.delete(record.getId());

                                // Remove from the table's in-memory list so the UI updates immediately.
                                getTableView().getItems().remove(record);
                            });
                        }

                        // "updateItem" is called every time the cell is redrawn.
                        // We must set the graphic to null when the cell is empty (empty row placeholder).
                        @Override
                        protected void updateItem(Void item, boolean empty) {
                            super.updateItem(item, empty);
                            if (empty) {
                                // No data in this row, so show nothing.
                                setGraphic(null);
                            } else {
                                // Real row, show our delete button centered in the cell.
                                HBox box = new HBox(deleteBtn);
                                box.setAlignment(Pos.CENTER);
                                setGraphic(box);
                            }
                        }
                    };
                }
            };

        // Apply our custom cell factory to the delete column.
        deleteColumn.setCellFactory(cellFactory);
    }

    // Called when the user clicks the "Refresh Analytics Data" button.
    // Re-fetches everything from the database and redraws the table and charts.
    @FXML
    public void refreshData() {
        // Load all records from the database.
        List<AnalyticsRecord> records = dao.getAll();

        // Wrap them in an ObservableList so JavaFX can react when items are added or removed.
        ObservableList<AnalyticsRecord> data = FXCollections.observableArrayList(records);

        // Set the table's data source.
        analyticsTable.setItems(data);

        // Update both charts with the new data.
        updateCharts(records);
    }

    // Rebuilds the pie chart and bar chart from a fresh list of records.
    private void updateCharts(List<AnalyticsRecord> records) {
        // These maps accumulate totals per algorithm as we loop through records.
        Map<String, Integer> usageCount = new HashMap<>();  // how many times each algorithm was used
        Map<String, Double> totalPayload = new HashMap<>(); // total KB hidden per algorithm

        for (AnalyticsRecord record : records) {
            String algo = record.getAlgorithmName();

            // Increment the count for this algorithm. "getOrDefault" returns 0 if not seen yet.
            usageCount.put(algo, usageCount.getOrDefault(algo, 0) + 1);

            // Add this record's payload size to the running total for its algorithm.
            totalPayload.put(algo, totalPayload.getOrDefault(algo, 0.0) + record.getPayloadSizeKB());
        }

        // Build the pie chart data: one slice per algorithm, sized by usage count.
        ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList();
        for (Map.Entry<String, Integer> entry : usageCount.entrySet()) {
            // Label shows the algorithm name and how many times it was used.
            pieChartData.add(new PieChart.Data(entry.getKey() + " (" + entry.getValue() + ")", entry.getValue()));
        }
        algorithmUsageChart.setData(pieChartData);

        // Build the bar chart data: one bar per algorithm, showing the average payload size.
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Average KB Embedded");

        for (Map.Entry<String, Double> entry : totalPayload.entrySet()) {
            // Get how many times this algorithm was used so we can compute the average.
            int count = usageCount.get(entry.getKey());
            double average = entry.getValue() / count;

            // Add one bar to the chart.
            series.getData().add(new XYChart.Data<>(entry.getKey(), average));
        }

        // Clear old data and add the fresh series.
        metricsChart.getData().clear();
        metricsChart.getData().add(series);
    }
}
