package io.github.gcng54.cuaseval.model;

import java.util.*;

/**
 * Library of pre-configured CUAS sensor templates based on real-world system categories.
 * Provides selectable sensor configurations for DTI pipeline evaluation.
 *
 * <p>Templates are organized by sensor type and represent typical performance
 * envelopes of commercial and military CUAS sensors.</p>
 *
 * <h3>Available Templates:</h3>
 * <ul>
 *   <li><b>Radar:</b> Pulse Doppler, FMCW, Phased Array, 3D Surveillance</li>
 *   <li><b>EO/IR:</b> Daylight Camera, MWIR Thermal, LWIR Thermal, Multi-Spectral</li>
 *   <li><b>RF Detector:</b> Wideband SDR, Narrowband C2, Protocol Analyser</li>
 *   <li><b>Acoustic:</b> Microphone Array, Directional Acoustic</li>
 *   <li><b>Lidar:</b> Scanning Lidar</li>
 *   <li><b>Multi-Sensor:</b> Integrated Radar+EO/IR, Full Spectrum Suite</li>
 * </ul>
 */
public class SensorLibrary {

    private static final Map<String, CuasSensor> TEMPLATES = new LinkedHashMap<>();

    static {
        // ──────────────────── RADAR SENSORS ────────────────────────────

        // RD-01: Short-range pulse Doppler radar (e.g., Blighter A400)
        CuasSensor rd01 = new CuasSensor("RD-01", "Pulse Doppler Radar — Short Range",
                CuasSensor.SensorType.RADAR);
        rd01.setSubType("PULSE_DOPPLER");
        rd01.setManufacturer("Generic");
        rd01.setMaxRangeM(4800);
        rd01.setMinRangeM(50);
        rd01.setMaxAltitudeM(1500);
        rd01.setMinAltitudeM(0);
        rd01.setAzimuthCoverageDeg(90);
        rd01.setElevationCoverageDeg(20);
        rd01.setBasePd(0.92);
        rd01.setReferenceRangeM(2000);
        rd01.setMinDetectableRcs(0.01);
        rd01.setFalseAlarmRate(0.015);
        rd01.setDetectionLatencyS(0.5);
        rd01.setPositionAccuracyM(3.0);
        rd01.setUpdateRateHz(2.0);
        rd01.setTrackingNoiseM(2.5);
        rd01.setTrackMaintenanceProb(0.97);
        rd01.setCanIdentify(false);
        rd01.setCanClassify(true);
        rd01.setClassificationAccuracy(0.75);
        rd01.setRainFactor(0.85);
        rd01.setFogFactor(0.95);
        rd01.setNightFactor(1.0);
        rd01.setSnowFactor(0.80);
        rd01.setPowerConsumptionW(250);
        rd01.setWeightKg(25);
        rd01.setSetupTimeMin(30);
        rd01.setFrequencyBand("Ku-band (16.8 GHz)");
        rd01.setMobile(true);
        rd01.setEwSensitivity(0.9);
        TEMPLATES.put("RD-01", rd01);

        // RD-02: FMCW radar — medium range (e.g., Robin Radar ELVIRA)
        CuasSensor rd02 = new CuasSensor("RD-02", "FMCW Radar — Medium Range",
                CuasSensor.SensorType.RADAR);
        rd02.setSubType("FMCW");
        rd02.setManufacturer("Generic");
        rd02.setMaxRangeM(8000);
        rd02.setMinRangeM(30);
        rd02.setMaxAltitudeM(3000);
        rd02.setMinAltitudeM(0);
        rd02.setAzimuthCoverageDeg(360);
        rd02.setElevationCoverageDeg(30);
        rd02.setBasePd(0.94);
        rd02.setReferenceRangeM(3000);
        rd02.setMinDetectableRcs(0.005);
        rd02.setFalseAlarmRate(0.01);
        rd02.setDetectionLatencyS(0.3);
        rd02.setPositionAccuracyM(5.0);
        rd02.setUpdateRateHz(4.0);
        rd02.setTrackingNoiseM(3.0);
        rd02.setTrackMaintenanceProb(0.98);
        rd02.setCanIdentify(false);
        rd02.setCanClassify(true);
        rd02.setClassificationAccuracy(0.80);
        rd02.setRainFactor(0.88);
        rd02.setFogFactor(0.96);
        rd02.setNightFactor(1.0);
        rd02.setSnowFactor(0.82);
        rd02.setPowerConsumptionW(400);
        rd02.setWeightKg(40);
        rd02.setSetupTimeMin(45);
        rd02.setFrequencyBand("X-band (9.5 GHz)");
        rd02.setMobile(false);
        rd02.setEwSensitivity(0.9);
        TEMPLATES.put("RD-02", rd02);

        // RD-03: Phased Array 3D radar — long range (e.g., Thales Squire)
        CuasSensor rd03 = new CuasSensor("RD-03", "Phased Array 3D Radar — Long Range",
                CuasSensor.SensorType.RADAR);
        rd03.setSubType("PHASED_ARRAY");
        rd03.setManufacturer("Generic");
        rd03.setMaxRangeM(15000);
        rd03.setMinRangeM(100);
        rd03.setMaxAltitudeM(5000);
        rd03.setMinAltitudeM(0);
        rd03.setAzimuthCoverageDeg(360);
        rd03.setElevationCoverageDeg(60);
        rd03.setBasePd(0.96);
        rd03.setReferenceRangeM(5000);
        rd03.setMinDetectableRcs(0.01);
        rd03.setFalseAlarmRate(0.008);
        rd03.setDetectionLatencyS(0.2);
        rd03.setPositionAccuracyM(8.0);
        rd03.setUpdateRateHz(10.0);
        rd03.setTrackingNoiseM(4.0);
        rd03.setTrackMaintenanceProb(0.99);
        rd03.setCanIdentify(false);
        rd03.setCanClassify(true);
        rd03.setClassificationAccuracy(0.85);
        rd03.setRainFactor(0.90);
        rd03.setFogFactor(0.97);
        rd03.setNightFactor(1.0);
        rd03.setSnowFactor(0.85);
        rd03.setPowerConsumptionW(1500);
        rd03.setWeightKg(200);
        rd03.setSetupTimeMin(120);
        rd03.setFrequencyBand("S-band (3.0 GHz)");
        rd03.setMobile(false);
        rd03.setEwSensitivity(0.85);
        TEMPLATES.put("RD-03", rd03);

        // RD-04: Compact surveillance radar (e.g., Hensoldt Spexer 500)
        CuasSensor rd04 = new CuasSensor("RD-04", "Compact Surveillance Radar",
                CuasSensor.SensorType.RADAR);
        rd04.setSubType("FMCW");
        rd04.setManufacturer("Generic");
        rd04.setMaxRangeM(5000);
        rd04.setMinRangeM(20);
        rd04.setMaxAltitudeM(2000);
        rd04.setMinAltitudeM(0);
        rd04.setAzimuthCoverageDeg(120);
        rd04.setElevationCoverageDeg(40);
        rd04.setBasePd(0.93);
        rd04.setReferenceRangeM(2500);
        rd04.setMinDetectableRcs(0.003);
        rd04.setFalseAlarmRate(0.012);
        rd04.setDetectionLatencyS(0.4);
        rd04.setPositionAccuracyM(2.0);
        rd04.setUpdateRateHz(5.0);
        rd04.setTrackingNoiseM(1.5);
        rd04.setTrackMaintenanceProb(0.97);
        rd04.setCanIdentify(false);
        rd04.setCanClassify(true);
        rd04.setClassificationAccuracy(0.82);
        rd04.setRainFactor(0.87);
        rd04.setFogFactor(0.95);
        rd04.setNightFactor(1.0);
        rd04.setSnowFactor(0.83);
        rd04.setPowerConsumptionW(180);
        rd04.setWeightKg(15);
        rd04.setSetupTimeMin(15);
        rd04.setFrequencyBand("Ka-band (35 GHz)");
        rd04.setMobile(true);
        rd04.setEwSensitivity(0.9);
        TEMPLATES.put("RD-04", rd04);

        // ──────────────────── EO/IR SENSORS ────────────────────────────

        // EO-01: Daylight HD camera with tracking (e.g., FLIR Ranger HDC)
        CuasSensor eo01 = new CuasSensor("EO-01", "Daylight HD Camera — PTZ",
                CuasSensor.SensorType.EO_IR);
        eo01.setSubType("VISIBLE");
        eo01.setManufacturer("Generic");
        eo01.setMaxRangeM(3000);
        eo01.setMinRangeM(10);
        eo01.setMaxAltitudeM(2000);
        eo01.setMinAltitudeM(0);
        eo01.setAzimuthCoverageDeg(360);
        eo01.setElevationCoverageDeg(90);
        eo01.setBasePd(0.88);
        eo01.setReferenceRangeM(1500);
        eo01.setMinDetectableRcs(0.001);
        eo01.setFalseAlarmRate(0.02);
        eo01.setDetectionLatencyS(1.0);
        eo01.setPositionAccuracyM(5.0);
        eo01.setUpdateRateHz(30.0);
        eo01.setTrackingNoiseM(2.0);
        eo01.setTrackMaintenanceProb(0.93);
        eo01.setCanIdentify(true);
        eo01.setIdentificationPd(0.85);
        eo01.setCanClassify(true);
        eo01.setClassificationAccuracy(0.90);
        eo01.setRainFactor(0.60);
        eo01.setFogFactor(0.30);
        eo01.setNightFactor(0.10);
        eo01.setSnowFactor(0.50);
        eo01.setPowerConsumptionW(80);
        eo01.setWeightKg(12);
        eo01.setSetupTimeMin(15);
        eo01.setFrequencyBand("Visible (400–700 nm)");
        eo01.setMobile(true);
        TEMPLATES.put("EO-01", eo01);

        // EO-02: MWIR Thermal camera (e.g., FLIR MWIR cooled)
        CuasSensor eo02 = new CuasSensor("EO-02", "MWIR Thermal Imager — Cooled",
                CuasSensor.SensorType.EO_IR);
        eo02.setSubType("MWIR");
        eo02.setManufacturer("Generic");
        eo02.setMaxRangeM(5000);
        eo02.setMinRangeM(20);
        eo02.setMaxAltitudeM(3000);
        eo02.setMinAltitudeM(0);
        eo02.setAzimuthCoverageDeg(360);
        eo02.setElevationCoverageDeg(45);
        eo02.setBasePd(0.90);
        eo02.setReferenceRangeM(2500);
        eo02.setMinDetectableRcs(0.001);
        eo02.setFalseAlarmRate(0.018);
        eo02.setDetectionLatencyS(0.8);
        eo02.setPositionAccuracyM(4.0);
        eo02.setUpdateRateHz(30.0);
        eo02.setTrackingNoiseM(2.5);
        eo02.setTrackMaintenanceProb(0.94);
        eo02.setCanIdentify(true);
        eo02.setIdentificationPd(0.88);
        eo02.setCanClassify(true);
        eo02.setClassificationAccuracy(0.85);
        eo02.setRainFactor(0.75);
        eo02.setFogFactor(0.50);
        eo02.setNightFactor(0.95);
        eo02.setSnowFactor(0.70);
        eo02.setPowerConsumptionW(150);
        eo02.setWeightKg(18);
        eo02.setSetupTimeMin(20);
        eo02.setFrequencyBand("MWIR (3–5 µm)");
        eo02.setMobile(true);
        TEMPLATES.put("EO-02", eo02);

        // EO-03: LWIR Thermal camera — uncooled (e.g., FLIR uncooled)
        CuasSensor eo03 = new CuasSensor("EO-03", "LWIR Thermal Imager — Uncooled",
                CuasSensor.SensorType.EO_IR);
        eo03.setSubType("LWIR");
        eo03.setManufacturer("Generic");
        eo03.setMaxRangeM(3500);
        eo03.setMinRangeM(15);
        eo03.setMaxAltitudeM(2000);
        eo03.setMinAltitudeM(0);
        eo03.setAzimuthCoverageDeg(360);
        eo03.setElevationCoverageDeg(40);
        eo03.setBasePd(0.86);
        eo03.setReferenceRangeM(1800);
        eo03.setMinDetectableRcs(0.002);
        eo03.setFalseAlarmRate(0.02);
        eo03.setDetectionLatencyS(1.0);
        eo03.setPositionAccuracyM(5.0);
        eo03.setUpdateRateHz(25.0);
        eo03.setTrackingNoiseM(3.0);
        eo03.setTrackMaintenanceProb(0.92);
        eo03.setCanIdentify(true);
        eo03.setIdentificationPd(0.80);
        eo03.setCanClassify(true);
        eo03.setClassificationAccuracy(0.78);
        eo03.setRainFactor(0.70);
        eo03.setFogFactor(0.40);
        eo03.setNightFactor(0.90);
        eo03.setSnowFactor(0.65);
        eo03.setPowerConsumptionW(60);
        eo03.setWeightKg(8);
        eo03.setSetupTimeMin(10);
        eo03.setFrequencyBand("LWIR (8–14 µm)");
        eo03.setMobile(true);
        TEMPLATES.put("EO-03", eo03);

        // EO-04: Multi-spectral pan/tilt (combined visible + SWIR + MWIR)
        CuasSensor eo04 = new CuasSensor("EO-04", "Multi-Spectral EO/IR Suite",
                CuasSensor.SensorType.EO_IR);
        eo04.setSubType("MULTI_SPECTRAL");
        eo04.setManufacturer("Generic");
        eo04.setMaxRangeM(6000);
        eo04.setMinRangeM(10);
        eo04.setMaxAltitudeM(3000);
        eo04.setMinAltitudeM(0);
        eo04.setAzimuthCoverageDeg(360);
        eo04.setElevationCoverageDeg(60);
        eo04.setBasePd(0.93);
        eo04.setReferenceRangeM(3000);
        eo04.setMinDetectableRcs(0.001);
        eo04.setFalseAlarmRate(0.012);
        eo04.setDetectionLatencyS(0.6);
        eo04.setPositionAccuracyM(3.0);
        eo04.setUpdateRateHz(30.0);
        eo04.setTrackingNoiseM(2.0);
        eo04.setTrackMaintenanceProb(0.96);
        eo04.setCanIdentify(true);
        eo04.setIdentificationPd(0.92);
        eo04.setCanClassify(true);
        eo04.setClassificationAccuracy(0.93);
        eo04.setRainFactor(0.72);
        eo04.setFogFactor(0.45);
        eo04.setNightFactor(0.92);
        eo04.setSnowFactor(0.68);
        eo04.setPowerConsumptionW(200);
        eo04.setWeightKg(25);
        eo04.setSetupTimeMin(25);
        eo04.setFrequencyBand("VIS + SWIR + MWIR");
        eo04.setMobile(true);
        TEMPLATES.put("EO-04", eo04);

        // ──────────────────── RF DETECTORS ─────────────────────────────

        // RF-01: Wideband SDR scanner (e.g., DroneShield RfOne)
        CuasSensor rf01 = new CuasSensor("RF-01", "Wideband SDR Scanner",
                CuasSensor.SensorType.RF_DETECTOR);
        rf01.setSubType("SDR_WIDEBAND");
        rf01.setManufacturer("Generic");
        rf01.setMaxRangeM(5000);
        rf01.setMinRangeM(10);
        rf01.setMaxAltitudeM(10000);
        rf01.setMinAltitudeM(0);
        rf01.setAzimuthCoverageDeg(360);
        rf01.setElevationCoverageDeg(90);
        rf01.setBasePd(0.85);
        rf01.setReferenceRangeM(2000);
        rf01.setMinDetectableRcs(0.0);
        rf01.setFalseAlarmRate(0.03);
        rf01.setDetectionLatencyS(2.0);
        rf01.setPositionAccuracyM(50.0);
        rf01.setUpdateRateHz(1.0);
        rf01.setTrackingNoiseM(30.0);
        rf01.setTrackMaintenanceProb(0.88);
        rf01.setCanIdentify(true);
        rf01.setIdentificationPd(0.70);
        rf01.setCanClassify(true);
        rf01.setClassificationAccuracy(0.65);
        rf01.setRainFactor(0.98);
        rf01.setFogFactor(0.99);
        rf01.setNightFactor(1.0);
        rf01.setSnowFactor(0.97);
        rf01.setPowerConsumptionW(50);
        rf01.setWeightKg(5);
        rf01.setSetupTimeMin(10);
        rf01.setFrequencyBand("70 MHz – 6 GHz");
        rf01.setMobile(true);
        rf01.setEwSensitivity(1.0);
        TEMPLATES.put("RF-01", rf01);

        // RF-02: Narrowband C2 link detector (e.g., 2.4/5.8 GHz focused)
        CuasSensor rf02 = new CuasSensor("RF-02", "Narrowband C2 Link Detector",
                CuasSensor.SensorType.RF_DETECTOR);
        rf02.setSubType("NARROWBAND_C2");
        rf02.setManufacturer("Generic");
        rf02.setMaxRangeM(3000);
        rf02.setMinRangeM(5);
        rf02.setMaxAltitudeM(5000);
        rf02.setMinAltitudeM(0);
        rf02.setAzimuthCoverageDeg(360);
        rf02.setElevationCoverageDeg(90);
        rf02.setBasePd(0.80);
        rf02.setReferenceRangeM(1500);
        rf02.setMinDetectableRcs(0.0);
        rf02.setFalseAlarmRate(0.04);
        rf02.setDetectionLatencyS(3.0);
        rf02.setPositionAccuracyM(100.0);
        rf02.setUpdateRateHz(0.5);
        rf02.setTrackingNoiseM(50.0);
        rf02.setTrackMaintenanceProb(0.82);
        rf02.setCanIdentify(true);
        rf02.setIdentificationPd(0.75);
        rf02.setCanClassify(true);
        rf02.setClassificationAccuracy(0.70);
        rf02.setRainFactor(0.99);
        rf02.setFogFactor(1.0);
        rf02.setNightFactor(1.0);
        rf02.setSnowFactor(0.98);
        rf02.setPowerConsumptionW(30);
        rf02.setWeightKg(3);
        rf02.setSetupTimeMin(5);
        rf02.setFrequencyBand("2.4 / 5.8 GHz ISM");
        rf02.setMobile(true);
        rf02.setEwSensitivity(1.0);
        TEMPLATES.put("RF-02", rf02);

        // RF-03: Protocol analyser / deep inspection (e.g., Dedrone DroneTracker)
        CuasSensor rf03 = new CuasSensor("RF-03", "RF Protocol Analyser",
                CuasSensor.SensorType.RF_DETECTOR);
        rf03.setSubType("PROTOCOL_ANALYSER");
        rf03.setManufacturer("Generic");
        rf03.setMaxRangeM(2000);
        rf03.setMinRangeM(5);
        rf03.setMaxAltitudeM(3000);
        rf03.setMinAltitudeM(0);
        rf03.setAzimuthCoverageDeg(360);
        rf03.setElevationCoverageDeg(90);
        rf03.setBasePd(0.78);
        rf03.setReferenceRangeM(1000);
        rf03.setMinDetectableRcs(0.0);
        rf03.setFalseAlarmRate(0.02);
        rf03.setDetectionLatencyS(4.0);
        rf03.setPositionAccuracyM(80.0);
        rf03.setUpdateRateHz(0.25);
        rf03.setTrackingNoiseM(60.0);
        rf03.setTrackMaintenanceProb(0.80);
        rf03.setCanIdentify(true);
        rf03.setIdentificationPd(0.90);
        rf03.setCanClassify(true);
        rf03.setClassificationAccuracy(0.88);
        rf03.setRainFactor(0.99);
        rf03.setFogFactor(1.0);
        rf03.setNightFactor(1.0);
        rf03.setSnowFactor(0.99);
        rf03.setPowerConsumptionW(40);
        rf03.setWeightKg(4);
        rf03.setSetupTimeMin(10);
        rf03.setFrequencyBand("400 MHz – 6 GHz");
        rf03.setMobile(true);
        rf03.setEwSensitivity(1.0);
        TEMPLATES.put("RF-03", rf03);

        // ──────────────────── ACOUSTIC SENSORS ─────────────────────────

        // AC-01: Microphone array (e.g., Squarehead CUAS acoustic)
        CuasSensor ac01 = new CuasSensor("AC-01", "Microphone Array — 16-element",
                CuasSensor.SensorType.ACOUSTIC);
        ac01.setSubType("MIC_ARRAY");
        ac01.setManufacturer("Generic");
        ac01.setMaxRangeM(500);
        ac01.setMinRangeM(5);
        ac01.setMaxAltitudeM(300);
        ac01.setMinAltitudeM(0);
        ac01.setAzimuthCoverageDeg(360);
        ac01.setElevationCoverageDeg(90);
        ac01.setBasePd(0.75);
        ac01.setReferenceRangeM(200);
        ac01.setMinDetectableRcs(0.0);
        ac01.setFalseAlarmRate(0.05);
        ac01.setDetectionLatencyS(3.0);
        ac01.setPositionAccuracyM(15.0);
        ac01.setUpdateRateHz(2.0);
        ac01.setTrackingNoiseM(10.0);
        ac01.setTrackMaintenanceProb(0.85);
        ac01.setCanIdentify(false);
        ac01.setCanClassify(true);
        ac01.setClassificationAccuracy(0.60);
        ac01.setRainFactor(0.50);
        ac01.setFogFactor(0.90);
        ac01.setNightFactor(1.0);
        ac01.setSnowFactor(0.60);
        ac01.setMaxWindSpeedMs(8.0);
        ac01.setPowerConsumptionW(20);
        ac01.setWeightKg(8);
        ac01.setSetupTimeMin(15);
        ac01.setFrequencyBand("Audio (100 Hz – 20 kHz)");
        ac01.setMobile(true);
        TEMPLATES.put("AC-01", ac01);

        // AC-02: Directional acoustic sensor (long-range)
        CuasSensor ac02 = new CuasSensor("AC-02", "Directional Acoustic Sensor",
                CuasSensor.SensorType.ACOUSTIC);
        ac02.setSubType("DIRECTIONAL");
        ac02.setManufacturer("Generic");
        ac02.setMaxRangeM(1000);
        ac02.setMinRangeM(10);
        ac02.setMaxAltitudeM(500);
        ac02.setMinAltitudeM(0);
        ac02.setAzimuthCoverageDeg(60);
        ac02.setElevationCoverageDeg(45);
        ac02.setBasePd(0.80);
        ac02.setReferenceRangeM(400);
        ac02.setMinDetectableRcs(0.0);
        ac02.setFalseAlarmRate(0.04);
        ac02.setDetectionLatencyS(2.5);
        ac02.setPositionAccuracyM(10.0);
        ac02.setUpdateRateHz(2.0);
        ac02.setTrackingNoiseM(8.0);
        ac02.setTrackMaintenanceProb(0.87);
        ac02.setCanIdentify(false);
        ac02.setCanClassify(true);
        ac02.setClassificationAccuracy(0.65);
        ac02.setRainFactor(0.45);
        ac02.setFogFactor(0.88);
        ac02.setNightFactor(1.0);
        ac02.setSnowFactor(0.55);
        ac02.setMaxWindSpeedMs(10.0);
        ac02.setPowerConsumptionW(15);
        ac02.setWeightKg(5);
        ac02.setSetupTimeMin(10);
        ac02.setFrequencyBand("Audio (50 Hz – 25 kHz)");
        ac02.setMobile(true);
        TEMPLATES.put("AC-02", ac02);

        // ──────────────────── LIDAR ────────────────────────────────────

        // LI-01: Scanning Lidar for drone detection
        CuasSensor li01 = new CuasSensor("LI-01", "Scanning Lidar — Drone Detection",
                CuasSensor.SensorType.LIDAR);
        li01.setSubType("SCANNING");
        li01.setManufacturer("Generic");
        li01.setMaxRangeM(2000);
        li01.setMinRangeM(5);
        li01.setMaxAltitudeM(1000);
        li01.setMinAltitudeM(0);
        li01.setAzimuthCoverageDeg(360);
        li01.setElevationCoverageDeg(30);
        li01.setBasePd(0.88);
        li01.setReferenceRangeM(800);
        li01.setMinDetectableRcs(0.001);
        li01.setFalseAlarmRate(0.01);
        li01.setDetectionLatencyS(0.5);
        li01.setPositionAccuracyM(0.5);
        li01.setUpdateRateHz(10.0);
        li01.setTrackingNoiseM(0.3);
        li01.setTrackMaintenanceProb(0.90);
        li01.setCanIdentify(true);
        li01.setIdentificationPd(0.82);
        li01.setCanClassify(true);
        li01.setClassificationAccuracy(0.88);
        li01.setRainFactor(0.40);
        li01.setFogFactor(0.15);
        li01.setNightFactor(1.0);
        li01.setSnowFactor(0.35);
        li01.setPowerConsumptionW(120);
        li01.setWeightKg(15);
        li01.setSetupTimeMin(20);
        li01.setFrequencyBand("IR Laser (1550 nm)");
        li01.setMobile(true);
        TEMPLATES.put("LI-01", li01);

        // ──────────────────── MULTI-SENSOR ─────────────────────────────

        // MS-01: Integrated radar + EO/IR turret (e.g., Hensoldt Xpeller-like)
        CuasSensor ms01 = new CuasSensor("MS-01", "Integrated Radar + EO/IR Turret",
                CuasSensor.SensorType.MULTI_SENSOR);
        ms01.setSubType("RADAR_EOIR");
        ms01.setManufacturer("Generic");
        ms01.setMaxRangeM(7000);
        ms01.setMinRangeM(20);
        ms01.setMaxAltitudeM(3000);
        ms01.setMinAltitudeM(0);
        ms01.setAzimuthCoverageDeg(360);
        ms01.setElevationCoverageDeg(60);
        ms01.setBasePd(0.96);
        ms01.setReferenceRangeM(3500);
        ms01.setMinDetectableRcs(0.005);
        ms01.setFalseAlarmRate(0.005);
        ms01.setDetectionLatencyS(0.3);
        ms01.setPositionAccuracyM(2.0);
        ms01.setUpdateRateHz(10.0);
        ms01.setTrackingNoiseM(1.5);
        ms01.setTrackMaintenanceProb(0.98);
        ms01.setCanIdentify(true);
        ms01.setIdentificationPd(0.90);
        ms01.setCanClassify(true);
        ms01.setClassificationAccuracy(0.92);
        ms01.setRainFactor(0.82);
        ms01.setFogFactor(0.70);
        ms01.setNightFactor(0.95);
        ms01.setSnowFactor(0.75);
        ms01.setPowerConsumptionW(600);
        ms01.setWeightKg(80);
        ms01.setSetupTimeMin(60);
        ms01.setFrequencyBand("X-band + VIS + MWIR");
        ms01.setMobile(false);
        ms01.setEwSensitivity(0.5);
        TEMPLATES.put("MS-01", ms01);

        // MS-02: Full spectrum CUAS suite (radar + EO/IR + RF + acoustic)
        CuasSensor ms02 = new CuasSensor("MS-02", "Full Spectrum CUAS Suite",
                CuasSensor.SensorType.MULTI_SENSOR);
        ms02.setSubType("FULL_SPECTRUM");
        ms02.setManufacturer("Generic");
        ms02.setMaxRangeM(10000);
        ms02.setMinRangeM(5);
        ms02.setMaxAltitudeM(5000);
        ms02.setMinAltitudeM(0);
        ms02.setAzimuthCoverageDeg(360);
        ms02.setElevationCoverageDeg(90);
        ms02.setBasePd(0.98);
        ms02.setReferenceRangeM(5000);
        ms02.setMinDetectableRcs(0.003);
        ms02.setFalseAlarmRate(0.003);
        ms02.setDetectionLatencyS(0.2);
        ms02.setPositionAccuracyM(1.5);
        ms02.setUpdateRateHz(10.0);
        ms02.setTrackingNoiseM(1.0);
        ms02.setTrackMaintenanceProb(0.99);
        ms02.setCanIdentify(true);
        ms02.setIdentificationPd(0.95);
        ms02.setCanClassify(true);
        ms02.setClassificationAccuracy(0.95);
        ms02.setRainFactor(0.85);
        ms02.setFogFactor(0.75);
        ms02.setNightFactor(0.96);
        ms02.setSnowFactor(0.78);
        ms02.setPowerConsumptionW(2000);
        ms02.setWeightKg(300);
        ms02.setSetupTimeMin(180);
        ms02.setFrequencyBand("Multi-band");
        ms02.setMobile(false);
        ms02.setEwSensitivity(0.4);
        TEMPLATES.put("MS-02", ms02);
    }

