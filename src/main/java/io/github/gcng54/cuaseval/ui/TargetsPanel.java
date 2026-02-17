package io.github.gcng54.cuaseval.ui;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import io.github.gcng54.cuaseval.io.AssetManager;
import io.github.gcng54.cuaseval.model.*;
import io.github.gcng54.cuaseval.model.UasClass;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Targets panel — manage UAS targets (CUAS) and flight plan generation.
 * Provides sub-tabs for Target (CUAS) definition and Flight Plan editing.
 */
public class TargetsPanel extends VBox {

    private final TabPane subTabs;

    // ── Target definition fields ──
    private final TextField targetUidField;
    private final TextField designationField;
    private final ComboBox<String> uasTypeCombo;
    private final TextField rcsField;
    private final TextField speedField;
    private final TextField altitudeField;
    private final TextField latField;
    private final TextField lonField;
    private final CheckBox friendlyCheck;
    private final ListView<String> targetListView;
    private final List<UasTarget> targets = new ArrayList<>();

    // ── Flight plan fields ──
    private final ListView<String> flightPlanListView;
    private final TextField wpLatField;
    private final TextField wpLonField;
    private final TextField wpAltField;
    private final TextField wpSpeedField;
    private final TextField wpTimeField;
    private final ListView<String> waypointListView;
    private final List<TestScenario.FlightPlan> flightPlans = new ArrayList<>();
    private TestScenario.FlightPlan currentPlan;

    private final AssetManager assetManager = new AssetManager();

