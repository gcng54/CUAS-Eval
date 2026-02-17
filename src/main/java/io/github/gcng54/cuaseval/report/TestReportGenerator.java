package io.github.gcng54.cuaseval.report;

import io.github.gcng54.cuaseval.evaluator.MetricsCalculator;
import io.github.gcng54.cuaseval.model.*;
import io.github.gcng54.cuaseval.requirements.RequirementsManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Generates HTML test reports from evaluation results per CWA 18150.
 * Produces a self-contained HTML file with metrics tables, pass/fail
 * summary, and requirement traceability.
 */
public class TestReportGenerator {

    private static final Logger log = LoggerFactory.getLogger(TestReportGenerator.class);
    private final MetricsCalculator metricsCalc = new MetricsCalculator();
    private final RequirementsManager reqManager = new RequirementsManager();

    /**
     * Generate a full HTML evaluation report.
     *
     * @param scenario the evaluated scenario
     * @param result   evaluation result
     * @param outFile  output HTML file
     */
    public void generateHtmlReport(TestScenario scenario,
                                     EvaluationResult result, File outFile) {
        try (Writer w = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(outFile), StandardCharsets.UTF_8))) {

            Map<String, Double> metrics = metricsCalc.computeAllMetrics(result);

            w.write("<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n");
            w.write("<meta charset=\"UTF-8\">\n");
            w.write("<title>CUAS-Eval Report: " + esc(scenario.getName()) + "</title>\n");
            writeCss(w);
            w.write("</head>\n<body>\n");

            // Header
            w.write("<div class=\"header\">\n");
            w.write("<h1>CUAS-Eval Test Report</h1>\n");
            w.write("<h2>CWA 18150 — COURAGEOUS Counter-UAS Evaluation</h2>\n");
            w.write("<p class=\"date\">Generated: " +
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) +
                    "</p>\n");
            w.write("</div>\n");

            // Scenario summary
            w.write("<div class=\"section\">\n");
            w.write("<h3>1. Scenario Summary</h3>\n");
            w.write("<table>\n");
            writeRow(w, "Scenario ID", scenario.getScenarioId());
            writeRow(w, "Name", scenario.getName());
            writeRow(w, "Description", scenario.getDescription());
            writeRow(w, "Duration", String.format(Locale.ENGLISH, "%.0f s", scenario.getDurationSeconds()));
            writeRow(w, "Weather", scenario.getEnvironment().getWeather().name());
            writeRow(w, "Terrain", scenario.getEnvironment().getTerrainType());
            writeRow(w, "Targets", String.valueOf(scenario.getTargets().size()));
            writeRow(w, "Sensors", String.valueOf(scenario.getEnvironment().getSensorSites().size()));
            w.write("</table>\n</div>\n");

            // Overall verdict
            w.write("<div class=\"section\">\n");
            w.write("<h3>2. Overall Verdict</h3>\n");
            String verdict = result.isPassed() ? "PASS" : "FAIL";
            String verdictClass = result.isPassed() ? "pass" : "fail";
            w.write("<div class=\"verdict " + verdictClass + "\">" + verdict + "</div>\n");
            w.write("<table>\n");
            writeRow(w, "Overall Score", String.format(Locale.ENGLISH, "%.1f / 100", result.getOverallScore()));
            writeRow(w, "Compliance", String.format(Locale.ENGLISH, "%.1f%%", result.getCompliancePercent()));
            writeRow(w, "Requirements Passed", String.valueOf(result.getPassedRequirements().size()));
            writeRow(w, "Requirements Failed", String.valueOf(result.getFailedRequirements().size()));
            w.write("</table>\n</div>\n");

            // Metrics table
            w.write("<div class=\"section\">\n");
            w.write("<h3>3. Performance Metrics</h3>\n");
            w.write("<table>\n");
            w.write("<tr><th>Metric</th><th>Value</th></tr>\n");
            for (Map.Entry<String, Double> entry : metrics.entrySet()) {
                w.write("<tr><td>" + esc(entry.getKey()) + "</td><td>" +
                        String.format(Locale.ENGLISH, "%.4f", entry.getValue()) + "</td></tr>\n");
            }
            w.write("</table>\n</div>\n");

            // Requirement compliance
            w.write("<div class=\"section\">\n");
            w.write("<h3>4. Requirement Compliance</h3>\n");
            w.write("<table>\n");
            w.write("<tr><th>Req ID</th><th>Name</th><th>Status</th></tr>\n");
            for (String reqId : result.getPassedRequirements()) {
                Requirement req = reqManager.getById(reqId);
                String name = req != null ? req.getName() : reqId;
                w.write("<tr><td>" + reqId + "</td><td>" + esc(name)
                        + "</td><td class=\"pass\">PASS</td></tr>\n");
            }
            for (String reqId : result.getFailedRequirements()) {
                Requirement req = reqManager.getById(reqId);
                String name = req != null ? req.getName() : reqId;
                w.write("<tr><td>" + reqId + "</td><td>" + esc(name)
                        + "</td><td class=\"fail\">FAIL</td></tr>\n");
            }
            w.write("</table>\n</div>\n");

            // Detection details
            w.write("<div class=\"section\">\n");
            w.write("<h3>5. Detection Results</h3>\n");
            w.write("<table>\n");
            w.write("<tr><th>Target</th><th>Detected</th><th>Latency (s)</th><th>Error (m)</th><th>Sensor</th></tr>\n");
            for (DetectionResult det : result.getDetectionResults()) {
                w.write(String.format(Locale.ENGLISH, "<tr><td>%s</td><td>%b</td><td>%.2f</td><td>%.1f</td><td>%s</td></tr>\n",
                        det.getTargetUid(), det.isDetected(), det.getLatencySeconds(),
                        det.getPositionErrorMetres(), det.getSensorType()));
            }
            w.write("</table>\n</div>\n");

            // Tracking details
            w.write("<div class=\"section\">\n");
            w.write("<h3>6. Tracking Results</h3>\n");
            w.write("<table>\n");
            w.write("<tr><th>Target</th><th>Track ID</th><th>Maintained</th><th>Drops</th><th>Mean Error (m)</th><th>Points</th></tr>\n");
            for (TrackingResult trk : result.getTrackingResults()) {
                w.write(String.format(Locale.ENGLISH, "<tr><td>%s</td><td>%s</td><td>%b</td><td>%d</td><td>%.1f</td><td>%d</td></tr>\n",
                        trk.getTargetUid(), trk.getSystemTrackId(), trk.isTrackMaintained(),
                        trk.getTrackDropCount(), trk.getMeanPositionErrorMetres(),
                        trk.getTrackPoints().size()));
            }
            w.write("</table>\n</div>\n");

            // Identification details
            w.write("<div class=\"section\">\n");
            w.write("<h3>7. Identification Results</h3>\n");
            w.write("<table>\n");
            w.write("<tr><th>Target</th><th>Identified</th><th>Classification</th><th>Correct</th><th>Confidence</th></tr>\n");
            for (IdentificationResult id : result.getIdentificationResults()) {
                w.write(String.format(Locale.ENGLISH, "<tr><td>%s</td><td>%b</td><td>%s</td><td>%b</td><td>%.2f</td></tr>\n",
                        id.getTargetUid(), id.isIdentified(), id.getReportedClassification(),
                        id.isClassificationCorrect(), id.getConfidence()));
            }
            w.write("</table>\n</div>\n");

            // Footer
            w.write("<div class=\"footer\">\n");
            w.write("<p>CUAS-Eval v1.0 | CWA 18150 COURAGEOUS | Generated by CUAS-Evaluator</p>\n");
            w.write("</div>\n");

            w.write("</body>\n</html>\n");
            log.info("HTML report exported to: {}", outFile.getAbsolutePath());

        } catch (IOException e) {
            log.error("Failed to generate report: {}", e.getMessage(), e);
        }
    }

    /**
     * Generate a suite-level summary report for multiple scenarios.
     */
    public void generateSuiteReport(List<TestScenario> scenarios,
                                      List<EvaluationResult> results, File outFile) {
        try (Writer w = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(outFile), StandardCharsets.UTF_8))) {

            w.write("<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n");
            w.write("<meta charset=\"UTF-8\">\n");
            w.write("<title>CUAS-Eval Suite Report</title>\n");
            writeCss(w);
            w.write("</head>\n<body>\n");

            w.write("<div class=\"header\">\n");
            w.write("<h1>CUAS-Eval Test Suite Report</h1>\n");
            w.write("<h2>CWA 18150 — Full Compliance Summary</h2>\n");
            w.write("<p class=\"date\">Generated: " +
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) +
                    "</p>\n");
            w.write("</div>\n");

            w.write("<div class=\"section\">\n");
            w.write("<h3>Suite Overview</h3>\n");
            w.write("<table>\n");
            w.write("<tr><th>Scenario</th><th>Score</th><th>Pd</th><th>Continuity</th><th>Pi</th><th>Verdict</th></tr>\n");
            for (int i = 0; i < results.size(); i++) {
                EvaluationResult r = results.get(i);
                String sName = i < scenarios.size() ? scenarios.get(i).getName() : r.getScenarioId();
                String cls = r.isPassed() ? "pass" : "fail";
                w.write(String.format(Locale.ENGLISH, "<tr><td>%s</td><td>%.1f</td><td>%.2f</td><td>%.2f</td><td>%.2f</td><td class=\"%s\">%s</td></tr>\n",
                        esc(sName), r.getOverallScore(), r.getProbabilityOfDetection(),
                        r.getTrackContinuity(), r.getProbabilityOfIdentification(),
                        cls, r.isPassed() ? "PASS" : "FAIL"));
            }
            w.write("</table>\n</div>\n");

            w.write("<div class=\"footer\">\n");
            w.write("<p>CUAS-Eval v1.0 | CWA 18150 COURAGEOUS</p>\n");
            w.write("</div>\n");
            w.write("</body>\n</html>\n");

            log.info("Suite report exported to: {}", outFile.getAbsolutePath());

        } catch (IOException e) {
            log.error("Failed to generate suite report: {}", e.getMessage(), e);
        }
    }

    // ── CSS ─────────────────────────────────────────────────────────────

    private void writeCss(Writer w) throws IOException {
        w.write("<style>\n");
        w.write("body { font-family: 'Segoe UI', Arial, sans-serif; margin: 20px; background: #f5f5f5; }\n");
        w.write(".header { background: #1a237e; color: white; padding: 20px; border-radius: 8px; margin-bottom: 20px; }\n");
        w.write(".header h1 { margin: 0; } .header h2 { margin: 5px 0; font-weight: normal; opacity: 0.8; }\n");
        w.write(".date { opacity: 0.6; font-size: 0.9em; }\n");
        w.write(".section { background: white; padding: 15px 20px; margin-bottom: 15px; border-radius: 6px; box-shadow: 0 1px 3px rgba(0,0,0,0.1); }\n");
        w.write("h3 { color: #1a237e; border-bottom: 2px solid #1a237e; padding-bottom: 5px; }\n");
        w.write("table { width: 100%; border-collapse: collapse; margin-top: 10px; }\n");
        w.write("th { background: #e8eaf6; text-align: left; padding: 8px 12px; }\n");
        w.write("td { padding: 6px 12px; border-bottom: 1px solid #eee; }\n");
        w.write("tr:hover { background: #f5f5f5; }\n");
        w.write(".pass { color: #2e7d32; font-weight: bold; }\n");
        w.write(".fail { color: #c62828; font-weight: bold; }\n");
        w.write(".verdict { font-size: 2em; text-align: center; padding: 15px; border-radius: 8px; margin: 10px 0; }\n");
        w.write(".verdict.pass { background: #e8f5e9; color: #2e7d32; }\n");
        w.write(".verdict.fail { background: #ffebee; color: #c62828; }\n");
        w.write(".footer { text-align: center; color: #666; margin-top: 30px; font-size: 0.9em; }\n");
        w.write("</style>\n");
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private void writeRow(Writer w, String label, String value) throws IOException {
        w.write("<tr><td><strong>" + esc(label) + "</strong></td><td>" + esc(value) + "</td></tr>\n");
    }

    private String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
