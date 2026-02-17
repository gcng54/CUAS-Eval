package io.github.gcng54.cuaseval.ui;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import io.github.gcng54.cuaseval.io.AssetManager;
import io.github.gcng54.cuaseval.model.GeoPosition;
import io.github.gcng54.cuaseval.model.TestEnvironment;
import io.github.gcng54.cuaseval.terrain.SrtmDownloader;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Terrain panel — SRTM elevation data and Obstacles management.
 * DTED has been removed; only SRTM 1-arc-second tiles are supported.
 * <p>
 * Provides sub-tabs:
 * <ul>
 *   <li><b>SRTM:</b> Download, save, and load SRTM elevation tiles</li>
 *   <li><b>Obstacles:</b> Add, edit, remove, save, load obstacle definitions</li>
 * </ul>
 */
public class TerrainPanel extends VBox {

    private final TabPane subTabs;

    // ── SRTM fields ──
    private final TextField centreLonField;
    private final TextField centreLatField;
    private final TextField radiusField;
    private final Button downloadSrtmButton;
    private final Button loadSrtmButton;
    private final Button saveSrtmButton;
    private final Label statusLabel;
    private final ProgressBar progressBar;
    private final CheckBox showElevationCheck;

    // ── Obstacle fields ──
    private final TextField obsDescField;
    private final ComboBox<String> obsTypeCombo;
    private final TextField obsLatField;
    private final TextField obsLonField;
    private final TextField obsWidthField;
    private final TextField obsHeightField;
    private final TextField obsLengthField;
    private final ListView<String> obstacleListView;
    private final List<TestEnvironment.Obstacle> obstacles = new ArrayList<>();

    private static final String SRTM_DIR = "resources/srtm";

    private TerrainCallback callback;
    private Supplier<double[]> mapViewportSupplier;
    private final AssetManager assetManager = new AssetManager();

    /**
     * Callback interface for terrain events.
     */
    public interface TerrainCallback {
        void onSrtmLoaded(File srtmDir, double centreLon, double centreLat, double radiusKm);
        void onTerrainToggle(boolean showElevation);
        void onObstaclesUpdated(List<TestEnvironment.Obstacle> obstacles);
    }

