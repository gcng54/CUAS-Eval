package io.github.gcng54.cuaseval.requirements;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.gcng54.cuaseval.model.Requirement;
import io.github.gcng54.cuaseval.model.RequirementCategory;
import io.github.gcng54.cuaseval.model.RequirementType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Loads and manages CWA 18150 requirements from both the general requirements
 * JSON and the scenario-specific requirements JSON.
 * Provides lookup, filtering, scenario-based queries, and traceability functions.
 * <p>
 * Requirements sources:
 * <ul>
 *   <li>{@code /requirements/coutrage_requirements.json} — general FR/PR/TP</li>
 *   <li>{@code /requirements/coutrage_scenario_requirements.json} — S1–S10 scenario reqs</li>
 * </ul>
 * </p>
 */
public class RequirementsManager {

    private static final Logger log = LoggerFactory.getLogger(RequirementsManager.class);
    private static final String REQUIREMENTS_RESOURCE = "/requirements/coutrage_requirements.json";
    private static final String SCENARIO_REQUIREMENTS_RESOURCE = "/requirements/coutrage_scenario_requirements.json";

    private final List<Requirement> allRequirements = new ArrayList<>();
    private final Map<String, Requirement> requirementMap = new LinkedHashMap<>();
    private final Map<String, List<String>> requirementLinks = new HashMap<>();

    /** Scenario-specific requirements keyed by scenario code (S1..S10) */
    private final Map<String, List<Requirement>> scenarioRequirements = new LinkedHashMap<>();

    /** Scenario requirement links: scenarioCode → { domain → [reqIds] } */
    private final Map<String, Map<String, List<String>>> scenarioReqLinks = new LinkedHashMap<>();

    /** Scenario definitions loaded from JSON */
    private final List<ScenarioDefinition> scenarioDefinitions = new ArrayList<>();

    /**
     * Lightweight holder for the scenario descriptors in the JSON.
     */
    public static class ScenarioDefinition {
        private final String id, name, fullName, location, weather;
        private final String uasCategory, payload, description;

        public ScenarioDefinition(String id, String name, String fullName,
                                  String location, String weather, String uasCategory,
                                  String payload, String description) {
            this.id = id; this.name = name; this.fullName = fullName;
            this.location = location; this.weather = weather;
            this.uasCategory = uasCategory; this.payload = payload;
            this.description = description;
        }

        public String getId()           { return id; }
        public String getName()         { return name; }
        public String getFullName()     { return fullName; }
        public String getLocation()     { return location; }
        public String getWeather()      { return weather; }
        public String getUasCategory()  { return uasCategory; }
        public String getPayload()      { return payload; }
        public String getDescription()  { return description; }
    }

    // ── Constructor ─────────────────────────────────────────────────────

    public RequirementsManager() {
        loadRequirements();
        loadScenarioRequirements();
    }

    // ── Loading ─────────────────────────────────────────────────────────

    /**
     * Load requirements from the bundled JSON file.
     */
    private void loadRequirements() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            InputStream is = getClass().getResourceAsStream(REQUIREMENTS_RESOURCE);
            if (is == null) {
                log.warn("Requirements file not found: {}", REQUIREMENTS_RESOURCE);
                return;
            }

            JsonNode root = mapper.readTree(is);
            JsonNode sections = root.get("sections");

            // Functional requirements
            if (sections.has("functionalRequirements")) {
                for (JsonNode node : sections.get("functionalRequirements")) {
                    Requirement req = mapper.treeToValue(node, Requirement.class);
                    addRequirement(req);
                }
            }
            // Performance requirements
            if (sections.has("performanceRequirements")) {
                for (JsonNode node : sections.get("performanceRequirements")) {
                    Requirement req = mapper.treeToValue(node, Requirement.class);
                    addRequirement(req);
                }
            }
            // Testable parameters
            if (sections.has("testableParameters")) {
                for (JsonNode node : sections.get("testableParameters")) {
                    Requirement req = mapper.treeToValue(node, Requirement.class);
                    addRequirement(req);
                }
            }

            // Load requirement domain links
            if (root.has("requirementLinks")) {
                JsonNode links = root.get("requirementLinks");
                links.fieldNames().forEachRemaining(domain -> {
                    List<String> ids = new ArrayList<>();
                    links.get(domain).forEach(n -> ids.add(n.asText()));
                    requirementLinks.put(domain, ids);
                });
            }

