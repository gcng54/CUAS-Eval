package io.github.gcng54.cuaseval.dti;

import io.github.gcng54.cuaseval.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Multi-DTI system-of-systems evaluator.
 * Manages multiple DTI pipeline nodes, each with its own sensor configuration,
 * and fuses their results into a unified system-level evaluation.
 *
 * <p>Architecture:</p>
 * <pre>
 *  ┌─────────────┐   ┌─────────────┐   ┌─────────────┐
 *  │ DTI Node 1  │   │ DTI Node 2  │   │ DTI Node N  │
 *  │ (Sensor A)  │   │ (Sensor B)  │   │ (Sensor N)  │
 *  └──────┬──────┘   └──────┬──────┘   └──────┬──────┘
 *         │                 │                 │
 *         └────────────┬────┘─────────────────┘
 *                      │
 *              ┌───────▼───────┐
 *              │  Fusion Layer │
 *              │  (OR / Vote)  │
 *              └───────┬───────┘
 *                      │
 *              ┌───────▼───────┐
 *              │  System-Level │
 *              │  Evaluation   │
 *              └───────────────┘
 * </pre>
 *
 * <p>Supports:</p>
 * <ul>
 *   <li>Multiple sensor nodes at different locations</li>
 *   <li>Detection fusion (OR-logic, k-of-n voting)</li>
 *   <li>Track fusion (best track selection, weighted average)</li>
 *   <li>Identification fusion (consensus voting)</li>
 *   <li>System-of-systems metrics and coverage analysis</li>
 * </ul>
 */
public class MultiDtiSystem {

    private static final Logger log = LoggerFactory.getLogger(MultiDtiSystem.class);

    /** Named DTI nodes in this system */
    private final List<DtiNode> nodes = new ArrayList<>();

    /** Fusion strategy for detection */
    private FusionStrategy detectionFusion = FusionStrategy.OR_LOGIC;

    /** Voting threshold (k) for k-of-n detection fusion */
    private int votingThreshold = 2;

    // ── DTI Node ────────────────────────────────────────────────────────

    /**
     * A single DTI node — a sensor position with its own DTI pipeline.
     */
    public static class DtiNode {
        private final String nodeId;
        private final String nodeName;
        private final GeoPosition position;
        private final CuasSensor sensor;
        private final DtiPipeline pipeline;

        public DtiNode(String nodeId, String nodeName, GeoPosition position, CuasSensor sensor) {
            this.nodeId = nodeId;
            this.nodeName = nodeName;
            this.position = position;
            this.sensor = sensor;
            this.pipeline = new DtiPipeline();
        }

        public String getNodeId()        { return nodeId; }
        public String getNodeName()      { return nodeName; }
        public GeoPosition getPosition() { return position; }
        public CuasSensor getSensor()    { return sensor; }
        public DtiPipeline getPipeline() { return pipeline; }

        @Override
        public String toString() {
            return String.format(Locale.ENGLISH, "Node[%s] %s @ %s sensor=%s",
                    nodeId, nodeName, position, sensor.getName());
        }
    }

    /** Fusion strategy for combining multi-sensor detections. */
    public enum FusionStrategy {
        /** Any sensor detection counts (highest sensitivity, highest FAR) */
        OR_LOGIC,
        /** k-of-n sensors must detect (balanced) */
        VOTING,
        /** All sensors must detect (lowest FAR, lowest sensitivity) */
        AND_LOGIC,
        /** Use best individual sensor result per target */
        BEST_SENSOR
    }

    // ── Node Management ─────────────────────────────────────────────────

    /**
     * Add a DTI node with a sensor at a specific position.
     */
    public DtiNode addNode(String nodeId, String nodeName,
                           GeoPosition position, CuasSensor sensor) {
        DtiNode node = new DtiNode(nodeId, nodeName, position, sensor);
        nodes.add(node);
        log.info("Added DTI node: {}", node);
        return node;
    }

    /**
     * Remove a DTI node by ID.
     */
    public boolean removeNode(String nodeId) {
        return nodes.removeIf(n -> n.getNodeId().equals(nodeId));
    }

    /**
     * Get all DTI nodes.
     */
    public List<DtiNode> getNodes() {
        return Collections.unmodifiableList(nodes);
    }

    /**
     * Get node count.
     */
    public int getNodeCount() {
        return nodes.size();
    }

    /**
     * Clear all nodes.
     */
    public void clearNodes() {
        nodes.clear();
    }

