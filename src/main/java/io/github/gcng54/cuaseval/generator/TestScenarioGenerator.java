package io.github.gcng54.cuaseval.generator;

import io.github.gcng54.cuaseval.model.*;
import io.github.gcng54.cuaseval.model.TestEnvironment.WeatherCondition;
import io.github.gcng54.cuaseval.model.TestScenario.*;
import io.github.gcng54.cuaseval.requirements.RequirementsManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Generates complete test scenarios from environment + targets + flight plans.
 * Produces repeatable, requirement-linked scenarios per CWA 18150 methodology.
 * <p>
 * Supports both generic test scenarios (baseline, multi-target, tracking, weather)
 * and the 10 CWA 18150 COURAGEOUS application scenarios (S1–S10).
 * </p>
 * <p>
 * Requirement links: FR25 (embedded simulator), FR22 (detect while tracking),
 * FR02/FR05 (multi-target), FR13 (data fusion).
 * </p>
 */
public class TestScenarioGenerator {

    private static final Logger log = LoggerFactory.getLogger(TestScenarioGenerator.class);

    private final TestTargetGenerator targetGenerator = new TestTargetGenerator();
    private final TestEnvironmentDesigner envDesigner = new TestEnvironmentDesigner();
    private final RequirementsManager reqManager = new RequirementsManager();
    private final Random rng = new Random(200);

    /**
     * Generate a single-target baseline detection scenario.
     * Verifies: FR01, FR09, TP_D01, TP_D02, TP_D03.
     */
    public TestScenario createSingleTargetScenario(GeoPosition centrePosition) {
        TestEnvironment env = envDesigner.createOpenFieldEnvironment(
                "Single-Target Open Field", centrePosition, 5000);

        TestScenario scenario = new TestScenario("SC-001",
                "Single UAS Detection (Baseline)", env);
        scenario.setDescription("Single C1 UAS enters observation area from north. " +
                "Baseline test for detection latency, accuracy, and display.");
        scenario.setDurationSeconds(120);
        scenario.getRequirementIds().addAll(
                List.of("FR01", "FR09", "TP_D01", "TP_D02", "TP_D03"));

        // Generate single C1 target
        UasTarget target = targetGenerator.generateTarget("C1-BASELINE", UasClass.C1,
                new GeoPosition(centrePosition.getLatitude() + 0.04,
                        centrePosition.getLongitude(), 50),
                10, 180, 50);

        // Flight plan: straight line from north to south through observation area
        FlightPlan plan = new FlightPlan(target.getUid());
        plan.addWaypoint(new Waypoint(target.getPosition(), 10, 0));
        plan.addWaypoint(new Waypoint(
                new GeoPosition(centrePosition.getLatitude() - 0.04,
                        centrePosition.getLongitude(), 50),
                10, 120));

        scenario.addTarget(target, plan);
        log.info("Created scenario: {}", scenario);
        return scenario;
    }

    /**
     * Generate a multi-target simultaneous detection scenario.
     * Verifies: FR02, FR05, FR07, FR22, FR24.
     */
    public TestScenario createMultiTargetScenario(GeoPosition centrePosition, int targetCount) {
        TestEnvironment env = envDesigner.createOpenFieldEnvironment(
                "Multi-Target Test", centrePosition, 8000);

        TestScenario scenario = new TestScenario("SC-002",
                "Multi-UAS Simultaneous Detection (" + targetCount + " targets)", env);
        scenario.setDescription("Multiple UAS enter from different directions. " +
                "Tests multi-thread D/T/I without performance degradation.");
        scenario.setDurationSeconds(180);
        scenario.getRequirementIds().addAll(
                List.of("FR02", "FR05", "FR07", "FR22", "FR24"));

        List<UasTarget> targets = targetGenerator.generateMultiTargetSet(
                centrePosition, 8000, targetCount);

        for (UasTarget target : targets) {
            FlightPlan plan = generateConvergingPlan(target, centrePosition);
            scenario.addTarget(target, plan);
        }

        log.info("Created multi-target scenario: {}", scenario);
        return scenario;
    }

