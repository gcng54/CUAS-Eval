package io.github.gcng54.cuaseval.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Describes the test environment / site configuration per CWA 18150.
 * Includes geography, weather, sensor layout, and observation zone definition.
 * <p>
 * Requirement links: PR08 (weather resistance), PR06 (non-interference),
 * TP_D05 (terrain masking).
 * </p>
 */
public class TestEnvironment {

    /** Environment / test-site name */
    private String name;

    /** Centre of the observation area */
    private GeoPosition centrePosition;

    /** Radius of observation area in metres */
    private double observationRadiusM;

    /** Terrain type description */
    private String terrainType;

    /** Weather condition during test */
    private WeatherCondition weather;

    /** Time of day: DAY, NIGHT, DUSK, DAWN */
    private String timeOfDay;

    /** Sensor positions deployed in this environment */
    private List<SensorSite> sensorSites = new ArrayList<>();

    /** Obstacles / buildings that may cause terrain masking (TP_D05) */
    private List<Obstacle> obstacles = new ArrayList<>();

    /** Additional environmental parameters */
    private Map<String, String> parameters = new HashMap<>();

    /** Electronic Warfare condition affecting RF sensors (EV-01) */
    private EwCondition ewCondition = EwCondition.NONE;

    /** EW degradation factor (0.0 = full jamming, 1.0 = no effect) */
    private double ewFactor = 1.0;

    /** Frequency bands affected by EW */
    private List<String> ewAffectedBands = new ArrayList<>();

    /** EW jammer position (if directional) */
    private GeoPosition ewJammerPosition;

    // ── Inner types ─────────────────────────────────────────────────────

    /** Weather conditions affecting detection performance (PR08, PR13, PR14). */
    public enum WeatherCondition {
        CLEAR, CLOUDY, RAIN, FOG, SNOW, NIGHT_CLEAR, NIGHT_OVERCAST
    }

    /** Electronic Warfare conditions affecting RF sensor performance (EV-01, EV-02). */
    public enum EwCondition {
        NONE(1.0), LOW(0.8), MEDIUM(0.5), HIGH(0.2);

        private final double defaultFactor;
        EwCondition(double defaultFactor) { this.defaultFactor = defaultFactor; }
        public double getDefaultFactor() { return defaultFactor; }
    }

    /** Obstacle type for terrain masking (TR-01). */
    public enum ObstacleType {
        BUILDING, TOWER, TREE_LINE, HILL, WALL, CUSTOM
    }

    /** A positioned sensor/technology site within the environment. */
    public static class SensorSite {
        private String sensorId;
        private String sensorType;   // e.g. RADAR, EO/IR, RF, ACOUSTIC
        private GeoPosition position;
        private double maxRangeM;
        private double azimuthCoverageDeg; // horizontal coverage in degrees

        /** Optional reference to a full CuasSensor template */
        private CuasSensor sensorTemplate;

        public SensorSite() {}
        public SensorSite(String sensorId, String sensorType,
                          GeoPosition position, double maxRangeM) {
            this.sensorId = sensorId;
            this.sensorType = sensorType;
            this.position = position;
            this.maxRangeM = maxRangeM;
            this.azimuthCoverageDeg = 360;
        }

        /**
         * Create a SensorSite from a CuasSensor template at a given position.
         */
        public SensorSite(CuasSensor sensor, GeoPosition position) {
            this.sensorId = sensor.getSensorId();
            this.sensorType = sensor.getSensorType().getDisplayName();
            this.position = position;
            this.maxRangeM = sensor.getMaxRangeM();
            this.azimuthCoverageDeg = sensor.getAzimuthCoverageDeg();
            this.sensorTemplate = sensor;
        }

        public String getSensorId()          { return sensorId; }
        public String getSensorType()        { return sensorType; }
        public GeoPosition getPosition()     { return position; }
        public double getMaxRangeM()         { return maxRangeM; }
        public double getAzimuthCoverageDeg(){ return azimuthCoverageDeg; }
        public CuasSensor getSensorTemplate(){ return sensorTemplate; }

        public void setSensorId(String id)                  { this.sensorId = id; }
        public void setSensorType(String type)              { this.sensorType = type; }
        public void setPosition(GeoPosition pos)            { this.position = pos; }
        public void setMaxRangeM(double r)                  { this.maxRangeM = r; }
        public void setAzimuthCoverageDeg(double deg)       { this.azimuthCoverageDeg = deg; }
        public void setSensorTemplate(CuasSensor sensor)    { this.sensorTemplate = sensor; }
    }

