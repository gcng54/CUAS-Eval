package io.github.gcng54.cuaseval.ui;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import io.github.gcng54.cuaseval.io.AssetManager;
import io.github.gcng54.cuaseval.model.GeoPosition;
import io.github.gcng54.cuaseval.model.TestEnvironment;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Environment panel — configure weather conditions and Electronic Warfare (EW) setups.
 * Provides sub-tabs for Weather and EW configuration.
 */
public class EnvironmentPanel extends VBox {

    private final TabPane subTabs;

    // ── Weather fields ──
    private final TextField envNameField;
    private final ComboBox<String> weatherCombo;
    private final ComboBox<String> timeOfDayCombo;
    private final TextField windSpeedField;
    private final TextField visibilityField;
    private final TextField temperatureField;
    private final TextField humidityField;

    // ── EW fields ──
    private final ComboBox<String> ewConditionCombo;
    private final TextField ewFactorField;
    private final TextField ewJammerLatField;
    private final TextField ewJammerLonField;
    private final CheckBox ewDirectionalCheck;
    private final ListView<String> ewBandListView;
    private final TextField ewBandField;

    // ── Environment list ──
    private final ListView<String> envListView;
    private final List<TestEnvironment> environments = new ArrayList<>();

    private final AssetManager assetManager = new AssetManager();

