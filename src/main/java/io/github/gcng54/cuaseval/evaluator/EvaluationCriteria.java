package io.github.gcng54.cuaseval.evaluator;

import io.github.gcng54.cuaseval.model.Requirement;
import io.github.gcng54.cuaseval.model.EvaluationResult;

import java.util.HashMap;
import java.util.Map;

/**
 * Defines pass/fail criteria and threshold values for each CWA 18150 requirement.
 * Maps requirement IDs to their minimum acceptance thresholds.
 * <p>
 * Used by {@link TestEvaluator} to determine per-requirement compliance.
 * Supports JSON save/load via AssetManager (EM-01, EM-02).
 * </p>
 */
public class EvaluationCriteria {

    /** Name of this criteria profile (e.g. "CWA 18150 Default", "NATO Standard"). */
    private String profileName = "CWA 18150 Default";

    /** Scoring weights: detection, tracking, identification [0-100, sum = 100]. */
    private double detectionWeight = 40;
    private double trackingWeight = 30;
    private double identificationWeight = 30;

    /**
     * Threshold entry: minimum acceptable value for a metric.
     * Made mutable for JSON serialization.
     */
    public static class Threshold {
        private String requirementId;
        private String metricName;
        private double minValue;
        private double maxValue;
        private String unit;

        /** No-arg constructor for Jackson. */
        public Threshold() {}

        public Threshold(String requirementId, String metricName,
                         double minValue, double maxValue, String unit) {
            this.requirementId = requirementId;
            this.metricName = metricName;
            this.minValue = minValue;
            this.maxValue = maxValue;
            this.unit = unit;
        }

        public String getRequirementId() { return requirementId; }
        public String getMetricName()    { return metricName; }
        public double getMinValue()      { return minValue; }
        public double getMaxValue()      { return maxValue; }
        public String getUnit()          { return unit; }

        public void setRequirementId(String requirementId) { this.requirementId = requirementId; }
        public void setMetricName(String metricName)       { this.metricName = metricName; }
        public void setMinValue(double minValue)           { this.minValue = minValue; }
        public void setMaxValue(double maxValue)           { this.maxValue = maxValue; }
        public void setUnit(String unit)                   { this.unit = unit; }

        /**
         * Check if the given value passes this threshold.
         * For "higher is better" metrics, value must be >= minValue.
         * For "lower is better" metrics (maxValue < infinity), value must be <= maxValue.
         */
        public boolean passes(double value) {
            return value >= minValue && value <= maxValue;
        }

        @Override
        public String toString() {
            return String.format("[%s] %s: %.3f–%.3f %s", requirementId, metricName, minValue, maxValue, unit);
        }
    }

    /** Map from requirement ID to threshold */
    private final Map<String, Threshold> thresholds = new HashMap<>();

    // ── Constructor with defaults ───────────────────────────────────────

    public EvaluationCriteria() {
        initializeDefaults();
    }

    /**
     * Initialize default CWA 18150 acceptance thresholds.
     */
    private void initializeDefaults() {
        // Detection thresholds
        addThreshold("FR01", "probabilityOfDetection", 0.90, 1.0, "ratio");
        addThreshold("FR02", "probabilityOfDetection", 0.85, 1.0, "ratio");
        addThreshold("FR09", "probabilityOfDetection", 0.80, 1.0, "ratio");
        addThreshold("FR15", "falseAlarmRate",          0.0, 0.05, "ratio");
        addThreshold("PR09", "probabilityOfDetection", 0.95, 1.0, "ratio");

        // Detection timing
        addThreshold("TP_D01", "detectionLatency",  0.0, 5.0, "seconds");
        addThreshold("TP_D02", "displayLatency",     0.0, 3.0, "seconds");

        // Tracking thresholds
        addThreshold("FR04", "trackContinuity",      0.90, 1.0, "ratio");
        addThreshold("FR17", "uidPreservation",      0.95, 1.0, "ratio");
        addThreshold("FR18", "trackAfterLoss",       0.80, 1.0, "ratio");
        addThreshold("PR20", "positionAccuracy",      0.0, 10.0, "metres");

        // Tracking timing
        addThreshold("TP_D16", "updateRate",          1.0, Double.MAX_VALUE, "Hz");

        // Identification thresholds
        addThreshold("FR06", "probabilityOfId",      0.75, 1.0, "ratio");
        addThreshold("FR14", "iffAccuracy",           0.90, 1.0, "ratio");
        addThreshold("FR16", "birdRejection",        0.85, 1.0, "ratio");

        // Weather performance
        addThreshold("PR08", "weatherPd",             0.70, 1.0, "ratio");
        addThreshold("PR13", "nightPi",               0.60, 1.0, "ratio");
        addThreshold("PR14", "fogPi",                 0.40, 1.0, "ratio");
    }

    // ── API ─────────────────────────────────────────────────────────────

    public void addThreshold(String reqId, String metric,
                              double min, double max, String unit) {
        thresholds.put(reqId, new Threshold(reqId, metric, min, max, unit));
    }

    public void removeThreshold(String reqId) {
        thresholds.remove(reqId);
    }

    public Threshold getThreshold(String reqId) {
        return thresholds.get(reqId);
    }

    public Map<String, Threshold> getAllThresholds() {
        return thresholds;
    }

    public void setThresholds(Map<String, Threshold> thresholds) {
        this.thresholds.clear();
        this.thresholds.putAll(thresholds);
    }

    public String getProfileName()                    { return profileName; }
    public void setProfileName(String profileName)    { this.profileName = profileName; }

    public double getDetectionWeight()                { return detectionWeight; }
    public void setDetectionWeight(double w)          { this.detectionWeight = w; }

    public double getTrackingWeight()                 { return trackingWeight; }
    public void setTrackingWeight(double w)           { this.trackingWeight = w; }

    public double getIdentificationWeight()           { return identificationWeight; }
    public void setIdentificationWeight(double w)     { this.identificationWeight = w; }

    /**
     * Evaluate a single requirement against the evaluation result.
     * Returns true if the relevant metric passes the threshold.
     */
    public boolean evaluateRequirement(String reqId, EvaluationResult result) {
        Threshold threshold = thresholds.get(reqId);
        if (threshold == null) return true; // no threshold = pass by default

        double value = extractMetric(threshold.metricName, result);
        return threshold.passes(value);
    }

    /**
     * Extract the metric value from an EvaluationResult by metric name.
     */
    private double extractMetric(String metricName, EvaluationResult result) {
        return switch (metricName) {
            case "probabilityOfDetection" -> result.getProbabilityOfDetection();
            case "falseAlarmRate"          -> result.getFalseAlarmRate();
            case "detectionLatency"        -> result.getMeanDetectionLatencyS();
            case "displayLatency"          -> result.getMeanDetectionLatencyS() + 0.3;
            case "trackContinuity"         -> result.getTrackContinuity();
            case "uidPreservation"         -> result.getTrackContinuity(); // proxy
            case "trackAfterLoss"          -> result.getTrackContinuity(); // proxy
            case "positionAccuracy"        -> result.getMeanTrackErrorM();
            case "updateRate"              -> result.getTrackUpdateRateHz();
            case "probabilityOfId"         -> result.getProbabilityOfIdentification();
            case "iffAccuracy"             -> result.getIffAccuracy();
            case "birdRejection"           -> 0.9; // placeholder
            case "weatherPd"               -> result.getProbabilityOfDetection();
            case "nightPi"                 -> result.getProbabilityOfIdentification();
            case "fogPi"                   -> result.getProbabilityOfIdentification();
            default -> 0;
        };
    }
}
