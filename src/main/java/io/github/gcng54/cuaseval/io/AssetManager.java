package io.github.gcng54.cuaseval.io;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import io.github.gcng54.cuaseval.evaluator.EvaluationCriteria;
import io.github.gcng54.cuaseval.model.CuasSensor;
import io.github.gcng54.cuaseval.model.TestEnvironment;
import io.github.gcng54.cuaseval.model.TestScenario;
import io.github.gcng54.cuaseval.model.UasTarget;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Manages save/load operations for all CUAS-Eval asset types (AM-01, AM-02).
 * <p>
 * Assets include: Sensors, Targets, Environments, Scenarios, EvaluationCriteria.
 * All assets are stored as JSON files using Jackson.
 * </p>
 */
public class AssetManager {

    private static final Logger log = LoggerFactory.getLogger(AssetManager.class);

    /** Base directory for asset storage. */
    private Path assetRoot;

    /** Jackson mapper configured for pretty printing and Java 8 time. */
    private final ObjectMapper mapper;

    // Sub-directories per asset type
    private static final String SENSORS_DIR     = "sensors";
    private static final String TARGETS_DIR     = "targets";
    private static final String ENVIRONMENTS_DIR = "environments";
    private static final String SCENARIOS_DIR   = "scenarios";
    private static final String CRITERIA_DIR    = "criteria";

    // ── Constructor ─────────────────────────────────────────────────────

    public AssetManager(Path assetRoot) {
        this.assetRoot = assetRoot;
        this.mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        ensureDirectories();
    }

    /** Default constructor using ./assets as root. */
    public AssetManager() {
        this(Path.of("assets"));
    }

    // ── Public API ──────────────────────────────────────────────────────

    // ─── Sensors ────────────────────────────────────────────────────────

    public void saveSensor(CuasSensor sensor) throws IOException {
        String filename = sanitize(sensor.getSensorId()) + ".json";
        save(sensor, SENSORS_DIR, filename);
        log.info("Saved sensor: {}", sensor.getSensorId());
    }

    public CuasSensor loadSensor(String sensorId) throws IOException {
        String filename = sanitize(sensorId) + ".json";
        return load(CuasSensor.class, SENSORS_DIR, filename);
    }

    public List<CuasSensor> listSensors() {
        return listAll(CuasSensor.class, SENSORS_DIR);
    }

    public boolean deleteSensor(String sensorId) {
        return delete(SENSORS_DIR, sanitize(sensorId) + ".json");
    }

    // ─── Targets ────────────────────────────────────────────────────────

    public void saveTarget(UasTarget target) throws IOException {
        String filename = sanitize(target.getUid()) + ".json";
        save(target, TARGETS_DIR, filename);
        log.info("Saved target: {}", target.getUid());
    }

    public UasTarget loadTarget(String uid) throws IOException {
        String filename = sanitize(uid) + ".json";
        return load(UasTarget.class, TARGETS_DIR, filename);
    }

    public List<UasTarget> listTargets() {
        return listAll(UasTarget.class, TARGETS_DIR);
    }

    public boolean deleteTarget(String uid) {
        return delete(TARGETS_DIR, sanitize(uid) + ".json");
    }

    // ─── Environments ───────────────────────────────────────────────────

    public void saveEnvironment(TestEnvironment env, String name) throws IOException {
        String filename = sanitize(name) + ".json";
        save(env, ENVIRONMENTS_DIR, filename);
        log.info("Saved environment: {}", name);
    }

    public TestEnvironment loadEnvironment(String name) throws IOException {
        String filename = sanitize(name) + ".json";
        return load(TestEnvironment.class, ENVIRONMENTS_DIR, filename);
    }

    public List<String> listEnvironmentNames() {
        return listFileNames(ENVIRONMENTS_DIR);
    }

    public boolean deleteEnvironment(String name) {
        return delete(ENVIRONMENTS_DIR, sanitize(name) + ".json");
    }

    // ─── Scenarios ──────────────────────────────────────────────────────

    public void saveScenario(TestScenario scenario) throws IOException {
        String filename = sanitize(scenario.getScenarioId()) + ".json";
        save(scenario, SCENARIOS_DIR, filename);
        log.info("Saved scenario: {}", scenario.getScenarioId());
    }

