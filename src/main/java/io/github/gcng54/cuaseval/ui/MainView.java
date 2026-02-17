package io.github.gcng54.cuaseval.ui;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import io.github.gcng54.cuaseval.dti.MultiDtiSystem;
import io.github.gcng54.cuaseval.model.*;
import io.github.gcng54.cuaseval.report.UmlRenderer;
import io.github.gcng54.cuaseval.requirements.RequirementsManager;
import io.github.gcng54.cuaseval.terrain.DtedReader;
import io.github.gcng54.cuaseval.terrain.TerrainMaskCalculator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Main view composing all UI panels into the primary application layout.
 * <pre>
 * ┌─────────────────────────────────────────────────────────────┐
 * │  Menu Bar                                                    │
 * ├──────────┬──────────────────────────────────────────────────┤
 * │ Maps     │                                                  │
 * │ Sensors  │            Map View (expanded)                   │
 * │ Targets  │                                                  │
 * │ Environ. │                                                  │
 * │ Scenarios│                                                  │
 * └──────────┴──────────────────────────────────────────────────┘
 * </pre>
 * <p>
 * v2.0: Main tabs = Maps, Sensors, Targets, Environments, Scenarios.
 * "Results" renamed to "Evaluation". TerrainPanel replaces DtedPanel.
 * New TargetsPanel and EnvironmentPanel added.
 * </p>
 */
public class MainView extends BorderPane {

    private static final Logger log = LoggerFactory.getLogger(MainView.class);

    private final MapView mapView;
    private final ScenarioPanel scenarioPanel;
    private final EvaluationPanel evaluationPanel;
    private final ReportPanel reportPanel;
    private final TerrainPanel terrainPanel;
    private final SensorPanel sensorPanel;
    private final TargetsPanel targetsPanel;
    private final EnvironmentPanel environmentPanel;

    // ── Constructor ─────────────────────────────────────────────────────