    // ── Configuration ───────────────────────────────────────────────────

    public void setDetectionFusion(FusionStrategy strategy) { this.detectionFusion = strategy; }
    public FusionStrategy getDetectionFusion()              { return detectionFusion; }
    public void setVotingThreshold(int k)                   { this.votingThreshold = k; }
    public int getVotingThreshold()                         { return votingThreshold; }

    // ── System Execution ────────────────────────────────────────────────

    /**
     * Execute the multi-DTI system on a test scenario.
     * Each node evaluates independently, then results are fused.
     *
     * @param scenario the test scenario
     * @return fused system-level evaluation result
     */
    public EvaluationResult execute(TestScenario scenario) {
        if (nodes.isEmpty()) {
            log.warn("No DTI nodes configured — running single pipeline fallback");
            return new DtiPipeline().execute(scenario);
        }

        log.info("╔═══════════════════════════════════════════════════╗");
        log.info("║ MULTI-DTI SYSTEM EXECUTION: {} nodes              ║", nodes.size());
        log.info("╚═══════════════════════════════════════════════════╝");

        // Step 1: Create per-node scenarios with sensor-specific environments
        Map<DtiNode, EvaluationResult> nodeResults = new LinkedHashMap<>();
        for (DtiNode node : nodes) {
            log.info("── Evaluating node: {} ──", node.getNodeId());
            TestScenario nodeScenario = createNodeScenario(scenario, node);
            EvaluationResult result = node.getPipeline().execute(nodeScenario);
            nodeResults.put(node, result);
            log.info("  Node {} → Pd={:.3f} cont={:.3f} Pi={:.3f} score={:.1f}",
                    node.getNodeId(),
                    result.getProbabilityOfDetection(),
                    result.getTrackContinuity(),
                    result.getProbabilityOfIdentification(),
                    result.getOverallScore());
        }

        // Step 2: Fuse results
        EvaluationResult fusedResult = fuseResults(scenario, nodeResults);

        log.info("═══ FUSED RESULT: Pd={:.3f} cont={:.3f} Pi={:.3f} score={:.1f} ═══",
                fusedResult.getProbabilityOfDetection(),
                fusedResult.getTrackContinuity(),
                fusedResult.getProbabilityOfIdentification(),
                fusedResult.getOverallScore());

        return fusedResult;
    }

    /**
     * Create a modified scenario for a specific DTI node,
     * using the node's sensor as the only sensor in the environment.
     */
    private TestScenario createNodeScenario(TestScenario original, DtiNode node) {
        // Clone the environment with just this node's sensor
        TestEnvironment origEnv = original.getEnvironment();
        TestEnvironment nodeEnv = new TestEnvironment(
                origEnv.getName() + " [" + node.getNodeId() + "]",
                origEnv.getCentrePosition(),
                origEnv.getObservationRadiusM());
        nodeEnv.setWeather(origEnv.getWeather());
        nodeEnv.setTimeOfDay(origEnv.getTimeOfDay());
        nodeEnv.setTerrainType(origEnv.getTerrainType());
        nodeEnv.getParameters().putAll(origEnv.getParameters());
        nodeEnv.getObstacles().addAll(origEnv.getObstacles());

        // Add just this node's sensor
        TestEnvironment.SensorSite site = new TestEnvironment.SensorSite(
                node.getSensor(), node.getPosition());
        nodeEnv.getSensorSites().add(site);

        // Create scenario copy with the node environment
        TestScenario nodeScenario = new TestScenario(
                original.getScenarioId() + "-" + node.getNodeId(),
                original.getName() + " [" + node.getNodeId() + "]",
                nodeEnv);
        nodeScenario.setDescription(original.getDescription());
        nodeScenario.setDurationSeconds(original.getDurationSeconds());
        nodeScenario.getRequirementIds().addAll(original.getRequirementIds());

        // Copy targets and flight plans
        for (int i = 0; i < original.getTargets().size(); i++) {
            UasTarget target = original.getTargets().get(i);
            TestScenario.FlightPlan plan = i < original.getFlightPlans().size()
                    ? original.getFlightPlans().get(i) : null;
            nodeScenario.addTarget(target, plan);
        }

        return nodeScenario;
    }

