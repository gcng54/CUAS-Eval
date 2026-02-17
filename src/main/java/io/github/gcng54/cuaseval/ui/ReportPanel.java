package io.github.gcng54.cuaseval.ui;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import io.github.gcng54.cuaseval.model.*;
import io.github.gcng54.cuaseval.report.KmlExporter;
import io.github.gcng54.cuaseval.report.TestReportGenerator;

import java.io.File;

/**
 * Report export panel for generating HTML reports and KML files.
 */
public class ReportPanel extends VBox {

    private final Button htmlButton;
    private final Button kmlButton;
    private final Button suiteButton;
    private final Label statusLabel;

    private TestScenario currentScenario;
    private EvaluationResult currentResult;

    private final TestReportGenerator reportGen = new TestReportGenerator();
    private final KmlExporter kmlExporter = new KmlExporter();

    // ── Constructor ─────────────────────────────────────────────────────

    public ReportPanel() {
        setSpacing(10);
        setPadding(new Insets(10));
        setPrefWidth(300);

        Label title = new Label("Report Export");
        title.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1a237e;");

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

        statusLabel = new Label("Run an evaluation to enable exports.");
        statusLabel.setWrapText(true);
        statusLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 11px;");

        getChildren().addAll(
                title, new Separator(),
                htmlButton, kmlButton, suiteButton,
                new Separator(),
                statusLabel
        );
    }

    // ── Data Binding ────────────────────────────────────────────────────

    /**
     * Set current evaluation data for export.
     */
    public void setEvaluationData(TestScenario scenario, EvaluationResult result) {
        this.currentScenario = scenario;
        this.currentResult = result;
        htmlButton.setDisable(false);
        kmlButton.setDisable(false);
        statusLabel.setText("Ready to export: " + scenario.getName());
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
