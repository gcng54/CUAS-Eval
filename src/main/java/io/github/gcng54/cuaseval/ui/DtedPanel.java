package io.github.gcng54.cuaseval.ui;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import io.github.gcng54.cuaseval.model.GeoPosition;
import io.github.gcng54.cuaseval.model.TestEnvironment;
import io.github.gcng54.cuaseval.terrain.DtedReader;
import io.github.gcng54.cuaseval.terrain.SrtmDownloader;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

/**
 * DTED / SRTM control panel — download elevation data, build/load cache,
 * set boundary parameters, and configure terrain display on the map.
 * <p>
 * Provides controls for:
 * <ul>
 *   <li>Downloading SRTM 1-arc-second tiles from AWS (no auth required)</li>
 *   <li>Building DTED cache from .dt0/.dt1/.dt2/HGT files</li>
 *   <li>Loading pre-built .dtcache file</li>
 *   <li>Setting circular boundary (centre lon/lat, radius in km)</li>
 *   <li>Toggling terrain overlay on the map</li>
 * </ul>
 * </p>
 */
public class DtedPanel extends VBox {

    private final TextField centreLonField;
    private final TextField centreLatField;
    private final TextField radiusField;
    private final TextField cacheNameField;           // UI-05: named cache
    private final Button downloadSrtmButton;
    private final Button buildCacheButton;
    private final Button loadCacheButton;
    private final Button showTerrainButton;
    private final Button syncFromMapButton;           // UI-04: sync from map
    private final Label statusLabel;
    private final ProgressBar progressBar;
    private final CheckBox showElevationCheck;
    private final CheckBox showMaskCheck;
    private final ComboBox<String> sourceCombo;

    // Terrain mask parameters (TR-03)
    private final TextField maskNumAzField;
    private final TextField maskSamplesField;
    private final TextField maskAntennaHtField;

    // Obstacle generation (TR-01)
    private final Spinner<Integer> obstacleCountSpinner;
    private final ComboBox<String> obstacleTypeCombo;
    private final TextField obstacleMaxHeightField;
    private final Button generateObstaclesButton;
    private final ListView<String> obstacleListView;

    // Generated obstacles
    private final List<TestEnvironment.Obstacle> generatedObstacles = new ArrayList<>();

    private final DtedReader dtedReader = new DtedReader();

    private static final String DTED_ROOT = "D:\\PROJECTS\\Resources\\DTED";
    private static final String SRTM_DIR  = "resources/srtm";
    private static final String CACHE_DIR = "resources/dted";

    private DtedCallback callback;

    /** Supplier to query current map centre (lat, lon, radiusKm). Set by MainView. */
    private Supplier<double[]> mapViewportSupplier;

    /**
     * Callback interface for terrain data events.
     */
    public interface DtedCallback {
        void onTerrainLoaded(DtedReader reader, double centreLon, double centreLat, double radiusKm);
        void onTerrainToggle(boolean showElevation, boolean showMask);
    }

