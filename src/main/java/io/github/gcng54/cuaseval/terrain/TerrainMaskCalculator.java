package io.github.gcng54.cuaseval.terrain;

import io.github.gcng54.cuaseval.model.GeoPosition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Calculates terrain masking and detection angles from sensor positions
 * using DTED elevation data.
 * <p>
 * Performs line-of-sight analysis by sampling terrain elevation along
 * radial profiles from a sensor location to determine which azimuth/elevation
 * combinations are masked by terrain.
 * </p>
 */
public class TerrainMaskCalculator {

    private static final Logger log = LoggerFactory.getLogger(TerrainMaskCalculator.class);
    private static final double EARTH_RADIUS = 6_371_000.0; // metres

    private final DtedReader dtedReader;

    public TerrainMaskCalculator(DtedReader dtedReader) {
        this.dtedReader = dtedReader;
    }

    // ── Terrain Mask Profile ────────────────────────────────────────────

    /**
     * A radial terrain profile from a sensor position.
     */
    public static class TerrainProfile {
        private final double azimuthDeg;
        private final double maskAngleDeg;
        private final double[] distances; // metres
        private final double[] elevations; // metres MSL
        private final double[] losAngles; // degrees above horizon

        public TerrainProfile(double azimuthDeg, double maskAngleDeg,
                              double[] distances, double[] elevations, double[] losAngles) {
            this.azimuthDeg = azimuthDeg;
            this.maskAngleDeg = maskAngleDeg;
            this.distances = distances;
            this.elevations = elevations;
            this.losAngles = losAngles;
        }

        public double getAzimuthDeg()   { return azimuthDeg; }
        public double getMaskAngleDeg()  { return maskAngleDeg; }
        public double[] getDistances()   { return distances; }
        public double[] getElevations()  { return elevations; }
        public double[] getLosAngles()   { return losAngles; }
    }

    /**
     * Result of a full terrain mask analysis for a sensor position.
     */
    public static class TerrainMask {
        private final GeoPosition sensorPosition;
        private final double sensorAltitude;
        private final double maxRangeM;
        private final List<TerrainProfile> profiles;
        private final double[] azimuths;
        private final double[] maskAngles;

        public TerrainMask(GeoPosition sensorPosition, double sensorAltitude,
                           double maxRangeM, List<TerrainProfile> profiles) {
            this.sensorPosition = sensorPosition;
            this.sensorAltitude = sensorAltitude;
            this.maxRangeM = maxRangeM;
            this.profiles = profiles;
            this.azimuths = new double[profiles.size()];
            this.maskAngles = new double[profiles.size()];
            for (int i = 0; i < profiles.size(); i++) {
                azimuths[i] = profiles.get(i).azimuthDeg;
                maskAngles[i] = profiles.get(i).maskAngleDeg;
            }
        }

        public GeoPosition getSensorPosition()  { return sensorPosition; }
        public double getSensorAltitude()        { return sensorAltitude; }
        public double getMaxRangeM()             { return maxRangeM; }
        public List<TerrainProfile> getProfiles() { return profiles; }
        public double[] getAzimuths()            { return azimuths; }
        public double[] getMaskAngles()          { return maskAngles; }

        /**
         * Get the mask angle at a specific azimuth (interpolated).
         */
        public double getMaskAngleAt(double azimuthDeg) {
            double az = ((azimuthDeg % 360) + 360) % 360;
            for (int i = 0; i < azimuths.length - 1; i++) {
                if (az >= azimuths[i] && az < azimuths[i + 1]) {
                    double frac = (az - azimuths[i]) / (azimuths[i + 1] - azimuths[i]);
                    return maskAngles[i] + frac * (maskAngles[i + 1] - maskAngles[i]);
                }
            }
            return maskAngles[maskAngles.length - 1];
        }

