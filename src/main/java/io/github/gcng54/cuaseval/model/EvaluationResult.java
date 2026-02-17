package io.github.gcng54.cuaseval.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Aggregated evaluation result for a complete test scenario.
 * Combines Detection, Tracking, and Identification results with
 * computed metrics per CWA 18150 evaluation criteria.
 */
public class EvaluationResult {

    /** Scenario that was evaluated */
    private String scenarioId;

    /** Overall pass/fail verdict */
    private boolean passed;

    /** Overall evaluation score (0–100) */
    private double overallScore;

    // ── Detection Metrics ───────────────────────────────────────────────

    /** Probability of detection (Pd) — FR01, PR09 */
    private double probabilityOfDetection;

    /** False alarm rate — FR15 */
    private double falseAlarmRate;

    /** Mean detection latency in seconds — TP_D01 */
    private double meanDetectionLatencyS;

    /** Mean detection position error in metres — TP_D03 */
    private double meanDetectionErrorM;

    // ── Tracking Metrics ────────────────────────────────────────────────

    /** Track continuity ratio (0–1) — FR04, FR18 */
    private double trackContinuity;

    /** Mean track position error in metres — PR20 */
    private double meanTrackErrorM;

    /** Track update rate Hz — TP_D16 */
    private double trackUpdateRateHz;

    /** Number of track drops */
    private int totalTrackDrops;

    // ── Identification Metrics ──────────────────────────────────────────

    /** Probability of correct identification — FR06 */
    private double probabilityOfIdentification;

    /** IFF accuracy — FR14 */
    private double iffAccuracy;

    /** Mean identification latency in seconds */
    private double meanIdentificationLatencyS;

    // ── Requirement Compliance ──────────────────────────────────────────

    /** List of requirement IDs that passed */
    private List<String> passedRequirements = new ArrayList<>();

    /** List of requirement IDs that failed */
    private List<String> failedRequirements = new ArrayList<>();

    /** Individual detection results */
    private List<DetectionResult> detectionResults = new ArrayList<>();

    /** Individual tracking results */
    private List<TrackingResult> trackingResults = new ArrayList<>();

    /** Individual identification results */
    private List<IdentificationResult> identificationResults = new ArrayList<>();

    // ── Constructors ────────────────────────────────────────────────────

    public EvaluationResult() {}

    public EvaluationResult(String scenarioId) {
        this.scenarioId = scenarioId;
    }

    // ── Accessors ───────────────────────────────────────────────────────

    public String getScenarioId()                    { return scenarioId; }
    public boolean isPassed()                        { return passed; }
    public double getOverallScore()                  { return overallScore; }

    public double getProbabilityOfDetection()        { return probabilityOfDetection; }
    public double getFalseAlarmRate()                { return falseAlarmRate; }
    public double getMeanDetectionLatencyS()         { return meanDetectionLatencyS; }
    public double getMeanDetectionErrorM()           { return meanDetectionErrorM; }

    public double getTrackContinuity()               { return trackContinuity; }
    public double getMeanTrackErrorM()               { return meanTrackErrorM; }
    public double getTrackUpdateRateHz()             { return trackUpdateRateHz; }
    public int getTotalTrackDrops()                  { return totalTrackDrops; }

    public double getProbabilityOfIdentification()   { return probabilityOfIdentification; }
    public double getIffAccuracy()                   { return iffAccuracy; }
    public double getMeanIdentificationLatencyS()    { return meanIdentificationLatencyS; }

    public List<String> getPassedRequirements()      { return passedRequirements; }
    public List<String> getFailedRequirements()      { return failedRequirements; }

    public List<DetectionResult> getDetectionResults()             { return detectionResults; }
    public List<TrackingResult> getTrackingResults()               { return trackingResults; }
    public List<IdentificationResult> getIdentificationResults()   { return identificationResults; }

    public void setScenarioId(String scenarioId)                                  { this.scenarioId = scenarioId; }
    public void setPassed(boolean passed)                                          { this.passed = passed; }
    public void setOverallScore(double overallScore)                              { this.overallScore = overallScore; }
    public void setProbabilityOfDetection(double probabilityOfDetection)          { this.probabilityOfDetection = probabilityOfDetection; }
    public void setFalseAlarmRate(double falseAlarmRate)                          { this.falseAlarmRate = falseAlarmRate; }
    public void setMeanDetectionLatencyS(double meanDetectionLatencyS)           { this.meanDetectionLatencyS = meanDetectionLatencyS; }
    public void setMeanDetectionErrorM(double meanDetectionErrorM)               { this.meanDetectionErrorM = meanDetectionErrorM; }
    public void setTrackContinuity(double trackContinuity)                        { this.trackContinuity = trackContinuity; }
    public void setMeanTrackErrorM(double meanTrackErrorM)                        { this.meanTrackErrorM = meanTrackErrorM; }
    public void setTrackUpdateRateHz(double trackUpdateRateHz)                    { this.trackUpdateRateHz = trackUpdateRateHz; }
    public void setTotalTrackDrops(int totalTrackDrops)                           { this.totalTrackDrops = totalTrackDrops; }
    public void setProbabilityOfIdentification(double probabilityOfIdentification){ this.probabilityOfIdentification = probabilityOfIdentification; }
    public void setIffAccuracy(double iffAccuracy)                                { this.iffAccuracy = iffAccuracy; }
    public void setMeanIdentificationLatencyS(double meanIdentificationLatencyS)  { this.meanIdentificationLatencyS = meanIdentificationLatencyS; }

    public void setPassedRequirements(List<String> passedRequirements)                          { this.passedRequirements = passedRequirements; }
    public void setFailedRequirements(List<String> failedRequirements)                          { this.failedRequirements = failedRequirements; }
    public void setDetectionResults(List<DetectionResult> detectionResults)                     { this.detectionResults = detectionResults; }
    public void setTrackingResults(List<TrackingResult> trackingResults)                        { this.trackingResults = trackingResults; }
    public void setIdentificationResults(List<IdentificationResult> identificationResults)     { this.identificationResults = identificationResults; }

    /**
     * Compute compliance percentage — ratio of passed requirements to total.
     */
    public double getCompliancePercent() {
        int total = passedRequirements.size() + failedRequirements.size();
        return total > 0 ? (100.0 * passedRequirements.size() / total) : 0;
    }

    @Override
    public String toString() {
        return String.format(Locale.ENGLISH, "Eval[%s] score=%.1f pass=%b Pd=%.2f Pt=%.2f Pi=%.2f compliance=%.1f%%",
                scenarioId, overallScore, passed,
                probabilityOfDetection, trackContinuity,
                probabilityOfIdentification, getCompliancePercent());
    }
}