    public DtedPanel() {
        setSpacing(8);
        setPadding(new Insets(10));
        setPrefWidth(320);

        // Title
        Label title = new Label("Terrain / DTED");
        title.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1a237e;");

        // Source selector
        sourceCombo = new ComboBox<>();
        sourceCombo.getItems().addAll(
                "SRTM (Download — 30m)",
                "DTED Level 0 (Local — 900m)",
                "DTED Level 1 (Local — 90m)",
                "DTED Level 2 (Local — 30m)"
        );
        sourceCombo.getSelectionModel().selectFirst();
        sourceCombo.setMaxWidth(Double.MAX_VALUE);

        // Boundary parameters
        centreLonField = new TextField("27.1428");
        centreLonField.setPromptText("Centre Longitude");
        centreLatField = new TextField("38.4237");
        centreLatField.setPromptText("Centre Latitude");
        radiusField = new TextField("100");
        radiusField.setPromptText("Radius (km)");

        // Sync from map button (UI-04)
        syncFromMapButton = new Button("Sync from Map View");
        syncFromMapButton.setMaxWidth(Double.MAX_VALUE);
        syncFromMapButton.setStyle("-fx-base: #2196F3; -fx-text-fill: white;");
        syncFromMapButton.setOnAction(e -> syncFromMap());

        // Cache name field (UI-05)
        cacheNameField = new TextField("dted_all");
        cacheNameField.setPromptText("Cache file name");

        // Download SRTM button
        downloadSrtmButton = new Button("Download SRTM Tiles");
        downloadSrtmButton.setMaxWidth(Double.MAX_VALUE);
        downloadSrtmButton.setStyle("-fx-base: #4CAF50; -fx-text-fill: white;");
        downloadSrtmButton.setOnAction(e -> downloadSrtm());

        // Build cache button
        buildCacheButton = new Button("Build Elevation Cache");
        buildCacheButton.setMaxWidth(Double.MAX_VALUE);
        buildCacheButton.setOnAction(e -> buildCache());

        // Load cache button
        loadCacheButton = new Button("Load Elevation Cache");
        loadCacheButton.setMaxWidth(Double.MAX_VALUE);
        loadCacheButton.setOnAction(e -> loadCache());

        // Show terrain button
        showTerrainButton = new Button("Show Terrain on Map");
        showTerrainButton.setMaxWidth(Double.MAX_VALUE);
        showTerrainButton.setDisable(true);
        showTerrainButton.setOnAction(e -> showTerrain());

        // Checkboxes
        showElevationCheck = new CheckBox("Show Elevation Colours");
        showElevationCheck.setSelected(true);
        showElevationCheck.setOnAction(e -> toggleOverlays());

        showMaskCheck = new CheckBox("Show Terrain Mask");
        showMaskCheck.setSelected(false);
        showMaskCheck.setOnAction(e -> toggleOverlays());

        // ── Terrain Mask Parameters (TR-03) ─────────────────────────────
        maskNumAzField = new TextField("72");
        maskNumAzField.setPromptText("Number of azimuths (e.g. 72 = 5° steps)");
        maskSamplesField = new TextField("100");
        maskSamplesField.setPromptText("Samples per radial profile");
        maskAntennaHtField = new TextField("10");
        maskAntennaHtField.setPromptText("Antenna height AGL (m)");

        Label maskInfo = new Label(
                "Terrain Mask: Line-of-sight analysis from sensor\n"
              + "positions along radial profiles. Masked angles\n"
              + "indicate terrain blocking detection coverage.\n"
              + "Earth curvature correction: d²/2R subtracted.");
        maskInfo.setWrapText(true);
        maskInfo.setStyle("-fx-text-fill: #666; -fx-font-size: 10px;");

        // ── Obstacle Generation (TR-01) ─────────────────────────────────
        obstacleTypeCombo = new ComboBox<>();
        for (TestEnvironment.ObstacleType t : TestEnvironment.ObstacleType.values()) {
            obstacleTypeCombo.getItems().add(t.name());
        }
        obstacleTypeCombo.getSelectionModel().selectFirst();
        obstacleTypeCombo.setMaxWidth(Double.MAX_VALUE);

        obstacleCountSpinner = new Spinner<>(1, 50, 5);
        obstacleCountSpinner.setEditable(true);
        obstacleCountSpinner.setMaxWidth(Double.MAX_VALUE);

        obstacleMaxHeightField = new TextField("30");
        obstacleMaxHeightField.setPromptText("Max height (m)");

        generateObstaclesButton = new Button("Generate Obstacles over Terrain");
        generateObstaclesButton.setMaxWidth(Double.MAX_VALUE);
        generateObstaclesButton.setStyle("-fx-base: #FF9800; -fx-text-fill: white;");
        generateObstaclesButton.setOnAction(e -> generateObstacles());

        obstacleListView = new ListView<>();
        obstacleListView.setPrefHeight(100);

        // Progress
        progressBar = new ProgressBar(0);
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.setVisible(false);

        // Status
        statusLabel = new Label("Select source and download or load cache.");
        statusLabel.setWrapText(true);
        statusLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 11px;");

        // Source info label
        Label sourceInfo = new Label(
                "SRTM: AWS public (no login), ~30m res\n"
              + "DTED: local files in " + DTED_ROOT);
        sourceInfo.setWrapText(true);
        sourceInfo.setStyle("-fx-text-fill: #999; -fx-font-size: 10px;");

        // Layout
        getChildren().addAll(
                title,
                new Separator(),
                label("Elevation Source:"), sourceCombo,
                new Separator(),
                label("Centre Longitude:"), centreLonField,
                label("Centre Latitude:"), centreLatField,
                label("Radius (km):"), radiusField,
                syncFromMapButton,
                new Separator(),
                label("Cache Name:"), cacheNameField,
                downloadSrtmButton,
                buildCacheButton,
                loadCacheButton,
                showTerrainButton,
                new Separator(),
                showElevationCheck,
                showMaskCheck,
                new Separator(),
                label("── Terrain Mask Parameters ──"),
                label("Azimuths:"), maskNumAzField,
                label("Samples/Profile:"), maskSamplesField,
                label("Antenna Height AGL (m):"), maskAntennaHtField,
                maskInfo,
                new Separator(),
                label("── Obstacle Generation ──"),
                label("Type:"), obstacleTypeCombo,
                label("Count:"), obstacleCountSpinner,
                label("Max Height (m):"), obstacleMaxHeightField,
                generateObstaclesButton,
                obstacleListView,
                new Separator(),
                progressBar,
                statusLabel,
                sourceInfo
        );
    }

