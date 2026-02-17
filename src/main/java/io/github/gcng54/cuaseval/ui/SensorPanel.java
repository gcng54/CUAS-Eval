package io.github.gcng54.cuaseval.ui;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import io.github.gcng54.cuaseval.dti.MultiDtiSystem;
import io.github.gcng54.cuaseval.model.*;

import java.util.*;
import java.util.Locale;

/**
 * Sensor configuration panel for the CUAS-Eval UI.
 * Allows the user to:
 * <ul>
 *   <li>Browse and select CUAS sensors from the sensor library</li>
 *   <li>Configure DTI nodes with sensor type, count, and position</li>
 *   <li>Configure multi-DTI system with fusion strategy</li>
 *   <li>View sensor specifications and system coverage metrics</li>
 * </ul>
 */
public class SensorPanel extends VBox {

    private final ComboBox<String> sensorLibCombo;
    private final TextArea sensorSpecArea;
    private final ListView<String> nodeListView;
    private final TextField nodeIdField;
    private final TextField nodeLatField;
    private final TextField nodeLonField;
    private final ComboBox<String> fusionCombo;
    private final Spinner<Integer> votingKSpinner;
    private final TextArea systemSummaryArea;

    // Library editing fields (SN-02)
    private final TextField editRangeField;
    private final TextField editPdField;
    private final TextField editEwSensField;
    private final Button updateLibBtn;
    private final Button removeLibBtn;

    private final MultiDtiSystem multiDti = new MultiDtiSystem();
    private int nodeCounter = 0;

    /** Callback for notifying parent that the multi-DTI config changed. */
    public interface SensorCallback {
        void onMultiDtiConfigured(MultiDtiSystem system);
    }

    /** Callback for adding sensors to current scenario (SN-01). */
    public interface AddToScenarioCallback {
        void onAddSensorToScenario(CuasSensor sensor, GeoPosition position);
    }

    private SensorCallback callback;
    private AddToScenarioCallback addToScenarioCallback;

    // ── Constructor ─────────────────────────────────────────────────────

