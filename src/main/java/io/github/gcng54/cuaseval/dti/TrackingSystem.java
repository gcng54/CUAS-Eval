package io.github.gcng54.cuaseval.dti;

import io.github.gcng54.cuaseval.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Tracking subsystem of the DTI pipeline.
 * Evaluates continuous positional tracking of detected UAS targets.
 * <p>
 * Implements evaluation logic for: FR04, FR05, FR17, FR18, FR19,
 * PR19, PR20, TP_T01–TP_T09.
 * </p>
 */
public class TrackingSystem {

    private static final Logger log = LoggerFactory.getLogger(TrackingSystem.class);

    /** Track update interval in seconds */
    private double updateIntervalS = 1.0;

    /** Probability of maintaining track per update */
    private double trackMaintenanceProbability = 0.98;

    /** Position tracking noise std-dev in metres */
    private double trackingNoiseM = 3.0;

    /** Random generator */
    private final Random rng = new Random(43);

    // ── Configuration ───────────────────────────────────────────────────

    public void setUpdateIntervalS(double s)                        { this.updateIntervalS = s; }
    public void setTrackMaintenanceProbability(double p)             { this.trackMaintenanceProbability = p; }
    public void setTrackingNoiseM(double m)                         { this.trackingNoiseM = m; }

    public double getUpdateIntervalS()              { return updateIntervalS; }
    public double getTrackMaintenanceProbability()   { return trackMaintenanceProbability; }
    public double getTrackingNoiseM()               { return trackingNoiseM; }

    // ── Tracking Processing ─────────────────────────────────────────────

    /**
     * Process tracking for all detected targets in a scenario.
     * Generates track history based on flight plans.
     *
     * @param scenario  the test scenario
     * @param detected  list of detected target UIDs
     * @return list of TrackingResult objects
     */
    public List<TrackingResult> processTracks(TestScenario scenario,
                                               List<DetectionResult> detected) {
        List<TrackingResult> results = new ArrayList<>();

        for (DetectionResult det : detected) {
            if (!det.isDetected()) continue; // skip undetected targets
            if (det.getTargetUid().startsWith("FALSE_ALARM")) continue;

            // Find matching target and flight plan
            UasTarget target = findTarget(scenario, det.getTargetUid());
            TestScenario.FlightPlan plan = findFlightPlan(scenario, det.getTargetUid());
            if (target == null) continue;

            TrackingResult trackResult = evaluateTracking(target, plan, scenario);
            results.add(trackResult);
            log.info("Tracking: {}", trackResult);
        }

        return results;
    }

    /**
     * Evaluate tracking of a single target along its flight plan.
     */
    private TrackingResult evaluateTracking(UasTarget target,
                                             TestScenario.FlightPlan plan,
                                             TestScenario scenario) {
        String systemTrackId = "TRK-" + target.getUid();
        TrackingResult result = new TrackingResult(target.getUid(), systemTrackId);

        double duration = scenario.getDurationSeconds();
        if (duration <= 0) duration = 60; // default 60s scenario

        boolean uidPreserved = true;
        int drops = 0;
        boolean currentlyTracking = true;

        // Generate track points along the flight plan
        int numUpdates = (int) (duration / updateIntervalS);
        Instant startTime = scenario.getStartTime() != null
                ? scenario.getStartTime() : Instant.now();

        for (int i = 0; i <= numUpdates; i++) {
            double timeOffset = i * updateIntervalS;
            Instant timestamp = startTime.plusMillis((long) (timeOffset * 1000));

            // Compute ground truth position by interpolating flight plan
            GeoPosition truthPos = interpolatePosition(target, plan, timeOffset, duration);

            // Stochastic track loss check (FR18)
            if (currentlyTracking && rng.nextDouble() > trackMaintenanceProbability) {
                currentlyTracking = false;
                drops++;
                log.debug("Track drop for {} at t={:.1f}s", target.getUid(), timeOffset);
                continue;
            }
            // Re-acquisition after loss
            if (!currentlyTracking) {
                if (rng.nextDouble() < 0.7) {
                    currentlyTracking = true;
                    // FR18: UID should be preserved
                    if (rng.nextDouble() > 0.9) {
                        uidPreserved = false; // rare UID change
                    }
                } else {
                    continue; // still lost
                }
            }

            // Add position noise
            GeoPosition reportedPos = addTrackingNoise(truthPos);
            double speed = target.getSpeedMs() + rng.nextGaussian() * 0.5;
            double heading = target.getHeadingDeg() + rng.nextGaussian() * 2;

            TrackingResult.TrackPoint tp = new TrackingResult.TrackPoint(
                    timestamp, reportedPos, truthPos, speed, heading);
            result.addTrackPoint(tp);
        }

        result.setTrackMaintained(drops == 0);
        result.setUidPreserved(uidPreserved);
        result.setTrackDropCount(drops);
        result.setTrackDurationSeconds(duration);
        result.setUpdateRateHz(1.0 / updateIntervalS);

        return result;
    }