    public TargetsPanel() {
        setSpacing(8);
        setPadding(new Insets(5));

        subTabs = new TabPane();
        subTabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        // ═══ Target (CUAS) Sub-Tab ═══
        VBox targetBox = new VBox(8);
        targetBox.setPadding(new Insets(10));

        Label targetTitle = new Label("UAS Target Definition");
        targetTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #1a237e;");

        targetUidField = new TextField();
        targetUidField.setPromptText("Target UID (e.g. TGT-001)");

        designationField = new TextField();
        designationField.setPromptText("Designation (e.g. DJI Mavic 3)");

        uasTypeCombo = new ComboBox<>();
        uasTypeCombo.getItems().addAll("C0", "C1", "C2", "C3", "C4", "SPECIFIC", "UNKNOWN");
        uasTypeCombo.getSelectionModel().selectFirst();
        uasTypeCombo.setMaxWidth(Double.MAX_VALUE);

        rcsField = new TextField("0.01");
        rcsField.setPromptText("RCS (m²)");

        speedField = new TextField("15.0");
        speedField.setPromptText("Speed (m/s)");

        altitudeField = new TextField("100");
        altitudeField.setPromptText("Altitude AGL (m)");

        latField = new TextField("38.4");
        latField.setPromptText("Latitude");

        lonField = new TextField("26.8");
        lonField.setPromptText("Longitude");

        friendlyCheck = new CheckBox("Friendly (cooperative)");

        Button addTargetBtn = new Button("+ Add Target");
        addTargetBtn.setMaxWidth(Double.MAX_VALUE);
        addTargetBtn.setStyle("-fx-background-color: #2e7d32; -fx-text-fill: white;");
        addTargetBtn.setOnAction(e -> addTarget());

        Button removeTargetBtn = new Button("− Remove Selected");
        removeTargetBtn.setMaxWidth(Double.MAX_VALUE);
        removeTargetBtn.setStyle("-fx-background-color: #c62828; -fx-text-fill: white;");
        removeTargetBtn.setOnAction(e -> removeTarget());

        HBox targetBtns = new HBox(5, addTargetBtn, removeTargetBtn);
        HBox.setHgrow(addTargetBtn, Priority.ALWAYS);
        HBox.setHgrow(removeTargetBtn, Priority.ALWAYS);

        targetListView = new ListView<>();
        targetListView.setPrefHeight(200);
        targetListView.setStyle("-fx-font-family: Consolas; -fx-font-size: 11px;");

        // Save/Load
        Button saveTargetsBtn = new Button("Save Targets");
        saveTargetsBtn.setMaxWidth(Double.MAX_VALUE);
        saveTargetsBtn.setStyle("-fx-base: #4CAF50; -fx-text-fill: white;");
        saveTargetsBtn.setOnAction(e -> saveTargets());

        Button loadTargetsBtn = new Button("Load Targets");
        loadTargetsBtn.setMaxWidth(Double.MAX_VALUE);
        loadTargetsBtn.setStyle("-fx-base: #2196F3; -fx-text-fill: white;");
        loadTargetsBtn.setOnAction(e -> loadTargets());

        HBox savLoadBtns = new HBox(5, saveTargetsBtn, loadTargetsBtn);
        HBox.setHgrow(saveTargetsBtn, Priority.ALWAYS);
        HBox.setHgrow(loadTargetsBtn, Priority.ALWAYS);

        targetBox.getChildren().addAll(
                targetTitle, new Separator(),
                label("Target UID:"), targetUidField,
                label("Designation:"), designationField,
                label("UAS Type:"), uasTypeCombo,
                label("RCS (m²):"), rcsField,
                label("Speed (m/s):"), speedField,
                label("Altitude AGL (m):"), altitudeField,
                label("Position Lat:"), latField,
                label("Position Lon:"), lonField,
                friendlyCheck,
                new Separator(),
                targetBtns,
                label("Targets:"), targetListView,
                new Separator(),
                savLoadBtns
        );

        // ═══ Flight Plan Sub-Tab ═══
        VBox flightBox = new VBox(8);
        flightBox.setPadding(new Insets(10));

        Label fpTitle = new Label("Flight Plan Editor");
        fpTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #1a237e;");

        Button newPlanBtn = new Button("+ New Flight Plan");
        newPlanBtn.setMaxWidth(Double.MAX_VALUE);
        newPlanBtn.setStyle("-fx-background-color: #2e7d32; -fx-text-fill: white;");
        newPlanBtn.setOnAction(e -> newFlightPlan());

        flightPlanListView = new ListView<>();
        flightPlanListView.setPrefHeight(120);
        flightPlanListView.setStyle("-fx-font-family: Consolas; -fx-font-size: 11px;");
        flightPlanListView.getSelectionModel().selectedIndexProperty().addListener((obs, ov, nv) -> {
            int idx = nv.intValue();
            if (idx >= 0 && idx < flightPlans.size()) {
                currentPlan = flightPlans.get(idx);
                refreshWaypoints();
            }
        });

        Label wpLabel = new Label("── Waypoints ──");
        wpLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold;");

        wpLatField = new TextField("38.4");
        wpLatField.setPromptText("Waypoint Lat");
        wpLonField = new TextField("26.8");
        wpLonField.setPromptText("Waypoint Lon");
        wpAltField = new TextField("100");
        wpAltField.setPromptText("Altitude (m)");
        wpSpeedField = new TextField("15.0");
        wpSpeedField.setPromptText("Speed (m/s)");
        wpTimeField = new TextField("0");
        wpTimeField.setPromptText("Time offset (s)");

        Button addWpBtn = new Button("+ Add Waypoint");
        addWpBtn.setMaxWidth(Double.MAX_VALUE);
        addWpBtn.setStyle("-fx-background-color: #1565C0; -fx-text-fill: white;");
        addWpBtn.setOnAction(e -> addWaypoint());

        Button removeWpBtn = new Button("− Remove Waypoint");
        removeWpBtn.setMaxWidth(Double.MAX_VALUE);
        removeWpBtn.setStyle("-fx-background-color: #c62828; -fx-text-fill: white;");
        removeWpBtn.setOnAction(e -> removeWaypoint());

        HBox wpBtns = new HBox(5, addWpBtn, removeWpBtn);
        HBox.setHgrow(addWpBtn, Priority.ALWAYS);
        HBox.setHgrow(removeWpBtn, Priority.ALWAYS);

        waypointListView = new ListView<>();
        waypointListView.setPrefHeight(200);
        waypointListView.setStyle("-fx-font-family: Consolas; -fx-font-size: 11px;");

        Button savePlanBtn = new Button("Save Flight Plans");
        savePlanBtn.setMaxWidth(Double.MAX_VALUE);
        savePlanBtn.setStyle("-fx-base: #4CAF50; -fx-text-fill: white;");
        savePlanBtn.setOnAction(e -> saveFlightPlans());

        Button loadPlanBtn = new Button("Load Flight Plans");
        loadPlanBtn.setMaxWidth(Double.MAX_VALUE);
        loadPlanBtn.setStyle("-fx-base: #2196F3; -fx-text-fill: white;");
        loadPlanBtn.setOnAction(e -> loadFlightPlans());

        HBox fpSavLoad = new HBox(5, savePlanBtn, loadPlanBtn);
        HBox.setHgrow(savePlanBtn, Priority.ALWAYS);
        HBox.setHgrow(loadPlanBtn, Priority.ALWAYS);

        flightBox.getChildren().addAll(
                fpTitle, new Separator(),
                newPlanBtn,
                label("Flight Plans:"), flightPlanListView,
                new Separator(),
                wpLabel,
                label("Latitude:"), wpLatField,
                label("Longitude:"), wpLonField,
                label("Altitude (m):"), wpAltField,
                label("Speed (m/s):"), wpSpeedField,
                label("Time Offset (s):"), wpTimeField,
                wpBtns,
                label("Waypoints:"), waypointListView,
                new Separator(),
                fpSavLoad
        );

        // Add sub-tabs
        Tab targetTab = new Tab("Targets (CUAS)", wrapScroll(targetBox));
        Tab flightTab = new Tab("Flight Plans", wrapScroll(flightBox));
        subTabs.getTabs().addAll(targetTab, flightTab);

        getChildren().add(subTabs);
        VBox.setVgrow(subTabs, Priority.ALWAYS);
    }