    public void setCallback(DtedCallback callback) {
        this.callback = callback;
    }

    /** Set supplier for map viewport data: [lat, lon, radiusKm]. */
    public void setMapViewportSupplier(Supplier<double[]> supplier) {
        this.mapViewportSupplier = supplier;
    }

    public DtedReader getDtedReader() {
        return dtedReader;
    }

    /** Get the terrain mask parameters as [numAzimuths, samplesPerProfile, antennaHeightM]. */
    public int[] getMaskParams() {
        try {
            int az = Integer.parseInt(maskNumAzField.getText().trim());
            int samples = Integer.parseInt(maskSamplesField.getText().trim());
            return new int[] { az, samples };
        } catch (NumberFormatException e) {
            return new int[] { 72, 100 };
        }
    }

    /** Get the antenna height AGL setting. */
    public double getAntennaHeightM() {
        try {
            return Double.parseDouble(maskAntennaHtField.getText().trim());
        } catch (NumberFormatException e) {
            return 10.0;
        }
    }

    /** Get generated obstacles (TR-01). */
    public List<TestEnvironment.Obstacle> getGeneratedObstacles() {
        return new ArrayList<>(generatedObstacles);
    }

    // ── Sync from Map (UI-04) ───────────────────────────────────────────

    private void syncFromMap() {
        if (mapViewportSupplier == null) {
            statusLabel.setText("Map not connected.");
            return;
        }
        double[] viewport = mapViewportSupplier.get();
        centreLatField.setText(String.format(Locale.ENGLISH, "%.6f", viewport[0]));
        centreLonField.setText(String.format(Locale.ENGLISH, "%.6f", viewport[1]));
        radiusField.setText(String.format(Locale.ENGLISH, "%.1f", viewport[2]));
        statusLabel.setText("Synced centre/radius from map viewport.");
    }

    // ── Obstacle Generation (TR-01) ─────────────────────────────────────