    public MainView() {
        mapView = new MapView();
        scenarioPanel = new ScenarioPanel();
        evaluationPanel = new EvaluationPanel();
        reportPanel = new ReportPanel();
        terrainPanel = new TerrainPanel();
        sensorPanel = new SensorPanel();
        targetsPanel = new TargetsPanel();
        environmentPanel = new EnvironmentPanel();

        // Auto-render UML diagrams to docs/uml_pic/ at startup
        autoRenderUml();

        // Wire up scenario evaluation callback
        scenarioPanel.setCallback(this::onScenarioEvaluated);

        // Wire up sensor panel callback for multi-DTI configuration
        sensorPanel.setCallback(system -> scenarioPanel.setMultiDtiSystem(system));

        // Wire up TerrainPanel callback (SRTM + obstacles)
        terrainPanel.setCallback(new TerrainPanel.TerrainCallback() {
            @Override
            public void onSrtmLoaded(File srtmDir, double centreLon, double centreLat, double radiusKm) {
                // Build a DtedReader pointing at the SRTM directory for elevation queries
                DtedReader srtmReader = new DtedReader();
                srtmReader.setSrtmDir(srtmDir);
                mapView.setTerrainData(srtmReader, centreLon, centreLat, radiusKm);

                // Compute terrain masks for all sensor sites in current scenario
                if (scenario != null && scenario.getEnvironment() != null) {
                    TerrainMaskCalculator calc = new TerrainMaskCalculator(srtmReader);
                    List<TerrainMaskCalculator.TerrainMask> masks = new ArrayList<>();
                    for (TestEnvironment.SensorSite sensor : scenario.getEnvironment().getSensorSites()) {
                        double sensorAlt = srtmReader.getElevation(
                                sensor.getPosition().getLatitude(),
                                sensor.getPosition().getLongitude());
                        if (sensorAlt == DtedReader.NO_DATA) sensorAlt = 0;
                        sensorAlt += sensor.getPosition().getAltitudeMsl(); // sensor height AGL
                        TerrainMaskCalculator.TerrainMask mask = calc.computeTerrainMask(
                                sensor.getPosition(), sensorAlt,
                                sensor.getMaxRangeM(), 360, 1);
                        if (mask != null) masks.add(mask);
                    }
                    mapView.setTerrainMasks(masks);
                }
            }

            @Override
            public void onTerrainToggle(boolean showElevation) {
                mapView.setTerrainDisplay(showElevation, showElevation);
            }

            @Override
            public void onObstaclesUpdated(List<TestEnvironment.Obstacle> obstacles) {
                if (scenario != null && scenario.getEnvironment() != null) {
                    scenario.getEnvironment().getObstacles().clear();
                    scenario.getEnvironment().getObstacles().addAll(obstacles);
                    mapView.repaint();
                }
            }
        });

        // Wire up TerrainPanel map viewport supplier
        terrainPanel.setMapViewportSupplier(() -> new double[] {
                mapView.getCentreLat(), mapView.getCentreLon(), mapView.getVisibleRadiusKm()
        });

        // Wire up MapView right-click "Add Node Here" callback (UI-03)
        mapView.setAddNodeCallback((lat, lon) -> {
            log.info("Add-node requested at ({}, {})", lat, lon);
            if (scenario != null && scenario.getEnvironment() != null) {
                // Create a default sensor site at the clicked position
                GeoPosition pos = new GeoPosition(lat, lon, 0);
                TestEnvironment.SensorSite site = new TestEnvironment.SensorSite();
                site.setSensorId("USER-" + System.currentTimeMillis());
                site.setSensorType("Manual");
                site.setPosition(pos);
                site.setMaxRangeM(5000);
                site.setAzimuthCoverageDeg(360);
                scenario.getEnvironment().getSensorSites().add(site);
                mapView.setScenario(scenario);
                log.info("Added manual sensor site at ({}, {})", lat, lon);
            }
        });

        // Wire up SensorPanel "Add to Scenario" callback (SN-01)
        sensorPanel.setAddToScenarioCallback((sensor, position) -> {
            log.info("Adding sensor {} to scenario at {}", sensor.getSensorId(), position);
            if (scenario != null && scenario.getEnvironment() != null) {
                TestEnvironment.SensorSite site = new TestEnvironment.SensorSite(sensor, position);
                scenario.getEnvironment().getSensorSites().add(site);
                mapView.setScenario(scenario);
                log.info("Sensor {} added to scenario", sensor.getSensorId());
            } else {
                log.warn("No active scenario – run an evaluation first");
            }
        });

        // Menu bar
        MenuBar menuBar = createMenuBar();

        // ── Left panel: Main tabs ──
        // Maps (Terrain sub-tab embedded), Sensors, Targets, Environments, Scenarios
        TabPane leftTabs = new TabPane();
        leftTabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        // Maps tab — contains a sub-TabPane with Terrain
        TabPane mapsSubTabs = new TabPane();
        mapsSubTabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        mapsSubTabs.getTabs().addAll(
                new Tab("Terrain", wrapScroll(terrainPanel))
        );
        Tab mapsTab = new Tab("Maps", mapsSubTabs);

        // Sensors tab (already has sub-tabs: Sensors, DTI, Fusion)
        Tab sensorTab = new Tab("Sensors", wrapScroll(sensorPanel));

        // Targets tab (has sub-tabs: Targets, Flight Plans)
        Tab targetsTab = new Tab("Targets", wrapScroll(targetsPanel));

        // Environments tab (has sub-tabs: Weather, EW)
        Tab envTab = new Tab("Environments", wrapScroll(environmentPanel));

        // Scenarios tab — contains scenario config, evaluation, and reports
        TabPane scenarioSubTabs = new TabPane();
        scenarioSubTabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        scenarioSubTabs.getTabs().addAll(
                new Tab("Scenarios", wrapScroll(scenarioPanel)),
                new Tab("Evaluation", wrapScroll(evaluationPanel)),
                new Tab("Reports", wrapScroll(reportPanel))
        );
        Tab scenariosTab = new Tab("Scenarios", scenarioSubTabs);

        leftTabs.getTabs().addAll(mapsTab, sensorTab, targetsTab, envTab, scenariosTab);

        // Main split: left tabs | map (2-pane, map gets all remaining space)
        SplitPane mainSplit = new SplitPane();
        mainSplit.getItems().addAll(
                leftTabs,
                mapView
        );
        mainSplit.setDividerPositions(0.24);

        setTop(menuBar);
        setCenter(mainSplit);
        setPadding(new Insets(0));
    }

    // Keep reference to current scenario for terrain mask computation
    private TestScenario scenario;

    // ── Callback ────────────────────────────────────────────────────────

