package io.github.gcng54.cuaseval.ui;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import io.github.gcng54.cuaseval.evaluator.EvaluationCriteria;
import io.github.gcng54.cuaseval.evaluator.MetricsCalculator;
import io.github.gcng54.cuaseval.io.AssetManager;
import io.github.gcng54.cuaseval.model.EvaluationResult;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;

/**
 * Evaluation results panel showing metrics, scores, and requirement compliance.
 * Supports editable evaluation criteria and save/load of criteria profiles (EM-01, EM-02).
 */
public class EvaluationPanel extends VBox {

    private final Label verdictLabel;
    private final Label scoreLabel;
    private final TableView<Map.Entry<String, Double>> metricsTable;
    private final ListView<String> passedList;
    private final ListView<String> failedList;

    // Editable criteria controls (EM-01)
    private final TextField profileNameField;
    private final TextField detWeightField;
    private final TextField trkWeightField;
    private final TextField idWeightField;
    private final ListView<String> thresholdListView;
    private final TextField threshReqIdField;
    private final TextField threshMinField;
    private final TextField threshMaxField;

    private final MetricsCalculator metricsCalc = new MetricsCalculator();
    private EvaluationCriteria criteria = new EvaluationCriteria();
    private final AssetManager assetManager = new AssetManager();

    // ── Constructor ─────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    public EvaluationPanel() {
        setSpacing(8);
        setPadding(new Insets(10));
        setPrefWidth(350);

        Label title = new Label("Evaluation Results");
        title.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1a237e;");

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
        nameCol.setPrefWidth(200);

        TableColumn<Map.Entry<String, Double>, String> valCol = new TableColumn<>("Value");
        valCol.setCellValueFactory(cd ->
                new javafx.beans.property.SimpleStringProperty(
                        String.format(Locale.ENGLISH, "%.4f", cd.getValue().getValue())));
        valCol.setPrefWidth(100);

        metricsTable.getColumns().addAll(nameCol, valCol);
        metricsTable.setPrefHeight(320);

        // Requirement lists
        Label passLabel = new Label("Passed Requirements:");
        passLabel.setStyle("-fx-text-fill: #2e7d32; -fx-font-weight: bold;");
        passedList = new ListView<>();
        passedList.setPrefHeight(180);

        Label failLabel = new Label("Failed Requirements:");
        failLabel.setStyle("-fx-text-fill: #c62828; -fx-font-weight: bold;");
        failedList = new ListView<>();
        failedList.setPrefHeight(180);

        getChildren().addAll(
                title, new Separator(),
                verdictLabel, scoreLabel,
                new Separator(),
                new Label("Performance Metrics:"), metricsTable,
                new Separator(),
                passLabel, passedList,
                failLabel, failedList
        );

        // ── Editable Criteria Section (EM-01, EM-02) ────────────────────
        Label criteriaTitle = new Label("── Evaluation Criteria ──");
        criteriaTitle.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #1a237e;");

        profileNameField = new TextField(criteria.getProfileName());
        profileNameField.setPromptText("Profile name");

        detWeightField = new TextField(String.valueOf((int) criteria.getDetectionWeight()));
        detWeightField.setPromptText("Detection weight");
        trkWeightField = new TextField(String.valueOf((int) criteria.getTrackingWeight()));
        trkWeightField.setPromptText("Tracking weight");
        idWeightField = new TextField(String.valueOf((int) criteria.getIdentificationWeight()));
        idWeightField.setPromptText("Identification weight");

        Button applyWeightsBtn = new Button("Apply Weights");
        applyWeightsBtn.setMaxWidth(Double.MAX_VALUE);
        applyWeightsBtn.setOnAction(e -> applyWeights());

        // Threshold list
        thresholdListView = new ListView<>();
        thresholdListView.setPrefHeight(120);
        thresholdListView.setStyle("-fx-font-family: Consolas; -fx-font-size: 10px;");
        refreshThresholdList();

        threshReqIdField = new TextField();
        threshReqIdField.setPromptText("Req ID (e.g. FR01)");
        threshMinField = new TextField();
        threshMinField.setPromptText("Min value");
        threshMaxField = new TextField();
        threshMaxField.setPromptText("Max value");

        Button updateThreshBtn = new Button("Update Threshold");
        updateThreshBtn.setMaxWidth(Double.MAX_VALUE);
        updateThreshBtn.setOnAction(e -> updateThreshold());

        // Select threshold → fill fields
        thresholdListView.getSelectionModel().selectedItemProperty().addListener((obs, ov, nv) -> {
            if (nv != null) {
                String reqId = nv.split("]")[0].replace("[", "").trim();
                threshReqIdField.setText(reqId);
                EvaluationCriteria.Threshold t = criteria.getThreshold(reqId);
                if (t != null) {
                    threshMinField.setText(String.format(Locale.ENGLISH, "%.4f", t.getMinValue()));
                    threshMaxField.setText(String.format(Locale.ENGLISH, "%.4f", t.getMaxValue()));
                }
            }
        });

