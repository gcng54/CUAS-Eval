package io.github.gcng54.cuaseval.ui;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
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
import io.github.gcng54.cuaseval.terrain.ShapefileReader;
import io.github.gcng54.cuaseval.terrain.TerrainMaskCalculator;

import java.io.File;
import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;

/**
 * JavaFX canvas-based map display for CUAS evaluation visualization.
 * Renders sensor coverage areas, target positions, flight paths,
 * track data, and detection events on a 2-D map projection.
 * <p>
 * The map uses an equirectangular projection centred on the test environment.
 * </p>
 */
public class MapView extends Pane {

    private final Canvas canvas;
    private TestEnvironment environment;
    private TestScenario scenario;
    private EvaluationResult result;

    // View parameters
    private double centreLat = 38.4237;   // default: İzmir
    private double centreLon = 27.1428;
    private double zoomLevel = 1.0;        // pixels per metre
    private double viewScale = 0.05;       // degrees per view width

    // Shapefile data (world boundaries)
    private List<ShapefileReader.ShapeRecord> shapeRecords;

    // DTED terrain overlay
    private DtedReader dtedReader;
    private DtedReader.ElevationGrid elevationGrid;
    private WritableImage terrainImage;
    private double dtedCentreLon, dtedCentreLat, dtedRadiusKm;
    private boolean showElevation = true;
    private boolean showTerrainMask = false;

    // Terrain masking
    private List<TerrainMaskCalculator.TerrainMask> terrainMasks;

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

        // Load world shapefile on construction
        loadShapefile();

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
        contextMenu.getItems().addAll(addNodeItem, copyCoordItem);

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
     * Load world boundaries shapefile for coastline rendering.
     */
    private void loadShapefile() {
        File shpFile = new File("resources/world/World.shp");
        if (shpFile.exists()) {
            ShapefileReader reader = new ShapefileReader();
            shapeRecords = reader.read(shpFile);
        }
    }

    /**
     * Set DTED terrain data for elevation overlay.
     */
    public void setTerrainData(DtedReader reader, double centreLon, double centreLat, double radiusKm) {
        this.dtedReader = reader;
        this.dtedCentreLon = centreLon;
        this.dtedCentreLat = centreLat;
        this.dtedRadiusKm = radiusKm;
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

        // Terrain elevation overlay (under everything else)
        if (showElevation && terrainImage != null) {
            drawTerrainOverlay(gc, w, h);
        }

        // World boundaries (coastlines)
        if (shapeRecords != null) {
            drawShapefile(gc, w, h);
        }

        // DTED boundary circle
        if (dtedReader != null && dtedReader.isLoaded()) {
            drawDtedBoundary(gc, w, h);
        }

        // Grid
        drawGrid(gc, w, h);

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

        // Terrain mask overlay
        if (showTerrainMask && terrainMasks != null) {
            drawTerrainMasks(gc, w, h);
        }

        // Info overlay
        drawInfoOverlay(gc, w, h);
    }

    // ── Individual Renderers ────────────────────────────────────────────