    /**
     * Generate a tracking continuity scenario.
     * Verifies: FR04, FR17, FR18, PR19, PR20, TP_T01–TP_T09.
     */
    public TestScenario createTrackingScenario(GeoPosition centrePosition) {
        TestEnvironment env = envDesigner.createOpenFieldEnvironment(
                "Tracking Continuity Test", centrePosition, 6000);

        TestScenario scenario = new TestScenario("SC-003",
                "Tracking Continuity & UID Persistence", env);
        scenario.setDescription("UAS performs complex manoeuvres including orbit, " +
                "speed changes, and temporary occlusion. Tests track maintenance.");
        scenario.setDurationSeconds(300);
        scenario.getRequirementIds().addAll(
                List.of("FR04", "FR17", "FR18", "PR19", "PR20",
                        "TP_T01", "TP_T04", "TP_T05", "TP_T06"));

        UasTarget target = targetGenerator.generateTarget("MANOEUVRE-C2", UasClass.C2,
                new GeoPosition(centrePosition.getLatitude() + 0.03,
                        centrePosition.getLongitude() - 0.02, 80),
                15, 135, 80);

        // Complex flight plan: approach → orbit → retreat
        FlightPlan plan = new FlightPlan(target.getUid());
        plan.addWaypoint(new Waypoint(target.getPosition(), 15, 0));
        plan.addWaypoint(new Waypoint(
                new GeoPosition(centrePosition.getLatitude() + 0.01,
                        centrePosition.getLongitude(), 100), 12, 60));
        // Orbit
        for (int i = 0; i < 8; i++) {
            double angle = i * 45;
            double r = 0.01;
            plan.addWaypoint(new Waypoint(
                    new GeoPosition(centrePosition.getLatitude() + r * Math.cos(Math.toRadians(angle)),
                            centrePosition.getLongitude() + r * Math.sin(Math.toRadians(angle)),
                            100 + i * 5), 10, 80 + i * 20));
        }
        // Retreat
        plan.addWaypoint(new Waypoint(
                new GeoPosition(centrePosition.getLatitude() - 0.04,
                        centrePosition.getLongitude() + 0.03, 120), 20, 280));

        scenario.addTarget(target, plan);
        log.info("Created tracking scenario: {}", scenario);
        return scenario;
    }

    /**
     * Generate an adverse-weather scenario.
     * Verifies: PR08, PR13, PR14.
     */
    public TestScenario createAdverseWeatherScenario(GeoPosition centrePosition,
                                                       WeatherCondition weather) {
        TestEnvironment env = envDesigner.createAdverseWeatherEnvironment(
                "Adverse Weather: " + weather, centrePosition, 5000, weather);

        TestScenario scenario = new TestScenario("SC-004",
                "Adverse Weather D/T/I — " + weather, env);
        scenario.setDescription("Standard targets under " + weather +
                " conditions. Tests degraded performance acceptance.");
        scenario.setDurationSeconds(180);
        scenario.getRequirementIds().addAll(
                List.of("PR08", "PR13", "PR14"));

        List<UasTarget> targets = targetGenerator.generateStandardTargetSet(
                centrePosition, 5000);
        for (UasTarget target : targets) {
            FlightPlan plan = generateConvergingPlan(target, centrePosition);
            scenario.addTarget(target, plan);
        }

        log.info("Created adverse-weather scenario: {}", scenario);
        return scenario;
    }

    /**
     * Generate a full CWA 18150 compliance test suite containing all scenario types.
     */
    public List<TestScenario> createFullTestSuite(GeoPosition centrePosition) {
        List<TestScenario> suite = new ArrayList<>();
        suite.add(createSingleTargetScenario(centrePosition));
        suite.add(createMultiTargetScenario(centrePosition, 5));
        suite.add(createTrackingScenario(centrePosition));
        suite.add(createAdverseWeatherScenario(centrePosition, WeatherCondition.RAIN));
        suite.add(createAdverseWeatherScenario(centrePosition, WeatherCondition.FOG));
        suite.add(createAdverseWeatherScenario(centrePosition, WeatherCondition.NIGHT_CLEAR));
        log.info("Created full test suite: {} scenarios", suite.size());
        return suite;
    }