    public TestScenario loadScenario(String scenarioId) throws IOException {
        String filename = sanitize(scenarioId) + ".json";
        return load(TestScenario.class, SCENARIOS_DIR, filename);
    }

    public List<String> listScenarioIds() {
        return listFileNames(SCENARIOS_DIR);
    }

    public boolean deleteScenario(String scenarioId) {
        return delete(SCENARIOS_DIR, sanitize(scenarioId) + ".json");
    }

    // ─── Evaluation Criteria ────────────────────────────────────────────

    public void saveCriteria(EvaluationCriteria criteria, String name) throws IOException {
        String filename = sanitize(name) + ".json";
        save(criteria, CRITERIA_DIR, filename);
        log.info("Saved evaluation criteria: {}", name);
    }

    public EvaluationCriteria loadCriteria(String name) throws IOException {
        String filename = sanitize(name) + ".json";
        return load(EvaluationCriteria.class, CRITERIA_DIR, filename);
    }

    public List<String> listCriteriaNames() {
        return listFileNames(CRITERIA_DIR);
    }

    public boolean deleteCriteria(String name) {
        return delete(CRITERIA_DIR, sanitize(name) + ".json");
    }

    // ── Configuration ───────────────────────────────────────────────────

    public Path getAssetRoot() { return assetRoot; }

    public void setAssetRoot(Path assetRoot) {
        this.assetRoot = assetRoot;
        ensureDirectories();
    }

    public ObjectMapper getMapper() { return mapper; }

    // ── Internal helpers ────────────────────────────────────────────────

    private void ensureDirectories() {
        try {
            Files.createDirectories(assetRoot.resolve(SENSORS_DIR));
            Files.createDirectories(assetRoot.resolve(TARGETS_DIR));
            Files.createDirectories(assetRoot.resolve(ENVIRONMENTS_DIR));
            Files.createDirectories(assetRoot.resolve(SCENARIOS_DIR));
            Files.createDirectories(assetRoot.resolve(CRITERIA_DIR));
        } catch (IOException e) {
            log.error("Failed to create asset directories", e);
        }
    }

    private <T> void save(T obj, String subDir, String filename) throws IOException {
        Path filePath = assetRoot.resolve(subDir).resolve(filename);
        mapper.writeValue(filePath.toFile(), obj);
    }

    private <T> T load(Class<T> clazz, String subDir, String filename) throws IOException {
        Path filePath = assetRoot.resolve(subDir).resolve(filename);
        return mapper.readValue(filePath.toFile(), clazz);
    }

    private <T> List<T> listAll(Class<T> clazz, String subDir) {
        List<T> result = new ArrayList<>();
        Path dir = assetRoot.resolve(subDir);
        if (!Files.isDirectory(dir)) return result;

        try (Stream<Path> files = Files.list(dir)) {
            for (Path p : files.filter(f -> f.toString().endsWith(".json"))
                               .collect(Collectors.toList())) {
                try {
                    result.add(mapper.readValue(p.toFile(), clazz));
                } catch (IOException e) {
                    log.warn("Failed to load {}: {}", p, e.getMessage());
                }
            }
        } catch (IOException e) {
            log.warn("Failed to list {}: {}", dir, e.getMessage());
        }
        return result;
    }

    private List<String> listFileNames(String subDir) {
        List<String> names = new ArrayList<>();
        Path dir = assetRoot.resolve(subDir);
        if (!Files.isDirectory(dir)) return names;

        try (Stream<Path> files = Files.list(dir)) {
            files.filter(f -> f.toString().endsWith(".json"))
                 .map(f -> f.getFileName().toString().replace(".json", ""))
                 .forEach(names::add);
        } catch (IOException e) {
            log.warn("Failed to list {}: {}", dir, e.getMessage());
        }
        return names;
    }

    private boolean delete(String subDir, String filename) {
        try {
            Path filePath = assetRoot.resolve(subDir).resolve(filename);
            return Files.deleteIfExists(filePath);
        } catch (IOException e) {
            log.warn("Failed to delete {}/{}: {}", subDir, filename, e.getMessage());
            return false;
        }
    }

    private String sanitize(String name) {
        if (name == null) return "unnamed";
        return name.replaceAll("[^a-zA-Z0-9_\\-]", "_");
    }
}
