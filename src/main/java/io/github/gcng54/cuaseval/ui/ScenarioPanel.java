package io.github.gcng54.cuaseval.ui;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import io.github.gcng54.cuaseval.dti.MultiDtiSystem;
import io.github.gcng54.cuaseval.evaluator.TestEvaluator;
import io.github.gcng54.cuaseval.generator.TestScenarioGenerator;
import io.github.gcng54.cuaseval.model.*;

import java.util.List;
import java.util.Locale;

/**
 * Scenario configuration panel for the CUAS-Eval UI.
 * Allows the user to configure environment, targets, and run evaluations.
 * Supports both generic test scenarios and CWA 18150 COURAGEOUS scenarios (S1–S10).
 */
public class ScenarioPanel extends VBox {

    private final ComboBox<String> scenarioTypeCombo;
    private final ComboBox<String> courageousScenarioCombo;
    private final TextField latField;
    private final TextField lonField;
    private final Spinner<Integer> targetCountSpinner;
    private final ComboBox<String> weatherCombo;
    private final ComboBox<String> ewCombo;    // EW condition selector (EV-01)
    private final TextArea logArea;
    private final Label scenarioDescLabel;
    private final Button runButton;
    private final Button newScenarioButton;
    private final Button loadScenarioButton;

    private final TestScenarioGenerator scenarioGen = new TestScenarioGenerator();

    /** Optional multi-DTI system for sensor-aware evaluation */
    private MultiDtiSystem multiDtiSystem;

    // Callback for when a scenario is run
    private ScenarioCallback callback;

    /**
     * Callback interface for scenario execution results.
     */
    public interface ScenarioCallback {
        void onScenarioEvaluated(TestScenario scenario, EvaluationResult result);
    }

    // ── Constructor ─────────────────────────────────────────────────────

    public ScenarioPanel() {
        setSpacing(10);
        setPadding(new Insets(10));
        setPrefWidth(320);

        // Title
        Label title = new Label("Scenario Configuration");
        title.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1a237e;");

        // Scenario type (generic vs COURAGEOUS)
        scenarioTypeCombo = new ComboBox<>();
        scenarioTypeCombo.getItems().addAll(
                "── Generic Scenarios ──",
                "Single Target (Baseline)",
                "Multi-Target",
                "Tracking Continuity",
                "Adverse Weather",
                "Full Generic Suite",
                "── CWA 18150 COURAGEOUS ──",
                "S1: Prison",
                "S2: Airport",
                "S3: Nuclear Plant",
                "S4: Gov. Building",
                "S5: Stadium",
                "S6: Outdoor Concert",
                "S7: Political Rally",
                "S8: Int. Summit",
                "S9: Land Border",
                "S10: Maritime Border",
                "── Full Suites ──",
                "COURAGEOUS Full Suite (S1–S10)"
        );
        scenarioTypeCombo.getSelectionModel().select(1); // default: Single Target
        scenarioTypeCombo.setMaxWidth(Double.MAX_VALUE);
        scenarioTypeCombo.setOnAction(e -> onScenarioTypeChanged());

        // Scenario description label
        scenarioDescLabel = new Label("");
        scenarioDescLabel.setWrapText(true);
        scenarioDescLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #555; -fx-font-style: italic;");

        // COURAGEOUS scenario selector (hidden by default, for future use)
        courageousScenarioCombo = new ComboBox<>();
        for (ScenarioType st : ScenarioType.values()) {
            courageousScenarioCombo.getItems().add(st.toString());
        }
        courageousScenarioCombo.getSelectionModel().selectFirst();

        // Centre position (default: 26.8°E, 38.4°N)
        latField = new TextField("38.4000");
        latField.setPromptText("Latitude");
        lonField = new TextField("26.8000");
        lonField.setPromptText("Longitude");

        // Target count
        targetCountSpinner = new Spinner<>(1, 20, 5);
        targetCountSpinner.setEditable(true);

        // Weather
        weatherCombo = new ComboBox<>();
        for (TestEnvironment.WeatherCondition wc : TestEnvironment.WeatherCondition.values()) {
            weatherCombo.getItems().add(wc.name());
        }
        weatherCombo.getSelectionModel().selectFirst();

        // EW condition selector (EV-01)
        ewCombo = new ComboBox<>();
        for (TestEnvironment.EwCondition ew : TestEnvironment.EwCondition.values()) {
            ewCombo.getItems().add(ew.name() + " (factor=" + ew.getDefaultFactor() + ")");
        }
        ewCombo.getSelectionModel().selectFirst();

        // Scenario management buttons
        newScenarioButton = new Button("🆕 New Scenario");
        newScenarioButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-padding: 6 16;");
        newScenarioButton.setMaxWidth(Double.MAX_VALUE);
        newScenarioButton.setOnAction(e -> newScenario());

        loadScenarioButton = new Button("📂 Load Scenario");
        loadScenarioButton.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-padding: 6 16;");
        loadScenarioButton.setMaxWidth(Double.MAX_VALUE);
        loadScenarioButton.setOnAction(e -> loadScenario());

        HBox scenBtns = new HBox(5, newScenarioButton, loadScenarioButton);
        HBox.setHgrow(newScenarioButton, Priority.ALWAYS);
        HBox.setHgrow(loadScenarioButton, Priority.ALWAYS);

        // Run button
        runButton = new Button("▶  Run Evaluation");
        runButton.setStyle("-fx-background-color: #1a237e; -fx-text-fill: white; " +
                "-fx-font-weight: bold; -fx-padding: 8 20;");
        runButton.setMaxWidth(Double.MAX_VALUE);
        runButton.setOnAction(e -> runEvaluation());

        // Log area
        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setPrefRowCount(12);
        logArea.setStyle("-fx-font-family: Consolas; -fx-font-size: 11px;");

        // Layout
        getChildren().addAll(
                title,
                new Separator(),
                scenBtns,
                new Separator(),
                label("Scenario Type:"), scenarioTypeCombo,
                scenarioDescLabel,
                new Separator(),
                label("Centre Latitude:"), latField,
                label("Centre Longitude:"), lonField,
                label("Target Count (multi):"), targetCountSpinner,
                label("Weather (adverse):"), weatherCombo,
                label("EW Condition:"), ewCombo,
                new Separator(),
                runButton,
                new Separator(),
                label("Evaluation Log:"), logArea
        );

        onScenarioTypeChanged();
    }

