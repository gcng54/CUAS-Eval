package io.github.gcng54.cuaseval.model;

import java.util.Locale;

/**
 * Comprehensive CUAS sensor model with real-world detection parameters.
 * Supports radar, EO/IR, RF detection, acoustic, and lidar sensor types.
 * Each sensor defines detection envelope, performance curves, and environmental sensitivity.
 *
 * <p>Used by the DTI pipeline to compute sensor-specific Pd, tracking accuracy,
 * and identification capability based on target RCS, range, speed, and weather.</p>
 */
public class CuasSensor {

    // ── Identity ────────────────────────────────────────────────────────

    /** Unique sensor identifier */
    private String sensorId;

    /** Human-readable sensor name/model */
    private String name;

    /** Sensor category */
    private SensorType sensorType;

    /** Sensor subtype (e.g., PULSE_DOPPLER, FMCW, MWIR, LWIR, SDR) */
    private String subType;

    /** Manufacturer or origin */
    private String manufacturer;

    // ── Detection Envelope ──────────────────────────────────────────────

    /** Maximum instrumented range in metres */
    private double maxRangeM;

    /** Minimum detection range in metres */
    private double minRangeM;

    /** Maximum altitude coverage in metres AGL */
    private double maxAltitudeM;

    /** Minimum detectable altitude in metres AGL */
    private double minAltitudeM;

    /** Horizontal azimuth coverage in degrees (360 = omnidirectional) */
    private double azimuthCoverageDeg;

    /** Elevation coverage in degrees */
    private double elevationCoverageDeg;

    // ── Detection Performance ───────────────────────────────────────────

    /** Base probability of detection at reference range */
    private double basePd;

    /** Reference range (m) at which basePd applies */
    private double referenceRangeM;

    /** Minimum detectable RCS in m² (for radar) or equivalent */
    private double minDetectableRcs;

    /** False alarm rate per scan (probability) */
    private double falseAlarmRate;

    /** Detection latency mean in seconds */
    private double detectionLatencyS;

    /** Position accuracy (CEP) in metres at reference range */
    private double positionAccuracyM;

    /** Update rate in Hz */
    private double updateRateHz;

    // ── Tracking & Identification ───────────────────────────────────────

    /** Tracking noise standard deviation in metres */
    private double trackingNoiseM;

    /** Track maintenance probability per update */
    private double trackMaintenanceProb;

    /** Can this sensor contribute to identification? */
    private boolean canIdentify;

    /** Identification probability (if canIdentify) */
    private double identificationPd;

    /** Can classify UAS vs bird vs clutter? */
    private boolean canClassify;

    /** Classification accuracy (if canClassify) */
    private double classificationAccuracy;

    // ── Environmental Sensitivity ───────────────────────────────────────

    /** Performance factor in rain (0.0–1.0, 1.0 = no degradation) */
    private double rainFactor;

    /** Performance factor in fog (0.0–1.0) */
    private double fogFactor;

    /** Performance factor at night (0.0–1.0) */
    private double nightFactor;

    /** Performance factor in snow (0.0–1.0) */
    private double snowFactor;

    /** Wind resistance: max operational wind speed in m/s */
    private double maxWindSpeedMs;

    // ── Physical Characteristics ────────────────────────────────────────

    /** Power consumption in watts */
    private double powerConsumptionW;

    /** Weight in kg */
    private double weightKg;

    /** Setup time in minutes */
    private double setupTimeMin;

    /** Operating frequency band description */
    private String frequencyBand;

    /** Is the sensor mobile/relocatable? */
    private boolean mobile;

    // ── EW & Coverage Extensions (v2.0) ─────────────────────────────────

    /** EW sensitivity: how much EW degrades this sensor (0.0 = immune, 1.0 = fully affected) */
    private double ewSensitivity = 0.0;

    /** Minimum elevation coverage in degrees */
    private double minElevationDeg = 0.0;

    /** Maximum elevation coverage in degrees */
    private double maxElevationDeg = 90.0;

    /** Azimuth start angle (0=North, clockwise). With azimuthCoverageDeg defines sector. */
    private double azimuthStartDeg = 0.0;

    // ── Sensor Type Enum ────────────────────────────────────────────────

    /** Primary sensor technology categories for CUAS detection. */
    public enum SensorType {
        /** Active radar (pulse/CW/FMCW) */
        RADAR("Radar", true, true, true),
        /** Electro-optical / infrared camera */
        EO_IR("EO/IR", false, true, true),
        /** Radio frequency detector for C2 links */
        RF_DETECTOR("RF Detector", true, false, false),
        /** Acoustic / microphone array */
        ACOUSTIC("Acoustic", false, false, false),
        /** Lidar / laser scanner */
        LIDAR("Lidar", true, true, true),
        /** Combined / multi-modal sensor */
        MULTI_SENSOR("Multi-Sensor", true, true, true);