    // ── API ─────────────────────────────────────────────────────────────

    /**
     * Add or replace a sensor template in the library (SN-02).
     */
    public static void addTemplate(CuasSensor sensor) {
        TEMPLATES.put(sensor.getSensorId(), sensor.copy());
    }

    /**
     * Update an existing sensor template (SN-02).
     */
    public static void updateTemplate(CuasSensor sensor) {
        TEMPLATES.put(sensor.getSensorId(), sensor.copy());
    }

    /**
     * Remove a sensor template by ID (SN-02). Returns true if removed.
     */
    public static boolean removeTemplate(String sensorId) {
        return TEMPLATES.remove(sensorId) != null;
    }

    /**
     * Get the number of templates.
     */
    public static int getTemplateCount() {
        return TEMPLATES.size();
    }

    /**
     * Get all available sensor template IDs.
     */
    public static List<String> getTemplateIds() {
        return new ArrayList<>(TEMPLATES.keySet());
    }

    /**
     * Get all available sensor templates.
     */
    public static List<CuasSensor> getAllTemplates() {
        List<CuasSensor> list = new ArrayList<>();
        for (CuasSensor s : TEMPLATES.values()) {
            list.add(s.copy());
        }
        return list;
    }

    /**
     * Get a sensor template by ID (returns a deep copy).
     * @throws IllegalArgumentException if ID not found
     */
    public static CuasSensor getTemplate(String sensorId) {
        CuasSensor s = TEMPLATES.get(sensorId);
        if (s == null) throw new IllegalArgumentException("Unknown sensor template: " + sensorId);
        return s.copy();
    }