    /**
     * Generate a full CWA 18150 COURAGEOUS scenario suite (all 10 scenarios S1–S10).
     */
    public List<TestScenario> createCourageousTestSuite() {
        List<TestScenario> suite = new ArrayList<>();
        for (ScenarioType st : ScenarioType.values()) {
            suite.add(createCourageousScenario(st));
        }
        log.info("Created COURAGEOUS test suite: {} scenarios", suite.size());
        return suite;
    }

    // ── CWA 18150 COURAGEOUS Scenarios (S1–S10) ─────────────────────────

    /**
     * Create a CWA 18150 COURAGEOUS scenario by type.
     * Automatically configures environment, targets, and linked requirement IDs
     * from the scenario requirements JSON.
     */
    public TestScenario createCourageousScenario(ScenarioType scenarioType) {
        GeoPosition centre = new GeoPosition(
                scenarioType.getDefaultLat(), scenarioType.getDefaultLon(), 0);

        return switch (scenarioType) {
            case S1_PRISON          -> createPrisonScenario(centre);
            case S2_AIRPORT         -> createAirportScenario(centre);
            case S3_NUCLEAR_PLANT   -> createNuclearPlantScenario(centre);
            case S4_GOV_BUILDING    -> createGovBuildingScenario(centre);
            case S5_STADIUM         -> createStadiumScenario(centre);
            case S6_OUTDOOR_CONCERT -> createOutdoorConcertScenario(centre);
            case S7_POLITICAL_RALLY -> createPoliticalRallyScenario(centre);
            case S8_INT_SUMMIT      -> createIntSummitScenario(centre);
            case S9_LAND_BORDER     -> createLandBorderScenario(centre);
            case S10_MARITIME_BORDER -> createMaritimeBorderScenario(centre);
        };
    }

    /**
     * S1: Prison — Contraband delivery via mini multirotor.
     * Morning, Sunny. Class I mini multirotor. Drug payload (<250g).
     * Reqs: S1_GR1-4, FRPR_6, FRPR_12, FRPR_16, PRPR_1, PRPR_10, PRPR_18.
     */
    public TestScenario createPrisonScenario(GeoPosition centre) {
        TestEnvironment env = envDesigner.createOpenFieldEnvironment(
                "Prison Perimeter — Outskirts", centre, 3000);
        env.setWeather(WeatherCondition.CLEAR);
        env.setTimeOfDay("DAY");

        TestScenario scenario = new TestScenario("S1-PRISON",
                "S1: Prison — Contraband Delivery Defence", env);
        scenario.setDescription("Single C1 mini multirotor attempts drug payload " +
                "delivery to prison facility. Morning, sunny conditions.");
        scenario.setDurationSeconds(180);
        linkScenarioRequirements(scenario, "S1");

        // C1 mini multirotor with drug payload
        UasTarget target = targetGenerator.generateTarget("PRISON-C1", UasClass.C1,
                offsetPosition(centre, 2500, 45), 10, 225, 40);
        target.setHasPayload(true);
        target.setPayloadDescription("Drug payload (<250g)");

        FlightPlan plan = new FlightPlan(target.getUid());
        plan.addWaypoint(new Waypoint(target.getPosition(), 10, 0));
        plan.addWaypoint(new Waypoint(
                offsetPosition(centre, 200, 0), 8, 90));
        plan.addWaypoint(new Waypoint(
                centre, 3, 120));  // hover over delivery point
        plan.addWaypoint(new Waypoint(
                offsetPosition(centre, 2800, 225), 12, 170));

        scenario.addTarget(target, plan);
        log.info("Created S1-Prison scenario: {}", scenario);
        return scenario;
    }

