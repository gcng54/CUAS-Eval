package io.github.gcng54.cuaseval.generator;

import io.github.gcng54.cuaseval.model.*;
import io.github.gcng54.cuaseval.model.TestEnvironment.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Designs test environments (test sites) for CUAS evaluation.
 * Configures sensor deployments, terrain, weather, and observation zones
 * per CWA 18150 test methodology.
 * <p>
 * Requirement links: PR06 (non-interference), PR08 (weather),
 * TP_D05 (terrain masking), TP_D10 (detection angles).
 * </p>
 */
public class TestEnvironmentDesigner {

    private static final Logger log = LoggerFactory.getLogger(TestEnvironmentDesigner.class);

    /**
     * Create a standard open-field test environment.
     * Suitable for baseline detection/tracking tests.
     */
    public TestEnvironment createOpenFieldEnvironment(String name,
                                                       GeoPosition centre,
                                                       double radiusM) {
        TestEnvironment env = new TestEnvironment(name, centre, radiusM);
        env.setTerrainType("OPEN_FIELD");
        env.setWeather(WeatherCondition.CLEAR);
        env.setTimeOfDay("DAY");

        // Deploy a standard radar sensor at centre
        SensorSite radar = new SensorSite("RADAR-01", "RADAR",
                centre, radiusM * 1.2);
        radar.setAzimuthCoverageDeg(360);
        env.getSensorSites().add(radar);

        // Deploy an EO/IR sensor
        SensorSite eoIr = new SensorSite("EOIR-01", "EO/IR",
                new GeoPosition(centre.getLatitude() + 0.001,
                        centre.getLongitude() + 0.001, centre.getAltitudeMsl()),
                radiusM * 0.8);
        env.getSensorSites().add(eoIr);

        log.info("Created open-field environment: {}", env);
        return env;
    }

    /**
     * Create an urban test environment with obstacles.
     * Tests terrain masking (TP_D05) and detection behind obstacles (FR23).
     */
    public TestEnvironment createUrbanEnvironment(String name,
                                                    GeoPosition centre,
                                                    double radiusM) {
        TestEnvironment env = new TestEnvironment(name, centre, radiusM);
        env.setTerrainType("URBAN");
        env.setWeather(WeatherCondition.CLEAR);
        env.setTimeOfDay("DAY");

        // Multi-sensor deployment to cover urban gaps
        double offset = radiusM * 0.3 / 111_320.0;
        env.getSensorSites().add(new SensorSite("RADAR-01", "RADAR",
                new GeoPosition(centre.getLatitude() + offset,
                        centre.getLongitude(), centre.getAltitudeMsl()),
                radiusM));
        env.getSensorSites().add(new SensorSite("RADAR-02", "RADAR",
                new GeoPosition(centre.getLatitude() - offset,
                        centre.getLongitude(), centre.getAltitudeMsl()),
                radiusM));
        env.getSensorSites().add(new SensorSite("RF-01", "RF_DETECTOR",
                centre, radiusM * 0.6));
        env.getSensorSites().add(new SensorSite("ACOUSTIC-01", "ACOUSTIC",
                centre, radiusM * 0.3));

        // Add building obstacles (TP_D05)
        env.getObstacles().add(new Obstacle("Office Building A",
                new GeoPosition(centre.getLatitude() + offset * 0.5,
                        centre.getLongitude() + offset * 0.5), 30, 50));
        env.getObstacles().add(new Obstacle("Warehouse B",
                new GeoPosition(centre.getLatitude() - offset * 0.3,
                        centre.getLongitude() + offset * 0.8), 15, 80));

        log.info("Created urban environment: {}", env);
        return env;
    }

    /**
     * Create a night/adverse-weather environment.
     * Tests PR08, PR13, PR14 requirements.
     */
    public TestEnvironment createAdverseWeatherEnvironment(String name,
                                                             GeoPosition centre,
                                                             double radiusM,
                                                             WeatherCondition weather) {
        TestEnvironment env = new TestEnvironment(name, centre, radiusM);
        env.setTerrainType("OPEN_FIELD");
        env.setWeather(weather);
        env.setTimeOfDay(weather.name().startsWith("NIGHT") ? "NIGHT" : "DAY");

        // Deploy weather-resistant sensors
        env.getSensorSites().add(new SensorSite("RADAR-01", "RADAR",
                centre, radiusM * 1.0));
        env.getSensorSites().add(new SensorSite("IR-01", "IR",
                centre, radiusM * 0.5));

        log.info("Created adverse-weather environment: {}", env);
        return env;
    }

    /**
     * Create a custom multi-sensor environment.
     */
    public TestEnvironment createCustomEnvironment(String name,
                                                     GeoPosition centre,
                                                     double radiusM,
                                                     String terrainType,
                                                     WeatherCondition weather,
                                                     String timeOfDay,
                                                     List<SensorSite> sensors) {
        TestEnvironment env = new TestEnvironment(name, centre, radiusM);
        env.setTerrainType(terrainType);
        env.setWeather(weather);
        env.setTimeOfDay(timeOfDay);
        env.getSensorSites().addAll(sensors);
        log.info("Created custom environment: {}", env);
        return env;
    }
}