    public EnvironmentPanel() {
        setSpacing(8);
        setPadding(new Insets(5));

        subTabs = new TabPane();
        subTabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        // ═══ Weather Sub-Tab ═══
        VBox weatherBox = new VBox(8);
        weatherBox.setPadding(new Insets(10));

        Label weatherTitle = new Label("Weather Configuration");
        weatherTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #1a237e;");

        envNameField = new TextField("Default Environment");
        envNameField.setPromptText("Environment name");

        weatherCombo = new ComboBox<>();
        for (TestEnvironment.WeatherCondition wc : TestEnvironment.WeatherCondition.values()) {
            weatherCombo.getItems().add(wc.name());
        }
        weatherCombo.getSelectionModel().selectFirst();
        weatherCombo.setMaxWidth(Double.MAX_VALUE);

        timeOfDayCombo = new ComboBox<>();
        timeOfDayCombo.getItems().addAll("DAY", "NIGHT", "DUSK", "DAWN");
        timeOfDayCombo.getSelectionModel().selectFirst();
        timeOfDayCombo.setMaxWidth(Double.MAX_VALUE);

        windSpeedField = new TextField("5.0");
        windSpeedField.setPromptText("Wind speed (m/s)");

        visibilityField = new TextField("10000");
        visibilityField.setPromptText("Visibility (m)");

        temperatureField = new TextField("20");
        temperatureField.setPromptText("Temperature (°C)");

        humidityField = new TextField("50");
        humidityField.setPromptText("Humidity (%)");

        Button addEnvBtn = new Button("+ Add Environment");
        addEnvBtn.setMaxWidth(Double.MAX_VALUE);
        addEnvBtn.setStyle("-fx-background-color: #2e7d32; -fx-text-fill: white;");
        addEnvBtn.setOnAction(e -> addEnvironment());

        envListView = new ListView<>();
        envListView.setPrefHeight(200);
        envListView.setStyle("-fx-font-family: Consolas; -fx-font-size: 11px;");

        Button saveEnvBtn = new Button("Save Environments");
        saveEnvBtn.setMaxWidth(Double.MAX_VALUE);
        saveEnvBtn.setStyle("-fx-base: #4CAF50; -fx-text-fill: white;");
        saveEnvBtn.setOnAction(e -> saveEnvironments());

        Button loadEnvBtn = new Button("Load Environments");
        loadEnvBtn.setMaxWidth(Double.MAX_VALUE);
        loadEnvBtn.setStyle("-fx-base: #2196F3; -fx-text-fill: white;");
        loadEnvBtn.setOnAction(e -> loadEnvironments());

        HBox envSavLoad = new HBox(5, saveEnvBtn, loadEnvBtn);
        HBox.setHgrow(saveEnvBtn, Priority.ALWAYS);
        HBox.setHgrow(loadEnvBtn, Priority.ALWAYS);

        weatherBox.getChildren().addAll(
                weatherTitle, new Separator(),
                label("Environment Name:"), envNameField,
                label("Weather Condition:"), weatherCombo,
                label("Time of Day:"), timeOfDayCombo,
                label("Wind Speed (m/s):"), windSpeedField,
                label("Visibility (m):"), visibilityField,
                label("Temperature (°C):"), temperatureField,
                label("Humidity (%):"), humidityField,
                new Separator(),
                addEnvBtn,
                label("Environments:"), envListView,
                new Separator(),
                envSavLoad
        );

        // ═══ EW (Electronic Warfare) Sub-Tab ═══
        VBox ewBox = new VBox(8);
        ewBox.setPadding(new Insets(10));

        Label ewTitle = new Label("Electronic Warfare Setup");
        ewTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #1a237e;");

        ewConditionCombo = new ComboBox<>();
        for (TestEnvironment.EwCondition ew : TestEnvironment.EwCondition.values()) {
            ewConditionCombo.getItems().add(ew.name() + " (factor=" + ew.getDefaultFactor() + ")");
        }
        ewConditionCombo.getSelectionModel().selectFirst();
        ewConditionCombo.setMaxWidth(Double.MAX_VALUE);
        ewConditionCombo.setOnAction(e -> onEwChanged());

        ewFactorField = new TextField("1.0");
        ewFactorField.setPromptText("Custom EW factor (0.0–1.0)");

        ewDirectionalCheck = new CheckBox("Directional Jammer");

        ewJammerLatField = new TextField("38.4");
        ewJammerLatField.setPromptText("Jammer Latitude");
        ewJammerLonField = new TextField("26.8");
        ewJammerLonField.setPromptText("Jammer Longitude");

        ewBandField = new TextField();
        ewBandField.setPromptText("Frequency band (e.g. 2.4 GHz)");

        ewBandListView = new ListView<>();
        ewBandListView.setPrefHeight(120);

        Button addBandBtn = new Button("+ Add Band");
        addBandBtn.setStyle("-fx-background-color: #FF9800; -fx-text-fill: white;");
        addBandBtn.setOnAction(e -> {
            String band = ewBandField.getText().trim();
            if (!band.isEmpty()) {
                ewBandListView.getItems().add(band);
                ewBandField.clear();
            }
        });

        Button removeBandBtn = new Button("− Remove");
        removeBandBtn.setStyle("-fx-background-color: #c62828; -fx-text-fill: white;");
        removeBandBtn.setOnAction(e -> {
            int idx = ewBandListView.getSelectionModel().getSelectedIndex();
            if (idx >= 0) ewBandListView.getItems().remove(idx);
        });

        HBox bandBtns = new HBox(5, addBandBtn, removeBandBtn);
        ewBandListView.setStyle("-fx-font-family: Consolas; -fx-font-size: 11px;");

        // Default affected bands
        ewBandListView.getItems().addAll("2.4 GHz", "5.8 GHz", "915 MHz", "433 MHz");

        ewBox.getChildren().addAll(
                ewTitle, new Separator(),
                label("EW Condition:"), ewConditionCombo,
                label("Custom EW Factor:"), ewFactorField,
                new Separator(),
                ewDirectionalCheck,
                label("Jammer Latitude:"), ewJammerLatField,
                label("Jammer Longitude:"), ewJammerLonField,
                new Separator(),
                label("Affected Frequency Bands:"),
                label("Band:"), ewBandField,
                bandBtns,
                ewBandListView
        );

        // Add sub-tabs
        Tab weatherTab = new Tab("Weather", wrapScroll(weatherBox));
        Tab ewTab = new Tab("EW Setup", wrapScroll(ewBox));
        subTabs.getTabs().addAll(weatherTab, ewTab);

        getChildren().add(subTabs);
        VBox.setVgrow(subTabs, Priority.ALWAYS);
    }

    // ── EW Changed ──────────────────────────────────────────────────────