    public TerrainPanel() {
        setSpacing(8);
        setPadding(new Insets(5));

        subTabs = new TabPane();
        subTabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        // ═══ SRTM Sub-Tab ═══
        VBox srtmBox = new VBox(8);
        srtmBox.setPadding(new Insets(10));

        Label srtmTitle = new Label("SRTM Elevation Data");
        srtmTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #1a237e;");

        centreLonField = new TextField("26.8");
        centreLonField.setPromptText("Centre Longitude");
        centreLatField = new TextField("38.4");
        centreLatField.setPromptText("Centre Latitude");
        radiusField = new TextField("40");
        radiusField.setPromptText("Radius (km)");

        Button syncFromMapButton = new Button("Sync from Map View");
        syncFromMapButton.setMaxWidth(Double.MAX_VALUE);
        syncFromMapButton.setStyle("-fx-base: #2196F3; -fx-text-fill: white;");
        syncFromMapButton.setOnAction(e -> syncFromMap());

        downloadSrtmButton = new Button("Download SRTM Tiles");
        downloadSrtmButton.setMaxWidth(Double.MAX_VALUE);
        downloadSrtmButton.setStyle("-fx-base: #4CAF50; -fx-text-fill: white;");
        downloadSrtmButton.setOnAction(e -> downloadSrtm());

        saveSrtmButton = new Button("Save SRTM Data");
        saveSrtmButton.setMaxWidth(Double.MAX_VALUE);
        saveSrtmButton.setOnAction(e -> saveSrtmData());

        loadSrtmButton = new Button("Load SRTM Data");
        loadSrtmButton.setMaxWidth(Double.MAX_VALUE);
        loadSrtmButton.setOnAction(e -> loadSrtmData());

        HBox srtmSavLoad = new HBox(5, saveSrtmButton, loadSrtmButton);
        HBox.setHgrow(saveSrtmButton, Priority.ALWAYS);
        HBox.setHgrow(loadSrtmButton, Priority.ALWAYS);

        showElevationCheck = new CheckBox("Show Elevation Colours");
        showElevationCheck.setSelected(true);
        showElevationCheck.setOnAction(e -> {
            if (callback != null) callback.onTerrainToggle(showElevationCheck.isSelected());
        });

        progressBar = new ProgressBar(0);
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.setVisible(false);

        statusLabel = new Label("Download or load SRTM tiles for elevation data.");
        statusLabel.setWrapText(true);
        statusLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 11px;");

        Label srtmInfo = new Label(
                "SRTM: AWS public (no login), ~30m resolution\n"
              + "Tiles: resources/srtm/");
        srtmInfo.setWrapText(true);
        srtmInfo.setStyle("-fx-text-fill: #999; -fx-font-size: 10px;");

        // List existing SRTM tiles
        ListView<String> srtmTileList = new ListView<>();
        srtmTileList.setPrefHeight(150);
        srtmTileList.setStyle("-fx-font-family: Consolas; -fx-font-size: 11px;");
        refreshSrtmTiles(srtmTileList);

        Button refreshTilesBtn = new Button("Refresh Tile List");
        refreshTilesBtn.setMaxWidth(Double.MAX_VALUE);
        refreshTilesBtn.setOnAction(e -> refreshSrtmTiles(srtmTileList));

        srtmBox.getChildren().addAll(
                srtmTitle, new Separator(),
                label("Centre Longitude:"), centreLonField,
                label("Centre Latitude:"), centreLatField,
                label("Radius (km):"), radiusField,
                syncFromMapButton,
                new Separator(),
                downloadSrtmButton,
                srtmSavLoad,
                new Separator(),
                showElevationCheck,
                new Separator(),
                label("Available SRTM Tiles:"), srtmTileList,
                refreshTilesBtn,
                new Separator(),
                progressBar,
                statusLabel,
                srtmInfo
        );

        // ═══ Obstacles Sub-Tab ═══
        VBox obsBox = new VBox(8);
        obsBox.setPadding(new Insets(10));

        Label obsTitle = new Label("Obstacle Definitions");
        obsTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #1a237e;");

        obsDescField = new TextField();
        obsDescField.setPromptText("Description (e.g. Building A)");

        obsTypeCombo = new ComboBox<>();
        for (TestEnvironment.ObstacleType t : TestEnvironment.ObstacleType.values()) {
            obsTypeCombo.getItems().add(t.name());
        }
        obsTypeCombo.getSelectionModel().selectFirst();
        obsTypeCombo.setMaxWidth(Double.MAX_VALUE);

        obsLatField = new TextField("38.4");
        obsLatField.setPromptText("Latitude");
        obsLonField = new TextField("26.8");
        obsLonField.setPromptText("Longitude");
        obsWidthField = new TextField("20");
        obsWidthField.setPromptText("Width (m)");
        obsHeightField = new TextField("15");
        obsHeightField.setPromptText("Height (m)");
        obsLengthField = new TextField("20");
        obsLengthField.setPromptText("Length (m)");

        Button addObsBtn = new Button("+ Add Obstacle");
        addObsBtn.setMaxWidth(Double.MAX_VALUE);
        addObsBtn.setStyle("-fx-background-color: #2e7d32; -fx-text-fill: white;");
        addObsBtn.setOnAction(e -> addObstacle());

        Button removeObsBtn = new Button("− Remove Selected");
        removeObsBtn.setMaxWidth(Double.MAX_VALUE);
        removeObsBtn.setStyle("-fx-background-color: #c62828; -fx-text-fill: white;");
        removeObsBtn.setOnAction(e -> removeObstacle());

        Button editObsBtn = new Button("Edit Selected");
        editObsBtn.setMaxWidth(Double.MAX_VALUE);
        editObsBtn.setStyle("-fx-base: #FF9800; -fx-text-fill: white;");
        editObsBtn.setOnAction(e -> editObstacle());

        HBox obsBtns = new HBox(5, addObsBtn, editObsBtn, removeObsBtn);
        HBox.setHgrow(addObsBtn, Priority.ALWAYS);
        HBox.setHgrow(editObsBtn, Priority.ALWAYS);
        HBox.setHgrow(removeObsBtn, Priority.ALWAYS);

        obstacleListView = new ListView<>();
        obstacleListView.setPrefHeight(250);
        obstacleListView.setStyle("-fx-font-family: Consolas; -fx-font-size: 11px;");
        obstacleListView.getSelectionModel().selectedIndexProperty().addListener((obs, ov, nv) -> {
            int idx = nv.intValue();
            if (idx >= 0 && idx < obstacles.size()) {
                TestEnvironment.Obstacle ob = obstacles.get(idx);
                obsDescField.setText(ob.getDescription());
                obsTypeCombo.setValue(ob.getObstacleType().name());
                obsLatField.setText(String.format(Locale.ENGLISH, "%.6f", ob.getPosition().getLatitude()));
                obsLonField.setText(String.format(Locale.ENGLISH, "%.6f", ob.getPosition().getLongitude()));
                obsWidthField.setText(String.format(Locale.ENGLISH, "%.1f", ob.getWidthM()));
                obsHeightField.setText(String.format(Locale.ENGLISH, "%.1f", ob.getHeightM()));
                obsLengthField.setText(String.format(Locale.ENGLISH, "%.1f", ob.getLengthM()));
            }
        });

        Button saveObsBtn = new Button("Save Obstacles");
        saveObsBtn.setMaxWidth(Double.MAX_VALUE);
        saveObsBtn.setStyle("-fx-base: #4CAF50; -fx-text-fill: white;");
        saveObsBtn.setOnAction(e -> saveObstacles());

        Button loadObsBtn = new Button("Load Obstacles");
        loadObsBtn.setMaxWidth(Double.MAX_VALUE);
        loadObsBtn.setStyle("-fx-base: #2196F3; -fx-text-fill: white;");
        loadObsBtn.setOnAction(e -> loadObstacles());

        HBox obsSavLoad = new HBox(5, saveObsBtn, loadObsBtn);
        HBox.setHgrow(saveObsBtn, Priority.ALWAYS);
        HBox.setHgrow(loadObsBtn, Priority.ALWAYS);

        obsBox.getChildren().addAll(
                obsTitle, new Separator(),
                label("Description:"), obsDescField,
                label("Type:"), obsTypeCombo,
                label("Latitude:"), obsLatField,
                label("Longitude:"), obsLonField,
                label("Width (m):"), obsWidthField,
                label("Height (m):"), obsHeightField,
                label("Length (m):"), obsLengthField,
                new Separator(),
                obsBtns,
                label("Obstacles:"), obstacleListView,
                new Separator(),
                obsSavLoad
        );

        // Add sub-tabs
        Tab srtmTab = new Tab("SRTM", wrapScroll(srtmBox));
        Tab obsTab = new Tab("Obstacles", wrapScroll(obsBox));
        subTabs.getTabs().addAll(srtmTab, obsTab);

        getChildren().add(subTabs);
        VBox.setVgrow(subTabs, Priority.ALWAYS);
    }