    // ── Target Management ───────────────────────────────────────────────

    private void addTarget() {
        try {
            UasTarget target = new UasTarget();
            String uid = targetUidField.getText().trim();
            if (uid.isEmpty()) uid = "TGT-" + System.currentTimeMillis();
            // uid is auto-generated in constructor
            target.setDesignation(uid.startsWith("TGT-") ? designationField.getText().trim() : uid);
            try { target.setUasClass(UasClass.valueOf(uasTypeCombo.getValue())); }
            catch (IllegalArgumentException ignored) { target.setUasClass(UasClass.UNKNOWN); }
            target.setRcsSqm(Double.parseDouble(rcsField.getText()));
            target.setSpeedMs(Double.parseDouble(speedField.getText()));
            double lat = Double.parseDouble(latField.getText());
            double lon = Double.parseDouble(lonField.getText());
            double alt = Double.parseDouble(altitudeField.getText());
            target.setPosition(new GeoPosition(lat, lon, alt));
            target.setFriendly(friendlyCheck.isSelected());
            targets.add(target);
            targetListView.getItems().add(String.format(Locale.ENGLISH,
                    "%s [%s] %s  RCS=%.3f  %.1fm/s @ %.4f,%.4f",
                    target.getUid(), target.getUasClass(), target.getDesignation(),
                    target.getRcsSqm(), target.getSpeedMs(), lat, lon));
        } catch (NumberFormatException ex) {
            showAlert("Invalid numeric values.");
        }
    }

    private void removeTarget() {
        int idx = targetListView.getSelectionModel().getSelectedIndex();
        if (idx >= 0) {
            targets.remove(idx);
            targetListView.getItems().remove(idx);
        }
    }

    private void saveTargets() {
        for (UasTarget t : targets) {
            try {
                assetManager.saveTarget(t);
            } catch (IOException ex) {
                showAlert("Failed to save target: " + ex.getMessage());
            }
        }
        showAlert("Saved " + targets.size() + " targets to assets.");
    }