    /**
     * Interpolate target position along flight plan at given time offset.
     * If no flight plan, uses simple linear extrapolation from start position.
     */
    private GeoPosition interpolatePosition(UasTarget target,
                                             TestScenario.FlightPlan plan,
                                             double timeOffset,
                                             double totalDuration) {
        if (plan == null || plan.getWaypoints().isEmpty()) {
            // Simple linear motion from start position
            double distM = target.getSpeedMs() * timeOffset;
            double headingRad = Math.toRadians(target.getHeadingDeg());
            double dLat = (distM * Math.cos(headingRad)) / 111_320.0;
            double dLon = (distM * Math.sin(headingRad))
                    / (111_320.0 * Math.cos(Math.toRadians(target.getPosition().getLatitude())));
            return new GeoPosition(
                    target.getPosition().getLatitude() + dLat,
                    target.getPosition().getLongitude() + dLon,
                    target.getPosition().getAltitudeMsl()
            );
        }

        // Interpolate between waypoints
        List<TestScenario.Waypoint> wps = plan.getWaypoints();
        for (int i = 0; i < wps.size() - 1; i++) {
            double t0 = wps.get(i).getTimeOffsetSeconds();
            double t1 = wps.get(i + 1).getTimeOffsetSeconds();
            if (timeOffset >= t0 && timeOffset <= t1) {
                double frac = (timeOffset - t0) / (t1 - t0);
                GeoPosition p0 = wps.get(i).getPosition();
                GeoPosition p1 = wps.get(i + 1).getPosition();
                return new GeoPosition(
                        p0.getLatitude() + frac * (p1.getLatitude() - p0.getLatitude()),
                        p0.getLongitude() + frac * (p1.getLongitude() - p0.getLongitude()),
                        p0.getAltitudeMsl() + frac * (p1.getAltitudeMsl() - p0.getAltitudeMsl())
                );
            }
        }
        // Beyond last waypoint — hold position
        return wps.get(wps.size() - 1).getPosition();
    }

    /**
     * Add Gaussian noise to a tracked position.
     */
    private GeoPosition addTrackingNoise(GeoPosition truth) {
        double latNoise = rng.nextGaussian() * trackingNoiseM / 111_320.0;
        double lonNoise = rng.nextGaussian() * trackingNoiseM
                / (111_320.0 * Math.cos(Math.toRadians(truth.getLatitude())));
        return new GeoPosition(
                truth.getLatitude() + latNoise,
                truth.getLongitude() + lonNoise,
                truth.getAltitudeMsl() + rng.nextGaussian() * trackingNoiseM * 0.5
        );
    }

    /** Find target by UID in scenario. */
    private UasTarget findTarget(TestScenario scenario, String uid) {
        return scenario.getTargets().stream()
                .filter(t -> t.getUid().equals(uid))
                .findFirst().orElse(null);
    }

    /** Find flight plan by target UID. */
    private TestScenario.FlightPlan findFlightPlan(TestScenario scenario, String uid) {
        return scenario.getFlightPlans().stream()
                .filter(fp -> uid.equals(fp.getTargetUid()))
                .findFirst().orElse(null);
    }
}