    /** An obstacle that may mask detection (TP_D05, TR-01, TR-02). */
    public static class Obstacle {
        private String description;
        private GeoPosition position;
        private double heightM;
        private double widthM;
        private double lengthM;
        private ObstacleType obstacleType = ObstacleType.BUILDING;

        public Obstacle() {}
        public Obstacle(String description, GeoPosition position,
                        double heightM, double widthM) {
            this.description = description;
            this.position = position;
            this.heightM = heightM;
            this.widthM = widthM;
            this.lengthM = widthM;
        }

        public Obstacle(String description, GeoPosition position,
                        double heightM, double widthM, double lengthM,
                        ObstacleType type) {
            this.description = description;
            this.position = position;
            this.heightM = heightM;
            this.widthM = widthM;
            this.lengthM = lengthM;
            this.obstacleType = type;
        }

        public String getDescription()    { return description; }
        public GeoPosition getPosition()  { return position; }
        public double getHeightM()        { return heightM; }
        public double getWidthM()         { return widthM; }
        public double getLengthM()        { return lengthM; }
        public ObstacleType getObstacleType() { return obstacleType; }

        public void setDescription(String desc)        { this.description = desc; }
        public void setPosition(GeoPosition pos)       { this.position = pos; }
        public void setHeightM(double h)               { this.heightM = h; }
        public void setWidthM(double w)                { this.widthM = w; }
        public void setLengthM(double l)               { this.lengthM = l; }
        public void setObstacleType(ObstacleType type) { this.obstacleType = type; }

        @Override
        public String toString() {
            return String.format(Locale.ENGLISH, "%s [%s] %.0fx%.0fx%.0fm @ %.5f,%.5f",
                    description, obstacleType, widthM, lengthM, heightM,
                    position != null ? position.getLatitude() : 0,
                    position != null ? position.getLongitude() : 0);
        }
    }

    // ── Constructors ────────────────────────────────────────────────────

    public TestEnvironment() {}

    public TestEnvironment(String name, GeoPosition centre, double radiusM) {
        this.name = name;
        this.centrePosition = centre;
        this.observationRadiusM = radiusM;
        this.weather = WeatherCondition.CLEAR;
        this.timeOfDay = "DAY";
    }

    // ── Accessors ───────────────────────────────────────────────────────

    public String getName()                       { return name; }
    public GeoPosition getCentrePosition()        { return centrePosition; }
    public double getObservationRadiusM()         { return observationRadiusM; }
    public String getTerrainType()                { return terrainType; }
    public WeatherCondition getWeather()           { return weather; }
    public String getTimeOfDay()                  { return timeOfDay; }
    public List<SensorSite> getSensorSites()      { return sensorSites; }
    public List<Obstacle> getObstacles()          { return obstacles; }
    public Map<String, String> getParameters()    { return parameters; }

    public void setName(String name)                          { this.name = name; }
    public void setCentrePosition(GeoPosition centrePosition) { this.centrePosition = centrePosition; }
    public void setObservationRadiusM(double observationRadiusM){ this.observationRadiusM = observationRadiusM; }
    public void setTerrainType(String terrainType)            { this.terrainType = terrainType; }
    public void setWeather(WeatherCondition weather)           { this.weather = weather; }
    public void setTimeOfDay(String timeOfDay)                { this.timeOfDay = timeOfDay; }

    public EwCondition getEwCondition()                { return ewCondition; }
    public double getEwFactor()                        { return ewFactor; }
    public List<String> getEwAffectedBands()           { return ewAffectedBands; }
    public GeoPosition getEwJammerPosition()           { return ewJammerPosition; }

    public void setEwCondition(EwCondition ewCondition) {
        this.ewCondition = ewCondition;
        this.ewFactor = ewCondition.getDefaultFactor();
    }
    public void setEwFactor(double ewFactor)            { this.ewFactor = ewFactor; }
    public void setEwAffectedBands(List<String> bands)  { this.ewAffectedBands = bands; }
    public void setEwJammerPosition(GeoPosition pos)    { this.ewJammerPosition = pos; }

    @Override
    public String toString() {
        return String.format(Locale.ENGLISH, "Env[%s] centre=%s radius=%.0fm weather=%s ew=%s sensors=%d",
                name, centrePosition, observationRadiusM, weather, ewCondition, sensorSites.size());
    }
}