    private void loadTargets() {
        List<UasTarget> loaded = assetManager.listTargets();
        targets.clear();
        targetListView.getItems().clear();
        for (UasTarget t : loaded) {
            targets.add(t);
            GeoPosition p = t.getPosition();
            targetListView.getItems().add(String.format(Locale.ENGLISH,
                    "%s [%s] %s  RCS=%.3f  %.1fm/s @ %.4f,%.4f",
                    t.getUid(), t.getUasClass(), t.getDesignation(),
                    t.getRcsSqm(), t.getSpeedMs(),
                    p != null ? p.getLatitude() : 0, p != null ? p.getLongitude() : 0));
        }
        if (loaded.isEmpty()) showAlert("No saved targets found.");
    }

    // ── Flight Plan Management ──────────────────────────────────────────

    private void newFlightPlan() {
        String targetUid = "TGT-" + (flightPlans.size() + 1);
        if (!targets.isEmpty()) {
            int sel = targetListView.getSelectionModel().getSelectedIndex();
            if (sel >= 0) targetUid = targets.get(sel).getUid();
            else targetUid = targets.get(0).getUid();
        }
        TestScenario.FlightPlan plan = new TestScenario.FlightPlan(targetUid);
        flightPlans.add(plan);
        flightPlanListView.getItems().add("FP-" + flightPlans.size() + " → " + targetUid);
        flightPlanListView.getSelectionModel().selectLast();
        currentPlan = plan;
    }

    private void addWaypoint() {
        if (currentPlan == null) {
            showAlert("Select or create a flight plan first.");
            return;
        }
        try {
            double lat = Double.parseDouble(wpLatField.getText());
            double lon = Double.parseDouble(wpLonField.getText());
            double alt = Double.parseDouble(wpAltField.getText());
            double speed = Double.parseDouble(wpSpeedField.getText());
            double time = Double.parseDouble(wpTimeField.getText());
            GeoPosition pos = new GeoPosition(lat, lon, alt);
            TestScenario.Waypoint wp = new TestScenario.Waypoint(pos, speed, time);
            currentPlan.addWaypoint(wp);
            refreshWaypoints();
        } catch (NumberFormatException ex) {
            showAlert("Invalid waypoint values.");
        }
    }

    private void removeWaypoint() {
        if (currentPlan == null) return;
        int idx = waypointListView.getSelectionModel().getSelectedIndex();
        if (idx >= 0) {
            currentPlan.getWaypoints().remove(idx);
            refreshWaypoints();
        }
    }

    private void refreshWaypoints() {
        waypointListView.getItems().clear();
        if (currentPlan == null) return;
        int i = 0;
        for (TestScenario.Waypoint wp : currentPlan.getWaypoints()) {
            GeoPosition p = wp.getPosition();
            waypointListView.getItems().add(String.format(Locale.ENGLISH,
                    "WP%d: %.5f, %.5f  alt=%.0fm  spd=%.1fm/s  t=%.1fs",
                    ++i, p.getLatitude(), p.getLongitude(), p.getAltitudeMsl(),
                    wp.getSpeedMs(), wp.getTimeOffsetSeconds()));
        }
    }

    private void saveFlightPlans() {
        showAlert("Flight plans saved with scenario (use Scenarios tab).");
    }

    private void loadFlightPlans() {
        showAlert("Load a scenario to import flight plans.");
    }

    // ── Public API ──────────────────────────────────────────────────────

    public List<UasTarget> getTargets() { return new ArrayList<>(targets); }
    public List<TestScenario.FlightPlan> getFlightPlans() { return new ArrayList<>(flightPlans); }

    // ── Helpers ──────────────────────────────────────────────────────────

    private ScrollPane wrapScroll(javafx.scene.Node node) {
        ScrollPane sp = new ScrollPane(node);
        sp.setFitToWidth(true);
        sp.setFitToHeight(true);
        return sp;
    }

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Targets");
        alert.setContentText(msg);
        alert.showAndWait();
    }

    private Label label(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size: 11px; -fx-text-fill: #333;");
        return l;
    }
}
