package io.github.gcng54.cuaseval.model;

/**
 * UAS class codes per EU Regulation 2019/945 (as referenced in CWA 18150).
 */
public enum UasClass {
    /** Open Category C0 — less than 250 g */
    C0,
    /** Open Category C1 — less than 900 g */
    C1,
    /** Open Category C2 — less than 4 kg */
    C2,
    /** Open Category C3 — less than 25 kg */
    C3,
    /** Open Category C4 — less than 25 kg (legacy) */
    C4,
    /** Specific/Certified — heavier / special-purpose UAS */
    SPECIFIC,
    /** Unknown class */
    UNKNOWN
}