    /**
     * Fuse results from all DTI nodes into a unified evaluation.
     */
    private EvaluationResult fuseResults(TestScenario scenario,
                                          Map<DtiNode, EvaluationResult> nodeResults) {
        EvaluationResult fused = new EvaluationResult(scenario.getScenarioId() + "-FUSED");

        // ── Detection Fusion ────────────────────────────────────────
        List<DetectionResult> fusedDetections = fuseDetections(scenario, nodeResults);
        fused.setDetectionResults(fusedDetections);

        long totalTargets = scenario.getTargets().size();
        long trueDetections = fusedDetections.stream()
                .filter(d -> d.isDetected() && !d.getTargetUid().startsWith("FALSE_ALARM"))
                .count();
        long falseAlarms = fusedDetections.stream()
                .filter(d -> d.getTargetUid().startsWith("FALSE_ALARM"))
                .count();

        double pd = totalTargets > 0 ? (double) trueDetections / totalTargets : 0;
        fused.setProbabilityOfDetection(Math.min(1.0, pd));
        fused.setFalseAlarmRate(totalTargets > 0 ? (double) falseAlarms / totalTargets : 0);

        // Mean detection latency (best across nodes)
        double meanLat = fusedDetections.stream()
                .filter(d -> d.isDetected() && !d.getTargetUid().startsWith("FALSE_ALARM"))
                .mapToDouble(DetectionResult::getLatencySeconds)
                .average().orElse(0);
        fused.setMeanDetectionLatencyS(meanLat);

        double meanErr = fusedDetections.stream()
                .filter(d -> d.isDetected() && !d.getTargetUid().startsWith("FALSE_ALARM"))
                .mapToDouble(DetectionResult::getPositionErrorMetres)
                .average().orElse(0);
        fused.setMeanDetectionErrorM(meanErr);

        // ── Tracking Fusion ─────────────────────────────────────────
        // Take best tracking results per target across all nodes
        List<TrackingResult> fusedTracks = fuseTracks(nodeResults);
        fused.setTrackingResults(fusedTracks);

        long trackedCount = fusedTracks.stream()
                .filter(TrackingResult::isTrackMaintained).count();
        fused.setTrackContinuity(fusedTracks.isEmpty() ? 0
                : (double) trackedCount / fusedTracks.size());

        double meanTrackErr = fusedTracks.stream()
                .mapToDouble(TrackingResult::getMeanPositionErrorMetres)
                .average().orElse(0);
        fused.setMeanTrackErrorM(meanTrackErr);

        double meanUpdateRate = fusedTracks.stream()
                .mapToDouble(TrackingResult::getUpdateRateHz)
                .average().orElse(0);
        fused.setTrackUpdateRateHz(meanUpdateRate);

        int totalDrops = fusedTracks.stream()
                .mapToInt(TrackingResult::getTrackDropCount).sum();
        fused.setTotalTrackDrops(totalDrops);

        // ── Identification Fusion ───────────────────────────────────
        List<IdentificationResult> fusedIds = fuseIdentifications(nodeResults);
        fused.setIdentificationResults(fusedIds);

        long idCount = fusedIds.stream()
                .filter(IdentificationResult::isIdentified).count();
        double pi = fusedIds.isEmpty() ? 0 : (double) idCount / fusedIds.size();
        fused.setProbabilityOfIdentification(pi);

        long iffCorrect = fusedIds.stream()
                .filter(id -> id.getIffResult() != null
                        && id.getIffResult() == id.isTruthFriendly())
                .count();
        fused.setIffAccuracy(fusedIds.isEmpty() ? 0
                : (double) iffCorrect / fusedIds.size());

        double meanIdLat = fusedIds.stream()
                .filter(IdentificationResult::isIdentified)
                .mapToDouble(IdentificationResult::getLatencySeconds)
                .average().orElse(0);
        fused.setMeanIdentificationLatencyS(meanIdLat);

        // ── Overall Score ───────────────────────────────────────────
        double score = pd * 40 + fused.getTrackContinuity() * 30 + pi * 30;
        fused.setOverallScore(score);
        fused.setPassed(score >= 60 && pd >= 0.7);

        return fused;
    }

    // ── Fusion Algorithms ───────────────────────────────────────────────