    public SensorPanel() {
        setSpacing(8);
        setPadding(new Insets(10));
        setPrefWidth(320);

        // Title
        Label title = new Label("Sensor & DTI Configuration");
        title.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1a237e;");

        // ── Sensor Library Browser ──
        Label libLabel = label("Sensor Library:");
        sensorLibCombo = new ComboBox<>();
        sensorLibCombo.setMaxWidth(Double.MAX_VALUE);
        for (CuasSensor s : SensorLibrary.getAllTemplates()) {
            sensorLibCombo.getItems().add(s.getSensorId() + " — " + s.getName());
        }
        sensorLibCombo.getSelectionModel().selectFirst();
        sensorLibCombo.setOnAction(e -> showSensorSpec());

        sensorSpecArea = new TextArea();
        sensorSpecArea.setEditable(false);
        sensorSpecArea.setPrefRowCount(8);
        sensorSpecArea.setStyle("-fx-font-family: Consolas; -fx-font-size: 10px;");
        showSensorSpec();

        // Library editing inline controls (SN-02)
        Label editLabel = label("── Edit Selected Sensor ──");
        editLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #333;");

        editRangeField = new TextField();
        editRangeField.setPromptText("Max Range (m)");

        editPdField = new TextField();
        editPdField.setPromptText("Base Pd (0-1)");

        editEwSensField = new TextField();
        editEwSensField.setPromptText("EW Sensitivity (0-1)");

        updateLibBtn = new Button("Update in Library");
        updateLibBtn.setMaxWidth(Double.MAX_VALUE);
        updateLibBtn.setStyle("-fx-base: #FF9800; -fx-text-fill: white;");
        updateLibBtn.setOnAction(e -> updateSensorInLibrary());

        removeLibBtn = new Button("Remove from Library");
        removeLibBtn.setMaxWidth(Double.MAX_VALUE);
        removeLibBtn.setStyle("-fx-base: #c62828; -fx-text-fill: white;");
        removeLibBtn.setOnAction(e -> removeSensorFromLibrary());

        // Add to existing scenario button (SN-01)
        Button addToScenarioBtn = new Button("Add Sensor to Scenario");
        addToScenarioBtn.setMaxWidth(Double.MAX_VALUE);
        addToScenarioBtn.setStyle("-fx-base: #1565C0; -fx-text-fill: white;");
        addToScenarioBtn.setOnAction(e -> addSensorToScenario());

        // ── DTI Node Configuration ──
        Label nodeTitle = label("── DTI Nodes ──");
        nodeTitle.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #333;");

        nodeIdField = new TextField();
        nodeIdField.setPromptText("Node name (auto-generated)");

        nodeLatField = new TextField("38.4237");
        nodeLatField.setPromptText("Node Latitude");

        nodeLonField = new TextField("27.1428");
        nodeLonField.setPromptText("Node Longitude");

        Button addNodeBtn = new Button("+ Add Node");
        addNodeBtn.setStyle("-fx-background-color: #2e7d32; -fx-text-fill: white;");
        addNodeBtn.setMaxWidth(Double.MAX_VALUE);
        addNodeBtn.setOnAction(e -> addNode());

        Button removeNodeBtn = new Button("− Remove Selected");
        removeNodeBtn.setStyle("-fx-background-color: #c62828; -fx-text-fill: white;");
        removeNodeBtn.setMaxWidth(Double.MAX_VALUE);
        removeNodeBtn.setOnAction(e -> removeSelectedNode());

        Button clearBtn = new Button("Clear All");
        clearBtn.setMaxWidth(Double.MAX_VALUE);
        clearBtn.setOnAction(e -> clearAllNodes());

        HBox nodeButtons = new HBox(5, addNodeBtn, removeNodeBtn);
        HBox.setHgrow(addNodeBtn, Priority.ALWAYS);
        HBox.setHgrow(removeNodeBtn, Priority.ALWAYS);

        nodeListView = new ListView<>();
        nodeListView.setPrefHeight(120);
        nodeListView.setStyle("-fx-font-family: Consolas; -fx-font-size: 10px;");

        // ── Fusion Configuration ──
        Label fusionTitle = label("── Fusion Strategy ──");
        fusionTitle.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #333;");

        fusionCombo = new ComboBox<>();
        fusionCombo.getItems().addAll("OR Logic (Max Sensitivity)", "Voting (k-of-n)",
                "AND Logic (Min FAR)", "Best Sensor");
        fusionCombo.getSelectionModel().selectFirst();
        fusionCombo.setMaxWidth(Double.MAX_VALUE);
        fusionCombo.setOnAction(e -> onFusionChanged());

        votingKSpinner = new Spinner<>(2, 10, 2);
        votingKSpinner.setEditable(true);
        votingKSpinner.setDisable(true);

        // ── System Summary ──
        Label summaryTitle = label("── System Summary ──");
        summaryTitle.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #333;");

        systemSummaryArea = new TextArea("No nodes configured.");
        systemSummaryArea.setEditable(false);
        systemSummaryArea.setPrefRowCount(6);
        systemSummaryArea.setStyle("-fx-font-family: Consolas; -fx-font-size: 10px;");

        // ── Quick Templates ──
        Label quickLabel = label("── Quick Setup ──");
        quickLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #333;");

        Button basicSetup = new Button("Basic Radar + EO/IR");
        basicSetup.setMaxWidth(Double.MAX_VALUE);
        basicSetup.setOnAction(e -> setupBasicConfig());

        Button advancedSetup = new Button("Advanced Multi-Sensor");
        advancedSetup.setMaxWidth(Double.MAX_VALUE);
        advancedSetup.setOnAction(e -> setupAdvancedConfig());

        Button fullSetup = new Button("Full Spectrum Suite");
        fullSetup.setMaxWidth(Double.MAX_VALUE);
        fullSetup.setOnAction(e -> setupFullConfig());

        // ── Layout ──
        getChildren().addAll(
                title,
                new Separator(),
                libLabel, sensorLibCombo,
                sensorSpecArea,
                editLabel,
                label("Max Range (m):"), editRangeField,
                label("Base Pd:"), editPdField,
                label("EW Sensitivity:"), editEwSensField,
                updateLibBtn,
                removeLibBtn,
                addToScenarioBtn,
                new Separator(),
                nodeTitle,
                label("Node Name:"), nodeIdField,
                label("Latitude:"), nodeLatField,
                label("Longitude:"), nodeLonField,
                nodeButtons,
                clearBtn,
                label("Active Nodes:"), nodeListView,
                new Separator(),
                fusionTitle,
                label("Detection Fusion:"), fusionCombo,
                label("Voting k:"), votingKSpinner,
                new Separator(),
                summaryTitle,
                systemSummaryArea,
                new Separator(),
                quickLabel,
                basicSetup,
                advancedSetup,
                fullSetup
        );
    }

