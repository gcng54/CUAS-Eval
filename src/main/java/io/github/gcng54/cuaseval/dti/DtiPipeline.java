package io.github.gcng54.cuaseval.dti;

import io.github.gcng54.cuaseval.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Main DTI (Detection–Tracking–Identification) pipeline orchestrator.
 * Connects the three subsystems into an integrated evaluation flow.
 * <p>
 * This is the core of the CUAS-Eval performance evaluation:
 * <pre>
 *   TestScenario
 *       │
 *       ▼
 *  ┌──────────────┐
 *  │ DetectionSys  │ → List&lt;DetectionResult&gt;
 *  └──────────────┘
 *       │
 *       ▼
 *  ┌──────────────┐
 *  │ TrackingSys   │ → List&lt;TrackingResult&gt;
 *  └──────────────┘
 *       │
 *       ▼
 *  ┌──────────────┐
 *  │ IdentifySys   │ → List&lt;IdentificationResult&gt;
 *  └──────────────┘
 *       │
 *       ▼
 *  EvaluationResult
 * </pre>
 * Requirement links: FR13 (simultaneous / data fusion), architecture pipeline.
 * </p>
 */
public class DtiPipeline {

    private static final Logger log = LoggerFactory.getLogger(DtiPipeline.class);

    private final DetectionSystem detectionSystem;
    private final TrackingSystem trackingSystem;
    private final IdentificationSystem identificationSystem;

    // ── Constructors ────────────────────────────────────────────────────

    public DtiPipeline() {
        this.detectionSystem = new DetectionSystem();
        this.trackingSystem = new TrackingSystem();
        this.identificationSystem = new IdentificationSystem();
    }

    public DtiPipeline(DetectionSystem det, TrackingSystem trk, IdentificationSystem id) {
        this.detectionSystem = det;
        this.trackingSystem = trk;
        this.identificationSystem = id;
    }

    // ── Accessors ───────────────────────────────────────────────────────

    public DetectionSystem getDetectionSystem()              { return detectionSystem; }
    public TrackingSystem getTrackingSystem()                { return trackingSystem; }
    public IdentificationSystem getIdentificationSystem()    { return identificationSystem; }

    // ── Pipeline Execution ──────────────────────────────────────────────

    /**
     * Run the full DTI pipeline on a test scenario.
     * Produces an {@link EvaluationResult} with all metrics computed.
     *
     * @param scenario the test scenario to evaluate
     * @return aggregated evaluation result
     */
    public EvaluationResult execute(TestScenario scenario) {
        log.info("═══ DTI Pipeline START: {} ═══", scenario.getName());

        // Step 1: Detection
        log.info("── Phase 1: Detection ──");
        List<DetectionResult> detections = detectionSystem.processDetections(scenario);

        // Step 2: Tracking (only for detected targets)
        log.info("── Phase 2: Tracking ──");
        List<TrackingResult> tracks = trackingSystem.processTracks(scenario, detections);

        // Step 3: Identification (only for tracked targets)
        log.info("── Phase 3: Identification ──");
        List<IdentificationResult> identifications =
                identificationSystem.processIdentifications(scenario, tracks);

        // Step 4: Aggregate results
        EvaluationResult result = aggregateResults(
                scenario, detections, tracks, identifications);

        log.info("═══ DTI Pipeline COMPLETE: {} ═══", result);
        return result;
    }

    /**
     * Aggregate individual DTI results into a comprehensive evaluation.
     */
    private EvaluationResult aggregateResults(TestScenario scenario,
                                               List<DetectionResult> detections,
                                               List<TrackingResult> tracks,
                                               List<IdentificationResult> identifications) {
        EvaluationResult eval = new EvaluationResult(scenario.getScenarioId());
        eval.setDetectionResults(detections);
        eval.setTrackingResults(tracks);
        eval.setIdentificationResults(identifications);

        // ── Detection metrics ───────────────────────────────────────
        long totalTargets = scenario.getTargets().size();
        long realDetections = detections.stream()
                .filter(d -> !d.getTargetUid().startsWith("FALSE_ALARM"))
                .count();
        long trueDetections = detections.stream()
                .filter(d -> d.isDetected() && !d.getTargetUid().startsWith("FALSE_ALARM"))
                .count();
        long falseAlarms = detections.stream()
                .filter(d -> d.getTargetUid().startsWith("FALSE_ALARM"))
                .count();

        double pd = realDetections > 0 ? (double) trueDetections / realDetections : 0;
        eval.setProbabilityOfDetection(pd);
        eval.setFalseAlarmRate(totalTargets > 0 ? (double) falseAlarms / totalTargets : 0);

        double meanDetLatency = detections.stream()
                .filter(d -> d.isDetected() && !d.getTargetUid().startsWith("FALSE_ALARM"))
                .mapToDouble(DetectionResult::getLatencySeconds)
                .average().orElse(0);
        eval.setMeanDetectionLatencyS(meanDetLatency);

        double meanDetError = detections.stream()
                .filter(d -> d.isDetected() && !d.getTargetUid().startsWith("FALSE_ALARM"))
                .mapToDouble(DetectionResult::getPositionErrorMetres)
                .average().orElse(0);
        eval.setMeanDetectionErrorM(meanDetError);

        // ── Tracking metrics ────────────────────────────────────────
        long trackedCount = tracks.stream().filter(TrackingResult::isTrackMaintained).count();
        double continuity = tracks.isEmpty() ? 0 : (double) trackedCount / tracks.size();
        eval.setTrackContinuity(continuity);

        double meanTrackErr = tracks.stream()
                .mapToDouble(TrackingResult::getMeanPositionErrorMetres)
                .average().orElse(0);
        eval.setMeanTrackErrorM(meanTrackErr);

        double meanUpdateRate = tracks.stream()
                .mapToDouble(TrackingResult::getUpdateRateHz)
                .average().orElse(0);
        eval.setTrackUpdateRateHz(meanUpdateRate);

        int totalDrops = tracks.stream().mapToInt(TrackingResult::getTrackDropCount).sum();
        eval.setTotalTrackDrops(totalDrops);

        // ── Identification metrics ──────────────────────────────────
        long identifiedCount = identifications.stream()
                .filter(IdentificationResult::isIdentified).count();
        double pi = identifications.isEmpty()
                ? 0 : (double) identifiedCount / identifications.size();
        eval.setProbabilityOfIdentification(pi);

        long iffCorrect = identifications.stream()
                .filter(id -> id.getIffResult() != null
                        && id.getIffResult() == id.isTruthFriendly())
                .count();
        double iffAcc = identifications.isEmpty()
                ? 0 : (double) iffCorrect / identifications.size();
        eval.setIffAccuracy(iffAcc);

        double meanIdLatency = identifications.stream()
                .filter(IdentificationResult::isIdentified)
                .mapToDouble(IdentificationResult::getLatencySeconds)
                .average().orElse(0);
        eval.setMeanIdentificationLatencyS(meanIdLatency);

        // ── Overall score (weighted) ────────────────────────────────
        double score = pd * 40 + continuity * 30 + pi * 30; // 0–100
        eval.setOverallScore(score);
        eval.setPassed(score >= 60 && pd >= 0.7);

        return eval;
    }
}
