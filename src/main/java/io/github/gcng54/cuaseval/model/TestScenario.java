package io.github.gcng54.cuaseval.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * A complete test scenario combining environment, targets, and flight paths.
 * <p>
 * A scenario describes WHAT to test: which targets appear, where they fly,
 * and under what environmental conditions. The DTI pipeline then processes
 * the scenario to produce {@link EvaluationResult}.
 * </p>
 * <p>
 * Requirement links: FR25 (embedded simulator), FR22 (detect while tracking).
 * </p>
 */
public class TestScenario {

    /** Scenario identifier */
    private String scenarioId;

    /** Human-readable scenario name */
    private String name;

    /** Scenario description */
    private String description;

    /** Test environment configuration */
    private TestEnvironment environment;

    /** Targets in this scenario */
    private List<UasTarget> targets = new ArrayList<>();

    /** Flight plans for each target (parallel list with targets) */
    private List<FlightPlan> flightPlans = new ArrayList<>();

    /** Scenario duration in seconds */
    private double durationSeconds;

    /** Scenario start time */
    private Instant startTime;

    /** Requirements to be verified in this scenario */
    private List<String> requirementIds = new ArrayList<>();

    // ── Inner class ─────────────────────────────────────────────────────

    /**
     * A flight plan consisting of ordered waypoints.
     */
    public static class FlightPlan {
        private String targetUid;
        private List<Waypoint> waypoints = new ArrayList<>();

        public FlightPlan() {}
        public FlightPlan(String targetUid) { this.targetUid = targetUid; }

        public String getTargetUid()        { return targetUid; }
        public List<Waypoint> getWaypoints(){ return waypoints; }

        public void setTargetUid(String uid)          { this.targetUid = uid; }
        public void setWaypoints(List<Waypoint> wp)   { this.waypoints = wp; }

        public void addWaypoint(Waypoint wp) { waypoints.add(wp); }

        /** Total path length in metres. */
        public double totalDistanceM() {
            double total = 0;
            for (int i = 1; i < waypoints.size(); i++) {
                total += waypoints.get(i - 1).getPosition()
                         .distanceTo(waypoints.get(i).getPosition());
            }
            return total;
        }
    }

    /**
     * A single waypoint in a flight plan.
     */
    public static class Waypoint {
        private GeoPosition position;
        private double speedMs;
        private double timeOffsetSeconds; // time from scenario start

        public Waypoint() {}
        public Waypoint(GeoPosition position, double speedMs, double timeOffset) {
            this.position = position;
            this.speedMs = speedMs;
            this.timeOffsetSeconds = timeOffset;
        }

        public GeoPosition getPosition()      { return position; }
        public double getSpeedMs()             { return speedMs; }
        public double getTimeOffsetSeconds()   { return timeOffsetSeconds; }

        public void setPosition(GeoPosition p)            { this.position = p; }
        public void setSpeedMs(double s)                  { this.speedMs = s; }
        public void setTimeOffsetSeconds(double t)        { this.timeOffsetSeconds = t; }
    }

    // ── Constructors ────────────────────────────────────────────────────

    public TestScenario() {}

    public TestScenario(String scenarioId, String name, TestEnvironment environment) {
        this.scenarioId = scenarioId;
        this.name = name;
        this.environment = environment;
        this.startTime = Instant.now();
    }

    // ── Accessors ───────────────────────────────────────────────────────

    public String getScenarioId()               { return scenarioId; }
    public String getName()                     { return name; }
    public String getDescription()              { return description; }
    public TestEnvironment getEnvironment()      { return environment; }
    public List<UasTarget> getTargets()         { return targets; }
    public List<FlightPlan> getFlightPlans()    { return flightPlans; }
    public double getDurationSeconds()          { return durationSeconds; }
    public Instant getStartTime()               { return startTime; }
    public List<String> getRequirementIds()     { return requirementIds; }

    public void setScenarioId(String scenarioId)            { this.scenarioId = scenarioId; }
    public void setName(String name)                        { this.name = name; }
    public void setDescription(String description)          { this.description = description; }
    public void setEnvironment(TestEnvironment environment) { this.environment = environment; }
    public void setDurationSeconds(double durationSeconds)  { this.durationSeconds = durationSeconds; }
    public void setStartTime(Instant startTime)             { this.startTime = startTime; }

    /** Add a target with its flight plan. */
    public void addTarget(UasTarget target, FlightPlan plan) {
        targets.add(target);
        flightPlans.add(plan);
    }

    @Override
    public String toString() {
        return String.format(Locale.ENGLISH, "Scenario[%s] '%s' targets=%d duration=%.0fs reqs=%d",
                scenarioId, name, targets.size(), durationSeconds, requirementIds.size());
    }
}