    // ── API ─────────────────────────────────────────────────────────────

    public void setCallback(SensorCallback callback) {
        this.callback = callback;
    }

    public void setAddToScenarioCallback(AddToScenarioCallback callback) {
        this.addToScenarioCallback = callback;
    }

    public MultiDtiSystem getMultiDtiSystem() {
        return multiDti;
    }

    // ── Sensor Spec Display ─────────────────────────────────────────────

    private void showSensorSpec() {
        String selected = sensorLibCombo.getValue();
        if (selected == null) return;
        String sensorId = selected.split(" — ")[0].trim();

        try {
            CuasSensor s = SensorLibrary.getTemplate(sensorId);
            StringBuilder sb = new StringBuilder();
            sb.append(String.format(Locale.ENGLISH, "%s (%s)\n", s.getName(), s.getSensorType().getDisplayName()));
            sb.append(String.format(Locale.ENGLISH, "Range: %.0f–%.0f m | Alt: %.0f–%.0f m\n",
                    s.getMinRangeM(), s.getMaxRangeM(), s.getMinAltitudeM(), s.getMaxAltitudeM()));
            sb.append(String.format(Locale.ENGLISH, "Pd: %.2f | FAR: %.4f | Latency: %.1fs\n",
                    s.getBasePd(), s.getFalseAlarmRate(), s.getDetectionLatencyS()));
            sb.append(String.format(Locale.ENGLISH, "Accuracy: %.1fm | Update: %.1f Hz\n",
                    s.getPositionAccuracyM(), s.getUpdateRateHz()));
            sb.append(String.format(Locale.ENGLISH, "Az: %.0f° (start: %.0f°) | El: %.0f–%.0f°\n",
                    s.getAzimuthCoverageDeg(), s.getAzimuthStartDeg(),
                    s.getMinElevationDeg(), s.getMaxElevationDeg()));
            sb.append(String.format(Locale.ENGLISH, "Band: %s | EW Sens: %.2f\n",
                    s.getFrequencyBand() != null ? s.getFrequencyBand() : "N/A",
                    s.getEwSensitivity()));
            sb.append(String.format(Locale.ENGLISH, "Weather: rain=%.2f fog=%.2f night=%.2f\n",
                    s.getRainFactor(), s.getFogFactor(), s.getNightFactor()));
            sb.append(String.format(Locale.ENGLISH, "ID: %s (Pd=%.2f) | Classify: %s (%.0f%%)\n",
                    s.canIdentify() ? "Yes" : "No", s.getIdentificationPd(),
                    s.canClassify() ? "Yes" : "No", s.getClassificationAccuracy() * 100));
            sb.append(String.format(Locale.ENGLISH, "Power: %.0fW | Weight: %.0fkg | Mobile: %s",
                    s.getPowerConsumptionW(), s.getWeightKg(), s.isMobile() ? "Yes" : "No"));
            sensorSpecArea.setText(sb.toString());

            // Populate edit fields (SN-02)
            editRangeField.setText(String.format(Locale.ENGLISH, "%.0f", s.getMaxRangeM()));
            editPdField.setText(String.format(Locale.ENGLISH, "%.2f", s.getBasePd()));
            editEwSensField.setText(String.format(Locale.ENGLISH, "%.2f", s.getEwSensitivity()));
        } catch (Exception ex) {
            sensorSpecArea.setText("Error loading sensor: " + ex.getMessage());
        }
    }

    // ── Node Management ─────────────────────────────────────────────────

    private void addNode() {
        String selected = sensorLibCombo.getValue();
        if (selected == null) return;
        String sensorId = selected.split(" — ")[0].trim();

        try {
            CuasSensor sensor = SensorLibrary.getTemplate(sensorId);
            nodeCounter++;

            String nodeId = nodeIdField.getText().isEmpty()
                    ? "N" + String.format(Locale.ENGLISH, "%02d", nodeCounter)
                    : nodeIdField.getText();

            double lat = Double.parseDouble(nodeLatField.getText());
            double lon = Double.parseDouble(nodeLonField.getText());
            GeoPosition pos = new GeoPosition(lat, lon, 0);

            multiDti.addNode(nodeId, sensor.getName(), pos, sensor);

            nodeListView.getItems().add(String.format(Locale.ENGLISH,
                    "%s: %s @ %.4f,%.4f (%.0fm)",
                    nodeId, sensor.getSensorId(), lat, lon, sensor.getMaxRangeM()));

            nodeIdField.clear();
            updateSystemSummary();
            notifyCallback();

        } catch (NumberFormatException ex) {
            showAlert("Invalid position coordinates.");
        }
    }