    // ── API ─────────────────────────────────────────────────────────────

    public void setCallback(ScenarioCallback callback) {
        this.callback = callback;
    }

    public void setMultiDtiSystem(MultiDtiSystem system) {
        this.multiDtiSystem = system;
    }

    public void appendLog(String message) {
        logArea.appendText(message + "\n");
    }

    // ── Scenario changed ────────────────────────────────────────────────

    private void onScenarioTypeChanged() {
        String type = scenarioTypeCombo.getValue();
        if (type == null) return;

        // Disable separator items
        if (type.startsWith("──")) {
            scenarioTypeCombo.getSelectionModel().selectNext();
            return;
        }

        // Update description and default coordinates for COURAGEOUS scenarios
        boolean isCourageous = type.startsWith("S");
        boolean isCustomPosition = !isCourageous && !"Full Generic Suite".equals(type)
                && !"COURAGEOUS Full Suite (S1–S10)".equals(type);

        latField.setDisable(!isCustomPosition);
        lonField.setDisable(!isCustomPosition);
        targetCountSpinner.setDisable(!"Multi-Target".equals(type));
        weatherCombo.setDisable(!"Adverse Weather".equals(type));

        if (isCourageous) {
            ScenarioType st = parseScenarioType(type);
            if (st != null) {
                latField.setText(String.format(Locale.ENGLISH, "%.4f", st.getDefaultLat()));
                lonField.setText(String.format(Locale.ENGLISH, "%.4f", st.getDefaultLon()));
                scenarioDescLabel.setText(st.getDescription());
            }
        } else if ("COURAGEOUS Full Suite (S1–S10)".equals(type)) {
            scenarioDescLabel.setText("Run all 10 COURAGEOUS scenarios (S1–S10) sequentially.");
        } else if ("Full Generic Suite".equals(type)) {
            scenarioDescLabel.setText("Run baseline, multi-target, tracking, and weather scenarios.");
        } else {
            scenarioDescLabel.setText("");
        }
    }