        /**
         * Check if a target at given position is visible (not masked) from the sensor.
         */
        public boolean isVisible(GeoPosition target) {
            double az = sensorPosition.bearingTo(target);
            double dist = sensorPosition.distanceTo(target);
            double dAlt = target.getAltitudeMsl() - sensorAltitude;
            double elevAngle = Math.toDegrees(Math.atan2(dAlt, dist));
            double maskAngle = getMaskAngleAt(az);
            return elevAngle > maskAngle;
        }
    }

    // ── Compute Terrain Mask ────────────────────────────────────────────

    /**
     * Compute terrain masking for a sensor position.
     *
     * @param sensorPos     sensor geographic position (lat/lon)
     * @param sensorAltMsl  sensor antenna altitude in metres MSL
     * @param maxRangeM     maximum detection range in metres
     * @param numAzimuths   number of azimuth profiles (e.g. 360 for 1° resolution)
     * @param numSamples    samples per radial profile
     * @return terrain mask result with mask angles per azimuth
     */
    public TerrainMask computeTerrainMask(GeoPosition sensorPos, double sensorAltMsl,
                                           double maxRangeM, int numAzimuths, int numSamples) {
        if (!dtedReader.isLoaded()) {
            log.warn("DTED data not loaded — terrain masking unavailable");
            return null;
        }

        List<TerrainProfile> profiles = new ArrayList<>();

        for (int i = 0; i < numAzimuths; i++) {
            double azimuth = 360.0 * i / numAzimuths;
            TerrainProfile profile = computeRadialProfile(
                    sensorPos, sensorAltMsl, azimuth, maxRangeM, numSamples);
            profiles.add(profile);
        }

        TerrainMask mask = new TerrainMask(sensorPos, sensorAltMsl, maxRangeM, profiles);
        log.debug("Terrain mask computed: {} profiles, max mask angle = {:.1f}°",
                profiles.size(), maxMaskAngle(profiles));
        return mask;
    }

    /**
     * Compute a single radial terrain profile from the sensor.
     */
    private TerrainProfile computeRadialProfile(GeoPosition sensorPos, double sensorAltMsl,
                                                 double azimuthDeg, double maxRangeM,
                                                 int numSamples) {
        double[] distances = new double[numSamples];
        double[] elevations = new double[numSamples];
        double[] losAngles = new double[numSamples];
        double maxMaskAngle = -90.0;

        double stepM = maxRangeM / numSamples;
        double azRad = Math.toRadians(azimuthDeg);

        for (int s = 1; s <= numSamples; s++) {
            double distM = s * stepM;
            distances[s - 1] = distM;

            // Compute target point position
            double dLat = distM * Math.cos(azRad) / 111320.0;
            double dLon = distM * Math.sin(azRad) / (111320.0 * Math.cos(Math.toRadians(sensorPos.getLatitude())));

            double targetLat = sensorPos.getLatitude() + dLat;
            double targetLon = sensorPos.getLongitude() + dLon;

            double terrainElev = dtedReader.getElevation(targetLat, targetLon);
            if (terrainElev == DtedReader.NO_DATA) {
                terrainElev = 0; // assume sea level
            }
            elevations[s - 1] = terrainElev;

            // Line of sight angle (elevation angle from sensor to terrain point)
            // Account for Earth curvature
            double earthCurve = (distM * distM) / (2.0 * EARTH_RADIUS);
            double effectiveElev = terrainElev - earthCurve;
            double dHeight = effectiveElev - sensorAltMsl;
            double angle = Math.toDegrees(Math.atan2(dHeight, distM));

            losAngles[s - 1] = angle;
            if (angle > maxMaskAngle) {
                maxMaskAngle = angle;
            }
        }

        return new TerrainProfile(azimuthDeg, maxMaskAngle, distances, elevations, losAngles);
    }

    private double maxMaskAngle(List<TerrainProfile> profiles) {
        double max = -90;
        for (TerrainProfile p : profiles) {
            if (p.maskAngleDeg > max) max = p.maskAngleDeg;
        }
        return max;
    }
}