    private void removeSelectedNode() {
        int idx = nodeListView.getSelectionModel().getSelectedIndex();
        if (idx >= 0 && idx < multiDti.getNodes().size()) {
            String nodeId = multiDti.getNodes().get(idx).getNodeId();
            multiDti.removeNode(nodeId);
            nodeListView.getItems().remove(idx);
            updateSystemSummary();
            notifyCallback();
        }
    }

    private void clearAllNodes() {
        multiDti.clearNodes();
        nodeListView.getItems().clear();
        nodeCounter = 0;
        updateSystemSummary();
        notifyCallback();
    }

    // ── Fusion ──────────────────────────────────────────────────────────

    private void onFusionChanged() {
        String fusion = fusionCombo.getValue();
        votingKSpinner.setDisable(!"Voting (k-of-n)".equals(fusion));

        MultiDtiSystem.FusionStrategy strategy = switch (fusion) {
            case "Voting (k-of-n)" -> MultiDtiSystem.FusionStrategy.VOTING;
            case "AND Logic (Min FAR)" -> MultiDtiSystem.FusionStrategy.AND_LOGIC;
            case "Best Sensor" -> MultiDtiSystem.FusionStrategy.BEST_SENSOR;
            default -> MultiDtiSystem.FusionStrategy.OR_LOGIC;
        };

        multiDti.setDetectionFusion(strategy);
        multiDti.setVotingThreshold(votingKSpinner.getValue());
        notifyCallback();
    }

    // ── Quick Configs ───────────────────────────────────────────────────

    private void setupBasicConfig() {
        clearAllNodes();
        double lat = Double.parseDouble(nodeLatField.getText());
        double lon = Double.parseDouble(nodeLonField.getText());

        addQuickNode("RD-02", lat, lon);
        addQuickNode("EO-02", lat + 0.002, lon + 0.002);

        multiDti.setDetectionFusion(MultiDtiSystem.FusionStrategy.OR_LOGIC);
        fusionCombo.getSelectionModel().select(0);
        updateSystemSummary();
        notifyCallback();
    }

    private void setupAdvancedConfig() {
        clearAllNodes();
        double lat = Double.parseDouble(nodeLatField.getText());
        double lon = Double.parseDouble(nodeLonField.getText());

        addQuickNode("RD-03", lat, lon);                          // Long-range radar
        addQuickNode("RD-04", lat + 0.005, lon - 0.005);          // Compact radar
        addQuickNode("EO-04", lat - 0.002, lon + 0.002);          // Multi-spectral EO/IR
        addQuickNode("RF-01", lat + 0.003, lon + 0.003);          // Wideband RF

        multiDti.setDetectionFusion(MultiDtiSystem.FusionStrategy.OR_LOGIC);
        fusionCombo.getSelectionModel().select(0);
        updateSystemSummary();
        notifyCallback();
    }

    private void setupFullConfig() {
        clearAllNodes();
        double lat = Double.parseDouble(nodeLatField.getText());
        double lon = Double.parseDouble(nodeLonField.getText());

        addQuickNode("RD-03", lat, lon);                          // 3D phased array
        addQuickNode("RD-01", lat + 0.010, lon);                  // Short-range PD
        addQuickNode("RD-04", lat, lon + 0.010);                  // Compact
        addQuickNode("EO-04", lat - 0.003, lon + 0.003);          // Multi-spectral
        addQuickNode("EO-02", lat + 0.005, lon - 0.005);          // MWIR thermal
        addQuickNode("RF-01", lat, lon - 0.005);                  // SDR scanner
        addQuickNode("RF-03", lat + 0.002, lon + 0.005);          // Protocol analyser
        addQuickNode("AC-01", lat - 0.001, lon - 0.001);          // Acoustic array

        multiDti.setDetectionFusion(MultiDtiSystem.FusionStrategy.VOTING);
        multiDti.setVotingThreshold(2);
        fusionCombo.getSelectionModel().select(1);
        votingKSpinner.getValueFactory().setValue(2);
        votingKSpinner.setDisable(false);
        updateSystemSummary();
        notifyCallback();
    }