        private final String displayName;
        private final boolean providesRange;
        private final boolean providesBearing;
        private final boolean providesElevation;

        SensorType(String displayName, boolean providesRange,
                   boolean providesBearing, boolean providesElevation) {
            this.displayName = displayName;
            this.providesRange = providesRange;
            this.providesBearing = providesBearing;
            this.providesElevation = providesElevation;
        }

        public String getDisplayName()     { return displayName; }
        public boolean providesRange()     { return providesRange; }
        public boolean providesBearing()   { return providesBearing; }
        public boolean providesElevation() { return providesElevation; }
    }

    // ── Constructors ────────────────────────────────────────────────────

    public CuasSensor() {}

    public CuasSensor(String sensorId, String name, SensorType sensorType) {
        this.sensorId = sensorId;
        this.name = name;
        this.sensorType = sensorType;
        // Defaults
        this.basePd = 0.90;
        this.referenceRangeM = 1000;
        this.falseAlarmRate = 0.01;
        this.detectionLatencyS = 1.0;
        this.positionAccuracyM = 10.0;
        this.updateRateHz = 1.0;
        this.trackingNoiseM = 5.0;
        this.trackMaintenanceProb = 0.95;
        this.rainFactor = 0.9;
        this.fogFactor = 0.9;
        this.nightFactor = 1.0;
        this.snowFactor = 0.9;
        this.maxWindSpeedMs = 25.0;
    }

    // ── Performance Computation ─────────────────────────────────────────

    /**
     * Compute the effective probability of detection for a target at given range and RCS.
     * Uses sensor-specific detection model with range degradation, RCS sensitivity,
     * and environmental factors.
     *
     * @param rangeM     range to target in metres
     * @param rcsM2      radar cross section of target in m² (or equivalent for non-radar)
     * @param weather    current weather condition
     * @return effective Pd (0.0–1.0)
     */
    public double computePd(double rangeM, double rcsM2, TestEnvironment.WeatherCondition weather) {
        return computePd(rangeM, rcsM2, weather, null);
    }

    /**
     * Compute Pd with EW effects (EV-02).
     * EW degrades RF-based sensors (RADAR, RF_DETECTOR) by ewSensitivity * (1 - ewFactor).
     */
    public double computePd(double rangeM, double rcsM2,
                            TestEnvironment.WeatherCondition weather,
                            TestEnvironment.EwCondition ewCondition) {
        if (rangeM > maxRangeM || rangeM < minRangeM) return 0.0;

        // Range degradation: radar law (R^4 for radar, R^2 for others)
        double rangeFactor;
        if (sensorType == SensorType.RADAR || sensorType == SensorType.LIDAR) {
            // Radar equation: SNR ∝ 1/R^4, but Pd saturates → use Swerling-I model
            double normRange = rangeM / referenceRangeM;
            rangeFactor = Math.exp(-0.5 * Math.pow(normRange - 1, 2) * 2)
                    * Math.min(1.0, Math.pow(referenceRangeM / Math.max(rangeM, 1), 2));
        } else {
            double normRange = rangeM / referenceRangeM;
            rangeFactor = Math.min(1.0, 1.0 / Math.pow(normRange, 1.5));
        }

        // RCS factor (how detectable is the target)
        double rcsFactor = 1.0;
        if (sensorType == SensorType.RADAR) {
            rcsFactor = 0.3 + 0.7 * Math.min(1.0, Math.sqrt(rcsM2 / Math.max(minDetectableRcs, 0.001)));
        } else if (sensorType == SensorType.ACOUSTIC) {
            rcsFactor = 0.5 + 0.5 * Math.min(1.0, rcsM2 / 0.1); // prop noise proxy
        }

        // Environmental degradation
        double envFactor = getWeatherFactor(weather);

        // EW degradation (EV-02): RF-based sensors are affected
        double ewFactor = computeEwFactor(ewCondition);

        double pd = basePd * rangeFactor * rcsFactor * envFactor * ewFactor;
        return Math.max(0.0, Math.min(1.0, pd));
    }