    private void onEwChanged() {
        String sel = ewConditionCombo.getValue();
        if (sel != null) {
            String ewName = sel.split(" ")[0].trim();
            try {
                TestEnvironment.EwCondition cond = TestEnvironment.EwCondition.valueOf(ewName);
                ewFactorField.setText(String.format(Locale.ENGLISH, "%.2f", cond.getDefaultFactor()));
            } catch (IllegalArgumentException ignored) {}
        }
    }

    // ── Environment Management ──────────────────────────────────────────

    private void addEnvironment() {
        TestEnvironment env = new TestEnvironment();
        env.setName(envNameField.getText().trim());
        env.setWeather(TestEnvironment.WeatherCondition.valueOf(weatherCombo.getValue()));
        env.setTimeOfDay(timeOfDayCombo.getValue());
        env.getParameters().put("windSpeed", windSpeedField.getText());
        env.getParameters().put("visibility", visibilityField.getText());
        env.getParameters().put("temperature", temperatureField.getText());
        env.getParameters().put("humidity", humidityField.getText());
        env.setCentrePosition(new GeoPosition(38.4, 26.8, 0));
        env.setObservationRadiusM(40000);

        // Apply EW
        String ewSel = ewConditionCombo.getValue();
        if (ewSel != null) {
            String ewName = ewSel.split(" ")[0].trim();
            try {
                env.setEwCondition(TestEnvironment.EwCondition.valueOf(ewName));
            } catch (IllegalArgumentException ignored) {}
        }
        try {
            env.setEwFactor(Double.parseDouble(ewFactorField.getText()));
        } catch (NumberFormatException ignored) {}
        if (ewDirectionalCheck.isSelected()) {
            try {
                double jLat = Double.parseDouble(ewJammerLatField.getText());
                double jLon = Double.parseDouble(ewJammerLonField.getText());
                env.setEwJammerPosition(new GeoPosition(jLat, jLon, 0));
            } catch (NumberFormatException ignored) {}
        }
        env.setEwAffectedBands(new ArrayList<>(ewBandListView.getItems()));

        environments.add(env);
        envListView.getItems().add(String.format("%s | %s | %s | EW=%s",
                env.getName(), env.getWeather(), env.getTimeOfDay(), env.getEwCondition()));
    }

    private void saveEnvironments() {
        for (TestEnvironment env : environments) {
            try {
                assetManager.saveEnvironment(env, env.getName());
            } catch (IOException ex) {
                showAlert("Save failed: " + ex.getMessage());
            }
        }
        showAlert("Saved " + environments.size() + " environments.");
    }

    private void loadEnvironments() {
        List<String> names = assetManager.listEnvironmentNames();
        environments.clear();
        envListView.getItems().clear();
        for (String name : names) {
            try {
                TestEnvironment env = assetManager.loadEnvironment(name);
                environments.add(env);
                envListView.getItems().add(String.format("%s | %s | %s | EW=%s",
                        env.getName(), env.getWeather(), env.getTimeOfDay(), env.getEwCondition()));
            } catch (IOException ex) {
                // skip
            }
        }
        if (environments.isEmpty()) showAlert("No saved environments found.");
    }

    // ── Public API ──────────────────────────────────────────────────────

    public TestEnvironment.WeatherCondition getSelectedWeather() {
        return TestEnvironment.WeatherCondition.valueOf(weatherCombo.getValue());
    }

    public TestEnvironment.EwCondition getSelectedEwCondition() {
        String ewSel = ewConditionCombo.getValue();
        if (ewSel != null) {
            String ewName = ewSel.split(" ")[0].trim();
            try { return TestEnvironment.EwCondition.valueOf(ewName); }
            catch (IllegalArgumentException ignored) {}
        }
        return TestEnvironment.EwCondition.NONE;
    }

    public List<TestEnvironment> getEnvironments() { return new ArrayList<>(environments); }

    // ── Helpers ──────────────────────────────────────────────────────────

    private ScrollPane wrapScroll(javafx.scene.Node node) {
        ScrollPane sp = new ScrollPane(node);
        sp.setFitToWidth(true);
        sp.setFitToHeight(true);
        return sp;
    }

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Environments");
        alert.setContentText(msg);
        alert.showAndWait();
    }

    private Label label(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size: 11px; -fx-text-fill: #333;");
        return l;
    }
}
