package io.github.gcng54.cuaseval.model;

/**
 * CWA 18150 test scenario types — the 10 COURAGEOUS application scenarios.
 * Each scenario represents a distinct operational environment and threat profile.
 */
public enum ScenarioType {

    /** S1: Prison — contraband delivery defence */
    S1_PRISON("S1", "Prison",
            "Counter-UAS protection of prison facility against contraband delivery drones",
            38.4500, 27.2100),

    /** S2: Airport — airspace and glide-path protection */
    S2_AIRPORT("S2", "Airport",
            "Airport airspace protection and glide path defence",
            38.2921, 27.1570),

    /** S3: Nuclear Plant — critical infrastructure defence */
    S3_NUCLEAR_PLANT("S3", "Nuclear Plant",
            "Nuclear facility protection including reactor buildings and spent fuel storage",
            38.6800, 26.9700),

    /** S4: Government Building — VIP protection in urban area */
    S4_GOV_BUILDING("S4", "Gov. Building",
            "Government building protection in urban environment against multi-UAS threat",
            38.4192, 27.1287),

    /** S5: Stadium — event security with mobile deployment */
    S5_STADIUM("S5", "Stadium",
            "Stadium event protection with mobile CUAS deployment",
            38.4300, 27.1500),

    /** S6: Outdoor Concert — event protection with metallic interference */
    S6_OUTDOOR_CONCERT("S6", "Outdoor Concert",
            "Outdoor concert/event protection with metallic interference challenges",
            38.4350, 27.1420),

    /** S7: Political Rally — mobile urban protection */
    S7_POLITICAL_RALLY("S7", "Political Rally",
            "Political rally protection in urban square with mobile truck-based system",
            38.4237, 27.1428),

    /** S8: International Summit — covert CUAS operations */
    S8_INT_SUMMIT("S8", "Int. Summit",
            "International summit protection with covert CUAS operations",
            38.3920, 27.0800),

    /** S9: Land Border — long-distance linear border defence */
    S9_LAND_BORDER("S9", "Land Border",
            "Land border protection with chained systems over long-distance linear border",
            38.5600, 26.8900),

    /** S10: Maritime Border — saline environment operations */
    S10_MARITIME_BORDER("S10", "Maritime Border",
            "Maritime border protection in saline environment with stealth requirements",
            38.4100, 26.9400);

    private final String code;
    private final String displayName;
    private final String description;
    private final double defaultLat;
    private final double defaultLon;

    ScenarioType(String code, String displayName, String description,
                 double defaultLat, double defaultLon) {
        this.code = code;
        this.displayName = displayName;
        this.description = description;
        this.defaultLat = defaultLat;
        this.defaultLon = defaultLon;
    }

    public String getCode()            { return code; }
    public String getDisplayName()     { return displayName; }
    public String getDescription()     { return description; }
    public double getDefaultLat()      { return defaultLat; }
    public double getDefaultLon()      { return defaultLon; }

    /** Find by scenario code (S1, S2, ... S10). */
    public static ScenarioType fromCode(String code) {
        for (ScenarioType s : values()) {
            if (s.code.equalsIgnoreCase(code)) return s;
        }
        throw new IllegalArgumentException("Unknown scenario code: " + code);
    }

    @Override
    public String toString() {
        return code + ": " + displayName;
    }
}