    /**
     * S2: Airport — UAS in approach area, rainy evening.
     * Class I mini fixed-wing (<15kg) with optical camera.
     * Reqs: S2_GR1/4/5/9, FRAP_3/12/18, PRAP_9/12.
     */
    public TestScenario createAirportScenario(GeoPosition centre) {
        TestEnvironment env = envDesigner.createOpenFieldEnvironment(
                "Airport — Suburban, Glide Path", centre, 8000);
        env.setWeather(WeatherCondition.RAIN);
        env.setTimeOfDay("DUSK");
        env.setTerrainType("SUBURBAN");

        TestScenario scenario = new TestScenario("S2-AIRPORT",
                "S2: Airport — Airspace & Glide Path Defence", env);
        scenario.setDescription("Mini fixed-wing UAS enters glide path area " +
                "in rainy evening conditions with optical surveillance camera.");
        scenario.setDurationSeconds(240);
        linkScenarioRequirements(scenario, "S2");

        // C2 mini fixed-wing
        UasTarget target = targetGenerator.generateTarget("AIRPORT-FW", UasClass.C2,
                offsetPosition(centre, 7000, 270), 18, 90, 120);
        target.setHasPayload(true);
        target.setPayloadDescription("Optical camera; Surveillance");

        FlightPlan plan = new FlightPlan(target.getUid());
        plan.addWaypoint(new Waypoint(target.getPosition(), 18, 0));
        plan.addWaypoint(new Waypoint(
                offsetPosition(centre, 3000, 270), 15, 80));
        plan.addWaypoint(new Waypoint(
                offsetPosition(centre, 500, 270), 12, 160));
        plan.addWaypoint(new Waypoint(
                offsetPosition(centre, 6000, 90), 20, 230));

        scenario.addTarget(target, plan);
        log.info("Created S2-Airport scenario: {}", scenario);
        return scenario;
    }

    /**
     * S3: Nuclear Plant — Night cloudy, heavy custom drone with explosives.
     * Class I small custom multirotor (>15kg).
     * Reqs: S3_GR1/4/6, FRNP_7/13/17, PRNP_12/18/25.
     */
    public TestScenario createNuclearPlantScenario(GeoPosition centre) {
        TestEnvironment env = envDesigner.createAdverseWeatherEnvironment(
                "Nuclear Plant — Isolated Rural", centre, 5000, WeatherCondition.NIGHT_OVERCAST);
        env.setTerrainType("RURAL");

        TestScenario scenario = new TestScenario("S3-NUCLEAR",
                "S3: Nuclear Plant — Critical Infrastructure Defence", env);
        scenario.setDescription("Custom multirotor (>15kg) approaches reactor " +
                "buildings at night with explosive device. IFF and dark-tracking required.");
        scenario.setDurationSeconds(300);
        linkScenarioRequirements(scenario, "S3");

        // Heavy custom multirotor with explosive
        UasTarget target = targetGenerator.generateTarget("NUCLEAR-CUSTOM", UasClass.C3,
                offsetPosition(centre, 4500, 315), 12, 135, 80);
        target.setHasPayload(true);
        target.setPayloadDescription("3kg explosive device");

        FlightPlan plan = new FlightPlan(target.getUid());
        plan.addWaypoint(new Waypoint(target.getPosition(), 12, 0));
        plan.addWaypoint(new Waypoint(
                offsetPosition(centre, 2000, 315), 10, 60));
        plan.addWaypoint(new Waypoint(
                offsetPosition(centre, 500, 0), 6, 120));
        // Hover over reactor
        plan.addWaypoint(new Waypoint(centre, 2, 180));
        plan.addWaypoint(new Waypoint(centre, 0, 240));

        scenario.addTarget(target, plan);

        // Friendly UAS for IFF test (FRNP_17)
        UasTarget friendly = targetGenerator.generateTarget("NUCLEAR-FRIENDLY", UasClass.C1,
                offsetPosition(centre, 1000, 90), 5, 270, 30);
        friendly.setFriendly(true);
        FlightPlan friendlyPlan = new FlightPlan(friendly.getUid());
        friendlyPlan.addWaypoint(new Waypoint(friendly.getPosition(), 5, 0));
        friendlyPlan.addWaypoint(new Waypoint(
                offsetPosition(centre, 800, 180), 5, 120));
        scenario.addTarget(friendly, friendlyPlan);

        log.info("Created S3-Nuclear scenario: {}", scenario);
        return scenario;
    }