    /**
     * Fuse detection results from all nodes per target.
     */
    private List<DetectionResult> fuseDetections(TestScenario scenario,
                                                  Map<DtiNode, EvaluationResult> nodeResults) {
        List<DetectionResult> fused = new ArrayList<>();

        for (UasTarget target : scenario.getTargets()) {
            // Collect detections for this target from all nodes
            List<DetectionResult> perNode = new ArrayList<>();
            for (EvaluationResult nr : nodeResults.values()) {
                for (DetectionResult d : nr.getDetectionResults()) {
                    if (target.getUid().equals(d.getTargetUid())) {
                        perNode.add(d);
                    }
                }
            }

            DetectionResult fusedDet = applyDetectionFusion(target, perNode);
            fused.add(fusedDet);
        }

        // False alarms — deduplicate (AND reduces, OR increases)
        Set<String> faIds = new HashSet<>();
        for (EvaluationResult nr : nodeResults.values()) {
            for (DetectionResult d : nr.getDetectionResults()) {
                if (d.getTargetUid().startsWith("FALSE_ALARM")) {
                    if (detectionFusion == FusionStrategy.OR_LOGIC) {
                        faIds.add(d.getTargetUid());
                    }
                }
            }
        }
        // Reduce false alarms with multi-sensor confirmation
        int fusedFaCount = Math.max(0, faIds.size() / Math.max(1, nodes.size() / 2));
        for (int i = 0; i < fusedFaCount; i++) {
            DetectionResult fa = new DetectionResult();
            fa.setTargetUid("FALSE_ALARM_FUSED_" + (i + 1));
            fa.setDetected(true);
            fa.setCorrectClassification(false);
            fa.setSensorType("FUSED");
            fused.add(fa);
        }

        return fused;
    }

    /**
     * Apply detection fusion strategy for a single target.
     */
    private DetectionResult applyDetectionFusion(UasTarget target,
                                                  List<DetectionResult> nodeDetections) {
        DetectionResult best = new DetectionResult();
        best.setTargetUid(target.getUid());
        best.setTruthPosition(target.getPosition());

        if (nodeDetections.isEmpty()) {
            best.setDetected(false);
            best.setSensorType("NONE");
            return best;
        }

        long detectedCount = nodeDetections.stream()
                .filter(DetectionResult::isDetected).count();

        boolean fusedDetected = switch (detectionFusion) {
            case OR_LOGIC -> detectedCount > 0;
            case VOTING -> detectedCount >= Math.min(votingThreshold, nodes.size());
            case AND_LOGIC -> detectedCount == nodeDetections.size();
            case BEST_SENSOR -> detectedCount > 0;
        };

        best.setDetected(fusedDetected);
        best.setSensorType("FUSED(" + detectedCount + "/" + nodeDetections.size() + ")");

        if (fusedDetected) {
            // Use best latency and position from detecting nodes
            DetectionResult bestNode = nodeDetections.stream()
                    .filter(DetectionResult::isDetected)
                    .min(Comparator.comparingDouble(DetectionResult::getPositionErrorMetres))
                    .orElse(nodeDetections.get(0));

            best.setLatencySeconds(bestNode.getLatencySeconds());
            best.setDetectionTime(bestNode.getDetectionTime());
            best.setDisplayLatencySeconds(bestNode.getDisplayLatencySeconds());
            best.setReportedPosition(bestNode.getReportedPosition());
            best.setCorrectClassification(bestNode.isCorrectClassification());
            best.computePositionError();
        }

        return best;
    }

    /**
     * Fuse tracking results — take best track per target across nodes.
     */
    private List<TrackingResult> fuseTracks(Map<DtiNode, EvaluationResult> nodeResults) {
        // Group all track results by target UID
        Map<String, List<TrackingResult>> byTarget = new LinkedHashMap<>();
        for (EvaluationResult nr : nodeResults.values()) {
            for (TrackingResult tr : nr.getTrackingResults()) {
                byTarget.computeIfAbsent(tr.getTargetUid(), k -> new ArrayList<>())
                        .add(tr);
            }
        }

        // For each target, select best track (highest continuity)
        List<TrackingResult> fused = new ArrayList<>();
        for (Map.Entry<String, List<TrackingResult>> entry : byTarget.entrySet()) {
            TrackingResult best = entry.getValue().stream()
                    .filter(TrackingResult::isTrackMaintained)
                    .min(Comparator.comparingDouble(TrackingResult::getMeanPositionErrorMetres))
                    .orElse(entry.getValue().get(0));
            fused.add(best);
        }

        return fused;
    }

