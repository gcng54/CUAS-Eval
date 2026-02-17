package io.github.gcng54.cuaseval.evaluator;

import io.github.gcng54.cuaseval.dti.DtiPipeline;
import io.github.gcng54.cuaseval.model.*;
import io.github.gcng54.cuaseval.requirements.RequirementsManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * Top-level test evaluator orchestrating the full CWA 18150 evaluation pipeline.
 * Integrates: DTI pipeline execution → metrics computation → criteria checking → reporting.
 * <p>
 * Usage:
 * <pre>
 *   TestEvaluator evaluator = new TestEvaluator();
 *   TestScenario scenario = ...;
 *   EvaluationResult result = evaluator.evaluate(scenario);
 *   Map&lt;String, Double&gt; metrics = evaluator.getMetrics(result);
 * </pre>
 * </p>
 */
public class TestEvaluator {

    private static final Logger log = LoggerFactory.getLogger(TestEvaluator.class);

    private final DtiPipeline pipeline;
    private final EvaluationCriteria criteria;
    private final MetricsCalculator metricsCalc;
    private final RequirementsManager reqManager;

    // ── Constructors ────────────────────────────────────────────────────

    public TestEvaluator() {
        this.pipeline = new DtiPipeline();
        this.criteria = new EvaluationCriteria();
        this.metricsCalc = new MetricsCalculator();
        this.reqManager = new RequirementsManager();
    }

    public TestEvaluator(DtiPipeline pipeline, EvaluationCriteria criteria) {
        this.pipeline = pipeline;
        this.criteria = criteria;
        this.metricsCalc = new MetricsCalculator();
        this.reqManager = new RequirementsManager();
    }

    // ── Accessors ───────────────────────────────────────────────────────

    public DtiPipeline getPipeline()           { return pipeline; }
    public EvaluationCriteria getCriteria()     { return criteria; }
    public MetricsCalculator getMetricsCalc()   { return metricsCalc; }
    public RequirementsManager getReqManager()  { return reqManager; }

    // ── Evaluation ──────────────────────────────────────────────────────

    /**
     * Execute the full evaluation pipeline on a scenario.
     * <ol>
     *   <li>Run DTI pipeline (Detection → Tracking → Identification)</li>
     *   <li>Compute detailed metrics</li>
     *   <li>Check each linked requirement against criteria thresholds</li>
     *   <li>Populate pass/fail lists</li>
     * </ol>
     *
     * @param scenario the test scenario
     * @return evaluation result with all metrics and compliance data
     */
    public EvaluationResult evaluate(TestScenario scenario) {
        log.info("╔══════════════════════════════════════════════╗");
        log.info("║ EVALUATING: {} ", scenario.getName());
        log.info("╚══════════════════════════════════════════════╝");

        // 1. Execute DTI pipeline
        EvaluationResult result = pipeline.execute(scenario);

        // 2. Check each linked requirement against criteria
        for (String reqId : scenario.getRequirementIds()) {
            boolean passed = criteria.evaluateRequirement(reqId, result);
            if (passed) {
                result.getPassedRequirements().add(reqId);
            } else {
                result.getFailedRequirements().add(reqId);
            }
            log.info("  Requirement {} — {}", reqId, passed ? "PASS" : "FAIL");
        }

        // 3. Determine overall pass/fail
        boolean overallPass = result.getFailedRequirements().isEmpty()
                && result.getOverallScore() >= 60;
        result.setPassed(overallPass);

        log.info("RESULT: {} (score={:.1f}, compliance={:.1f}%)",
                overallPass ? "PASS" : "FAIL",
                result.getOverallScore(),
                result.getCompliancePercent());

        return result;
    }

    /**
     * Evaluate an entire test suite (list of scenarios).
     */
    public List<EvaluationResult> evaluateSuite(List<TestScenario> scenarios) {
        log.info("Evaluating test suite: {} scenarios", scenarios.size());
        return scenarios.stream()
                .map(this::evaluate)
                .toList();
    }

    /**
     * Compute detailed metrics for a result.
     */
    public Map<String, Double> getMetrics(EvaluationResult result) {
        return metricsCalc.computeAllMetrics(result);
    }
}
