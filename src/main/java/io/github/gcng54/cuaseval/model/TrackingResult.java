package io.github.gcng54.cuaseval.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Result of tracking a single target over time.
 * <p>
 * Requirement links: FR04 (track), FR17 (UID consistency), FR18 (track after loss),
 * TP_T01 (fast tracking), TP_T04 (speed vector), TP_T05/T06 (path).
 * </p>
 */
public class TrackingResult {

    /** Target UID being tracked */
    private String targetUid;

    /** System-assigned track ID (should map 1:1 to target UID — FR17) */
    private String systemTrackId;

    /** Whether tracking was maintained throughout the test */
    private boolean trackMaintained;

    /** Whether UID was preserved after track loss/reacquisition (FR18) */
    private boolean uidPreserved;

    /** Track duration in seconds */
    private double trackDurationSeconds;

    /** Number of track drops during the scenario */
    private int trackDropCount;

    /** Sequence of reported track positions (for path analysis) */
    private List<TrackPoint> trackPoints = new ArrayList<>();

    /** Mean position error in metres (computed over all track points) */
    private double meanPositionErrorMetres;

    /** Max position error in metres */
    private double maxPositionErrorMetres;

    /** Track update rate in Hz (TP_D16) */
    private double updateRateHz;

    // ── Inner class ─────────────────────────────────────────────────────

    /**
     * A single point in the track history.
     */
    public static class TrackPoint {
        private Instant timestamp;
        private GeoPosition reportedPosition;
        private GeoPosition truthPosition;
        private double speedMs;
        private double headingDeg;

        public TrackPoint() {}

        public TrackPoint(Instant timestamp, GeoPosition reported,
                          GeoPosition truth, double speed, double heading) {
            this.timestamp = timestamp;
            this.reportedPosition = reported;
            this.truthPosition = truth;
            this.speedMs = speed;
            this.headingDeg = heading;
        }

        public Instant getTimestamp()           { return timestamp; }
        public GeoPosition getReportedPosition(){ return reportedPosition; }
        public GeoPosition getTruthPosition()   { return truthPosition; }
        public double getSpeedMs()              { return speedMs; }
        public double getHeadingDeg()           { return headingDeg; }

        public void setTimestamp(Instant t)                     { this.timestamp = t; }
        public void setReportedPosition(GeoPosition p)          { this.reportedPosition = p; }
        public void setTruthPosition(GeoPosition p)             { this.truthPosition = p; }
        public void setSpeedMs(double s)                        { this.speedMs = s; }
        public void setHeadingDeg(double h)                     { this.headingDeg = h; }

        /** Position error between reported and truth. */
        public double positionError() {
            if (reportedPosition != null && truthPosition != null) {
                return reportedPosition.distanceTo(truthPosition);
            }
            return Double.NaN;
        }
    }

    // ── Constructors ────────────────────────────────────────────────────

    public TrackingResult() {}

    public TrackingResult(String targetUid, String systemTrackId) {
        this.targetUid = targetUid;
        this.systemTrackId = systemTrackId;
    }

    // ── Accessors ───────────────────────────────────────────────────────

    public String getTargetUid()                { return targetUid; }
    public String getSystemTrackId()            { return systemTrackId; }
    public boolean isTrackMaintained()          { return trackMaintained; }
    public boolean isUidPreserved()             { return uidPreserved; }
    public double getTrackDurationSeconds()     { return trackDurationSeconds; }
    public int getTrackDropCount()              { return trackDropCount; }
    public List<TrackPoint> getTrackPoints()    { return trackPoints; }
    public double getMeanPositionErrorMetres()  { return meanPositionErrorMetres; }
    public double getMaxPositionErrorMetres()   { return maxPositionErrorMetres; }
    public double getUpdateRateHz()             { return updateRateHz; }

    public void setTargetUid(String targetUid)                          { this.targetUid = targetUid; }
    public void setSystemTrackId(String systemTrackId)                  { this.systemTrackId = systemTrackId; }
    public void setTrackMaintained(boolean trackMaintained)             { this.trackMaintained = trackMaintained; }
    public void setUidPreserved(boolean uidPreserved)                   { this.uidPreserved = uidPreserved; }
    public void setTrackDurationSeconds(double trackDurationSeconds)    { this.trackDurationSeconds = trackDurationSeconds; }
    public void setTrackDropCount(int trackDropCount)                   { this.trackDropCount = trackDropCount; }
    public void setTrackPoints(List<TrackPoint> trackPoints)            { this.trackPoints = trackPoints; }
    public void setMeanPositionErrorMetres(double meanPositionErrorMetres){ this.meanPositionErrorMetres = meanPositionErrorMetres; }
    public void setMaxPositionErrorMetres(double maxPositionErrorMetres)  { this.maxPositionErrorMetres = maxPositionErrorMetres; }
    public void setUpdateRateHz(double updateRateHz)                      { this.updateRateHz = updateRateHz; }

    /** Add a single track point and recompute error statistics. */
    public void addTrackPoint(TrackPoint point) {
        trackPoints.add(point);
        recomputeErrors();
    }

    /** Recompute mean and max position errors from track points. */
    public void recomputeErrors() {
        double sumErr = 0;
        double maxErr = 0;
        int count = 0;
        for (TrackPoint tp : trackPoints) {
            double err = tp.positionError();
            if (!Double.isNaN(err)) {
                sumErr += err;
                if (err > maxErr) maxErr = err;
                count++;
            }
        }
        this.meanPositionErrorMetres = count > 0 ? sumErr / count : 0;
        this.maxPositionErrorMetres = maxErr;
    }

    @Override
    public String toString() {
        return String.format(Locale.ENGLISH, "Track[%s→%s] maintained=%b drops=%d meanErr=%.1fm points=%d",
                targetUid, systemTrackId, trackMaintained,
                trackDropCount, meanPositionErrorMetres, trackPoints.size());
    }
}