    // ── API ─────────────────────────────────────────────────────────────

    public void setCallback(TerrainCallback callback) { this.callback = callback; }
    public void setMapViewportSupplier(Supplier<double[]> supplier) { this.mapViewportSupplier = supplier; }
    public List<TestEnvironment.Obstacle> getObstacles() { return new ArrayList<>(obstacles); }
    public boolean isShowElevation() { return showElevationCheck.isSelected(); }

    // ── Sync from Map ───────────────────────────────────────────────────

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

    // ── SRTM Download ───────────────────────────────────────────────────

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
                                // Notify map
                                if (callback != null) {
                                    callback.onSrtmLoaded(srtmDirFile,
                                            Double.parseDouble(centreLonField.getText()),
                                            Double.parseDouble(centreLatField.getText()),
                                            Double.parseDouble(radiusField.getText()));
                                }
                            });
                        }
                    });
        }, "srtm-downloader");
        thread.setDaemon(true);
        thread.start();
    }

    private void saveSrtmData() {
        // SRTM .hgt files are already saved in resources/srtm
        statusLabel.setText("SRTM tiles are stored in resources/srtm/.");
    }

    private void loadSrtmData() {
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
        if (!srtmDirFile.exists() || srtmDirFile.listFiles() == null ||
                srtmDirFile.listFiles().length == 0) {
            statusLabel.setText("No SRTM tiles found. Download first.");
            return;
        }

        if (callback != null) {
            callback.onSrtmLoaded(srtmDirFile, lon, lat, radius);
        }
        statusLabel.setText(String.format(Locale.ENGLISH,
                "SRTM loaded for centre (%.4f, %.4f) r=%.0fkm", lon, lat, radius));
    }

    private void refreshSrtmTiles(ListView<String> listView) {
        listView.getItems().clear();
        File srtmDir = new File(SRTM_DIR);
        if (srtmDir.exists()) {
            try (Stream<Path> files = Files.list(srtmDir.toPath())) {
                files.filter(f -> f.toString().endsWith(".hgt"))
                     .forEach(f -> {
                         try {
                             long size = Files.size(f);
                             listView.getItems().add(String.format("%s (%.1f MB)",
                                     f.getFileName(), size / (1024.0 * 1024.0)));
                         } catch (IOException ignored) {
                             listView.getItems().add(f.getFileName().toString());
                         }
                     });
            } catch (IOException ignored) {}
        }
        if (listView.getItems().isEmpty()) {
            listView.getItems().add("(no tiles — download first)");
        }
    }

    // ── Obstacle Management ─────────────────────────────────────────────

    private void addObstacle() {
        try {
            String desc = obsDescField.getText().trim();
            if (desc.isEmpty()) desc = "OBS-" + (obstacles.size() + 1);
            double lat = Double.parseDouble(obsLatField.getText());
            double lon = Double.parseDouble(obsLonField.getText());
            double width = Double.parseDouble(obsWidthField.getText());
            double height = Double.parseDouble(obsHeightField.getText());
            double length = Double.parseDouble(obsLengthField.getText());
            TestEnvironment.ObstacleType type = TestEnvironment.ObstacleType.valueOf(obsTypeCombo.getValue());

            TestEnvironment.Obstacle obs = new TestEnvironment.Obstacle(
                    desc, new GeoPosition(lat, lon, 0), width, height, length, type);
            obstacles.add(obs);
            refreshObstacleList();
            if (callback != null) callback.onObstaclesUpdated(obstacles);
        } catch (NumberFormatException ex) {
            showAlert("Invalid numeric values.");
        }
    }

    private void editObstacle() {
        int idx = obstacleListView.getSelectionModel().getSelectedIndex();
        if (idx < 0 || idx >= obstacles.size()) return;
        try {
            TestEnvironment.Obstacle obs = obstacles.get(idx);
            obs.setDescription(obsDescField.getText().trim());
            obs.setObstacleType(TestEnvironment.ObstacleType.valueOf(obsTypeCombo.getValue()));
            obs.setPosition(new GeoPosition(
                    Double.parseDouble(obsLatField.getText()),
                    Double.parseDouble(obsLonField.getText()), 0));
            obs.setWidthM(Double.parseDouble(obsWidthField.getText()));
            obs.setHeightM(Double.parseDouble(obsHeightField.getText()));
            obs.setLengthM(Double.parseDouble(obsLengthField.getText()));
            refreshObstacleList();
            if (callback != null) callback.onObstaclesUpdated(obstacles);
        } catch (NumberFormatException ex) {
            showAlert("Invalid numeric values.");
        }
    }

    private void removeObstacle() {
        int idx = obstacleListView.getSelectionModel().getSelectedIndex();
        if (idx >= 0) {
            obstacles.remove(idx);
            refreshObstacleList();
            if (callback != null) callback.onObstaclesUpdated(obstacles);
        }
    }

    private void refreshObstacleList() {
        obstacleListView.getItems().clear();
        for (TestEnvironment.Obstacle obs : obstacles) {
            obstacleListView.getItems().add(obs.toString());
        }
    }

    private void saveObstacles() {
        // Save obstacles as a JSON file via file chooser
        javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
        fc.setTitle("Save Obstacles");
        fc.setInitialDirectory(new File("assets"));
        fc.setInitialFileName("obstacles.json");
        fc.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("JSON", "*.json"));
        javafx.stage.Stage stage = (javafx.stage.Stage) getScene().getWindow();
        File file = fc.showSaveDialog(stage);
        if (file != null) {
            try {
                assetManager.getMapper().writeValue(file, obstacles);
                showAlert("Saved " + obstacles.size() + " obstacles.");
            } catch (IOException ex) {
                showAlert("Save failed: " + ex.getMessage());
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void loadObstacles() {
        javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
        fc.setTitle("Load Obstacles");
        fc.setInitialDirectory(new File("assets"));
        fc.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("JSON", "*.json"));
        javafx.stage.Stage stage = (javafx.stage.Stage) getScene().getWindow();
        File file = fc.showOpenDialog(stage);
        if (file != null) {
            try {
                List<TestEnvironment.Obstacle> loaded = assetManager.getMapper().readValue(file,
                        assetManager.getMapper().getTypeFactory().constructCollectionType(
                                List.class, TestEnvironment.Obstacle.class));
                obstacles.clear();
                obstacles.addAll(loaded);
                refreshObstacleList();
                if (callback != null) callback.onObstaclesUpdated(obstacles);
                showAlert("Loaded " + obstacles.size() + " obstacles.");
            } catch (IOException ex) {
                showAlert("Load failed: " + ex.getMessage());
            }
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private ScrollPane wrapScroll(javafx.scene.Node node) {
        ScrollPane sp = new ScrollPane(node);
        sp.setFitToWidth(true);
        sp.setFitToHeight(true);
        return sp;
    }

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Terrain");
        alert.setContentText(msg);
        alert.showAndWait();
    }

    private Label label(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size: 11px; -fx-text-fill: #333;");
        return l;
    }
}