    /**
     * Compute EW degradation factor for this sensor.
     * Only RF-based sensors (RADAR, RF_DETECTOR, MULTI_SENSOR) are affected.
     */
    public double computeEwFactor(TestEnvironment.EwCondition ewCondition) {
        if (ewCondition == null || ewCondition == TestEnvironment.EwCondition.NONE) return 1.0;
        if (ewSensitivity <= 0.0) return 1.0;
        // EW factor: 1.0 - (sensitivity * (1 - conditionFactor))
        double degradation = ewSensitivity * (1.0 - ewCondition.getDefaultFactor());
        return Math.max(0.0, 1.0 - degradation);
    }

    /**
     * Get the combined weather degradation factor for this sensor.
     */
    public double getWeatherFactor(TestEnvironment.WeatherCondition weather) {
        if (weather == null) return 1.0;
        return switch (weather) {
            case CLEAR -> 1.0;
            case CLOUDY -> 0.98;
            case RAIN -> rainFactor;
            case FOG -> fogFactor;
            case SNOW -> snowFactor;
            case NIGHT_CLEAR -> nightFactor;
            case NIGHT_OVERCAST -> nightFactor * 0.95;
        };
    }

    /**
     * Compute position accuracy at a given range.
     */
    public double computePositionAccuracy(double rangeM) {
        double normRange = rangeM / Math.max(referenceRangeM, 1);
        return positionAccuracyM * Math.max(1.0, normRange);
    }

    // ── Deep Copy ───────────────────────────────────────────────────────

    /**
     * Create a deep copy of this sensor configuration.
     */
    public CuasSensor copy() {
        CuasSensor c = new CuasSensor();
        c.sensorId = this.sensorId;
        c.name = this.name;
        c.sensorType = this.sensorType;
        c.subType = this.subType;
        c.manufacturer = this.manufacturer;
        c.maxRangeM = this.maxRangeM;
        c.minRangeM = this.minRangeM;
        c.maxAltitudeM = this.maxAltitudeM;
        c.minAltitudeM = this.minAltitudeM;
        c.azimuthCoverageDeg = this.azimuthCoverageDeg;
        c.elevationCoverageDeg = this.elevationCoverageDeg;
        c.basePd = this.basePd;
        c.referenceRangeM = this.referenceRangeM;
        c.minDetectableRcs = this.minDetectableRcs;
        c.falseAlarmRate = this.falseAlarmRate;
        c.detectionLatencyS = this.detectionLatencyS;
        c.positionAccuracyM = this.positionAccuracyM;
        c.updateRateHz = this.updateRateHz;
        c.trackingNoiseM = this.trackingNoiseM;
        c.trackMaintenanceProb = this.trackMaintenanceProb;
        c.canIdentify = this.canIdentify;
        c.identificationPd = this.identificationPd;
        c.canClassify = this.canClassify;
        c.classificationAccuracy = this.classificationAccuracy;
        c.rainFactor = this.rainFactor;
        c.fogFactor = this.fogFactor;
        c.nightFactor = this.nightFactor;
        c.snowFactor = this.snowFactor;
        c.maxWindSpeedMs = this.maxWindSpeedMs;
        c.powerConsumptionW = this.powerConsumptionW;
        c.weightKg = this.weightKg;
        c.setupTimeMin = this.setupTimeMin;
        c.frequencyBand = this.frequencyBand;
        c.mobile = this.mobile;
        c.ewSensitivity = this.ewSensitivity;
        c.minElevationDeg = this.minElevationDeg;
        c.maxElevationDeg = this.maxElevationDeg;
        c.azimuthStartDeg = this.azimuthStartDeg;
        return c;
    }

    // ── Accessors ───────────────────────────────────────────────────────