    /**
     * Fuse identification results — consensus voting.
     */
    private List<IdentificationResult> fuseIdentifications(
            Map<DtiNode, EvaluationResult> nodeResults) {
        Map<String, List<IdentificationResult>> byTarget = new LinkedHashMap<>();
        for (EvaluationResult nr : nodeResults.values()) {
            for (IdentificationResult ir : nr.getIdentificationResults()) {
                byTarget.computeIfAbsent(ir.getTargetUid(), k -> new ArrayList<>())
                        .add(ir);
            }
        }

        List<IdentificationResult> fused = new ArrayList<>();
        for (Map.Entry<String, List<IdentificationResult>> entry : byTarget.entrySet()) {
            // Best identification = one with highest confidence
            IdentificationResult best = entry.getValue().stream()
                    .filter(IdentificationResult::isIdentified)
                    .max(Comparator.comparingDouble(IdentificationResult::getConfidence))
                    .orElse(entry.getValue().get(0));
            fused.add(best);
        }

        return fused;
    }

    // ── Coverage Analysis ───────────────────────────────────────────────

    /**
     * Compute system coverage statistics.
     * @return map of coverage metrics
     */
    public Map<String, Double> computeCoverageMetrics() {
        Map<String, Double> metrics = new LinkedHashMap<>();
        metrics.put("nodeCount", (double) nodes.size());

        if (nodes.isEmpty()) return metrics;

        // Total coverage area (union of all sensor ranges)
        double totalAreaKm2 = 0;
        for (DtiNode node : nodes) {
            double r = node.getSensor().getMaxRangeM() / 1000.0;
            double az = node.getSensor().getAzimuthCoverageDeg();
            totalAreaKm2 += Math.PI * r * r * (az / 360.0);
        }
        metrics.put("totalCoverageAreaKm2", totalAreaKm2);

        // Average sensor range
        double avgRange = nodes.stream()
                .mapToDouble(n -> n.getSensor().getMaxRangeM())
                .average().orElse(0);
        metrics.put("avgSensorRangeM", avgRange);

        // System-level Pd estimate (fused)
        double sysPd = 1.0;
        for (DtiNode node : nodes) {
            sysPd *= (1.0 - node.getSensor().getBasePd());
        }
        sysPd = 1.0 - sysPd; // OR fusion
        metrics.put("estimatedSystemPd", sysPd);

        // System-level FAR estimate (reduced by fusion)
        double sysFar = nodes.stream()
                .mapToDouble(n -> n.getSensor().getFalseAlarmRate())
                .reduce(1.0, (a, b) -> a * b);  // AND-like reduction
        metrics.put("estimatedSystemFAR", sysFar);

        // Power consumption
        double totalPower = nodes.stream()
                .mapToDouble(n -> n.getSensor().getPowerConsumptionW())
                .sum();
        metrics.put("totalPowerW", totalPower);

        // Weight
        double totalWeight = nodes.stream()
                .mapToDouble(n -> n.getSensor().getWeightKg())
                .sum();
        metrics.put("totalWeightKg", totalWeight);

        return metrics;
    }

    /**
     * Get a summary of the multi-DTI system configuration.
     */
    public String getSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format(Locale.ENGLISH,
                "Multi-DTI System — %d nodes, fusion=%s\n", nodes.size(), detectionFusion));
        sb.append("═══════════════════════════════════════\n");

        for (DtiNode node : nodes) {
            sb.append(String.format(Locale.ENGLISH,
                    "  %s: %s @ (%.4f, %.4f) — %s, range=%.0fm\n",
                    node.getNodeId(), node.getNodeName(),
                    node.getPosition().getLatitude(), node.getPosition().getLongitude(),
                    node.getSensor().getSensorType().getDisplayName(),
                    node.getSensor().getMaxRangeM()));
        }

        Map<String, Double> cov = computeCoverageMetrics();
        sb.append(String.format(Locale.ENGLISH,
                "\nCoverage: %.1f km² | Est. Pd: %.3f | Est. FAR: %.5f\n",
                cov.getOrDefault("totalCoverageAreaKm2", 0.0),
                cov.getOrDefault("estimatedSystemPd", 0.0),
                cov.getOrDefault("estimatedSystemFAR", 0.0)));
        sb.append(String.format(Locale.ENGLISH,
                "Power: %.0f W | Weight: %.0f kg\n",
                cov.getOrDefault("totalPowerW", 0.0),
                cov.getOrDefault("totalWeightKg", 0.0)));

        return sb.toString();
    }
}