            log.info("Loaded {} requirements ({} links)",
                    allRequirements.size(), requirementLinks.size());

        } catch (Exception e) {
            log.error("Failed to load requirements", e);
        }
    }

    private void addRequirement(Requirement req) {
        allRequirements.add(req);
        requirementMap.put(req.getId(), req);
        // Also index by scenario if applicable
        if (req.isScenarioSpecific()) {
            scenarioRequirements
                    .computeIfAbsent(req.getScenario(), k -> new ArrayList<>())
                    .add(req);
        }
    }

    /**
     * Load scenario-specific requirements from the bundled JSON file.
     */
    private void loadScenarioRequirements() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            InputStream is = getClass().getResourceAsStream(SCENARIO_REQUIREMENTS_RESOURCE);
            if (is == null) {
                log.warn("Scenario requirements file not found: {}", SCENARIO_REQUIREMENTS_RESOURCE);
                return;
            }

            JsonNode root = mapper.readTree(is);

            // Load scenario definitions
            if (root.has("scenarios")) {
                for (JsonNode sn : root.get("scenarios")) {
                    scenarioDefinitions.add(new ScenarioDefinition(
                            sn.path("id").asText(),
                            sn.path("name").asText(),
                            sn.path("fullName").asText(),
                            sn.path("location").asText(),
                            sn.path("weather").asText(),
                            sn.path("uasCategory").asText(),
                            sn.path("payload").asText(),
                            sn.path("description").asText()
                    ));
                }
            }

            JsonNode sections = root.get("sections");
            int count = 0;

            // Operational requirements
            if (sections.has("operationalRequirements")) {
                for (JsonNode node : sections.get("operationalRequirements")) {
                    Requirement req = mapper.treeToValue(node, Requirement.class);
                    addRequirement(req);
                    count++;
                }
            }
            // Scenario functional requirements
            if (sections.has("scenarioFunctionalRequirements")) {
                for (JsonNode node : sections.get("scenarioFunctionalRequirements")) {
                    Requirement req = mapper.treeToValue(node, Requirement.class);
                    addRequirement(req);
                    count++;
                }
            }
            // Scenario performance requirements
            if (sections.has("scenarioPerformanceRequirements")) {
                for (JsonNode node : sections.get("scenarioPerformanceRequirements")) {
                    Requirement req = mapper.treeToValue(node, Requirement.class);
                    addRequirement(req);
                    count++;
                }
            }

            // Load scenario requirement links
            if (root.has("scenarioRequirementLinks")) {
                JsonNode links = root.get("scenarioRequirementLinks");
                links.fieldNames().forEachRemaining(scenarioCode -> {
                    Map<String, List<String>> domainMap = new LinkedHashMap<>();
                    JsonNode domains = links.get(scenarioCode);
                    domains.fieldNames().forEachRemaining(domain -> {
                        List<String> ids = new ArrayList<>();
                        domains.get(domain).forEach(n -> ids.add(n.asText()));
                        domainMap.put(domain, ids);
                    });
                    scenarioReqLinks.put(scenarioCode, domainMap);
                });
            }

            log.info("Loaded {} scenario requirements, {} scenario definitions, {} scenario links",
                    count, scenarioDefinitions.size(), scenarioReqLinks.size());

        } catch (Exception e) {
            log.error("Failed to load scenario requirements", e);
        }
    }

    // ── Queries ─────────────────────────────────────────────────────────

    /** Get all requirements. */
    public List<Requirement> getAll() {
        return Collections.unmodifiableList(allRequirements);
    }

    /** Get requirement by ID. */
    public Requirement getById(String id) {
        return requirementMap.get(id);
    }

    /** Get requirements by type. */
    public List<Requirement> getByType(RequirementType type) {
        String typeStr = switch (type) {
            case FUNCTIONAL  -> "Functional";
            case PERFORMANCE -> "Performance";
            case TESTABLE    -> "Testable";
        };
        return allRequirements.stream()
                .filter(r -> typeStr.equalsIgnoreCase(r.getType()))
                .collect(Collectors.toList());
    }

    /** Get requirements by category. */
    public List<Requirement> getByCategory(RequirementCategory category) {
        String catStr = switch (category) {
            case DETECTION       -> "Detection";
            case TRACKING        -> "Tracking";
            case IDENTIFICATION  -> "Identification";
            case HMI             -> "HMI";
            case ARCHITECTURE    -> "Architecture";
            case LOGISTICS       -> "Logistics";
            case AUTOMATION      -> "Automation";
            case OPERATIONAL     -> "Operational";
            case SITE_SPECIFIC   -> "SiteSpecific";
        };
        return allRequirements.stream()
                .filter(r -> catStr.equalsIgnoreCase(r.getCategory()))
                .collect(Collectors.toList());
    }

    /** Get requirement IDs linked to a domain (detection, tracking, etc.). */
    public List<String> getLinkedRequirements(String domain) {
        return requirementLinks.getOrDefault(domain.toLowerCase(), List.of());
    }

    /** Get total count. */
    public int getCount() {
        return allRequirements.size();
    }

    /** Get all domain link keys. */
    public Set<String> getDomains() {
        return requirementLinks.keySet();
    }

    // ── Scenario Queries ────────────────────────────────────────────────

    /** Get all requirements for a specific scenario (S1..S10). */
    public List<Requirement> getByScenario(String scenarioCode) {
        return scenarioRequirements.getOrDefault(scenarioCode, List.of());
    }

    /** Get requirement IDs linked to a scenario and domain (operational/functional/performance). */
    public List<String> getScenarioLinkedRequirements(String scenarioCode, String domain) {
        Map<String, List<String>> domains = scenarioReqLinks.get(scenarioCode);
        if (domains == null) return List.of();
        return domains.getOrDefault(domain.toLowerCase(), List.of());
    }

    /** Get all requirement IDs for a scenario across all domains. */
    public List<String> getAllScenarioRequirementIds(String scenarioCode) {
        Map<String, List<String>> domains = scenarioReqLinks.get(scenarioCode);
        if (domains == null) return List.of();
        List<String> all = new ArrayList<>();
        domains.values().forEach(all::addAll);
        return all;
    }

    /** Get scenario definition by code. */
    public ScenarioDefinition getScenarioDefinition(String scenarioCode) {
        return scenarioDefinitions.stream()
                .filter(s -> s.getId().equalsIgnoreCase(scenarioCode))
                .findFirst().orElse(null);
    }

    /** Get all scenario definitions. */
    public List<ScenarioDefinition> getScenarioDefinitions() {
        return Collections.unmodifiableList(scenarioDefinitions);
    }

    /** Get all available scenario codes. */
    public Set<String> getScenarioCodes() {
        return scenarioReqLinks.keySet();
    }

    /** Get only scenario-specific requirements. */
    public List<Requirement> getAllScenarioRequirements() {
        return allRequirements.stream()
                .filter(Requirement::isScenarioSpecific)
                .collect(Collectors.toList());
    }
}
