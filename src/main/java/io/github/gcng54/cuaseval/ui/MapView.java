package io.github.gcng54.cuaseval.ui;

import javafx.application.Platform;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import io.github.gcng54.cuaseval.model.*;
import io.github.gcng54.cuaseval.model.TestEnvironment.SensorSite;
import io.github.gcng54.cuaseval.model.TrackingResult.TrackPoint;
import io.github.gcng54.cuaseval.terrain.DtedReader;
import io.github.gcng54.cuaseval.terrain.TerrainMaskCalculator;

import java.io.File;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JavaFX canvas-based map display for CUAS evaluation visualization.
 * Renders OpenStreetMap tile backgrounds, sensor coverage areas, target positions,
 * flight paths, track data, and detection events on a 2-D map projection.
 * <p>
 * The map uses an equirectangular projection centred on the test environment.
 * Background tiles are fetched from OpenStreetMap and cached locally.
 * </p>
 */
public class MapView extends Pane {

    private static final Logger log = LoggerFactory.getLogger(MapView.class);

    private final Canvas canvas;
    private TestEnvironment environment;
    private TestScenario scenario;
    private EvaluationResult result;

    // View parameters
    private double centreLat = 38.4;   // default screen centre
    private double centreLon = 26.8;
    private double zoomLevel = 1.0;   // pixels per metre
    private double viewScale = 0.05;  // degrees per view width

    // ── Web Map Tiles (OSM) ─────────────────────────────────────────────
    private static final String OSM_TILE_URL = "https://tile.openstreetmap.org/%d/%d/%d.png";
    private static final File TILE_CACHE_DIR = new File("resources/tile_cache");
    /** In-memory tile image cache: "z/x/y" → Image */
    private final Map<String, Image> tileCache = new ConcurrentHashMap<>();
    /** Tracks tiles currently being downloaded to avoid duplicates */
    private final Map<String, Boolean> tilesLoading = new ConcurrentHashMap<>();
    /** Map layer: "osm" (street) or "satellite" */
    private String mapLayer = "osm";
    /** Whether to show web tiles as background */
    private boolean showWebTiles = true;
    
    // Satellite tile URL (Esri World Imagery)
    private static final String SATELLITE_TILE_URL = "https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/%d/%d/%d";

    // ── SRTM terrain overlay ────────────────────────────────────────────
    private DtedReader srtmReader;
    private DtedReader.ElevationGrid elevationGrid;
    private WritableImage terrainImage;
    private double srtmCentreLon, srtmCentreLat, srtmRadiusKm;
    private boolean showElevation = true;
    private boolean showTerrainMask = false;

    // Terrain masking
    private List<TerrainMaskCalculator.TerrainMask> terrainMasks;
    /** Mask colour opacity (user-adjustable, 0.0–1.0) */
    private double maskOpacity = 0.35;

    // Mouse coordinate tracking (UI-02)
    private double mouseLat = Double.NaN;
    private double mouseLon = Double.NaN;

    // Right-click "Add Node" callback (UI-03): receives (lat, lon)
    private BiConsumer<Double, Double> addNodeCallback;

    // Context menu for right-click
    private ContextMenu contextMenu;

    // ── Constructor ─────────────────────────────────────────────────────

