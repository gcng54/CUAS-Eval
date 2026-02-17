package io.github.gcng54.cuaseval.generator;

import io.github.gcng54.cuaseval.model.GeoPosition;
import io.github.gcng54.cuaseval.model.TestScenario.FlightPlan;
import io.github.gcng54.cuaseval.model.TestScenario.Waypoint;

import java.util.Random;

/**
 * Generates standard flight plan patterns for UAS test targets (TG-02).
 * <p>
 * Supports: straight line, orbit, racetrack, random walk, approach-retreat.
 * </p>
 */
public class FlightPlanGenerator {

    private final Random rng = new Random(42);

    /** Flight pattern types. */
    public enum FlightPattern {
        STRAIGHT_LINE, ORBIT, RACETRACK, RANDOM_WALK, APPROACH_RETREAT, CUSTOM
    }

    /**
     * Generate a straight-line flight plan between two points.
     */
    public FlightPlan generateStraightLine(String targetUid,
                                            GeoPosition start, GeoPosition end,
                                            double speedMs) {
        FlightPlan plan = new FlightPlan(targetUid);
        double dist = start.distanceTo(end);
        double duration = dist / Math.max(speedMs, 1);

        plan.addWaypoint(new Waypoint(start, speedMs, 0));
        plan.addWaypoint(new Waypoint(end, speedMs, duration));
        return plan;
    }

    /**
     * Generate an orbital (circular) flight plan around a centre point.
     */
    public FlightPlan generateOrbit(String targetUid,
                                     GeoPosition centre, double radiusM,
                                     double altitudeM, double speedMs, int laps) {
        FlightPlan plan = new FlightPlan(targetUid);
        int pointsPerLap = 36; // every 10°
        double circumference = 2 * Math.PI * radiusM;
        double lapDuration = circumference / Math.max(speedMs, 1);

        double time = 0;
        for (int lap = 0; lap < laps; lap++) {
            for (int i = 0; i < pointsPerLap; i++) {
                double angleDeg = 360.0 * i / pointsPerLap;
                double angleRad = Math.toRadians(angleDeg);

                double dLat = radiusM * Math.cos(angleRad) / 111_320.0;
                double dLon = radiusM * Math.sin(angleRad) /
                        (111_320.0 * Math.cos(Math.toRadians(centre.getLatitude())));

                GeoPosition wp = new GeoPosition(
                        centre.getLatitude() + dLat,
                        centre.getLongitude() + dLon,
                        altitudeM);
                plan.addWaypoint(new Waypoint(wp, speedMs, time));
                time += lapDuration / pointsPerLap;
            }
        }
        // Close the loop
        if (!plan.getWaypoints().isEmpty()) {
            plan.addWaypoint(new Waypoint(
                    plan.getWaypoints().get(0).getPosition(), speedMs, time));
        }
        return plan;
    }

    /**
     * Generate a racetrack pattern (elongated oval).
     */
    public FlightPlan generateRacetrack(String targetUid,
                                         GeoPosition start, GeoPosition end,
                                         double widthM, double speedMs) {
        FlightPlan plan = new FlightPlan(targetUid);
        double dist = start.distanceTo(end);
        double bearing = start.bearingTo(end);
        double perpBearing = bearing + 90;

        double time = 0;
        double legTime = dist / Math.max(speedMs, 1);
        double turnRadius = widthM / 2;
        double turnTime = Math.PI * turnRadius / Math.max(speedMs, 1);

        // Leg 1: start → end
        plan.addWaypoint(new Waypoint(start, speedMs, time));
        time += legTime;
        plan.addWaypoint(new Waypoint(end, speedMs, time));

        // Turn 1
        time += turnTime;
        GeoPosition offsetEnd = offsetPosition(end, perpBearing, widthM);
        plan.addWaypoint(new Waypoint(offsetEnd, speedMs, time));

        // Leg 2: return
        time += legTime;
        GeoPosition offsetStart = offsetPosition(start, perpBearing, widthM);
        plan.addWaypoint(new Waypoint(offsetStart, speedMs, time));

        // Turn 2 (back to start)
        time += turnTime;
        plan.addWaypoint(new Waypoint(start, speedMs, time));

        return plan;
    }

    /**
     * Generate a random walk flight plan within a radius.
     */
    public FlightPlan generateRandomWalk(String targetUid,
                                          GeoPosition centre, double radiusM,
                                          double altitudeM, double speedMs,
                                          double durationS) {
        FlightPlan plan = new FlightPlan(targetUid);
        int numPoints = Math.max(5, (int) (durationS / 15)); // waypoint every ~15s

        double time = 0;
        double timeStep = durationS / numPoints;

        for (int i = 0; i <= numPoints; i++) {
            double angle = rng.nextDouble() * 2 * Math.PI;
            double r = radiusM * Math.sqrt(rng.nextDouble());

            double dLat = r * Math.cos(angle) / 111_320.0;
            double dLon = r * Math.sin(angle) /
                    (111_320.0 * Math.cos(Math.toRadians(centre.getLatitude())));

            GeoPosition wp = new GeoPosition(
                    centre.getLatitude() + dLat,
                    centre.getLongitude() + dLon,
                    altitudeM);
            plan.addWaypoint(new Waypoint(wp, speedMs, time));
            time += timeStep;
        }
        return plan;
    }

    /**
     * Generate an approach-retreat pattern: fly in, loiter, fly out.
     */
    public FlightPlan generateApproachRetreat(String targetUid,
                                               GeoPosition start,
                                               GeoPosition target,
                                               double speedMs) {
        FlightPlan plan = new FlightPlan(targetUid);
        double dist = start.distanceTo(target);
        double legTime = dist / Math.max(speedMs, 1);

        double time = 0;
        plan.addWaypoint(new Waypoint(start, speedMs, time));

        // Approach
        time += legTime;
        plan.addWaypoint(new Waypoint(target, speedMs * 0.5, time));

        // Loiter near target (30s)
        time += 30;
        GeoPosition loiter = offsetPosition(target, 90, 100);
        plan.addWaypoint(new Waypoint(loiter, speedMs * 0.3, time));

        // Retreat
        time += legTime;
        plan.addWaypoint(new Waypoint(start, speedMs, time));

        return plan;
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private GeoPosition offsetPosition(GeoPosition pos, double bearingDeg, double distM) {
        double bearRad = Math.toRadians(bearingDeg);
        double dLat = distM * Math.cos(bearRad) / 111_320.0;
        double dLon = distM * Math.sin(bearRad) /
                (111_320.0 * Math.cos(Math.toRadians(pos.getLatitude())));
        return new GeoPosition(
                pos.getLatitude() + dLat,
                pos.getLongitude() + dLon,
                pos.getAltitudeMsl());
    }
}