    private void addQuickNode(String sensorId, double lat, double lon) {
        CuasSensor sensor = SensorLibrary.getTemplate(sensorId);
        nodeCounter++;
        String nodeId = "N" + String.format(Locale.ENGLISH, "%02d", nodeCounter);
        GeoPosition pos = new GeoPosition(lat, lon, 0);
        multiDti.addNode(nodeId, sensor.getName(), pos, sensor);
        nodeListView.getItems().add(String.format(Locale.ENGLISH,
                "%s: %s @ %.4f,%.4f (%.0fm)",
                nodeId, sensorId, lat, lon, sensor.getMaxRangeM()));
    }

    // ── Helpers ─────────────────────────────────────────────────────────

    // ── Library Editing (SN-02) ─────────────────────────────────────────

    private void updateSensorInLibrary() {
        String selected = sensorLibCombo.getValue();
        if (selected == null) return;
        String sensorId = selected.split(" — ")[0].trim();

        try {
            CuasSensor sensor = SensorLibrary.getTemplate(sensorId).copy();

            String rangeStr = editRangeField.getText().trim();
            if (!rangeStr.isEmpty()) sensor.setMaxRangeM(Double.parseDouble(rangeStr));

            String pdStr = editPdField.getText().trim();
            if (!pdStr.isEmpty()) sensor.setBasePd(Double.parseDouble(pdStr));

            String ewStr = editEwSensField.getText().trim();
            if (!ewStr.isEmpty()) sensor.setEwSensitivity(Double.parseDouble(ewStr));

            SensorLibrary.updateTemplate(sensor);
            showSensorSpec();
            showAlert("Sensor '" + sensorId + "' updated in library.");
        } catch (NumberFormatException ex) {
            showAlert("Invalid number format in edit fields.");
        } catch (Exception ex) {
            showAlert("Error updating sensor: " + ex.getMessage());
        }
    }

    private void removeSensorFromLibrary() {
        String selected = sensorLibCombo.getValue();
        if (selected == null) return;
        String sensorId = selected.split(" — ")[0].trim();

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Remove Sensor");
        confirm.setContentText("Remove '" + sensorId + "' from the library?");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                if (SensorLibrary.removeTemplate(sensorId)) {
                    sensorLibCombo.getItems().remove(selected);
                    sensorLibCombo.getSelectionModel().selectFirst();
                    showSensorSpec();
                }
            }
        });
    }

    // ── Add to Existing Scenario (SN-01) ────────────────────────────────

    private void addSensorToScenario() {
        String selected = sensorLibCombo.getValue();
        if (selected == null) return;
        String sensorId = selected.split(" — ")[0].trim();

        try {
            CuasSensor sensor = SensorLibrary.getTemplate(sensorId).copy();
            double lat = Double.parseDouble(nodeLatField.getText());
            double lon = Double.parseDouble(nodeLonField.getText());
            GeoPosition pos = new GeoPosition(lat, lon, 0);

            if (addToScenarioCallback != null) {
                addToScenarioCallback.onAddSensorToScenario(sensor, pos);
                showAlert("Sensor '" + sensorId + "' added to current scenario at "
                        + String.format(Locale.ENGLISH, "(%.4f, %.4f)", lat, lon));
            } else {
                showAlert("No active scenario. Run an evaluation first.");
            }
        } catch (NumberFormatException ex) {
            showAlert("Invalid position coordinates.");
        }
    }

    // ── Helpers (continued) ─────────────────────────────────────────────

    private void updateSystemSummary() {
        if (multiDti.getNodeCount() == 0) {
            systemSummaryArea.setText("No nodes configured.\n\nUse 'Add Node' or quick setup buttons.");
            return;
        }
        systemSummaryArea.setText(multiDti.getSummary());
    }

    private void notifyCallback() {
        if (callback != null) {
            callback.onMultiDtiConfigured(multiDti);
        }
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Sensor Configuration");
        alert.setContentText(message);
        alert.showAndWait();
    }

    private Label label(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size: 11px; -fx-text-fill: #333;");
        return l;
    }
}
