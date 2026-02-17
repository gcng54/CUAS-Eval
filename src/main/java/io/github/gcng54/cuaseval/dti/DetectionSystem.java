package io.github.gcng54.cuaseval.dti;

import io.github.gcng54.cuaseval.model.*;
import io.github.gcng54.cuaseval.model.CuasSensor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Detection subsystem of the DTI pipeline.
 * Simulates / evaluates the detection capability of a CUAS system
 * against test targets within a test environment.
 * <p>
 * Implements evaluation logic for: FR01, FR02, FR09, FR11, FR12, FR15, FR22, FR23,
 * PR08, PR09, TP_D01–TP_D21.
 * </p>
 */
public class DetectionSystem {

    private static final Logger log = LoggerFactory.getLogger(DetectionSystem.class);

    /** Base probability of detection for an ideal sensor */
    private double basePd = 0.95;

    /** Detection latency standard deviation in seconds */
    private double latencyStdDev = 0.5;

    /** Position measurement noise std-dev in metres */
    private double positionNoiseM = 5.0;

    /** False alarm probability per scan */
    private double falseAlarmProbability = 0.02;

    /** Random generator for simulation */
    private final Random rng = new Random(42);

    // ── Configuration ───────────────────────────────────────────────────

    public void setBasePd(double basePd)                        { this.basePd = basePd; }
    public void setLatencyStdDev(double latencyStdDev)          { this.latencyStdDev = latencyStdDev; }
    public void setPositionNoiseM(double positionNoiseM)        { this.positionNoiseM = positionNoiseM; }
    public void setFalseAlarmProbability(double pfa)             { this.falseAlarmProbability = pfa; }

    public double getBasePd()               { return basePd; }
    public double getLatencyStdDev()        { return latencyStdDev; }
    public double getPositionNoiseM()       { return positionNoiseM; }
    public double getFalseAlarmProbability() { return falseAlarmProbability; }

    // ── Detection Processing ────────────────────────────────────────────

    /**
     * Process all targets in a scenario and produce detection results.
     * Each target is evaluated against each sensor in the environment.
     *
     * @param scenario the test scenario
     * @return list of DetectionResult objects
     */
    public List<DetectionResult> processDetections(TestScenario scenario) {
        List<DetectionResult> results = new ArrayList<>();
        TestEnvironment env = scenario.getEnvironment();
        Instant now = scenario.getStartTime() != null ? scenario.getStartTime() : Instant.now();

        for (UasTarget target : scenario.getTargets()) {
            DetectionResult result = evaluateDetection(target, env, now);
            results.add(result);
            log.info("Detection: {}", result);
        }

        // Simulate false alarms (FR15 — bird immunity evaluation)
        int falseAlarms = generateFalseAlarms(env);
        for (int i = 0; i < falseAlarms; i++) {
            DetectionResult fa = new DetectionResult();
            fa.setTargetUid("FALSE_ALARM_" + (i + 1));
            fa.setDetected(true);
            fa.setCorrectClassification(false); // false alarm
            fa.setDetectionTime(now.plusMillis(rng.nextInt(10000)));
            fa.setSensorType("UNKNOWN");
            results.add(fa);
            log.warn("False alarm generated: {}", fa.getTargetUid());
        }

        return results;
    }

