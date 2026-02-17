package io.github.gcng54.cuaseval.evaluator;

import io.github.gcng54.cuaseval.model.*;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Computes statistical metrics from DTI evaluation results.
 * Provides COURAGEOUS CWA 18150 performance indicators
 * including Pd, Pfa, track accuracy, and identification rates.
 * <p>
 * Uses Apache Commons Math for robust statistical computations.
 * </p>
 */
public class MetricsCalculator {

    private static final Logger log = LoggerFactory.getLogger(MetricsCalculator.class);

    /**
     * Compute a comprehensive metrics map from evaluation results.
     * Keys are metric names; values are computed statistics.
     */
    public Map<String, Double> computeAllMetrics(EvaluationResult result) {
        Map<String, Double> metrics = new LinkedHashMap<>();

        // ── Detection metrics ───────────────────────────────────────
        metrics.put("Pd (Probability of Detection)", result.getProbabilityOfDetection());
        metrics.put("Pfa (False Alarm Rate)", result.getFalseAlarmRate());
        metrics.put("Mean Detection Latency (s)", result.getMeanDetectionLatencyS());
        metrics.put("Mean Detection Error (m)", result.getMeanDetectionErrorM());

        // Detailed detection statistics
        if (!result.getDetectionResults().isEmpty()) {
            DescriptiveStatistics latencyStats = new DescriptiveStatistics();
            DescriptiveStatistics errorStats = new DescriptiveStatistics();
            for (DetectionResult det : result.getDetectionResults()) {
                if (det.isDetected() && !det.getTargetUid().startsWith("FALSE_ALARM")) {
                    latencyStats.addValue(det.getLatencySeconds());
                    errorStats.addValue(det.getPositionErrorMetres());
                }
            }
            if (latencyStats.getN() > 0) {
                metrics.put("Detection Latency StdDev (s)", latencyStats.getStandardDeviation());
                metrics.put("Detection Latency P95 (s)", latencyStats.getPercentile(95));
                metrics.put("Detection Error StdDev (m)", errorStats.getStandardDeviation());
                metrics.put("Detection Error P95 (m)", errorStats.getPercentile(95));
            }
        }

        // ── Tracking metrics ────────────────────────────────────────
        metrics.put("Track Continuity Ratio", result.getTrackContinuity());
        metrics.put("Mean Track Error (m)", result.getMeanTrackErrorM());
        metrics.put("Track Update Rate (Hz)", result.getTrackUpdateRateHz());
        metrics.put("Total Track Drops", (double) result.getTotalTrackDrops());

        if (!result.getTrackingResults().isEmpty()) {
            DescriptiveStatistics trackErrStats = new DescriptiveStatistics();
            for (TrackingResult tr : result.getTrackingResults()) {
                trackErrStats.addValue(tr.getMeanPositionErrorMetres());
            }
            if (trackErrStats.getN() > 0) {
                metrics.put("Track Error P95 (m)", trackErrStats.getPercentile(95));
                metrics.put("Track Error Max (m)",
                        result.getTrackingResults().stream()
                                .mapToDouble(TrackingResult::getMaxPositionErrorMetres)
                                .max().orElse(0));
            }
        }

        // ── Identification metrics ──────────────────────────────────
        metrics.put("Pi (Probability of Identification)", result.getProbabilityOfIdentification());
        metrics.put("IFF Accuracy", result.getIffAccuracy());
        metrics.put("Mean Identification Latency (s)", result.getMeanIdentificationLatencyS());

        if (!result.getIdentificationResults().isEmpty()) {
            long correctClass = result.getIdentificationResults().stream()
                    .filter(IdentificationResult::isClassificationCorrect).count();
            metrics.put("Classification Accuracy",
                    (double) correctClass / result.getIdentificationResults().size());

            long payloadDetected = result.getIdentificationResults().stream()
                    .filter(IdentificationResult::isPayloadIdentified).count();
            long payloadTotal = result.getIdentificationResults().stream()
                    .filter(id -> {
                        // proxy check — those with > 0 estimated size likely had payload
                        return id.getEstimatedSizeCm2() > 0;
                    }).count();
            if (payloadTotal > 0) {
                metrics.put("Payload Detection Rate",
                        (double) payloadDetected / payloadTotal);
            }
        }

        // ── Overall ─────────────────────────────────────────────────
        metrics.put("Overall Score (0-100)", result.getOverallScore());
        metrics.put("Compliance (%)", result.getCompliancePercent());

        log.info("Computed {} metrics for scenario {}", metrics.size(), result.getScenarioId());
        return metrics;
    }

    /**
     * Compute CEP (Circular Error Probable) from position errors.
     * CEP50 = median radial error (50% of positions within this radius).
     */
    public double computeCep50(List<Double> positionErrors) {
        if (positionErrors.isEmpty()) return 0;
        DescriptiveStatistics stats = new DescriptiveStatistics();
        positionErrors.forEach(stats::addValue);
        return stats.getPercentile(50);
    }

    /**
     * Compute CEP90 (90th percentile of radial position errors).
     */
    public double computeCep90(List<Double> positionErrors) {
        if (positionErrors.isEmpty()) return 0;
        DescriptiveStatistics stats = new DescriptiveStatistics();
        positionErrors.forEach(stats::addValue);
        return stats.getPercentile(90);
    }
}
