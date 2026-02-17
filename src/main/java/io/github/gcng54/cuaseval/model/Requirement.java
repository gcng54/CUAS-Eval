package io.github.gcng54.cuaseval.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Locale;

/**
 * A single CWA 18150 requirement entry.
 * Covers Functional (FR), Performance (PR), and Testable Parameter (TP) items.
 * <p>
 * Links to CWA 18150 sections via {@link #id}.
 * For testable parameters, {@link #parameter} and {@link #unit} carry
 * measurement metadata.
 * </p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Requirement {

    /** Unique requirement ID, e.g. FR01, PR05, TP_D01 */
    @JsonProperty("id")
    private String id;

    /** Requirement type: Functional | Performance | Testable */
    @JsonProperty("type")
    private String type;

    /** Requirement category: Detection | Tracking | Identification | HMI | Architecture | Logistics */
    @JsonProperty("category")
    private String category;

    /** Human-readable name (functional/performance) or parameter name (testable) */
    @JsonProperty("name")
    private String name;

    /** Parameter name — used only for testable parameters (TP_*) */
    @JsonProperty("parameter")
    private String parameter;

    /** Unit of measurement — used only for testable parameters (TP_*) */
    @JsonProperty("unit")
    private String unit;

    /** Full description / justification text */
    @JsonProperty("description")
    private String description;

    /** Importance level: SHALL, SHOULD, MAY (scenario requirements) */
    @JsonProperty("importance")
    private String importance;

    /** Scenario code this requirement belongs to, e.g. S1, S2 ... S10 (null for general) */
    @JsonProperty("scenario")
    private String scenario;

    // ── Constructors ────────────────────────────────────────────────────

    public Requirement() {}

    public Requirement(String id, String type, String category,
                       String name, String description) {
        this.id = id;
        this.type = type;
        this.category = category;
        this.name = name;
        this.description = description;
    }

    // ── Accessors ───────────────────────────────────────────────────────

    public String getId()          { return id; }
    public String getType()        { return type; }
    public String getCategory()    { return category; }
    public String getName()        { return name != null ? name : parameter; }
    public String getParameter()   { return parameter; }
    public String getUnit()        { return unit; }
    public String getDescription() { return description; }
    public String getImportance()   { return importance; }
    public String getScenario()     { return scenario; }

    /** Whether this is a scenario-specific requirement (has a scenario code). */
    public boolean isScenarioSpecific() { return scenario != null && !scenario.isBlank(); }

    public void setId(String id)                   { this.id = id; }
    public void setType(String type)               { this.type = type; }
    public void setCategory(String category)       { this.category = category; }
    public void setName(String name)               { this.name = name; }
    public void setParameter(String parameter)     { this.parameter = parameter; }
    public void setUnit(String unit)               { this.unit = unit; }
    public void setDescription(String description) { this.description = description; }
    public void setImportance(String importance)     { this.importance = importance; }
    public void setScenario(String scenario)         { this.scenario = scenario; }

    /**
     * Returns the classified {@link RequirementType} enum from the JSON string.
     */
    public RequirementType getRequirementType() {
        if (type == null) return RequirementType.FUNCTIONAL;
        return switch (type.toLowerCase()) {
            case "performance" -> RequirementType.PERFORMANCE;
            case "testable"    -> RequirementType.TESTABLE;
            case "functional", "operational" -> RequirementType.FUNCTIONAL;
            default            -> RequirementType.FUNCTIONAL;
        };
    }

    /**
     * Returns the classified {@link RequirementCategory} enum from the JSON string.
     */
    public RequirementCategory getRequirementCategory() {
        if (category == null) return RequirementCategory.ARCHITECTURE;
        return switch (category.toLowerCase()) {
            case "detection"       -> RequirementCategory.DETECTION;
            case "tracking"        -> RequirementCategory.TRACKING;
            case "identification"  -> RequirementCategory.IDENTIFICATION;
            case "hmi"             -> RequirementCategory.HMI;
            case "logistics"       -> RequirementCategory.LOGISTICS;
            case "automation"      -> RequirementCategory.AUTOMATION;
            case "operational"     -> RequirementCategory.OPERATIONAL;
            case "sitespecific"    -> RequirementCategory.SITE_SPECIFIC;
            default                -> RequirementCategory.ARCHITECTURE;
        };
    }

    @Override
    public String toString() {
        return String.format(Locale.ENGLISH, "[%s] %s — %s", id, getName(), description);
    }
}
