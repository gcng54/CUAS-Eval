package io.github.gcng54.cuaseval.ui;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import io.github.gcng54.cuaseval.evaluator.MetricsCalculator;
import io.github.gcng54.cuaseval.model.*;
import io.github.gcng54.cuaseval.report.KmlExporter;
import io.github.gcng54.cuaseval.report.TestReportGenerator;

import java.io.File;
import java.util.Locale;
import java.util.Map;

/**
 * Reports panel showing evaluation results and export options.
 * Displays verdict, metrics, passed/failed requirements, and export buttons.
 */
public class ReportPanel extends VBox {

    private final Label verdictLabel;
    private final Label scoreLabel;
    private final TableView<Map.Entry<String, Double>> metricsTable;
    private final ListView<String> passedList;
    private final ListView<String> failedList;
    
    private final Button htmlButton;
    private final Button kmlButton;
    private final Button suiteButton;
    private final Label statusLabel;

    private TestScenario currentScenario;
    private EvaluationResult currentResult;

    private final TestReportGenerator reportGen = new TestReportGenerator();
    private final KmlExporter kmlExporter = new KmlExporter();
    private final MetricsCalculator metricsCalc = new MetricsCalculator();

    // ── Constructor ─────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    public ReportPanel() {
        setSpacing(10);
        setPadding(new Insets(10));
        setPrefWidth(350);

        Label title = new Label("Evaluation Results & Reports");
        title.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1a237e;");

        // ═══ Evaluation Results Section ═══
        
        // Verdict
        verdictLabel = new Label("—");
        verdictLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-padding: 10;");
        scoreLabel = new Label("Score: — / 100");
        scoreLabel.setStyle("-fx-font-size: 13px;");

        // Metrics table
        metricsTable = new TableView<>();
        metricsTable.setPlaceholder(new Label("Run an evaluation to see metrics"));
        TableColumn<Map.Entry<String, Double>, String> nameCol = new TableColumn<>("Metric");
        nameCol.setCellValueFactory(cd ->
                new javafx.beans.property.SimpleStringProperty(cd.getValue().getKey()));
        nameCol.setPrefWidth(180);

        TableColumn<Map.Entry<String, Double>, String> valCol = new TableColumn<>("Value");
        valCol.setCellValueFactory(cd ->
                new javafx.beans.property.SimpleStringProperty(
                        String.format(Locale.ENGLISH, "%.4f", cd.getValue().getValue())));
        valCol.setPrefWidth(100);

        metricsTable.getColumns().addAll(nameCol, valCol);
        metricsTable.setPrefHeight(250);

        // Requirement lists
        Label passLabel = new Label("Passed Requirements:");
        passLabel.setStyle("-fx-text-fill: #2e7d32; -fx-font-weight: bold;");
        passedList = new ListView<>();
        passedList.setPrefHeight(120);

        Label failLabel = new Label("Failed Requirements:");
        failLabel.setStyle("-fx-text-fill: #c62828; -fx-font-weight: bold;");
        failedList = new ListView<>();
        failedList.setPrefHeight(120);

        // ═══ Export Section ═══
        
        Label exportTitle = new Label("── Export Reports ──");
        exportTitle.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #1a237e;");

        htmlButton = new Button("Export HTML Report");
        htmlButton.setMaxWidth(Double.MAX_VALUE);
        htmlButton.setOnAction(e -> exportHtml());
        htmlButton.setDisable(true);

        kmlButton = new Button("Export KML (Google Earth)");
        kmlButton.setMaxWidth(Double.MAX_VALUE);
        kmlButton.setOnAction(e -> exportKml());
        kmlButton.setDisable(true);

        suiteButton = new Button("Export Suite Summary");
        suiteButton.setMaxWidth(Double.MAX_VALUE);
        suiteButton.setDisable(true);

        statusLabel = new Label("Run an evaluation to see results.");
        statusLabel.setWrapText(true);
        statusLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 11px;");

        getChildren().addAll(
                title, new Separator(),
                verdictLabel, scoreLabel,
                new Separator(),
                new Label("Performance Metrics:"), metricsTable,
                new Separator(),
                passLabel, passedList,
                failLabel, failedList,
                new Separator(),
                exportTitle,
                htmlButton, kmlButton, suiteButton,
                new Separator(),
                statusLabel
        );
    }

    // ── Data Binding ────────────────────────────────────────────────────

    /**
     * Set current evaluation data and update results display.
     */
    public void setEvaluationData(TestScenario scenario, EvaluationResult result) {
        this.currentScenario = scenario;
        this.currentResult = result;
        updateResults(result);
        htmlButton.setDisable(false);
        kmlButton.setDisable(false);
        statusLabel.setText("Ready to export: " + scenario.getName());
    }

    /**
     * Update the results display with evaluation data.
     */
    private void updateResults(EvaluationResult result) {
        if (result == null) return;

        // Verdict
        if (result.isPassed()) {
            verdictLabel.setText("PASS");
            verdictLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; " +
                    "-fx-text-fill: #2e7d32; -fx-padding: 10;");
        } else {
            verdictLabel.setText("FAIL");
            verdictLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; " +
                    "-fx-text-fill: #c62828; -fx-padding: 10;");
        }
        scoreLabel.setText(String.format(Locale.ENGLISH, "Score: %.1f / 100 (Compliance: %.1f%%)",
                result.getOverallScore(), result.getCompliancePercent()));

        // Metrics
        Map<String, Double> metrics = metricsCalc.computeAllMetrics(result);
        metricsTable.getItems().clear();
        metricsTable.getItems().addAll(metrics.entrySet());

        // Requirements
        passedList.getItems().clear();
        passedList.getItems().addAll(result.getPassedRequirements());
        failedList.getItems().clear();
        failedList.getItems().addAll(result.getFailedRequirements());
    }

    // ── Export Actions ──────────────────────────────────────────────────

    private void exportHtml() {
        if (currentScenario == null || currentResult == null) return;

        FileChooser fc = new FileChooser();
        fc.setTitle("Save HTML Report");
        fc.setInitialFileName("cuas_eval_report.html");
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("HTML Files", "*.html"));

        Stage stage = (Stage) getScene().getWindow();
        File file = fc.showSaveDialog(stage);
        if (file != null) {
            reportGen.generateHtmlReport(currentScenario, currentResult, file);
            statusLabel.setText("HTML report saved: " + file.getName());
        }
    }

    private void exportKml() {
        if (currentScenario == null || currentResult == null) return;

        FileChooser fc = new FileChooser();
        fc.setTitle("Save KML File");
        fc.setInitialFileName("cuas_eval_tracks.kml");
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("KML Files", "*.kml"));

        Stage stage = (Stage) getScene().getWindow();
        File file = fc.showSaveDialog(stage);
        if (file != null) {
            kmlExporter.export(currentScenario, currentResult, file);
            statusLabel.setText("KML exported: " + file.getName());
        }
    }
}