    /**
     * Get all templates of a given sensor type.
     */
    public static List<CuasSensor> getTemplatesByType(CuasSensor.SensorType type) {
        List<CuasSensor> list = new ArrayList<>();
        for (CuasSensor s : TEMPLATES.values()) {
            if (s.getSensorType() == type) list.add(s.copy());
        }
        return list;
    }

    /**
     * Get a formatted summary of all templates.
     */
    public static String getSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("CUAS Sensor Library — ").append(TEMPLATES.size()).append(" templates\n");
        sb.append("═══════════════════════════════════════════════════\n\n");

        CuasSensor.SensorType currentType = null;
        for (CuasSensor s : TEMPLATES.values()) {
            if (s.getSensorType() != currentType) {
                currentType = s.getSensorType();
                sb.append("── ").append(currentType.getDisplayName()).append(" ──\n");
            }
            sb.append(String.format(Locale.ENGLISH,
                    "  %s: %s (%.0f–%.0fm, Pd=%.2f, FAR=%.4f, az=%s°)\n",
                    s.getSensorId(), s.getName(),
                    s.getMinRangeM(), s.getMaxRangeM(),
                    s.getBasePd(), s.getFalseAlarmRate(),
                    s.getAzimuthCoverageDeg() >= 360 ? "360" :
                            String.format(Locale.ENGLISH, "%.0f", s.getAzimuthCoverageDeg())));
        }
        return sb.toString();
    }
}
