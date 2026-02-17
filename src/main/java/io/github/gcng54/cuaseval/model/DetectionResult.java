package io.github.gcng54.cuaseval.model;

import java.time.Instant;
import java.util.Locale;

/**
 * Result of a single detection event from the CUAS system under test.
 * <p>
 * Requirement links: FR01 (detect), TP_D01 (timestamp), TP_D02 (latency),
 * TP_D03 (coordinates), TP_D04 (bird classification).
 * </p>
 */
public class DetectionResult {

    /** Identifier of the target that was (or should have been) detected */
    private String targetUid;

    /** Whether detection was successful */
    private boolean detected;

    /** System-reported detection timestamp */
    private Instant detectionTime;

    /** Ground truth appearance time */
    private Instant groundTruthTime;

    /** Detection latency in seconds (TP_D01: detectionTime − groundTruthTime) */
    private double latencySeconds;

    /** Display latency in seconds (TP_D02) */
    private double displayLatencySeconds;

    /** System-reported position of the detected target */
    private GeoPosition reportedPosition;

    /** Ground truth position at detection time */
    private GeoPosition truthPosition;

    /** Position error in metres */
    private double positionErrorMetres;

    /** Whether the system classified this correctly (not a bird — TP_D04) */
    private boolean correctClassification;

    /** Signal-to-noise ratio at detection (if available) */
    private double snrDb;

    /** The sensor/technology that produced this detection */
    private String sensorType;

    // ── Constructors ────────────────────────────────────────────────────

    public DetectionResult() {}

    public DetectionResult(String targetUid, boolean detected,
                           Instant detectionTime, Instant groundTruthTime) {
        this.targetUid = targetUid;
        this.detected = detected;
        this.detectionTime = detectionTime;
        this.groundTruthTime = groundTruthTime;
        if (detectionTime != null && groundTruthTime != null) {
            this.latencySeconds = (detectionTime.toEpochMilli()
                                 - groundTruthTime.toEpochMilli()) / 1000.0;
        }
    }

    // ── Accessors ───────────────────────────────────────────────────────

    public String getTargetUid()                { return targetUid; }
    public boolean isDetected()                 { return detected; }
    public Instant getDetectionTime()           { return detectionTime; }
    public Instant getGroundTruthTime()         { return groundTruthTime; }
    public double getLatencySeconds()           { return latencySeconds; }
    public double getDisplayLatencySeconds()    { return displayLatencySeconds; }
    public GeoPosition getReportedPosition()    { return reportedPosition; }
    public GeoPosition getTruthPosition()       { return truthPosition; }
    public double getPositionErrorMetres()      { return positionErrorMetres; }
    public boolean isCorrectClassification()    { return correctClassification; }
    public double getSnrDb()                    { return snrDb; }
    public String getSensorType()               { return sensorType; }

    public void setTargetUid(String targetUid)                       { this.targetUid = targetUid; }
    public void setDetected(boolean detected)                        { this.detected = detected; }
    public void setDetectionTime(Instant detectionTime)              { this.detectionTime = detectionTime; }
    public void setGroundTruthTime(Instant groundTruthTime)          { this.groundTruthTime = groundTruthTime; }
    public void setLatencySeconds(double latencySeconds)             { this.latencySeconds = latencySeconds; }
    public void setDisplayLatencySeconds(double displayLatencySeconds){ this.displayLatencySeconds = displayLatencySeconds; }
    public void setReportedPosition(GeoPosition reportedPosition)    { this.reportedPosition = reportedPosition; }
    public void setTruthPosition(GeoPosition truthPosition)          { this.truthPosition = truthPosition; }
    public void setPositionErrorMetres(double positionErrorMetres)    { this.positionErrorMetres = positionErrorMetres; }
    public void setCorrectClassification(boolean correctClassification){ this.correctClassification = correctClassification; }
    public void setSnrDb(double snrDb)                                { this.snrDb = snrDb; }
    public void setSensorType(String sensorType)                      { this.sensorType = sensorType; }

    /** Compute position error from reported and truth positions. */
    public void computePositionError() {
        if (reportedPosition != null && truthPosition != null) {
            this.positionErrorMetres = reportedPosition.distanceTo(truthPosition);
        }
    }

    @Override
    public String toString() {
        return String.format(Locale.ENGLISH, "Detection[%s] detected=%b latency=%.2fs error=%.1fm",
                targetUid, detected, latencySeconds, positionErrorMetres);
    }
}