    public String getSensorId()           { return sensorId; }
    public String getName()               { return name; }
    public SensorType getSensorType()     { return sensorType; }
    public String getSubType()            { return subType; }
    public String getManufacturer()       { return manufacturer; }
    public double getMaxRangeM()          { return maxRangeM; }
    public double getMinRangeM()          { return minRangeM; }
    public double getMaxAltitudeM()       { return maxAltitudeM; }
    public double getMinAltitudeM()       { return minAltitudeM; }
    public double getAzimuthCoverageDeg() { return azimuthCoverageDeg; }
    public double getElevationCoverageDeg() { return elevationCoverageDeg; }
    public double getBasePd()             { return basePd; }
    public double getReferenceRangeM()    { return referenceRangeM; }
    public double getMinDetectableRcs()   { return minDetectableRcs; }
    public double getFalseAlarmRate()     { return falseAlarmRate; }
    public double getDetectionLatencyS()  { return detectionLatencyS; }
    public double getPositionAccuracyM()  { return positionAccuracyM; }
    public double getUpdateRateHz()       { return updateRateHz; }
    public double getTrackingNoiseM()     { return trackingNoiseM; }
    public double getTrackMaintenanceProb() { return trackMaintenanceProb; }
    public boolean canIdentify()          { return canIdentify; }
    public double getIdentificationPd()   { return identificationPd; }
    public boolean canClassify()          { return canClassify; }
    public double getClassificationAccuracy() { return classificationAccuracy; }
    public double getRainFactor()         { return rainFactor; }
    public double getFogFactor()          { return fogFactor; }
    public double getNightFactor()        { return nightFactor; }
    public double getSnowFactor()         { return snowFactor; }
    public double getMaxWindSpeedMs()     { return maxWindSpeedMs; }
    public double getPowerConsumptionW()  { return powerConsumptionW; }
    public double getWeightKg()           { return weightKg; }
    public double getSetupTimeMin()       { return setupTimeMin; }
    public String getFrequencyBand()      { return frequencyBand; }
    public boolean isMobile()             { return mobile; }
    public double getEwSensitivity()       { return ewSensitivity; }
    public double getMinElevationDeg()     { return minElevationDeg; }
    public double getMaxElevationDeg()     { return maxElevationDeg; }
    public double getAzimuthStartDeg()     { return azimuthStartDeg; }

    public void setSensorId(String id)              { this.sensorId = id; }
    public void setName(String name)                { this.name = name; }
    public void setSensorType(SensorType t)         { this.sensorType = t; }
    public void setSubType(String st)               { this.subType = st; }
    public void setManufacturer(String m)            { this.manufacturer = m; }
    public void setMaxRangeM(double v)              { this.maxRangeM = v; }
    public void setMinRangeM(double v)              { this.minRangeM = v; }
    public void setMaxAltitudeM(double v)           { this.maxAltitudeM = v; }
    public void setMinAltitudeM(double v)           { this.minAltitudeM = v; }
    public void setAzimuthCoverageDeg(double v)     { this.azimuthCoverageDeg = v; }
    public void setElevationCoverageDeg(double v)   { this.elevationCoverageDeg = v; }
    public void setBasePd(double v)                 { this.basePd = v; }
    public void setReferenceRangeM(double v)        { this.referenceRangeM = v; }
    public void setMinDetectableRcs(double v)       { this.minDetectableRcs = v; }
    public void setFalseAlarmRate(double v)         { this.falseAlarmRate = v; }
    public void setDetectionLatencyS(double v)      { this.detectionLatencyS = v; }
    public void setPositionAccuracyM(double v)      { this.positionAccuracyM = v; }
    public void setUpdateRateHz(double v)           { this.updateRateHz = v; }
    public void setTrackingNoiseM(double v)         { this.trackingNoiseM = v; }
    public void setTrackMaintenanceProb(double v)   { this.trackMaintenanceProb = v; }
    public void setCanIdentify(boolean v)           { this.canIdentify = v; }
    public void setIdentificationPd(double v)       { this.identificationPd = v; }
    public void setCanClassify(boolean v)           { this.canClassify = v; }
    public void setClassificationAccuracy(double v) { this.classificationAccuracy = v; }
    public void setRainFactor(double v)             { this.rainFactor = v; }
    public void setFogFactor(double v)              { this.fogFactor = v; }
    public void setNightFactor(double v)            { this.nightFactor = v; }
    public void setSnowFactor(double v)             { this.snowFactor = v; }
    public void setMaxWindSpeedMs(double v)         { this.maxWindSpeedMs = v; }
    public void setPowerConsumptionW(double v)      { this.powerConsumptionW = v; }
    public void setWeightKg(double v)               { this.weightKg = v; }
    public void setSetupTimeMin(double v)           { this.setupTimeMin = v; }
    public void setFrequencyBand(String v)          { this.frequencyBand = v; }
    public void setMobile(boolean v)                { this.mobile = v; }
    public void setEwSensitivity(double v)           { this.ewSensitivity = v; }
    public void setMinElevationDeg(double v)         { this.minElevationDeg = v; }
    public void setMaxElevationDeg(double v)         { this.maxElevationDeg = v; }
    public void setAzimuthStartDeg(double v)         { this.azimuthStartDeg = v; }

    @Override
    public String toString() {
        return String.format(Locale.ENGLISH,
                "%s [%s] %s — range=%.0f–%.0fm Pd=%.2f FAR=%.4f az=%.0f°",
                sensorId, sensorType.getDisplayName(), name,
                minRangeM, maxRangeM, basePd, falseAlarmRate, azimuthCoverageDeg);
    }
}
