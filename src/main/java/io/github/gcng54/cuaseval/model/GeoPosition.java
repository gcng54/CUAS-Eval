package io.github.gcng54.cuaseval.model;

import java.util.Locale;

/**
 * Geographic position (WGS-84) with latitude, longitude, and altitude.
 * Used throughout the DTI pipeline for target, sensor, and track positions.
 */
public class GeoPosition {

    /** Latitude in decimal degrees (WGS-84). North positive. */
    private double latitude;

    /** Longitude in decimal degrees (WGS-84). East positive. */
    private double longitude;

    /** Altitude in metres above mean sea level (MSL). */
    private double altitudeMsl;

    // ── Constructors ────────────────────────────────────────────────────

    public GeoPosition() {}

    public GeoPosition(double latitude, double longitude, double altitudeMsl) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.altitudeMsl = altitudeMsl;
    }

    /** 2-D convenience constructor (altitude = 0). */
    public GeoPosition(double latitude, double longitude) {
        this(latitude, longitude, 0.0);
    }

    // ── Accessors ───────────────────────────────────────────────────────

    public double getLatitude()    { return latitude; }
    public double getLongitude()   { return longitude; }
    public double getAltitudeMsl() { return altitudeMsl; }

    public void setLatitude(double latitude)        { this.latitude = latitude; }
    public void setLongitude(double longitude)      { this.longitude = longitude; }
    public void setAltitudeMsl(double altitudeMsl)  { this.altitudeMsl = altitudeMsl; }

    // ── Calculations ────────────────────────────────────────────────────

    /**
     * Haversine distance in metres to another position.
     */
    public double distanceTo(GeoPosition other) {
        double R = 6_371_000.0; // Earth radius in metres
        double dLat = Math.toRadians(other.latitude - this.latitude);
        double dLon = Math.toRadians(other.longitude - this.longitude);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                 + Math.cos(Math.toRadians(this.latitude))
                 * Math.cos(Math.toRadians(other.latitude))
                 * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    /**
     * Bearing in degrees (0–360) from this position to another.
     */
    public double bearingTo(GeoPosition other) {
        double dLon = Math.toRadians(other.longitude - this.longitude);
        double lat1 = Math.toRadians(this.latitude);
        double lat2 = Math.toRadians(other.latitude);
        double y = Math.sin(dLon) * Math.cos(lat2);
        double x = Math.cos(lat1) * Math.sin(lat2)
                 - Math.sin(lat1) * Math.cos(lat2) * Math.cos(dLon);
        double brng = Math.toDegrees(Math.atan2(y, x));
        return (brng + 360) % 360;
    }

    @Override
    public String toString() {
        return String.format(Locale.ENGLISH, "(%.6f, %.6f, %.1f m)", latitude, longitude, altitudeMsl);
    }
}
