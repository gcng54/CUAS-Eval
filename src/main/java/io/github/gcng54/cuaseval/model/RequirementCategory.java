package io.github.gcng54.cuaseval.model;

/**
 * DTI domain categories per CWA 18150.
 * Maps each requirement/parameter to its functional domain.
 */
public enum RequirementCategory {
    /** Detection — sensing and alerting to UAS presence */
    DETECTION,
    /** Tracking — continuous positional monitoring of UAS */
    TRACKING,
    /** Identification — classifying and recognizing UAS */
    IDENTIFICATION,
    /** HMI — Human-Machine Interface requirements */
    HMI,
    /** Architecture — system-level structural requirements */
    ARCHITECTURE,
    /** Logistics — deployment and operational support */
    LOGISTICS,
    /** Automation — automated response and recording */
    AUTOMATION,
    /** Operational — operational and legal compliance */
    OPERATIONAL,
    /** SiteSpecific — site-specific installation and area coverage */
    SITE_SPECIFIC
}