    /**
     * S4: Government Building — Urban multi-UAS attack, clear afternoon.
     * 3 UAS (Class I mini/micro/small) with video equipment.
     * Reqs: S4_GR1/2, FRGB_13/22, PRGB_12/21/25.
     */
    public TestScenario createGovBuildingScenario(GeoPosition centre) {
        TestEnvironment env = envDesigner.createUrbanEnvironment(
                "Government Building — Urban", centre, 4000);
        env.setWeather(WeatherCondition.CLEAR);
        env.setTimeOfDay("DAY");

        TestScenario scenario = new TestScenario("S4-GOVBUILD",
                "S4: Government Building — Multi-UAS Urban Attack", env);
        scenario.setDescription("3 simultaneous UAS approach government building " +
                "with video equipment in urban environment. VIP protection scenario.");
        scenario.setDurationSeconds(240);
        linkScenarioRequirements(scenario, "S4");

        // 3 simultaneous UAS from different directions
        UasClass[] classes = { UasClass.C0, UasClass.C1, UasClass.C2 };
        String[] designations = { "GOVB-MICRO", "GOVB-MINI", "GOVB-SMALL" };
        double[] bearings = { 0, 120, 240 };
        double[] speeds = { 8, 12, 15 };

        for (int i = 0; i < 3; i++) {
            UasTarget target = targetGenerator.generateTarget(designations[i], classes[i],
                    offsetPosition(centre, 3500, bearings[i]), speeds[i],
                    (bearings[i] + 180) % 360, 60 + i * 20);
            target.setHasPayload(true);
            target.setPayloadDescription("Video equipment");

            FlightPlan plan = new FlightPlan(target.getUid());
            plan.addWaypoint(new Waypoint(target.getPosition(), speeds[i], 0));
            plan.addWaypoint(new Waypoint(
                    offsetPosition(centre, 500, bearings[i]), speeds[i] * 0.7, 100));
            plan.addWaypoint(new Waypoint(centre, 3, 160));
            scenario.addTarget(target, plan);
        }

        log.info("Created S4-GovBuilding scenario: {}", scenario);
        return scenario;
    }

    /**
     * S5: Stadium — Heavy custom multirotor with aerosol device, cloudy evening.
     * Class I small custom multirotor (>15kg).
     * Reqs: S5_GR1/6, FRST_7/36, PRST_15/16/26.
     */
    public TestScenario createStadiumScenario(GeoPosition centre) {
        TestEnvironment env = envDesigner.createOpenFieldEnvironment(
                "Stadium — Suburban, Industrial", centre, 5000);
        env.setWeather(WeatherCondition.CLOUDY);
        env.setTimeOfDay("DUSK");
        env.setTerrainType("SUBURBAN");

        TestScenario scenario = new TestScenario("S5-STADIUM",
                "S5: Stadium — Event Security Threat", env);
        scenario.setDescription("Heavy custom multirotor approaches stadium " +
                "with aerosol dispersing device during evening event.");
        scenario.setDurationSeconds(300);
        linkScenarioRequirements(scenario, "S5");

        UasTarget target = targetGenerator.generateTarget("STAD-CUSTOM", UasClass.C3,
                offsetPosition(centre, 4000, 180), 14, 0, 100);
        target.setHasPayload(true);
        target.setPayloadDescription("Aerosol dispersing device");

        FlightPlan plan = new FlightPlan(target.getUid());
        plan.addWaypoint(new Waypoint(target.getPosition(), 14, 0));
        plan.addWaypoint(new Waypoint(
                offsetPosition(centre, 2000, 180), 12, 50));
        plan.addWaypoint(new Waypoint(
                offsetPosition(centre, 500, 0), 8, 100));
        // Hover to disperse payload
        plan.addWaypoint(new Waypoint(centre, 3, 150));
        plan.addWaypoint(new Waypoint(centre, 0, 200));
        plan.addWaypoint(new Waypoint(
                offsetPosition(centre, 3000, 0), 16, 270));

        scenario.addTarget(target, plan);
        log.info("Created S5-Stadium scenario: {}", scenario);
        return scenario;
    }

