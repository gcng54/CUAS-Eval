package io.github.gcng54.cuaseval.dti;

import io.github.gcng54.cuaseval.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Identification subsystem of the DTI pipeline.
 * Evaluates the system's ability to classify and identify detected UAS.
 * <p>
 * Implements evaluation logic for: FR06, FR07, FR14, FR16, FR20, FR21,
 * PR13–PR16, TP_I01–TP_I11.
 * </p>
 */
public class IdentificationSystem {

    private static final Logger log = LoggerFactory.getLogger(IdentificationSystem.class);

    /** Base probability of correct identification */
    private double basePi = 0.85;

    /** IFF accuracy (friend-or-foe determination) */
    private double iffAccuracy = 0.95;

    /** Payload detection probability (FR20, FR21) */
    private double payloadDetectionProbability = 0.80;

    /** Bird rejection probability (FR16) */
    private double birdRejectionProbability = 0.90;

    /** Identification latency mean in seconds */
    private double identificationLatencyMeanS = 2.0;

    /** Random generator */
    private final Random rng = new Random(44);

    // ── Configuration ───────────────────────────────────────────────────

    public void setBasePi(double basePi)                                  { this.basePi = basePi; }
    public void setIffAccuracy(double iffAccuracy)                        { this.iffAccuracy = iffAccuracy; }
    public void setPayloadDetectionProbability(double p)                  { this.payloadDetectionProbability = p; }
    public void setBirdRejectionProbability(double p)                     { this.birdRejectionProbability = p; }
    public void setIdentificationLatencyMeanS(double s)                   { this.identificationLatencyMeanS = s; }

    public double getBasePi()                       { return basePi; }
    public double getIffAccuracy()                  { return iffAccuracy; }
    public double getPayloadDetectionProbability()  { return payloadDetectionProbability; }
    public double getBirdRejectionProbability()      { return birdRejectionProbability; }
    public double getIdentificationLatencyMeanS()   { return identificationLatencyMeanS; }

    // ── Identification Processing ───────────────────────────────────────

    /**
     * Process identification for all tracked targets.
     *
     * @param scenario the test scenario
     * @param tracks   tracking results
     * @return list of IdentificationResult objects
     */
    public List<IdentificationResult> processIdentifications(
            TestScenario scenario, List<TrackingResult> tracks) {

        List<IdentificationResult> results = new ArrayList<>();

        for (TrackingResult track : tracks) {
            UasTarget target = findTarget(scenario, track.getTargetUid());
            if (target == null) continue;

            IdentificationResult result = evaluateIdentification(target, scenario);
            results.add(result);
            log.info("Identification: {}", result);
        }

        return results;
    }

    /**
     * Evaluate identification of a single target.
     * Considers UAS class, weather, and target properties.
     */
    private IdentificationResult evaluateIdentification(UasTarget target,
                                                         TestScenario scenario) {
        IdentificationResult result = new IdentificationResult();
        result.setTargetUid(target.getUid());
        result.setTruthClassification(target.getUasClass().name());
        result.setTruthFriendly(target.isFriendly());

        // Weather-adjusted identification probability (PR13, PR14)
        double pi = computePi(scenario.getEnvironment().getWeather());

        // Stochastic identification decision
        boolean identified = rng.nextDouble() < pi;
        result.setIdentified(identified);

        if (identified) {
            // Classification correctness (TP_I08)
            boolean classCorrect = rng.nextDouble() < (basePi + 0.05);
            result.setClassificationCorrect(classCorrect);
            result.setReportedClassification(
                    classCorrect ? target.getUasClass().name() : "UNKNOWN");

            // IFF evaluation (FR14)
            boolean iffCorrect = rng.nextDouble() < iffAccuracy;
            result.setIffResult(iffCorrect ? target.isFriendly() : !target.isFriendly());

            // Payload identification (FR20, FR21)
            if (target.isHasPayload()) {
                result.setPayloadIdentified(rng.nextDouble() < payloadDetectionProbability);
            }

            // Bird rejection (FR16) — not applicable for real UAS, always true
            result.setBirdRejected(true);

            // Size estimation (TP_I01)
            double trueRcs = target.getRcsSqm();
            double estimatedSize = trueRcs * 10000 + rng.nextGaussian() * 50; // cm²
            result.setEstimatedSizeCm2(Math.max(0, estimatedSize));

            // Confidence score
            result.setConfidence(0.5 + rng.nextDouble() * 0.5);

            // Latency
            double latency = identificationLatencyMeanS + rng.nextGaussian() * 0.5;
            result.setLatencySeconds(Math.max(0.5, latency));
        } else {
            result.setConfidence(0);
            result.setReportedClassification("NOT_IDENTIFIED");
        }

        return result;
    }

    /**
     * Compute weather-adjusted identification probability.
     */
    private double computePi(TestEnvironment.WeatherCondition weather) {
        double weatherFactor = switch (weather) {
            case CLEAR          -> 1.0;
            case CLOUDY         -> 0.95;
            case NIGHT_CLEAR    -> 0.80; // PR13: night
            case NIGHT_OVERCAST -> 0.70;
            case RAIN           -> 0.75;
            case FOG            -> 0.50; // PR14: fog
            case SNOW           -> 0.65;
        };
        return basePi * weatherFactor;
    }

    /** Find target by UID in scenario. */
    private UasTarget findTarget(TestScenario scenario, String uid) {
        return scenario.getTargets().stream()
                .filter(t -> t.getUid().equals(uid))
                .findFirst().orElse(null);
    }
}
