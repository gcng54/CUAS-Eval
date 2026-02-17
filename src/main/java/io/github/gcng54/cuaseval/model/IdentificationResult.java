package io.github.gcng54.cuaseval.model;

import java.util.Locale;

/** attempt by the CUAS system.
 * <p>
 * Requirement links: FR06 (identify), FR07 (multi-identify), FR14 (IFF),
 * FR16 (bird immunity), FR20/FR21 (payload identification),
 * TP_I01 (size), TP_I08 (classification).
 * </p>
 */
public class IdentificationResult {

    /** Target UID being identified */
    private String targetUid;

    /** Whether identification was successful */
    private boolean identified;

    /** System-reported UAS classification (TP_I08) */
    private String reportedClassification;

    /** Ground truth UAS type */
    private String truthClassification;

    /** Whether classification matches ground truth */
    private boolean classificationCorrect;

    /** IFF result: true = friendly, false = hostile, null = unknown (FR14) */
    private Boolean iffResult;

    /** Ground truth IFF */
    private boolean truthFriendly;

    /** Whether payload was correctly detected (FR20, FR21) */
    private boolean payloadIdentified;

    /** Whether bird was correctly rejected (FR16) */
    private boolean birdRejected;

    /** System-estimated UAS size in cm² (TP_I01) */
    private double estimatedSizeCm2;

    /** Identification confidence (0.0 – 1.0) */
    private double confidence;

    /** Identification latency in seconds */
    private double latencySeconds;

    // ── Constructors ────────────────────────────────────────────────────

    public IdentificationResult() {}

    public IdentificationResult(String targetUid, boolean identified) {
        this.targetUid = targetUid;
        this.identified = identified;
    }

    // ── Accessors ───────────────────────────────────────────────────────

    public String getTargetUid()              { return targetUid; }
    public boolean isIdentified()             { return identified; }
    public String getReportedClassification() { return reportedClassification; }
    public String getTruthClassification()    { return truthClassification; }
    public boolean isClassificationCorrect()  { return classificationCorrect; }
    public Boolean getIffResult()             { return iffResult; }
    public boolean isTruthFriendly()          { return truthFriendly; }
    public boolean isPayloadIdentified()      { return payloadIdentified; }
    public boolean isBirdRejected()           { return birdRejected; }
    public double getEstimatedSizeCm2()       { return estimatedSizeCm2; }
    public double getConfidence()             { return confidence; }
    public double getLatencySeconds()         { return latencySeconds; }

    public void setTargetUid(String targetUid)                         { this.targetUid = targetUid; }
    public void setIdentified(boolean identified)                       { this.identified = identified; }
    public void setReportedClassification(String reportedClassification){ this.reportedClassification = reportedClassification; }
    public void setTruthClassification(String truthClassification)      { this.truthClassification = truthClassification; }
    public void setClassificationCorrect(boolean classificationCorrect) { this.classificationCorrect = classificationCorrect; }
    public void setIffResult(Boolean iffResult)                         { this.iffResult = iffResult; }
    public void setTruthFriendly(boolean truthFriendly)                 { this.truthFriendly = truthFriendly; }
    public void setPayloadIdentified(boolean payloadIdentified)         { this.payloadIdentified = payloadIdentified; }
    public void setBirdRejected(boolean birdRejected)                   { this.birdRejected = birdRejected; }
    public void setEstimatedSizeCm2(double estimatedSizeCm2)            { this.estimatedSizeCm2 = estimatedSizeCm2; }
    public void setConfidence(double confidence)                        { this.confidence = confidence; }
    public void setLatencySeconds(double latencySeconds)                { this.latencySeconds = latencySeconds; }

    @Override
    public String toString() {
        return String.format(Locale.ENGLISH, "ID[%s] identified=%b class=%s correct=%b conf=%.2f",
                targetUid, identified, reportedClassification,
                classificationCorrect, confidence);
    }
}