    /**
     * S6: Outdoor Concert — Mini multirotor with dazzling laser, windy evening.
     * Class I mini multirotor (15kg).
     * Reqs: S6_GR3, FROC_13/19, PROC_5/12.
     */
    public TestScenario createOutdoorConcertScenario(GeoPosition centre) {
        TestEnvironment env = envDesigner.createOpenFieldEnvironment(
                "Outdoor Concert — 20m Metallic Stage", centre, 3000);
        env.setWeather(WeatherCondition.CLOUDY);  // windy
        env.setTimeOfDay("DUSK");
        env.setTerrainType("SUBURBAN");
        env.getParameters().put("metallic_interference", "true");
        env.getParameters().put("wind_speed_ms", "12");

        TestScenario scenario = new TestScenario("S6-CONCERT",
                "S6: Outdoor Concert — Laser Threat", env);
        scenario.setDescription("Mini multirotor equipped with dazzling laser " +
                "approaches concert venue in windy evening with metallic stage interference.");
        scenario.setDurationSeconds(180);
        linkScenarioRequirements(scenario, "S6");

        UasTarget target = targetGenerator.generateTarget("CONCERT-LASER", UasClass.C2,
                offsetPosition(centre, 2500, 90), 10, 270, 50);
        target.setHasPayload(true);
        target.setPayloadDescription("Dazzling laser");

        FlightPlan plan = new FlightPlan(target.getUid());
        plan.addWaypoint(new Waypoint(target.getPosition(), 10, 0));
        plan.addWaypoint(new Waypoint(
                offsetPosition(centre, 800, 90), 8, 60));
        plan.addWaypoint(new Waypoint(
                offsetPosition(centre, 200, 0), 5, 100));
        plan.addWaypoint(new Waypoint(
                offsetPosition(centre, 2500, 270), 14, 160));

        scenario.addTarget(target, plan);
        log.info("Created S6-Concert scenario: {}", scenario);
        return scenario;
    }

    /**
     * S7: Political Rally — Commercial multirotor with noise generator, clear evening.
     * Class I mini commercial multirotor (<15kg).
     * Reqs: S7_GR1, FROR_2/13, PROR_6/14.
     */
    public TestScenario createPoliticalRallyScenario(GeoPosition centre) {
        TestEnvironment env = envDesigner.createUrbanEnvironment(
                "Political Rally — City Square", centre, 3000);
        env.setWeather(WeatherCondition.CLEAR);
        env.setTimeOfDay("DUSK");
        env.getParameters().put("em_interference", "strong");

        TestScenario scenario = new TestScenario("S7-RALLY",
                "S7: Political Rally — Urban Disruption", env);
        scenario.setDescription("Commercial multirotor with noise generator " +
                "operates in strong electromagnetic interference during evening rally.");
        scenario.setDurationSeconds(240);
        linkScenarioRequirements(scenario, "S7");

        UasTarget target = targetGenerator.generateTarget("RALLY-COM", UasClass.C2,
                offsetPosition(centre, 2500, 315), 12, 135, 60);
        target.setHasPayload(true);
        target.setPayloadDescription("Noise generator");

        // Complex flight with temporary loss-of-sight behind buildings
        FlightPlan plan = new FlightPlan(target.getUid());
        plan.addWaypoint(new Waypoint(target.getPosition(), 12, 0));
        plan.addWaypoint(new Waypoint(
                offsetPosition(centre, 1200, 315), 10, 40));
        // Behind buildings (FROR_13: continue tracking despite loss of sight)
        plan.addWaypoint(new Waypoint(
                offsetPosition(centre, 800, 270), 8, 70));
        plan.addWaypoint(new Waypoint(
                offsetPosition(centre, 300, 0), 5, 110));
        plan.addWaypoint(new Waypoint(
                offsetPosition(centre, 2500, 135), 15, 200));

        scenario.addTarget(target, plan);
        log.info("Created S7-Rally scenario: {}", scenario);
        return scenario;
    }