    private ScenarioType parseScenarioType(String comboText) {
        if (comboText == null) return null;
        String code = comboText.split(":")[0].trim();
        try {
            return ScenarioType.fromCode(code);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    // ── Execution ───────────────────────────────────────────────────────

    private void runEvaluation() {
        logArea.clear();
        appendLog("Starting evaluation...");

        // Check for multi-DTI system
        boolean useMultiDti = multiDtiSystem != null && multiDtiSystem.getNodeCount() > 0;
        if (useMultiDti) {
            appendLog(String.format(Locale.ENGLISH, "Using Multi-DTI: %d nodes, fusion=%s",
                    multiDtiSystem.getNodeCount(), multiDtiSystem.getDetectionFusion()));
        }

        // Read EW condition (EV-01)
        String ewText = ewCombo.getValue();
        TestEnvironment.EwCondition ewCondition = TestEnvironment.EwCondition.NONE;
        if (ewText != null) {
            String ewName = ewText.split(" ")[0].trim();
            try { ewCondition = TestEnvironment.EwCondition.valueOf(ewName); }
            catch (IllegalArgumentException ignored) {}
        }
        if (ewCondition != TestEnvironment.EwCondition.NONE) {
            appendLog("EW Condition: " + ewCondition.name() + " (factor=" + ewCondition.getDefaultFactor() + ")");
        }

        final TestEnvironment.EwCondition finalEw = ewCondition;

        try {
            String type = scenarioTypeCombo.getValue();
            TestEvaluator evaluator = new TestEvaluator();

            if ("COURAGEOUS Full Suite (S1–S10)".equals(type)) {
                // Run all 10 COURAGEOUS scenarios
                appendLog("═══ COURAGEOUS Full Suite (CWA 18150) ═══\n");
                List<TestScenario> suite = scenarioGen.createCourageousTestSuite();
                List<EvaluationResult> results = useMultiDti
                        ? suite.stream().map(multiDtiSystem::execute).toList()
                        : evaluator.evaluateSuite(suite);
                int passCount = 0;
                for (int i = 0; i < suite.size(); i++) {
                    logResult(suite.get(i), results.get(i));
                    if (callback != null) {
                        callback.onScenarioEvaluated(suite.get(i), results.get(i));
                    }
                    if (results.get(i).isPassed()) passCount++;
                }
                appendLog(String.format(Locale.ENGLISH, "\n═══ Suite Complete: %d/%d scenarios passed ═══",
                        passCount, suite.size()));

            } else if ("Full Generic Suite".equals(type)) {
                double lat = Double.parseDouble(latField.getText());
                double lon = Double.parseDouble(lonField.getText());
                GeoPosition centre = new GeoPosition(lat, lon, 0);

                List<TestScenario> suite = scenarioGen.createFullTestSuite(centre);
                List<EvaluationResult> results = useMultiDti
                        ? suite.stream().map(multiDtiSystem::execute).toList()
                        : evaluator.evaluateSuite(suite);
                for (int i = 0; i < suite.size(); i++) {
                    logResult(suite.get(i), results.get(i));
                    if (callback != null) {
                        callback.onScenarioEvaluated(suite.get(i), results.get(i));
                    }
                }
                appendLog("\n=== Suite Complete: " + suite.size() + " scenarios ===");

            } else if (type.startsWith("S")) {
                // Individual COURAGEOUS scenario
                ScenarioType st = parseScenarioType(type);
                if (st == null) { appendLog("ERROR: Unknown scenario type"); return; }

                appendLog("═══ " + st + " ═══");
                TestScenario scenario = scenarioGen.createCourageousScenario(st);
                EvaluationResult result = useMultiDti
                        ? multiDtiSystem.execute(scenario) : evaluator.evaluate(scenario);
                logResult(scenario, result);
                logRequirementDetails(scenario, result);
                if (callback != null) {
                    callback.onScenarioEvaluated(scenario, result);
                }

            } else {
                // Generic scenarios
                double lat = Double.parseDouble(latField.getText());
                double lon = Double.parseDouble(lonField.getText());
                GeoPosition centre = new GeoPosition(lat, lon, 0);

                TestScenario scenario = switch (type) {
                    case "Multi-Target" ->
                            scenarioGen.createMultiTargetScenario(centre, targetCountSpinner.getValue());
                    case "Tracking Continuity" ->
                            scenarioGen.createTrackingScenario(centre);
                    case "Adverse Weather" ->
                            scenarioGen.createAdverseWeatherScenario(centre,
                                    TestEnvironment.WeatherCondition.valueOf(weatherCombo.getValue()));
                    default ->
                            scenarioGen.createSingleTargetScenario(centre);
                };

                // Apply EW condition (EV-01)
                applyEwCondition(scenario, finalEw);

                EvaluationResult result = useMultiDti
                        ? multiDtiSystem.execute(scenario) : evaluator.evaluate(scenario);
                logResult(scenario, result);
                if (callback != null) {
                    callback.onScenarioEvaluated(scenario, result);
                }
            }

        } catch (Exception ex) {
            appendLog("ERROR: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void logResult(TestScenario scenario, EvaluationResult result) {
        appendLog("\n── " + scenario.getName() + " ──");
        appendLog(String.format(Locale.ENGLISH, "  Score: %.1f / 100", result.getOverallScore()));
        appendLog(String.format(Locale.ENGLISH, "  Pd: %.2f | Continuity: %.2f | Pi: %.2f",
                result.getProbabilityOfDetection(),
                result.getTrackContinuity(),
                result.getProbabilityOfIdentification()));
        appendLog(String.format(Locale.ENGLISH, "  Verdict: %s", result.isPassed() ? "PASS ✓" : "FAIL ✗"));
        appendLog(String.format(Locale.ENGLISH, "  Reqs Passed: %d | Failed: %d",
                result.getPassedRequirements().size(),
                result.getFailedRequirements().size()));
    }

    private void logRequirementDetails(TestScenario scenario, EvaluationResult result) {
        if (!result.getPassedRequirements().isEmpty()) {
            appendLog("  Passed: " + String.join(", ", result.getPassedRequirements()));
        }
        if (!result.getFailedRequirements().isEmpty()) {
            appendLog("  Failed: " + String.join(", ", result.getFailedRequirements()));
        }
        appendLog(String.format(Locale.ENGLISH, "  Linked requirements: %d", scenario.getRequirementIds().size()));
    }

    /**
     * Apply EW condition to a scenario's environment (EV-01).
     */
    private void applyEwCondition(TestScenario scenario, TestEnvironment.EwCondition ewCondition) {
        if (ewCondition != null && ewCondition != TestEnvironment.EwCondition.NONE
                && scenario.getEnvironment() != null) {
            scenario.getEnvironment().setEwCondition(ewCondition);
        }
    }

    // ── New/Load Scenario ───────────────────────────────────────────────

    /**
     * Create a new scenario by resetting all fields to defaults.
     */
    private void newScenario() {
        scenarioTypeCombo.setValue("Single Target");
        latField.setText("38.4000");
        lonField.setText("26.8000");
        targetCountSpinner.getValueFactory().setValue(5);
        weatherCombo.setValue("Clear");
        ewCombo.setValue("None");
        logArea.clear();
        appendLog("New scenario created. Configure parameters and run evaluation.");
    }

    /**
     * Load a scenario from a JSON file.
     */
    private void loadScenario() {
        javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
        fc.setTitle("Load Scenario");
        fc.getExtensionFilters().add(
                new javafx.stage.FileChooser.ExtensionFilter("JSON Files", "*.json"));
        fc.setInitialDirectory(new java.io.File("assets/scenarios/"));
        
        java.io.File file = fc.showOpenDialog(getScene().getWindow());
        if (file != null) {
            try {
                // Extract scenario ID from filename (remove .json extension)
                String scenarioId = file.getName().replace(".json", "");
                io.github.gcng54.cuaseval.io.AssetManager assetMgr = 
                        new io.github.gcng54.cuaseval.io.AssetManager();
                TestScenario loaded = assetMgr.loadScenario(scenarioId);
                appendLog("Loaded scenario: " + loaded.getName());
                
                // Populate fields from loaded scenario
                if (loaded.getEnvironment() != null && 
                        loaded.getEnvironment().getCentrePosition() != null) {
                    latField.setText(String.format(Locale.ENGLISH, "%.4f", 
                            loaded.getEnvironment().getCentrePosition().getLatitude()));
                    lonField.setText(String.format(Locale.ENGLISH, "%.4f", 
                            loaded.getEnvironment().getCentrePosition().getLongitude()));
                }
                
                // Set scenario type from name if possible
                if (loaded.getName().startsWith("S")) {
                    String code = loaded.getName().split("\\s+")[0];
                    for (String item : scenarioTypeCombo.getItems()) {
                        if (item.startsWith(code + ":")) {
                            scenarioTypeCombo.setValue(item);
                            break;
                        }
                    }
                }
                
            } catch (Exception ex) {
                appendLog("Error loading scenario: " + ex.getMessage());
            }
        }
    }

    private Label label(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size: 11px; -fx-text-fill: #333;");
        return l;
    }
}