    private void generateObstacles() {
        double lat, lon, radius;
        try {
            lon = Double.parseDouble(centreLonField.getText());
            lat = Double.parseDouble(centreLatField.getText());
            radius = Double.parseDouble(radiusField.getText());
        } catch (NumberFormatException ex) {
            statusLabel.setText("Invalid coordinates or radius.");
            return;
        }

        int count = obstacleCountSpinner.getValue();
        double maxHeight;
        try {
            maxHeight = Double.parseDouble(obstacleMaxHeightField.getText());
        } catch (NumberFormatException ex) {
            maxHeight = 30;
        }
        String typeName = obstacleTypeCombo.getSelectionModel().getSelectedItem();
        TestEnvironment.ObstacleType type = TestEnvironment.ObstacleType.valueOf(typeName);

        generatedObstacles.clear();
        obstacleListView.getItems().clear();

        java.util.Random rng = new java.util.Random();
        double radiusM = radius * 1000;
        double degPerM = 1.0 / 111_320.0;

        for (int i = 0; i < count; i++) {
            double angle = rng.nextDouble() * 2 * Math.PI;
            double r = radiusM * Math.sqrt(rng.nextDouble());
            double dLat = r * Math.cos(angle) * degPerM;
            double dLon = r * Math.sin(angle) * degPerM / Math.cos(Math.toRadians(lat));

            double obsLat = lat + dLat;
            double obsLon = lon + dLon;
            double height = 5 + rng.nextDouble() * (maxHeight - 5);
            double width = 5 + rng.nextDouble() * 20;
            double length = 5 + rng.nextDouble() * 20;

            // Use terrain elevation if available
            double elev = 0;
            if (dtedReader != null && dtedReader.isLoaded()) {
                double e = dtedReader.getElevation(obsLat, obsLon);
                if (e != DtedReader.NO_DATA) elev = e;
            }

            TestEnvironment.Obstacle obs = new TestEnvironment.Obstacle(
                    String.format("OBS-%s-%02d", type.name().substring(0, 3), i + 1),
                    new GeoPosition(obsLat, obsLon, elev),
                    width, height, length, type);
            generatedObstacles.add(obs);
            obstacleListView.getItems().add(obs.toString());
        }

        statusLabel.setText(String.format(Locale.ENGLISH,
                "Generated %d %s obstacles over terrain.", count, type.name()));
    }

    // ── Download SRTM tiles ─────────────────────────────────────────────

    private void downloadSrtm() {
        double lat, lon, radius;
        try {
            lon = Double.parseDouble(centreLonField.getText());
            lat = Double.parseDouble(centreLatField.getText());
            radius = Double.parseDouble(radiusField.getText());
        } catch (NumberFormatException ex) {
            statusLabel.setText("Invalid coordinates or radius.");
            return;
        }

        File srtmDirFile = new File(SRTM_DIR);
        SrtmDownloader downloader = new SrtmDownloader(srtmDirFile);

        List<String> tiles = downloader.computeTilesInBoundary(lat, lon, radius);
        statusLabel.setText(String.format(Locale.ENGLISH,
                "Downloading %d SRTM tiles...", tiles.size()));
        progressBar.setVisible(true);
        progressBar.setProgress(0);
        downloadSrtmButton.setDisable(true);

        Thread thread = new Thread(() -> {
            downloader.downloadForBoundary(lat, lon, radius,
                    new SrtmDownloader.ProgressCallback() {
                        @Override
                        public void onProgress(String tileName, int tileIndex, int totalTiles,
                                               long bytesReceived, long totalBytes) {
                            Platform.runLater(() -> {
                                double progress = (tileIndex + (totalBytes > 0 ? (double) bytesReceived / totalBytes : 0.5))
                                        / totalTiles;
                                progressBar.setProgress(progress);
                                statusLabel.setText(String.format(Locale.ENGLISH,
                                        "Downloading %s (%d/%d)...", tileName, tileIndex + 1, totalTiles));
                            });
                        }

                        @Override
                        public void onComplete(int downloaded, int skipped, int failed) {
                            Platform.runLater(() -> {
                                progressBar.setProgress(1);
                                progressBar.setVisible(false);
                                downloadSrtmButton.setDisable(false);
                                statusLabel.setText(String.format(Locale.ENGLISH,
                                        "SRTM: %d downloaded, %d cached, %d failed",
                                        downloaded, skipped, failed));
                                // Set SRTM dir on DtedReader for high-res queries
                                dtedReader.setSrtmDir(srtmDirFile);
                            });
                        }
                    });
        }, "srtm-downloader");
        thread.setDaemon(true);
        thread.start();
    }

    // ── Build cache ─────────────────────────────────────────────────────