    /**
     * S8: International Summit — Fixed-wing, dusty evening, covert ops.
     * Class I small commercial fixed-wing (>15kg).
     * Reqs: S8_GR10, FRIS_3/17, PRIS_4/20.
     */
    public TestScenario createIntSummitScenario(GeoPosition centre) {
        TestEnvironment env = envDesigner.createOpenFieldEnvironment(
                "International Summit — Rural Historical", centre, 6000);
        env.setWeather(WeatherCondition.CLOUDY);  // dusty
        env.setTimeOfDay("DUSK");
        env.setTerrainType("RURAL");
        env.getParameters().put("dust_level", "moderate");

        TestScenario scenario = new TestScenario("S8-SUMMIT",
                "S8: International Summit — Autonomous Threat", env);
        scenario.setDescription("Autonomous fixed-wing UAS approaches summit " +
                "location in dusty evening. Requires covert CUAS operation.");
        scenario.setDurationSeconds(300);
        linkScenarioRequirements(scenario, "S8");

        // Autonomous fixed-wing (FRIS_3)
        UasTarget target = targetGenerator.generateTarget("SUMMIT-FW", UasClass.C3,
                offsetPosition(centre, 5500, 225), 22, 45, 150);
        target.setHasPayload(false);
        target.setPayloadDescription("Terrorist disturbance");

        FlightPlan plan = new FlightPlan(target.getUid());
        plan.addWaypoint(new Waypoint(target.getPosition(), 22, 0));
        plan.addWaypoint(new Waypoint(
                offsetPosition(centre, 2000, 225), 18, 60));
        plan.addWaypoint(new Waypoint(
                offsetPosition(centre, 500, 0), 15, 120));
        // Orbit
        for (int i = 0; i < 6; i++) {
            double angle = i * 60;
            plan.addWaypoint(new Waypoint(
                    offsetPosition(centre, 800, angle), 12, 150 + i * 15));
        }
        plan.addWaypoint(new Waypoint(
                offsetPosition(centre, 5000, 45), 25, 280));

        scenario.addTarget(target, plan);
        log.info("Created S8-Summit scenario: {}", scenario);
        return scenario;
    }

    /**
     * S9: Land Border — Dense vegetation, misty night, trafficking drone.
     * Class I small custom multirotor (>15kg).
     * Reqs: S9_GR1/4/8, FRLB_4/19, PRLB_3/4/14.
     */
    public TestScenario createLandBorderScenario(GeoPosition centre) {
        TestEnvironment env = envDesigner.createAdverseWeatherEnvironment(
                "Land Border — Dense Vegetation", centre, 10000, WeatherCondition.FOG);
        env.setTimeOfDay("NIGHT");
        env.setTerrainType("FOREST");
        env.getParameters().put("vegetation_density", "dense");
        env.getParameters().put("border_length_km", "100");

        TestScenario scenario = new TestScenario("S9-LANDBORDER",
                "S9: Land Border — Trafficking Interdiction", env);
        scenario.setDescription("Custom multirotor with trafficking payload " +
                "crosses dense vegetation border in misty night conditions.");
        scenario.setDurationSeconds(360);
        linkScenarioRequirements(scenario, "S9");

        // Primary trafficking drone
        UasTarget target = targetGenerator.generateTarget("BORDER-TRAF", UasClass.C3,
                offsetPosition(centre, 9000, 0), 15, 180, 60);
        target.setHasPayload(true);
        target.setPayloadDescription("2kg box; Trafficking payload");

        FlightPlan plan = new FlightPlan(target.getUid());
        plan.addWaypoint(new Waypoint(target.getPosition(), 15, 0));
        plan.addWaypoint(new Waypoint(
                offsetPosition(centre, 5000, 0), 12, 80));
        plan.addWaypoint(new Waypoint(
                offsetPosition(centre, 2000, 10), 10, 140));
        // Load drop point
        plan.addWaypoint(new Waypoint(
                offsetPosition(centre, 1000, 20), 5, 180));
        plan.addWaypoint(new Waypoint(
                offsetPosition(centre, 1000, 20), 0, 210));  // hover & drop
        plan.addWaypoint(new Waypoint(
                offsetPosition(centre, 8000, 350), 18, 330));

        scenario.addTarget(target, plan);

        // Second drone for chaining test (S9_GR4)
        UasTarget target2 = targetGenerator.generateTarget("BORDER-TRAF2", UasClass.C3,
                offsetPosition(centre, 8000, 30), 14, 210, 50);
        target2.setHasPayload(true);
        target2.setPayloadDescription("2kg box; Trafficking payload");

        FlightPlan plan2 = new FlightPlan(target2.getUid());
        plan2.addWaypoint(new Waypoint(target2.getPosition(), 14, 30));
        plan2.addWaypoint(new Waypoint(
                offsetPosition(centre, 3000, 30), 11, 120));
        plan2.addWaypoint(new Waypoint(
                offsetPosition(centre, 7000, 200), 16, 300));

        scenario.addTarget(target2, plan2);
        log.info("Created S9-LandBorder scenario: {}", scenario);
        return scenario;
    }