    public MapView() {
        canvas = new Canvas(800, 600);
        getChildren().add(canvas);

        // Ensure tile cache directory exists
        if (!TILE_CACHE_DIR.exists()) {
            TILE_CACHE_DIR.mkdirs();
        }

        // Auto-resize canvas with pane
        widthProperty().addListener((obs, ov, nv) -> {
            canvas.setWidth(nv.doubleValue());
            repaint();
        });
        heightProperty().addListener((obs, ov, nv) -> {
            canvas.setHeight(nv.doubleValue());
            repaint();
        });

        // Mouse scroll zoom
        setOnScroll(e -> {
            if (e.getDeltaY() > 0) viewScale *= 0.9;
            else viewScale *= 1.1;
            viewScale = Math.max(0.001, Math.min(1.0, viewScale));
            repaint();
        });

        // Mouse drag pan
        final double[] dragStart = new double[2];
        setOnMousePressed(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                dragStart[0] = e.getX();
                dragStart[1] = e.getY();
            }
            if (contextMenu != null) contextMenu.hide();
        });
        setOnMouseDragged(e -> {
            double dx = e.getX() - dragStart[0];
            double dy = e.getY() - dragStart[1];
            centreLon -= dx * viewScale / canvas.getWidth();
            centreLat += dy * viewScale / canvas.getHeight();
            dragStart[0] = e.getX();
            dragStart[1] = e.getY();
            repaint();
        });

        // Mouse move → track coordinates (UI-02)
        setOnMouseMoved(e -> {
            mouseLon = xToLon(e.getX(), canvas.getWidth());
            mouseLat = yToLat(e.getY(), canvas.getHeight());
            repaint();
        });

        // Right-click context menu (UI-03)
        contextMenu = new ContextMenu();
        MenuItem addNodeItem = new MenuItem("Add Node Here");
        addNodeItem.setOnAction(e -> {
            if (addNodeCallback != null && !Double.isNaN(mouseLat)) {
                addNodeCallback.accept(mouseLat, mouseLon);
            }
        });
        MenuItem copyCoordItem = new MenuItem("Copy Coordinates");
        copyCoordItem.setOnAction(e -> {
            if (!Double.isNaN(mouseLat)) {
                String coordStr = String.format(Locale.ENGLISH, "%.6f, %.6f", mouseLat, mouseLon);
                javafx.scene.input.Clipboard cb = javafx.scene.input.Clipboard.getSystemClipboard();
                javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
                content.putString(coordStr);
                cb.setContent(content);
            }
        });
        MenuItem toggleTilesItem = new MenuItem("Toggle Map Tiles");
        toggleTilesItem.setOnAction(e -> {
            showWebTiles = !showWebTiles;
            repaint();
        });
        MenuItem switchLayerItem = new MenuItem("Switch to Satellite");
        switchLayerItem.setOnAction(e -> {
            if ("osm".equals(mapLayer)) {
                mapLayer = "satellite";
                switchLayerItem.setText("Switch to OSM");
            } else {
                mapLayer = "osm";
                switchLayerItem.setText("Switch to Satellite");
            }
            // Clear tile cache to force reload with new layer
            tileCache.clear();
            repaint();
        });
        contextMenu.getItems().addAll(addNodeItem, copyCoordItem, toggleTilesItem, switchLayerItem);

        setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.SECONDARY) {
                mouseLon = xToLon(e.getX(), canvas.getWidth());
                mouseLat = yToLat(e.getY(), canvas.getHeight());
                contextMenu.show(this, e.getScreenX(), e.getScreenY());
            }
        });
    }

    // ── Data Binding ────────────────────────────────────────────────────

    /**
     * Set SRTM terrain data for elevation overlay.
     * Accepts a DtedReader loaded with SRTM HGT files.
     */
    public void setTerrainData(DtedReader reader, double centreLon, double centreLat, double radiusKm) {
        this.srtmReader = reader;
        this.srtmCentreLon = centreLon;
        this.srtmCentreLat = centreLat;
        this.srtmRadiusKm = radiusKm;
        buildTerrainImage();
        repaint();
    }

    /**
     * Toggle terrain overlay display options.
     */
    public void setTerrainDisplay(boolean showElev, boolean showMask) {
        this.showElevation = showElev;
        this.showTerrainMask = showMask;
        repaint();
    }

    /**
     * Set terrain mask data for detection angle visualization.
     */
    public void setTerrainMasks(List<TerrainMaskCalculator.TerrainMask> masks) {
        this.terrainMasks = masks;
        repaint();
    }

    /**
     * Set the mask overlay opacity (0.0–1.0).
     */
    public void setMaskOpacity(double opacity) {
        this.maskOpacity = Math.max(0.0, Math.min(1.0, opacity));
        repaint();
    }

    /**
     * Set callback invoked when user right-clicks "Add Node Here" (UI-03).
     * @param callback receives (latitude, longitude)
     */
    public void setAddNodeCallback(BiConsumer<Double, Double> callback) {
        this.addNodeCallback = callback;
    }

    /**
     * Get the current map centre latitude.
     */
    public double getCentreLat() { return centreLat; }

    /**
     * Get the current map centre longitude.
     */
    public double getCentreLon() { return centreLon; }

    /**
     * Get the approximate visible radius in km.
     */
    public double getVisibleRadiusKm() {
        return viewScale * 111.32 / 2;
    }

    /**
     * Set the test scenario to display.
     */
    public void setScenario(TestScenario scenario) {
        this.scenario = scenario;
        this.environment = scenario.getEnvironment();
        if (environment != null && environment.getCentrePosition() != null) {
            centreLat = environment.getCentrePosition().getLatitude();
            centreLon = environment.getCentrePosition().getLongitude();
            // Auto-scale to fit observation area
            viewScale = Math.max(0.005,
                    environment.getObservationRadiusM() * 3.0 / 111_320.0);
        }
        repaint();
    }

    /**
     * Set evaluation result to overlay on the map.
     */
    public void setEvaluationResult(EvaluationResult result) {
        this.result = result;
        repaint();
    }

    /**
     * Toggle the web tile background on/off.
     */
    public void setShowWebTiles(boolean show) {
        this.showWebTiles = show;
        repaint();
    }

    /**
     * Set the map layer (e.g. "osm").
     */
    public void setMapLayer(String layer) {
        this.mapLayer = layer;
        tileCache.clear();
        repaint();
    }

    // ── Rendering ───────────────────────────────────────────────────────

    /**
     * Repaint the entire map.
     */
    public void repaint() {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        double w = canvas.getWidth();
        double h = canvas.getHeight();

        // Clear background
        gc.setFill(Color.rgb(20, 30, 50));
        gc.fillRect(0, 0, w, h);

        // Web map tile background (OSM)
        if (showWebTiles) {
            drawWebTiles(gc, w, h);
        }

        // Terrain elevation overlay (semi-transparent, over tiles)
        if (showElevation && terrainImage != null) {
            drawTerrainOverlay(gc, w, h);
        }

        // Grid
        drawGrid(gc, w, h);

        // SRTM boundary circle
        if (srtmReader != null && srtmReader.isLoaded()) {
            drawSrtmBoundary(gc, w, h);
        }

        // Environment
        if (environment != null) {
            drawObservationArea(gc, w, h);
            drawSensors(gc, w, h);
            drawObstacles(gc, w, h);
        }

        // Scenario
        if (scenario != null) {
            drawFlightPaths(gc, w, h);
            drawTargets(gc, w, h);
        }

        // Results overlay
        if (result != null) {
            drawDetections(gc, w, h);
            drawTracks(gc, w, h);
        }

        // Terrain mask overlay (semitransparent)
        if (showTerrainMask && terrainMasks != null) {
            drawTerrainMasks(gc, w, h);
        }

        // Bottom info bar (no background rectangle)
        drawBottomInfoBar(gc, w, h);
    }

    // ── Web Map Tiles ───────────────────────────────────────────────────

    /**
     * Compute appropriate OSM zoom level from our viewScale.
     * OSM zoom: z=0 → 360°/256px, z=18 → ~0.0005°/256px
     */
    private int computeOsmZoom(double w) {
        // viewScale = degrees per view width
        // At OSM zoom z, one tile = 360/2^z degrees wide
        // We want enough tiles to cover the view width
        double degreesPerTile = viewScale; // rough: 1 tile ≈ view width
        // tile size in degrees at zoom z: 360/(2^z)
        // z = log2(360 / degreesPerTile)
        int z = (int) Math.round(Math.log(360.0 / viewScale) / Math.log(2));
        return Math.max(1, Math.min(18, z));
    }

    /**
     * Convert latitude to OSM tile Y at given zoom.
     */
    private int latToTileY(double lat, int zoom) {
        double latRad = Math.toRadians(lat);
        int n = 1 << zoom;
        return (int) ((1.0 - Math.log(Math.tan(latRad) + 1.0 / Math.cos(latRad)) / Math.PI) / 2.0 * n);
    }

    /**
     * Convert longitude to OSM tile X at given zoom.
     */
    private int lonToTileX(double lon, int zoom) {
        int n = 1 << zoom;
        return (int) ((lon + 180.0) / 360.0 * n);
    }

    /**
     * Convert OSM tile X back to longitude (west edge).
     */
    private double tileXToLon(int x, int zoom) {
        int n = 1 << zoom;
        return (double) x / n * 360.0 - 180.0;
    }

    /**
     * Convert OSM tile Y back to latitude (north edge, Mercator).
     */
    private double tileYToLat(int y, int zoom) {
        int n = 1 << zoom;
        double latRad = Math.atan(Math.sinh(Math.PI * (1 - 2.0 * y / n)));
        return Math.toDegrees(latRad);
    }

    /**
     * Draw OSM background tiles covering the current viewport.
     */
    private void drawWebTiles(GraphicsContext gc, double w, double h) {
        int zoom = computeOsmZoom(w);

        // Compute view bounds
        double viewLatMin = yToLat(h, h);
        double viewLatMax = yToLat(0, h);
        double viewLonMin = xToLon(0, w);
        double viewLonMax = xToLon(w, w);

        // Compute tile range
        int tileXMin = lonToTileX(viewLonMin, zoom);
        int tileXMax = lonToTileX(viewLonMax, zoom);
        int tileYMin = latToTileY(viewLatMax, zoom); // note: Y is inverted
        int tileYMax = latToTileY(viewLatMin, zoom);

        // Clamp
        int maxTile = (1 << zoom) - 1;
        tileXMin = Math.max(0, tileXMin);
        tileXMax = Math.min(maxTile, tileXMax);
        tileYMin = Math.max(0, tileYMin);
        tileYMax = Math.min(maxTile, tileYMax);

        // Limit tile count to avoid overload
        if ((tileXMax - tileXMin + 1) * (tileYMax - tileYMin + 1) > 200) {
            return; // too many tiles at this zoom, skip
        }

        for (int tx = tileXMin; tx <= tileXMax; tx++) {
            for (int ty = tileYMin; ty <= tileYMax; ty++) {
                Image tileImg = getTileImage(zoom, tx, ty);
                if (tileImg != null) {
                    // Tile bounds in geographic coords
                    double tileLonW = tileXToLon(tx, zoom);
                    double tileLonE = tileXToLon(tx + 1, zoom);
                    double tileLatN = tileYToLat(ty, zoom);
                    double tileLatS = tileYToLat(ty + 1, zoom);

                    // Convert to screen coords
                    double sx1 = lonToX(tileLonW, w);
                    double sx2 = lonToX(tileLonE, w);
                    double sy1 = latToY(tileLatN, h);
                    double sy2 = latToY(tileLatS, h);

                    gc.setGlobalAlpha(0.85);
                    gc.drawImage(tileImg, sx1, sy1, sx2 - sx1, sy2 - sy1);
                    gc.setGlobalAlpha(1.0);
                }
            }
        }
    }

    /**
     * Get a tile image from cache, disk, or initiate async download.
     */
    private Image getTileImage(int z, int x, int y) {
        String key = mapLayer + "/" + z + "/" + x + "/" + y;

        // Check in-memory cache first
        Image cached = tileCache.get(key);
        if (cached != null) return cached;

        // Check disk cache
        File cacheFile = new File(TILE_CACHE_DIR, key.replace('/', File.separatorChar) + ".png");
        cacheFile.getParentFile().mkdirs();
        if (cacheFile.exists()) {
            try {
                Image img = new Image(cacheFile.toURI().toString(), 256, 256, true, true);
                tileCache.put(key, img);
                return img;
            } catch (Exception e) {
                log.debug("Failed to load cached tile {}: {}", key, e.getMessage());
            }
        }

        // Async download
        if (tilesLoading.putIfAbsent(key, Boolean.TRUE) == null) {
            Thread downloader = new Thread(() -> {
                try {
                    String url = "satellite".equals(mapLayer) 
                            ? String.format(SATELLITE_TILE_URL, z, y, x)  // note: y,x order for ArcGIS
                            : String.format(OSM_TILE_URL, z, x, y);
                    HttpURLConnection conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
                    conn.setRequestProperty("User-Agent", "CUAS-Eval/2.0 (Educational)");
                    conn.setConnectTimeout(5000);
                    conn.setReadTimeout(10000);

                    if (conn.getResponseCode() == 200) {
                        // Ensure cache directory exists
                        cacheFile.getParentFile().mkdirs();

                        // Download to disk
                        try (InputStream in = conn.getInputStream()) {
                            Files.copy(in, cacheFile.toPath());
                        }

                        // Load image and cache in memory
                        Image img = new Image(cacheFile.toURI().toString(), 256, 256, true, true);
                        tileCache.put(key, img);

                        // Trigger repaint on FX thread
                        Platform.runLater(this::repaint);
                    }
                } catch (Exception e) {
                    log.debug("Tile download failed {}: {}", key, e.getMessage());
                } finally {
                    tilesLoading.remove(key);
                }
            }, "tile-dl-" + key);
            downloader.setDaemon(true);
            downloader.start();
        }

        return null; // tile not yet available
    }

    // ── Individual Renderers ────────────────────────────────────────────

    private void drawGrid(GraphicsContext gc, double w, double h) {
        gc.setStroke(Color.rgb(40, 50, 70, 0.6));
        gc.setLineWidth(0.5);
        gc.setFont(Font.font("Consolas", 10));
        gc.setFill(Color.rgb(200, 210, 230, 0.8));

        // Determine grid spacing in degrees
        double gridStep = viewScale / 5;
        double startLat = centreLat - viewScale * h / (2 * w);
        double startLon = centreLon - viewScale / 2;

        for (double lat = Math.floor(startLat / gridStep) * gridStep;
             lat < centreLat + viewScale; lat += gridStep) {
            double y = latToY(lat, h);
            gc.strokeLine(0, y, w, y);
            gc.fillText(String.format(Locale.ENGLISH, "%.4f°", lat), 5, y - 2);
        }
        for (double lon = Math.floor(startLon / gridStep) * gridStep;
             lon < centreLon + viewScale; lon += gridStep) {
            double x = lonToX(lon, w);
            gc.strokeLine(x, 0, x, h);
            gc.fillText(String.format(Locale.ENGLISH, "%.4f°", lon), x + 2, h - 25);
        }
    }

    private void drawObservationArea(GraphicsContext gc, double w, double h) {
        GeoPosition centre = environment.getCentrePosition();
        double cx = lonToX(centre.getLongitude(), w);
        double cy = latToY(centre.getLatitude(), h);
        double radiusPx = metresToPixels(environment.getObservationRadiusM(), w);

        // Circle
        gc.setStroke(Color.rgb(0, 200, 255, 0.5));
        gc.setLineWidth(2);
        gc.setLineDashes(10, 5);
        gc.strokeOval(cx - radiusPx, cy - radiusPx, radiusPx * 2, radiusPx * 2);
        gc.setLineDashes(); // reset

        // Centre marker
        gc.setFill(Color.CYAN);
        gc.fillOval(cx - 4, cy - 4, 8, 8);
    }

    private void drawSensors(GraphicsContext gc, double w, double h) {
        for (SensorSite sensor : environment.getSensorSites()) {
            double sx = lonToX(sensor.getPosition().getLongitude(), w);
            double sy = latToY(sensor.getPosition().getLatitude(), h);
            double rangePx = metresToPixels(sensor.getMaxRangeM(), w);

            Color sensorColor = sensorColor(sensor.getSensorType());

            double azCoverage = sensor.getAzimuthCoverageDeg();
            double azStart = 0;
            double minElev = 0;
            double maxElev = 90;

            // Use detailed params from CuasSensor template if available (SN-03, SN-04)
            CuasSensor tmpl = sensor.getSensorTemplate();
            if (tmpl != null) {
                azStart = tmpl.getAzimuthStartDeg();
                minElev = tmpl.getMinElevationDeg();
                maxElev = tmpl.getMaxElevationDeg();
            }

            if (azCoverage >= 360) {
                // Full circle coverage
                gc.setStroke(sensorColor.deriveColor(0, 1, 1, 0.3));
                gc.setLineWidth(1);
                gc.strokeOval(sx - rangePx, sy - rangePx, rangePx * 2, rangePx * 2);
            } else {
                // Sector arc (SN-04)
                gc.setFill(sensorColor.deriveColor(0, 1, 1, 0.15));
                gc.setStroke(sensorColor.deriveColor(0, 1, 1, 0.5));
                gc.setLineWidth(1.5);

                // JavaFX arc uses angle from 3 o'clock, counter-clockwise
                // We convert from geographic bearing (0=N, clockwise)
                double fxStartAngle = 90 - azStart - azCoverage;
                gc.fillArc(sx - rangePx, sy - rangePx, rangePx * 2, rangePx * 2,
                        fxStartAngle, azCoverage, javafx.scene.shape.ArcType.ROUND);
                gc.strokeArc(sx - rangePx, sy - rangePx, rangePx * 2, rangePx * 2,
                        fxStartAngle, azCoverage, javafx.scene.shape.ArcType.ROUND);

                // Draw min elevation ring if > 0 (inner exclusion zone)
                if (minElev > 0 && maxElev > minElev) {
                    double elevRatio = 1.0 - (minElev / maxElev);
                    double innerPx = rangePx * elevRatio;
                    gc.setStroke(sensorColor.deriveColor(0, 1, 1, 0.2));
                    gc.setLineDashes(4, 4);
                    gc.strokeArc(sx - innerPx, sy - innerPx, innerPx * 2, innerPx * 2,
                            fxStartAngle, azCoverage, javafx.scene.shape.ArcType.ROUND);
                    gc.setLineDashes();
                }
            }

            // Max elevation label
            if (maxElev < 90 && tmpl != null) {
                gc.setFill(sensorColor.deriveColor(0, 1, 0.7, 0.8));
                gc.setFont(Font.font("Consolas", 8));
                gc.fillText(String.format(Locale.ENGLISH, "El:%.0f-%.0f°", minElev, maxElev),
                        sx + 8, sy + 15);
            }

            // Sensor icon
            gc.setFill(sensorColor);
            gc.fillRect(sx - 5, sy - 5, 10, 10);
            gc.setFill(Color.WHITE);
            gc.setFont(Font.font("Consolas", 9));
            gc.fillText(sensor.getSensorId(), sx + 8, sy + 3);
        }
    }

    private void drawObstacles(GraphicsContext gc, double w, double h) {
        gc.setFill(Color.rgb(100, 100, 100, 0.6));
        for (TestEnvironment.Obstacle obs : environment.getObstacles()) {
            double ox = lonToX(obs.getPosition().getLongitude(), w);
            double oy = latToY(obs.getPosition().getLatitude(), h);
            double wPx = metresToPixels(obs.getWidthM(), w);
            double hPx = metresToPixels(obs.getHeightM(), w);
            gc.fillRect(ox - wPx / 2, oy - hPx / 2, wPx, hPx);
        }
    }

    private void drawFlightPaths(GraphicsContext gc, double w, double h) {
        gc.setStroke(Color.rgb(255, 200, 0, 0.7));
        gc.setLineWidth(2);
        gc.setLineDashes(6, 4);

        for (TestScenario.FlightPlan plan : scenario.getFlightPlans()) {
            List<TestScenario.Waypoint> wps = plan.getWaypoints();
            for (int i = 0; i < wps.size() - 1; i++) {
                GeoPosition p1 = wps.get(i).getPosition();
                GeoPosition p2 = wps.get(i + 1).getPosition();
                gc.strokeLine(
                        lonToX(p1.getLongitude(), w), latToY(p1.getLatitude(), h),
                        lonToX(p2.getLongitude(), w), latToY(p2.getLatitude(), h));
            }
        }
        gc.setLineDashes();
    }

    private void drawTargets(GraphicsContext gc, double w, double h) {
        for (UasTarget target : scenario.getTargets()) {
            GeoPosition p = target.getPosition();
            double tx = lonToX(p.getLongitude(), w);
            double ty = latToY(p.getLatitude(), h);

            Color c = target.isFriendly() ? Color.LIMEGREEN : Color.RED;
            gc.setFill(c);
            // Triangle marker
            gc.fillPolygon(
                    new double[]{tx, tx - 6, tx + 6},
                    new double[]{ty - 8, ty + 4, ty + 4}, 3);

            gc.setFill(Color.WHITE);
            gc.setFont(Font.font("Consolas", 10));
            gc.fillText(target.getDesignation(), tx + 10, ty);
        }
    }

    private void drawDetections(GraphicsContext gc, double w, double h) {
        for (DetectionResult det : result.getDetectionResults()) {
            if (det.getReportedPosition() == null) continue;
            GeoPosition p = det.getReportedPosition();
            double dx = lonToX(p.getLongitude(), w);
            double dy = latToY(p.getLatitude(), h);

            Color c = det.isDetected() ? Color.YELLOW : Color.DARKGRAY;
            gc.setStroke(c);
            gc.setLineWidth(1.5);
            gc.strokeOval(dx - 6, dy - 6, 12, 12);

            if (det.isDetected()) {
                gc.setFill(Color.YELLOW);
                gc.fillOval(dx - 2, dy - 2, 4, 4);
            }
        }
    }

    private void drawTracks(GraphicsContext gc, double w, double h) {
        gc.setLineWidth(1.5);
        for (TrackingResult track : result.getTrackingResults()) {
            gc.setStroke(Color.rgb(0, 150, 255, 0.8));
            List<TrackPoint> points = track.getTrackPoints();
            for (int i = 0; i < points.size() - 1; i++) {
                GeoPosition p1 = points.get(i).getReportedPosition();
                GeoPosition p2 = points.get(i + 1).getReportedPosition();
                if (p1 != null && p2 != null) {
                    gc.strokeLine(
                            lonToX(p1.getLongitude(), w), latToY(p1.getLatitude(), h),
                            lonToX(p2.getLongitude(), w), latToY(p2.getLatitude(), h));
                }
            }
        }
    }

    /**
     * Draw bottom info bar: screen centre, mouse coords, scenario info.
     * No background rectangle — text only, positioned at bottom of canvas.
     */
    private void drawBottomInfoBar(GraphicsContext gc, double w, double h) {
        double barY = h - 8; // bottom text line
        gc.setFont(Font.font("Consolas", 11));

        // Left side: Screen centre
        gc.setFill(Color.rgb(255, 255, 255, 0.9));
        String centreStr = String.format(Locale.ENGLISH,
                "Centre: %.5f°N  %.5f°E  Scale: %.4f°/view", centreLat, centreLon, viewScale);
        gc.fillText(centreStr, 8, barY);

        // Middle: Scenario + result info
        if (scenario != null || result != null) {
            double midX = w * 0.4;
            String infoStr = "";
            if (scenario != null) {
                infoStr += scenario.getName();
            }
            if (result != null) {
                infoStr += String.format(Locale.ENGLISH, "  Score: %.1f  %s",
                        result.getOverallScore(), result.isPassed() ? "PASS" : "FAIL");
            }
            gc.setFill(result != null && result.isPassed()
                    ? Color.rgb(100, 255, 100, 0.9) : Color.rgb(255, 200, 100, 0.9));
            gc.fillText(infoStr, midX, barY);
        }

        // Right side: Mouse coordinates + elevation
        if (!Double.isNaN(mouseLat)) {
            String mouseStr = String.format(Locale.ENGLISH,
                    "Mouse: %.6f°N  %.6f°E", mouseLat, mouseLon);

            // SRTM elevation lookup
            if (srtmReader != null && srtmReader.isLoaded()) {
                double elev = srtmReader.getElevation(mouseLat, mouseLon);
                if (elev != DtedReader.NO_DATA) {
                    mouseStr += String.format(Locale.ENGLISH, "  Elev: %.0fm", elev);
                }
            }

            double textWidth = mouseStr.length() * 7; // approximate
            gc.setFill(Color.rgb(255, 255, 255, 0.9));
            gc.fillText(mouseStr, w - textWidth - 8, barY);
        }
    }

    // ── SRTM Terrain Overlay ────────────────────────────────────────────

    /**
     * Build a rasterized elevation image from the SRTM grid.
     */
    private void buildTerrainImage() {
        if (srtmReader == null || !srtmReader.isLoaded()) return;

        int res = 400; // render resolution
        elevationGrid = srtmReader.getElevationGrid(srtmCentreLat, srtmCentreLon, srtmRadiusKm, res);

        terrainImage = new WritableImage(res, res);
        PixelWriter pw = terrainImage.getPixelWriter();

        double eMin = elevationGrid.getElevMin();
        double eMax = elevationGrid.getElevMax();
        double eRange = Math.max(1.0, eMax - eMin);

        for (int x = 0; x < res; x++) {
            for (int y = 0; y < res; y++) {
                double elev = elevationGrid.getData()[x][res - 1 - y]; // flip Y
                if (elev == DtedReader.NO_DATA) {
                    pw.setColor(x, y, Color.rgb(20, 40, 80, 0.3)); // ocean
                } else {
                    double t = (elev - eMin) / eRange;
                    Color c = elevationColor(t, elev);
                    pw.setColor(x, y, c);
                }
            }
        }
    }

    /**
     * Map elevation fraction (0-1) to a terrain colour (semitransparent).
     */
    private Color elevationColor(double t, double elev) {
        if (elev <= 0) return Color.rgb(20, 60, 120, 0.35);          // water
        if (t < 0.15) return Color.rgb(34, 139, 34, 0.40);           // green lowland
        if (t < 0.30) return Color.rgb(85, 160, 50, 0.40);           // light green
        if (t < 0.50) return Color.rgb(180, 170, 80, 0.40);          // yellow/brown
        if (t < 0.70) return Color.rgb(160, 110, 60, 0.40);          // brown
        if (t < 0.85) return Color.rgb(140, 90, 50, 0.40);           // dark brown
        return Color.rgb(220, 220, 220, 0.40);                        // snow/peak
    }

    private void drawTerrainOverlay(GraphicsContext gc, double w, double h) {
        if (elevationGrid == null || terrainImage == null) return;

        double x1 = lonToX(elevationGrid.getLonMin(), w);
        double y1 = latToY(elevationGrid.getLatMax(), h);
        double x2 = lonToX(elevationGrid.getLonMax(), w);
        double y2 = latToY(elevationGrid.getLatMin(), h);

        gc.drawImage(terrainImage, x1, y1, x2 - x1, y2 - y1);
    }

    private void drawSrtmBoundary(GraphicsContext gc, double w, double h) {
        double cx = lonToX(srtmCentreLon, w);
        double cy = latToY(srtmCentreLat, h);
        double radiusPx = metresToPixels(srtmRadiusKm * 1000, w);

        gc.setStroke(Color.rgb(60, 180, 60, 0.5));
        gc.setLineWidth(1.5);
        gc.setLineDashes(8, 4);
        gc.strokeOval(cx - radiusPx, cy - radiusPx, radiusPx * 2, radiusPx * 2);
        gc.setLineDashes();

        // Label
        gc.setFill(Color.rgb(60, 180, 60, 0.8));
        gc.setFont(Font.font("Consolas", 10));
        gc.fillText(String.format(Locale.ENGLISH, "SRTM r=%.0fkm", srtmRadiusKm),
                cx + radiusPx + 5, cy);
    }

    /**
     * Draw terrain masks with semitransparent, adjustable colours.
     */
    private void drawTerrainMasks(GraphicsContext gc, double w, double h) {
        if (terrainMasks == null) return;

        for (TerrainMaskCalculator.TerrainMask mask : terrainMasks) {
            GeoPosition sPos = mask.getSensorPosition();
            double sx = lonToX(sPos.getLongitude(), w);
            double sy = latToY(sPos.getLatitude(), h);
            double maxRangePx = metresToPixels(mask.getMaxRangeM(), w);

            double[] azimuths = mask.getAzimuths();
            double[] maskAngles = mask.getMaskAngles();

            // Draw mask angle fan with semitransparent colours
            gc.setLineWidth(1.5);
            for (int i = 0; i < azimuths.length; i++) {
                double az = Math.toRadians(azimuths[i]);
                double maskAngle = maskAngles[i];

                // Colour by mask angle: green = clear, yellow = partial, red = masked
                double t = Math.min(1.0, Math.max(0, maskAngle / 5.0)); // 0-5° scale
                Color c;
                if (t < 0.3) {
                    c = Color.color(0, 0.8, 0.2, maskOpacity * 0.5); // green — clear
                } else if (t < 0.7) {
                    c = Color.color(1.0, 0.8, 0, maskOpacity * 0.7); // yellow — partial
                } else {
                    c = Color.color(1.0, 0.2, 0, maskOpacity); // red — masked
                }
                gc.setStroke(c);

                double endX = sx + maxRangePx * Math.sin(az);
                double endY = sy - maxRangePx * Math.cos(az);
                gc.strokeLine(sx, sy, endX, endY);
            }

            // Label
            gc.setFill(Color.rgb(255, 255, 255, 0.7));
            gc.setFont(Font.font("Consolas", 9));
            gc.fillText("Mask", sx + 8, sy - 8);
        }
    }

    // ── Projection Helpers ──────────────────────────────────────────────

    private double lonToX(double lon, double w) {
        return w / 2 + (lon - centreLon) / viewScale * w;
    }

    private double latToY(double lat, double h) {
        return h / 2 - (lat - centreLat) / viewScale * h;
    }

    /** Inverse projection: screen X → longitude (UI-02). */
    private double xToLon(double x, double w) {
        return centreLon + (x - w / 2) * viewScale / w;
    }

    /** Inverse projection: screen Y → latitude (UI-02). */
    private double yToLat(double y, double h) {
        return centreLat - (y - h / 2) * viewScale / h;
    }

    private double metresToPixels(double metres, double w) {
        double degreesPerMetre = 1.0 / 111_320.0;
        return metres * degreesPerMetre / viewScale * w;
    }

    private Color sensorColor(String type) {
        if (type == null) return Color.WHITE;
        return switch (type.toUpperCase()) {
            case "RADAR"        -> Color.LIME;
            case "EO/IR", "IR"  -> Color.ORANGE;
            case "RF_DETECTOR"  -> Color.MAGENTA;
            case "ACOUSTIC"     -> Color.CYAN;
            default             -> Color.WHITE;
        };
    }
}