        // Save/Load buttons (EM-02)
        Button saveCriteriaBtn = new Button("Save Criteria Profile");
        saveCriteriaBtn.setMaxWidth(Double.MAX_VALUE);
        saveCriteriaBtn.setStyle("-fx-base: #4CAF50; -fx-text-fill: white;");
        saveCriteriaBtn.setOnAction(e -> saveCriteria());

        Button loadCriteriaBtn = new Button("Load Criteria Profile");
        loadCriteriaBtn.setMaxWidth(Double.MAX_VALUE);
        loadCriteriaBtn.setStyle("-fx-base: #2196F3; -fx-text-fill: white;");
        loadCriteriaBtn.setOnAction(e -> loadCriteria());

        getChildren().addAll(
                new Separator(),
                criteriaTitle,
                label("Profile Name:"), profileNameField,
                label("Detection Weight:"), detWeightField,
                label("Tracking Weight:"), trkWeightField,
                label("Identification Weight:"), idWeightField,
                applyWeightsBtn,
                new Separator(),
                label("Thresholds:"), thresholdListView,
                label("Req ID:"), threshReqIdField,
                label("Min:"), threshMinField,
                label("Max:"), threshMaxField,
                updateThreshBtn,
                new Separator(),
                saveCriteriaBtn,
                loadCriteriaBtn
        );
    }

    // ── Data Update ─────────────────────────────────────────────────────

    /**
     * Update the panel with evaluation results.
     */
    public void updateResults(EvaluationResult result) {
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

    /** Get the current evaluation criteria (for use by TestEvaluator). */
    public EvaluationCriteria getCriteria() {
        return criteria;
    }

    // ── Criteria Editing (EM-01) ────────────────────────────────────────

    private void applyWeights() {
        try {
            criteria.setDetectionWeight(Double.parseDouble(detWeightField.getText().trim()));
            criteria.setTrackingWeight(Double.parseDouble(trkWeightField.getText().trim()));
            criteria.setIdentificationWeight(Double.parseDouble(idWeightField.getText().trim()));
            criteria.setProfileName(profileNameField.getText().trim());
        } catch (NumberFormatException ex) {
            showAlert("Invalid weight values.");
        }
    }

    private void updateThreshold() {
        try {
            String reqId = threshReqIdField.getText().trim();
            if (reqId.isEmpty()) return;

            double min = Double.parseDouble(threshMinField.getText().trim());
            double max = Double.parseDouble(threshMaxField.getText().trim());

            EvaluationCriteria.Threshold existing = criteria.getThreshold(reqId);
            String metric = existing != null ? existing.getMetricName() : reqId.toLowerCase();
            String unit = existing != null ? existing.getUnit() : "ratio";

            criteria.addThreshold(reqId, metric, min, max, unit);
            refreshThresholdList();
        } catch (NumberFormatException ex) {
            showAlert("Invalid threshold values.");
        }
    }

    private void refreshThresholdList() {
        thresholdListView.getItems().clear();
        for (EvaluationCriteria.Threshold t : criteria.getAllThresholds().values()) {
            thresholdListView.getItems().add(t.toString());
        }
    }

    // ── Save/Load (EM-02) ───────────────────────────────────────────────

    private void saveCriteria() {
        applyWeights();
        String name = profileNameField.getText().trim();
        if (name.isEmpty()) name = "default";
        try {
            assetManager.saveCriteria(criteria, name);
            showAlert("Criteria saved as '" + name + "'.");
        } catch (IOException ex) {
            showAlert("Failed to save: " + ex.getMessage());
        }
    }

    private void loadCriteria() {
        var names = assetManager.listCriteriaNames();
        if (names.isEmpty()) {
            showAlert("No saved criteria profiles found.");
            return;
        }

        ChoiceDialog<String> dialog = new ChoiceDialog<>(names.get(0), names);
        dialog.setTitle("Load Criteria Profile");
        dialog.setHeaderText("Select a criteria profile to load:");
        dialog.showAndWait().ifPresent(name -> {
            try {
                criteria = assetManager.loadCriteria(name);
                profileNameField.setText(criteria.getProfileName());
                detWeightField.setText(String.valueOf((int) criteria.getDetectionWeight()));
                trkWeightField.setText(String.valueOf((int) criteria.getTrackingWeight()));
                idWeightField.setText(String.valueOf((int) criteria.getIdentificationWeight()));
                refreshThresholdList();
            } catch (IOException ex) {
                showAlert("Failed to load: " + ex.getMessage());
            }
        });
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Evaluation Criteria");
        alert.setContentText(message);
        alert.showAndWait();
    }

    private Label label(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size: 11px; -fx-text-fill: #333;");
        return l;
    }
}