    private void buildCache() {
        statusLabel.setText("Building elevation cache...");
        progressBar.setVisible(true);
        progressBar.setProgress(-1);
        buildCacheButton.setDisable(true);

        String cacheName = cacheNameField.getText().trim();
        if (cacheName.isEmpty()) cacheName = "dted_all";

        final String finalCacheName = cacheName;
        Thread thread = new Thread(() -> {
            File srcDir = getSourceDirectory();
            File cacheFile = new File(CACHE_DIR + "/" + finalCacheName + ".dtcache");
            cacheFile.getParentFile().mkdirs();

            dtedReader.buildCache(srcDir, cacheFile);

            Platform.runLater(() -> {
                progressBar.setProgress(1);
                progressBar.setVisible(false);
                buildCacheButton.setDisable(false);
                statusLabel.setText(String.format(Locale.ENGLISH,
                        "Cache built: %s (%.1f MB)",
                        cacheFile.getName(), cacheFile.length() / (1024.0 * 1024.0)));
                loadCacheFromFile(cacheFile);
            });
        }, "dted-cache-builder");
        thread.setDaemon(true);
        thread.start();
    }

    // ── Load cache ──────────────────────────────────────────────────────

    private void loadCache() {
        String cacheName = cacheNameField.getText().trim();
        if (cacheName.isEmpty()) cacheName = "dted_all";
        File cacheFile = new File(CACHE_DIR + "/" + cacheName + ".dtcache");
        loadCacheFromFile(cacheFile);
    }

    private void loadCacheFromFile(File cacheFile) {
        if (!cacheFile.exists()) {
            statusLabel.setText("Cache file not found: " + cacheFile.getName());
            return;
        }

        statusLabel.setText("Loading elevation cache...");
        progressBar.setVisible(true);
        progressBar.setProgress(-1);
        loadCacheButton.setDisable(true);

        Thread thread = new Thread(() -> {
            boolean ok = dtedReader.loadCache(cacheFile);
            Platform.runLater(() -> {
                progressBar.setProgress(1);
                progressBar.setVisible(false);
                loadCacheButton.setDisable(false);

                if (ok) {
                    double[] bounds = dtedReader.getBounds();
                    statusLabel.setText(String.format(Locale.ENGLISH,
                            "Loaded: lon [%.0f°, %.0f°], lat [%.0f°, %.0f°]  %d posts",
                            bounds[0], bounds[2], bounds[1], bounds[3],
                            dtedReader.getTilePosts()));
                    showTerrainButton.setDisable(false);

                    // Auto-set SRTM dir if available
                    File srtmDirFile = new File(SRTM_DIR);
                    if (srtmDirFile.exists()) {
                        dtedReader.setSrtmDir(srtmDirFile);
                    }
                } else {
                    statusLabel.setText("Failed to load cache.");
                }
            });
        }, "dted-cache-loader");
        thread.setDaemon(true);
        thread.start();
    }

    // ── Show terrain ────────────────────────────────────────────────────

    private void showTerrain() {
        if (!dtedReader.isLoaded()) {
            statusLabel.setText("Load cache first.");
            return;
        }

        try {
            double lon = Double.parseDouble(centreLonField.getText());
            double lat = Double.parseDouble(centreLatField.getText());
            double radius = Double.parseDouble(radiusField.getText());

            if (callback != null) {
                callback.onTerrainLoaded(dtedReader, lon, lat, radius);
            }

            statusLabel.setText(String.format(Locale.ENGLISH,
                    "Terrain shown: centre (%.4f, %.4f), r=%.0f km", lon, lat, radius));

        } catch (NumberFormatException ex) {
            statusLabel.setText("Invalid coordinates or radius.");
        }
    }

    private void toggleOverlays() {
        if (callback != null) {
            callback.onTerrainToggle(showElevationCheck.isSelected(), showMaskCheck.isSelected());
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    /**
     * Get the source directory based on the selected combo item.
     */
    private File getSourceDirectory() {
        int idx = sourceCombo.getSelectionModel().getSelectedIndex();
        if (idx == 0) {
            return new File(SRTM_DIR); // SRTM downloads
        }
        return new File(DTED_ROOT); // DTED local
    }

    private Label label(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size: 11px; -fx-text-fill: #333;");
        return l;
    }
}
