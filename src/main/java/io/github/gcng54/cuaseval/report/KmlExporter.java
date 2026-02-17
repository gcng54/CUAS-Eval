package io.github.gcng54.cuaseval.report;

import io.github.gcng54.cuaseval.model.*;
import io.github.gcng54.cuaseval.model.TrackingResult.TrackPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Exports track data to KML format for visualization in Google Earth.
 * Generates placemarks for targets, flight paths, sensor positions,
 * and detection/identification events.
 * <p>
 * Usage: {@code KmlExporter.export(scenario, result, outputFile)}
 * </p>
 */
public class KmlExporter {

    private static final Logger log = LoggerFactory.getLogger(KmlExporter.class);
    private static final DateTimeFormatter ISO_FMT =
            DateTimeFormatter.ISO_INSTANT.withZone(ZoneOffset.UTC);

    /**
     * Export a full scenario with evaluation results to a KML file (Track2KML).
     *
     * @param scenario the test scenario
     * @param result   evaluation result with DTI data
     * @param outFile  output .kml file
     */
    public void export(TestScenario scenario, EvaluationResult result, File outFile) {
        try (Writer writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(outFile), StandardCharsets.UTF_8))) {

            writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            writer.write("<kml xmlns=\"http://www.opengis.net/kml/2.2\">\n");
            writer.write("<Document>\n");
            writer.write("  <name>CUAS-Eval: " + escapeXml(scenario.getName()) + "</name>\n");
            writer.write("  <description>CWA 18150 Test Evaluation — Track2KML Export</description>\n");

            // Styles
            writeStyles(writer);

            // Environment folder
            writeEnvironmentFolder(writer, scenario.getEnvironment());

            // Target flight paths folder
            writeFlightPathsFolder(writer, scenario, result);

            // Detection events folder
            writeDetectionsFolder(writer, result);

            // Track points folder
            writeTracksFolder(writer, result);

            writer.write("</Document>\n");
            writer.write("</kml>\n");

            log.info("KML exported to: {}", outFile.getAbsolutePath());

        } catch (IOException e) {
            log.error("Failed to export KML: {}", e.getMessage(), e);
        }
    }

    // ── KML Style Definitions ───────────────────────────────────────────

    private void writeStyles(Writer w) throws IOException {
        // Target style
        w.write("  <Style id=\"target\">\n");
        w.write("    <IconStyle><color>ff0000ff</color><scale>1.2</scale>\n");
        w.write("      <Icon><href>http://maps.google.com/mapfiles/kml/shapes/triangle.png</href></Icon>\n");
        w.write("    </IconStyle>\n");
        w.write("  </Style>\n");

        // Sensor style
        w.write("  <Style id=\"sensor\">\n");
        w.write("    <IconStyle><color>ff00ff00</color><scale>1.0</scale>\n");
        w.write("      <Icon><href>http://maps.google.com/mapfiles/kml/shapes/square.png</href></Icon>\n");
        w.write("    </IconStyle>\n");
        w.write("  </Style>\n");

        // Detection event style
        w.write("  <Style id=\"detection\">\n");
        w.write("    <IconStyle><color>ff00ffff</color><scale>0.8</scale>\n");
        w.write("      <Icon><href>http://maps.google.com/mapfiles/kml/shapes/star.png</href></Icon>\n");
        w.write("    </IconStyle>\n");
        w.write("  </Style>\n");

        // Flight path style
        w.write("  <Style id=\"flightpath\">\n");
        w.write("    <LineStyle><color>ff0000ff</color><width>3</width></LineStyle>\n");
        w.write("  </Style>\n");

        // Track path style
        w.write("  <Style id=\"trackpath\">\n");
        w.write("    <LineStyle><color>ffff0000</color><width>2</width></LineStyle>\n");
        w.write("  </Style>\n");
    }

    // ── Environment ─────────────────────────────────────────────────────

    private void writeEnvironmentFolder(Writer w, TestEnvironment env) throws IOException {
        w.write("  <Folder>\n");
        w.write("    <name>Test Environment</name>\n");

        // Centre point
        if (env.getCentrePosition() != null) {
            writePlacemark(w, "Observation Centre", env.getCentrePosition(),
                    "sensor", "Centre of observation area. Radius: "
                            + env.getObservationRadiusM() + " m");
        }

        // Sensor sites
        for (TestEnvironment.SensorSite sensor : env.getSensorSites()) {
            writePlacemark(w, sensor.getSensorId() + " (" + sensor.getSensorType() + ")",
                    sensor.getPosition(), "sensor",
                    "Max range: " + sensor.getMaxRangeM() + " m");
        }

        // Obstacles
        for (TestEnvironment.Obstacle obs : env.getObstacles()) {
            writePlacemark(w, "Obstacle: " + obs.getDescription(),
                    obs.getPosition(), "sensor",
                    "Height: " + obs.getHeightM() + " m, Width: " + obs.getWidthM() + " m");
        }

        w.write("  </Folder>\n");
    }

    // ── Flight Paths ────────────────────────────────────────────────────

    private void writeFlightPathsFolder(Writer w, TestScenario scenario,
                                         EvaluationResult result) throws IOException {
        w.write("  <Folder>\n");
        w.write("    <name>Flight Paths (Ground Truth)</name>\n");

        for (TestScenario.FlightPlan plan : scenario.getFlightPlans()) {
            if (plan.getWaypoints().isEmpty()) continue;

            w.write("    <Placemark>\n");
            w.write("      <name>Path: " + escapeXml(plan.getTargetUid()) + "</name>\n");
            w.write("      <styleUrl>#flightpath</styleUrl>\n");
            w.write("      <LineString>\n");
            w.write("        <altitudeMode>absolute</altitudeMode>\n");
            w.write("        <coordinates>\n");
            for (TestScenario.Waypoint wp : plan.getWaypoints()) {
                GeoPosition p = wp.getPosition();
                w.write(String.format(Locale.ENGLISH, "          %.8f,%.8f,%.1f\n",
                        p.getLongitude(), p.getLatitude(), p.getAltitudeMsl()));
            }
            w.write("        </coordinates>\n");
            w.write("      </LineString>\n");
            w.write("    </Placemark>\n");
        }

        w.write("  </Folder>\n");
    }

    // ── Detections ──────────────────────────────────────────────────────

    private void writeDetectionsFolder(Writer w, EvaluationResult result) throws IOException {
        w.write("  <Folder>\n");
        w.write("    <name>Detection Events</name>\n");

        for (DetectionResult det : result.getDetectionResults()) {
            if (det.getReportedPosition() != null) {
                String desc = String.format(Locale.ENGLISH, "Detected: %b | Latency: %.2fs | Error: %.1fm | Sensor: %s",
                        det.isDetected(), det.getLatencySeconds(),
                        det.getPositionErrorMetres(), det.getSensorType());
                writePlacemark(w, "Detection: " + det.getTargetUid(),
                        det.getReportedPosition(), "detection", desc);
            }
        }

        w.write("  </Folder>\n");
    }

    // ── Tracks ──────────────────────────────────────────────────────────

    private void writeTracksFolder(Writer w, EvaluationResult result) throws IOException {
        w.write("  <Folder>\n");
        w.write("    <name>Tracking Data</name>\n");

        for (TrackingResult track : result.getTrackingResults()) {
            List<TrackPoint> points = track.getTrackPoints();
            if (points.isEmpty()) continue;

            // Track line
            w.write("    <Placemark>\n");
            w.write("      <name>Track: " + escapeXml(track.getSystemTrackId()) + "</name>\n");
            w.write("      <description>UID: " + track.getTargetUid()
                    + " | Drops: " + track.getTrackDropCount()
                    + " | MeanErr: " + String.format(Locale.ENGLISH, "%.1f", track.getMeanPositionErrorMetres()) + "m"
                    + "</description>\n");
            w.write("      <styleUrl>#trackpath</styleUrl>\n");
            w.write("      <LineString>\n");
            w.write("        <altitudeMode>absolute</altitudeMode>\n");
            w.write("        <coordinates>\n");
            for (TrackPoint tp : points) {
                GeoPosition p = tp.getReportedPosition();
                if (p != null) {
                    w.write(String.format(Locale.ENGLISH, "          %.8f,%.8f,%.1f\n",
                            p.getLongitude(), p.getLatitude(), p.getAltitudeMsl()));
                }
            }
            w.write("        </coordinates>\n");
            w.write("      </LineString>\n");
            w.write("    </Placemark>\n");
        }

        w.write("  </Folder>\n");
    }

    // ── Utilities ───────────────────────────────────────────────────────

    private void writePlacemark(Writer w, String name, GeoPosition pos,
                                 String styleId, String description) throws IOException {
        w.write("    <Placemark>\n");
        w.write("      <name>" + escapeXml(name) + "</name>\n");
        w.write("      <description>" + escapeXml(description) + "</description>\n");
        w.write("      <styleUrl>#" + styleId + "</styleUrl>\n");
        w.write("      <Point>\n");
        w.write("        <altitudeMode>absolute</altitudeMode>\n");
        w.write(String.format(Locale.ENGLISH, "        <coordinates>%.8f,%.8f,%.1f</coordinates>\n",
                pos.getLongitude(), pos.getLatitude(), pos.getAltitudeMsl()));
        w.write("      </Point>\n");
        w.write("    </Placemark>\n");
    }

    private String escapeXml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