    /**
     * Evaluate detection of a single target.
     * Uses CuasSensor templates when available for sensor-specific performance modelling.
     * Falls back to generic basePd model when no sensor template is attached.
     */
    private DetectionResult evaluateDetection(UasTarget target,
                                               TestEnvironment env,
                                               Instant baseTime) {
        DetectionResult result = new DetectionResult();
        result.setTargetUid(target.getUid());
        result.setGroundTruthTime(baseTime);
        result.setTruthPosition(target.getPosition());

        // Find best sensor and compute detection probability
        double bestPd = 0;
        String bestSensor = "NONE";
        double bestLatency = latencyStdDev;
        double bestNoise = positionNoiseM;

        for (TestEnvironment.SensorSite sensor : env.getSensorSites()) {
            double range = sensor.getPosition().distanceTo(target.getPosition());

            if (sensor.getSensorTemplate() != null) {
                // Use sensor-specific model from CuasSensor template
                CuasSensor tmpl = sensor.getSensorTemplate();
                double pd = tmpl.computePd(range, target.getRcsSqm(), env.getWeather());
                if (pd > bestPd) {
                    bestPd = pd;
                    bestSensor = tmpl.getSensorId() + " (" + tmpl.getSensorType().getDisplayName() + ")";
                    bestLatency = tmpl.getDetectionLatencyS();
                    bestNoise = tmpl.getPositionAccuracyM();
                }
            } else {
                // Fallback: generic detection model
                double pd = computePd(range, sensor.getMaxRangeM(),
                                      target.getRcsSqm(), env.getWeather());
                if (pd > bestPd) {
                    bestPd = pd;
                    bestSensor = sensor.getSensorType();
                }
            }
        }

        // If no sensors, use base Pd (standalone evaluation)
        if (env.getSensorSites().isEmpty()) {
            bestPd = basePd;
            bestSensor = "SIMULATED";
        }

        // Stochastic detection decision
        boolean detected = rng.nextDouble() < bestPd;
        result.setDetected(detected);
        result.setSensorType(bestSensor);

        if (detected) {
            // Compute latency (TP_D01)
            double latency = Math.abs(rng.nextGaussian() * bestLatency * 0.3) + 0.1;
            result.setLatencySeconds(latency);
            result.setDetectionTime(baseTime.plusMillis((long) (latency * 1000)));

            // Display latency (TP_D02) — typically latency + rendering
            result.setDisplayLatencySeconds(latency + 0.2 + rng.nextDouble() * 0.3);

            // Reported position with noise (uses sensor-specific noise)
            GeoPosition reported = addPositionNoise(target.getPosition(), bestNoise);
            result.setReportedPosition(reported);
            result.computePositionError();

            // Classification correctness (TP_D04 — bird vs UAS)
            result.setCorrectClassification(true);
        }

        return result;
    }

    /**
     * Compute probability of detection considering range, RCS, and weather.
     * Uses a simplified Swerling-I-like model.
     */
    private double computePd(double rangeM, double maxRangeM,
                              double rcsSqm, TestEnvironment.WeatherCondition weather) {
        // Range factor: Pd degrades with range^4 (radar equation)
        double rangeFactor = 1.0 - Math.pow(rangeM / maxRangeM, 2);
        rangeFactor = Math.max(0, Math.min(1, rangeFactor));

        // RCS factor: larger targets are easier to detect
        double rcsFactor = Math.min(1.0, 0.3 + 0.7 * Math.sqrt(Math.max(rcsSqm, 0.001)));

        // Weather degradation
        double weatherFactor = switch (weather) {
            case CLEAR, NIGHT_CLEAR -> 1.0;
            case CLOUDY             -> 0.95;
            case RAIN               -> 0.80;
            case FOG                -> 0.60;
            case SNOW               -> 0.70;
            case NIGHT_OVERCAST     -> 0.85;
        };

        return basePd * rangeFactor * rcsFactor * weatherFactor;
    }

    /**
     * Generate false alarm count for the environment.
     * Uses sensor-specific FAR when CuasSensor templates are available.
     */
    private int generateFalseAlarms(TestEnvironment env) {
        double totalFar = 0;
        for (TestEnvironment.SensorSite sensor : env.getSensorSites()) {
            if (sensor.getSensorTemplate() != null) {
                totalFar += sensor.getSensorTemplate().getFalseAlarmRate();
            } else {
                totalFar += falseAlarmProbability;
            }
        }
        double lambda = totalFar * 10;
        return poissonSample(lambda);
    }

    /**
     * Add Gaussian noise to a position.
     */
    private GeoPosition addPositionNoise(GeoPosition truth, double noiseM) {
        // Convert metre noise to degree offset (approximate)
        double latNoise = rng.nextGaussian() * noiseM / 111_320.0;
        double lonNoise = rng.nextGaussian() * noiseM
                / (111_320.0 * Math.cos(Math.toRadians(truth.getLatitude())));
        double altNoise = rng.nextGaussian() * noiseM * 0.5;

        return new GeoPosition(
                truth.getLatitude() + latNoise,
                truth.getLongitude() + lonNoise,
                truth.getAltitudeMsl() + altNoise
        );
    }

    /**
     * Simple Poisson random sample.
     */
    private int poissonSample(double lambda) {
        double l = Math.exp(-lambda);
        int k = 0;
        double p = 1;
        do {
            k++;
            p *= rng.nextDouble();
        } while (p > l);
        return k - 1;
    }
}