    private void drawGrid(GraphicsContext gc, double w, double h) {
        gc.setStroke(Color.rgb(40, 50, 70));
        gc.setLineWidth(0.5);
        gc.setFont(Font.font("Consolas", 10));
        gc.setFill(Color.rgb(80, 90, 110));

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
            gc.fillText(String.format(Locale.ENGLISH, "%.4f°", lon), x + 2, h - 5);
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

    private void drawInfoOverlay(GraphicsContext gc, double w, double h) {
        gc.setFill(Color.rgb(0, 0, 0, 0.6));
        gc.fillRect(0, 0, 260, 100);
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Consolas", 11));
        gc.fillText(String.format(Locale.ENGLISH, "Centre: %.5f, %.5f", centreLat, centreLon), 10, 18);
        gc.fillText(String.format(Locale.ENGLISH, "Scale: %.4f°/view", viewScale), 10, 34);
        if (scenario != null) {
            gc.fillText("Scenario: " + scenario.getName(), 10, 50);
        }
        if (result != null) {
            gc.fillText(String.format(Locale.ENGLISH, "Score: %.1f | %s",
                    result.getOverallScore(),
                    result.isPassed() ? "PASS" : "FAIL"), 10, 66);
        }

        // Mouse coordinate display (UI-02)
        if (!Double.isNaN(mouseLat)) {
            String coordStr = String.format(Locale.ENGLISH, "Mouse: %.6f, %.6f", mouseLat, mouseLon);
            String elevStr = "";
            if (dtedReader != null && dtedReader.isLoaded()) {
                double elev = dtedReader.getElevation(mouseLat, mouseLon);
                if (elev != DtedReader.NO_DATA) {
                    elevStr = String.format(Locale.ENGLISH, "  Elev: %.0f m", elev);
                }
            }
            gc.fillText(coordStr + elevStr, 10, 82);
        }
    }

    // ── Projection Helpers ──────────────────────────────────────────────

    /**
     * Build a rasterized elevation image from the DTED grid.
     */
    private void buildTerrainImage() {
        if (dtedReader == null || !dtedReader.isLoaded()) return;

        int res = 400; // render resolution
        elevationGrid = dtedReader.getElevationGrid(dtedCentreLat, dtedCentreLon, dtedRadiusKm, res);

        terrainImage = new WritableImage(res, res);
        PixelWriter pw = terrainImage.getPixelWriter();

        double eMin = elevationGrid.getElevMin();
        double eMax = elevationGrid.getElevMax();
        double eRange = Math.max(1.0, eMax - eMin);

        for (int x = 0; x < res; x++) {
            for (int y = 0; y < res; y++) {
                double elev = elevationGrid.getData()[x][res - 1 - y]; // flip Y
                if (elev == DtedReader.NO_DATA) {
                    pw.setColor(x, y, Color.rgb(20, 40, 80, 0.4)); // ocean
                } else {
                    double t = (elev - eMin) / eRange;
                    Color c = elevationColor(t, elev);
                    pw.setColor(x, y, c);
                }
            }
        }
    }

    /**
     * Map elevation fraction (0-1) to a terrain colour.
     */
    private Color elevationColor(double t, double elev) {
        if (elev <= 0) return Color.rgb(20, 60, 120, 0.5);          // water
        if (t < 0.15) return Color.rgb(34, 139, 34, 0.6);           // green lowland
        if (t < 0.30) return Color.rgb(85, 160, 50, 0.6);           // light green
        if (t < 0.50) return Color.rgb(180, 170, 80, 0.6);          // yellow/brown
        if (t < 0.70) return Color.rgb(160, 110, 60, 0.6);          // brown
        if (t < 0.85) return Color.rgb(140, 90, 50, 0.6);           // dark brown
        return Color.rgb(220, 220, 220, 0.6);                        // snow/peak
    }

    private void drawTerrainOverlay(GraphicsContext gc, double w, double h) {
        if (elevationGrid == null || terrainImage == null) return;

        double x1 = lonToX(elevationGrid.getLonMin(), w);
        double y1 = latToY(elevationGrid.getLatMax(), h);
        double x2 = lonToX(elevationGrid.getLonMax(), w);
        double y2 = latToY(elevationGrid.getLatMin(), h);

        gc.drawImage(terrainImage, x1, y1, x2 - x1, y2 - y1);
    }

    private void drawDtedBoundary(GraphicsContext gc, double w, double h) {
        double cx = lonToX(dtedCentreLon, w);
        double cy = latToY(dtedCentreLat, h);
        double radiusPx = metresToPixels(dtedRadiusKm * 1000, w);

        gc.setStroke(Color.rgb(255, 100, 0, 0.7));
        gc.setLineWidth(2);
        gc.setLineDashes(8, 4);
        gc.strokeOval(cx - radiusPx, cy - radiusPx, radiusPx * 2, radiusPx * 2);
        gc.setLineDashes();

        // Label
        gc.setFill(Color.rgb(255, 100, 0));
        gc.setFont(Font.font("Consolas", 10));
        gc.fillText(String.format(Locale.ENGLISH, "DTED r=%.0fkm", dtedRadiusKm),
                cx + radiusPx + 5, cy);
    }

    private void drawShapefile(GraphicsContext gc, double w, double h) {
        gc.setStroke(Color.rgb(80, 120, 80, 0.8));
        gc.setLineWidth(1.0);

        // View bounds for culling
        double viewLatMin = centreLat - viewScale * h / (2 * w);
        double viewLatMax = centreLat + viewScale * h / (2 * w);
        double viewLonMin = centreLon - viewScale / 2;
        double viewLonMax = centreLon + viewScale / 2;

        for (ShapefileReader.ShapeRecord rec : shapeRecords) {
            // Bounding box cull
            if (rec.getMaxX() < viewLonMin || rec.getMinX() > viewLonMax ||
                rec.getMaxY() < viewLatMin || rec.getMinY() > viewLatMax) {
                continue;
            }

            for (double[][] part : rec.getParts()) {
                if (part.length < 2) continue;
                gc.beginPath();
                gc.moveTo(lonToX(part[0][0], w), latToY(part[0][1], h));
                for (int i = 1; i < part.length; i++) {
                    gc.lineTo(lonToX(part[i][0], w), latToY(part[i][1], h));
                }
                gc.stroke();
            }
        }
    }

    private void drawTerrainMasks(GraphicsContext gc, double w, double h) {
        if (terrainMasks == null) return;

        for (TerrainMaskCalculator.TerrainMask mask : terrainMasks) {
            GeoPosition sPos = mask.getSensorPosition();
            double sx = lonToX(sPos.getLongitude(), w);
            double sy = latToY(sPos.getLatitude(), h);
            double maxRangePx = metresToPixels(mask.getMaxRangeM(), w);

            double[] azimuths = mask.getAzimuths();
            double[] maskAngles = mask.getMaskAngles();

            // Draw mask angle fan
            gc.setLineWidth(1);
            for (int i = 0; i < azimuths.length; i++) {
                double az = Math.toRadians(azimuths[i]);
                double maskAngle = maskAngles[i];

                // Colour by mask angle: green = clear, red = masked
                double t = Math.min(1.0, Math.max(0, maskAngle / 5.0)); // 0-5° scale
                Color c = Color.color(t, 1.0 - t, 0, 0.4);
                gc.setStroke(c);

                double endX = sx + maxRangePx * Math.sin(az);
                double endY = sy - maxRangePx * Math.cos(az);
                gc.strokeLine(sx, sy, endX, endY);
            }

            // Label
            gc.setFill(Color.WHITE);
            gc.setFont(Font.font("Consolas", 9));
            gc.fillText("Mask", sx + 8, sy - 8);
        }
    }

    // ── Projection Helpers (original) ───────────────────────────────────

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