    private void onScenarioEvaluated(TestScenario scenario, EvaluationResult result) {
        this.scenario = scenario;
        mapView.setScenario(scenario);
        mapView.setEvaluationResult(result);
        evaluationPanel.updateResults(result);
        reportPanel.setEvaluationData(scenario, result);
    }

    // ── Menu Bar ────────────────────────────────────────────────────────

    private MenuBar createMenuBar() {
        MenuBar menuBar = new MenuBar();

        // File menu
        Menu fileMenu = new Menu("File");
        MenuItem exitItem = new MenuItem("Exit");
        exitItem.setOnAction(e -> System.exit(0));
        fileMenu.getItems().add(exitItem);

        // View menu
        Menu viewMenu = new Menu("View");
        MenuItem resetZoom = new MenuItem("Reset Zoom");
        resetZoom.setOnAction(e -> mapView.repaint());
        MenuItem toggleTiles = new MenuItem("Toggle Map Tiles");
        toggleTiles.setOnAction(e -> mapView.setShowWebTiles(true));
        viewMenu.getItems().addAll(resetZoom, toggleTiles);

        // Requirements menu
        Menu reqMenu = new Menu("Requirements");
        MenuItem showReqs = new MenuItem("Show All Requirements");
        showReqs.setOnAction(e -> showRequirementsDialog());
        reqMenu.getItems().add(showReqs);

        // Help menu
        Menu helpMenu = new Menu("Help");
        MenuItem aboutItem = new MenuItem("About CUAS-Eval");
        aboutItem.setOnAction(e -> showAboutDialog());
        helpMenu.getItems().add(aboutItem);

        menuBar.getMenus().addAll(fileMenu, viewMenu, reqMenu, helpMenu);
        return menuBar;
    }

    // ── Dialogs ─────────────────────────────────────────────────────────

    private void showRequirementsDialog() {
        RequirementsManager rm = new RequirementsManager();
        StringBuilder sb = new StringBuilder();
        sb.append("CWA 18150 Requirements (").append(rm.getCount()).append(" total)\n\n");

        for (Requirement req : rm.getAll()) {
            sb.append(String.format(Locale.ENGLISH, "[%s] %s — %s\n", req.getId(), req.getName(), req.getDescription()));
        }

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("CWA 18150 Requirements");
        alert.setHeaderText("COURAGEOUS Counter-UAS Requirements");
        TextArea ta = new TextArea(sb.toString());
        ta.setEditable(false);
        ta.setWrapText(true);
        ta.setPrefSize(700, 500);
        alert.getDialogPane().setContent(ta);
        alert.showAndWait();
    }

    private void showAboutDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("About CUAS-Eval");
        alert.setHeaderText("CUAS-Eval v2.0");
        alert.setContentText(
                "Counter-UAS Evaluation Module\n" +
                "Based on CEN Workshop Agreement CWA 18150 (COURAGEOUS)\n\n" +
                "Integrated DTI Performance Evaluation Pipeline\n" +
                "Detection • Tracking • Identification\n\n" +
                "Features: OSM Map Tiles, SRTM Terrain,\n" +
                "Scenario Generation, Metrics Computation,\n" +
                "Requirement Traceability, Track2KML, HTML Reports"
        );
        alert.showAndWait();
    }

    // ── UML Auto-Render ───────────────────────────────────────────────

    private void autoRenderUml() {
        Thread renderThread = new Thread(() -> {
            try {
                File sourceDir = new File("docs/uml");
                File outputDir = new File("docs/uml_pic");
                if (!sourceDir.exists()) return;

                UmlRenderer renderer = new UmlRenderer(sourceDir, outputDir);
                var results = renderer.renderAll();
                long ok = results.stream().filter(UmlRenderer.RenderResult::isSuccess).count();
                org.slf4j.LoggerFactory.getLogger(MainView.class)
                        .info("Auto-rendered {}/{} UML diagrams to docs/uml_pic/", ok, results.size());
            } catch (Exception e) {
                org.slf4j.LoggerFactory.getLogger(MainView.class)
                        .warn("UML auto-render failed: {}", e.getMessage());
            }
        }, "uml-auto-render");
        renderThread.setDaemon(true);
        renderThread.start();
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private ScrollPane wrapScroll(javafx.scene.Node node) {
        ScrollPane sp = new ScrollPane(node);
        sp.setFitToWidth(true);
        sp.setFitToHeight(true);
        return sp;
    }
}
