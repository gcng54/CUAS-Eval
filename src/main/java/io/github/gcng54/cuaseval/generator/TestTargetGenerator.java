package io.github.gcng54.cuaseval.generator;

import io.github.gcng54.cuaseval.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Generates synthetic UAS test targets per CWA 18150 test categories.
 * Creates targets of different classes, speeds, altitudes, and payload
 * configurations to exercise all relevant requirement domains.
 * <p>
 * Requirement links: FR01, FR02, FR09 (C1), FR10 (payload), FR11 (fast),
 * FR12 (high altitude), FR14 (IFF).
 * </p>
 */
public class TestTargetGenerator {

    private static final Logger log = LoggerFactory.getLogger(TestTargetGenerator.class);
    private final Random rng = new Random(100);

    /**
     * Generate a single UAS target with specified parameters.
     */
    public UasTarget generateTarget(String designation, UasClass uasClass,
                                     GeoPosition startPosition, double speedMs,
                                     double headingDeg, double altitudeMsl) {
        UasTarget target = new UasTarget(designation, uasClass,
                massForClass(uasClass), startPosition);
        target.setSpeedMs(speedMs);
        target.setMaxSpeedMs(speedMs * 1.5);
        target.setHeadingDeg(headingDeg);
        target.getPosition().setAltitudeMsl(altitudeMsl);
        target.setRcsSqm(rcsForClass(uasClass));
        log.info("Generated target: {}", target);
        return target;
    }

    /**
     * Generate a standard set of test targets covering all CWA 18150 categories.
     * Returns targets for single detection, multi-target, fast, high-altitude,
     * payload, and IFF test cases.
     *
     * @param centrePosition the centre of the observation area
     * @param radiusM        observation area radius in metres
     * @return list of generated targets
     */
    public List<UasTarget> generateStandardTargetSet(GeoPosition centrePosition,
                                                       double radiusM) {
        List<UasTarget> targets = new ArrayList<>();

        // FR09: Class C1 target (< 900g)
        targets.add(generateTarget("C1-SMALL", UasClass.C1,
                offsetPosition(centrePosition, radiusM * 0.8, 0),
                10, 180, 50));

        // FR01: Standard C2 target
        targets.add(generateTarget("C2-STD", UasClass.C2,
                offsetPosition(centrePosition, radiusM * 0.6, 90),
                15, 270, 100));

        // FR11: Fast target
        UasTarget fast = generateTarget("FAST-C2", UasClass.C2,
                offsetPosition(centrePosition, radiusM * 0.9, 45),
                40, 225, 80);
        targets.add(fast);

        // FR12: High altitude target
        targets.add(generateTarget("HIGH-C3", UasClass.C3,
                offsetPosition(centrePosition, radiusM * 0.7, 180),
                20, 0, 400));

        // FR10, FR20: Target with payload
        UasTarget payload = generateTarget("PAYLOAD-C2", UasClass.C2,
                offsetPosition(centrePosition, radiusM * 0.5, 270),
                12, 90, 60);
        payload.setHasPayload(true);
        payload.setPayloadDescription("Camera + small package");
        targets.add(payload);

        // FR14: Friendly UAS (IFF test)
        UasTarget friendly = generateTarget("FRIENDLY-C1", UasClass.C1,
                offsetPosition(centrePosition, radiusM * 0.4, 135),
                8, 315, 30);
        friendly.setFriendly(true);
        targets.add(friendly);

        log.info("Generated standard target set: {} targets", targets.size());
        return targets;
    }

    /**
     * Generate a multi-target set for FR02/FR05/FR07 (multi-thread) testing.
     *
     * @param centrePosition centre of observations area
     * @param radiusM        observation area radius
     * @param count          number of simultaneous targets
     * @return list of targets
     */
    public List<UasTarget> generateMultiTargetSet(GeoPosition centrePosition,
                                                    double radiusM, int count) {
        List<UasTarget> targets = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            double angle = (360.0 / count) * i;
            double dist = radiusM * (0.3 + rng.nextDouble() * 0.6);
            UasClass cls = UasClass.values()[rng.nextInt(4)]; // C0–C3
            double speed = 5 + rng.nextDouble() * 30;
            double alt = 20 + rng.nextDouble() * 200;
            targets.add(generateTarget("MULTI-" + (i + 1), cls,
                    offsetPosition(centrePosition, dist, angle),
                    speed, (angle + 180) % 360, alt));
        }
        log.info("Generated multi-target set: {} targets", targets.size());
        return targets;
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    /** Offset a position by distance (m) and bearing (deg). */
    private GeoPosition offsetPosition(GeoPosition origin, double distM, double bearingDeg) {
        double bearingRad = Math.toRadians(bearingDeg);
        double dLat = (distM * Math.cos(bearingRad)) / 111_320.0;
        double dLon = (distM * Math.sin(bearingRad))
                / (111_320.0 * Math.cos(Math.toRadians(origin.getLatitude())));
        return new GeoPosition(
                origin.getLatitude() + dLat,
                origin.getLongitude() + dLon,
                origin.getAltitudeMsl()
        );
    }

    /** Typical mass in grams for a UAS class. */
    private double massForClass(UasClass cls) {
        return switch (cls) {
            case C0 -> 200;
            case C1 -> 800;
            case C2 -> 3500;
            case C3 -> 20000;
            case C4 -> 22000;
            case SPECIFIC -> 50000;
            case UNKNOWN -> 1000;
        };
    }

    /** Typical radar cross section in m² for a UAS class. */
    private double rcsForClass(UasClass cls) {
        return switch (cls) {
            case C0 -> 0.001;
            case C1 -> 0.005;
            case C2 -> 0.02;
            case C3 -> 0.1;
            case C4 -> 0.12;
            case SPECIFIC -> 0.5;
            case UNKNOWN -> 0.01;
        };
    }
}