    /**
     * S10: Maritime Border — Mini multirotor evading coast guard, night clear.
     * Class I mini commercial multirotor (<15kg).
     * Reqs: S10_GR1/8, FRMB_1/7/27, PRMB_1/16/37.
     */
    public TestScenario createMaritimeBorderScenario(GeoPosition centre) {
        TestEnvironment env = envDesigner.createAdverseWeatherEnvironment(
                "Maritime Border — Sea Environment", centre, 8000, WeatherCondition.NIGHT_CLEAR);
        env.setTerrainType("SEA");
        env.getParameters().put("saline_environment", "true");
        env.getParameters().put("sea_state", "moderate");

        TestScenario scenario = new TestScenario("S10-MARITIME",
                "S10: Maritime Border — Coast Guard Evasion", env);
        scenario.setDescription("Mini commercial multirotor with thermal sensor " +
                "attempts to evade coast guard at night over sea.");
        scenario.setDurationSeconds(300);
        linkScenarioRequirements(scenario, "S10");

        UasTarget target = targetGenerator.generateTarget("SEA-EVADER", UasClass.C1,
                offsetPosition(centre, 7000, 180), 12, 0, 15);  // low altitude
        target.setHasPayload(true);
        target.setPayloadDescription("Thermal sensor; Coast guard evasion");

        FlightPlan plan = new FlightPlan(target.getUid());
        plan.addWaypoint(new Waypoint(target.getPosition(), 12, 0));
        plan.addWaypoint(new Waypoint(
                offsetPosition(centre, 4000, 180), 10, 80));
        // Low altitude evasive manoeuvres (PRMB_16)
        plan.addWaypoint(new Waypoint(
                offsetPosition(centre, 2000, 200), 8, 120));
        plan.addWaypoint(new Waypoint(
                offsetPosition(centre, 1000, 160), 6, 150));
        plan.addWaypoint(new Waypoint(
                offsetPosition(centre, 5000, 350), 14, 250));

        scenario.addTarget(target, plan);
        log.info("Created S10-Maritime scenario: {}", scenario);
        return scenario;
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    /**
     * Generate a flight plan converging on the centre position.
     */
    private FlightPlan generateConvergingPlan(UasTarget target, GeoPosition centre) {
        FlightPlan plan = new FlightPlan(target.getUid());
        plan.addWaypoint(new Waypoint(target.getPosition(), target.getSpeedMs(), 0));
        plan.addWaypoint(new Waypoint(
                new GeoPosition(
                        centre.getLatitude() + rng.nextGaussian() * 0.005,
                        centre.getLongitude() + rng.nextGaussian() * 0.005,
                        target.getPosition().getAltitudeMsl()),
                target.getSpeedMs(), 90));
        return plan;
    }

    /**
     * Link all scenario-specific requirements from the JSON to a TestScenario.
     */
    private void linkScenarioRequirements(TestScenario scenario, String scenarioCode) {
        List<String> allIds = reqManager.getAllScenarioRequirementIds(scenarioCode);
        scenario.getRequirementIds().addAll(allIds);
    }

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
}
