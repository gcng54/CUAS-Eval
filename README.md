# CUAS-Eval v2.0 — Counter-UAS Evaluation Module

**CWA 18150 (COURAGEOUS) Performance Evaluation Pipeline**

A standalone Java/JavaFX module for evaluating Counter-UAS Detection, Tracking,
and Identification (DTI) systems against CWA 18150 test methodology requirements.

## What's New in v2.0

- **Enhanced UI**: Restructured 2-pane layout with 5 tabs (Scenarios, Sensors, Terrain, Results, Report); expanded map viewport
- **Interactive Map**: Mouse coordinates with elevation display, right-click "Add Node Here" context menu, sensor coverage sectors with azimuth arcs and elevation rings
- **Terrain Enhancements**: Map viewport sync, named elevation cache, obstacle generation over terrain, editable terrain mask parameters with explanations
- **Sensor Management**: Editable sensor library (update/remove), "Add to Scenario" button, EW sensitivity display, min/max elevation and azimuth configuration
- **Flight Plans**: FlightPlanGenerator with 5 patterns (straight line, orbit, racetrack, random walk, approach-retreat)
- **Asset Management**: Full CRUD via AssetManager — save/load sensors, targets, environments, scenarios, and evaluation criteria as JSON files in `./assets/`
- **Evaluation Criteria Editing**: Adjustable scoring weights (detection/tracking/identification), threshold list editor, save/load profiles
- **EW Effects**: Electronic Warfare conditions (NONE/LOW/MEDIUM/HIGH) with configurable jamming impacts on RF sensors, EW-aware Pd computation

---

## Quick Start

```bash
# Requires Java 21
# Compile
mvnw.cmd clean compile          # Windows
./mvnw clean compile             # Linux/macOS

# Run the JavaFX application
mvnw.cmd javafx:run              # Windows
./mvnw javafx:run                # Linux/macOS

# Package as fat JAR
mvnw.cmd clean package           # Windows
```

## Architecture

```
CUAS-Eval/
├── src/main/java/io/github/gcng54/cuaseval/
│   ├── CuasEvalApp.java          # Entry point
│   ├── Launcher.java             # Application launcher
│   ├── model/                     # Domain models (12+ classes)
│   │   ├── Requirement.java       # CWA 18150 requirement
│   │   ├── UasTarget.java         # UAS target (C0–C4) + EW jammer
│   │   ├── CuasSensor.java        # Sensor with EW sensitivity, elevation, azimuth
│   │   ├── SensorLibrary.java     # 16 sensor templates (mutable)
│   │   ├── DetectionResult.java   # Detection outcome
│   │   ├── TrackingResult.java    # Track with waypoints
│   │   ├── IdentificationResult   # ID result
│   │   ├── EvaluationResult.java  # Aggregated metrics
│   │   ├── TestEnvironment.java   # Environment config with EW conditions, obstacles
│   │   ├── TestScenario.java      # Scenario definition
│   │   └── GeoPosition.java       # Lat/lon/alt position
│   ├── dti/                       # DTI Pipeline
│   │   ├── DetectionSystem.java   # Swerling-I Pd model + EW attenuation
│   │   ├── TrackingSystem.java    # Track interpolation
│   │   ├── IdentificationSystem   # Classification engine
│   │   ├── MultiDtiSystem.java    # Multi-sensor fusion
│   │   └── DtiPipeline.java       # Orchestrator
│   ├── generator/                 # Test Generators
│   │   ├── TestTargetGenerator    # Standard target sets
│   │   ├── TestEnvironmentDesigner# Open-field, urban, weather, EW
│   │   ├── TestScenarioGenerator  # Pre-built scenarios
│   │   └── FlightPlanGenerator    # 5 flight patterns (v2.0)
│   ├── evaluator/                 # Evaluation Engine
│   │   ├── EvaluationCriteria     # Pass/fail thresholds, scoring weights
│   │   ├── MetricsCalculator      # Statistical metrics
│   │   └── TestEvaluator.java     # Top-level evaluator
│   ├── requirements/              # Requirements Manager
│   │   └── RequirementsManager    # JSON loader, queries
│   ├── report/                    # Report Generation
│   │   ├── KmlExporter.java       # Track2KML (Google Earth)
│   │   ├── TestReportGenerator    # HTML reports
│   │   └── UmlRenderer.java       # PlantUML auto-render
│   ├── terrain/                   # Terrain Analysis (v2.0)
│   │   ├── DtedReader.java        # DTED/SRTM elevation data
│   │   └── TerrainMaskCalculator  # Line-of-sight masking
│   ├── io/                        # Asset Management (v2.0)
│   │   └── AssetManager.java      # JSON save/load for all asset types
│   └── ui/                        # JavaFX UI
│       ├── MainView.java          # Application layout (2-pane, 5 tabs)
│       ├── MapView.java           # Canvas 2D map with mouse coords, context menu
│       ├── ScenarioPanel.java     # Config & run with EW conditions
│       ├── SensorPanel.java       # Sensor library editor, add-to-scenario
│       ├── DtedPanel.java         # Terrain controls, obstacle generation
│       ├── EvaluationPanel.java   # Results display + criteria editor
│       └── ReportPanel.java       # Export controls
├── src/main/resources/
│   ├── requirements/
│   │   ├── coutrage_requirements.json           # 75 CWA 18150 requirements
│   │   └── coutrage_scenario_requirements.json  # Scenario mappings
│   ├── css/main.css                              # Dark theme
│   └── logback.xml                               # Logging config
├── docs/
│   ├── CUAS-Eval-v2-requirements.md   # v2.0 requirements document
│   └── uml/                            # PlantUML diagrams (6)
│       ├── system-overview.puml
│       ├── dti-pipeline.puml
│       ├── data-model.puml           # Updated for v2.0
│       ├── test-evaluation.puml
│       ├── component-diagram.puml    # Updated for v2.0
│       └── requirements-traceability.puml
├── assets/                           # Asset storage (v2.0)
│   ├── sensors/                      # Saved sensor definitions
│   ├── targets/                      # Saved target profiles
│   ├── environments/                 # Saved environments
│   ├── scenarios/                    # Saved scenarios
│   └── criteria/                     # Evaluation criteria profiles
├── resources/
│   ├── dted/                         # DTED elevation cache
│   ├── srtm/                         # SRTM elevation data
│   └── world/                        # World shapefile for coastlines
└── pom.xml                           # Maven build
```

## Features

| Feature | Description |
|---------|-------------|
| **DTI Pipeline** | Simulated Detection→Tracking→Identification with Swerling-I model, track interpolation, classification |
| **EW Effects** | Electronic Warfare conditions (NONE/LOW/MEDIUM/HIGH) with jamming impacts on RF sensors, EW-aware Pd computation |
| **75 Requirements** | All CWA 18150 functional (25), performance (20), and testable parameter (30) requirements |
| **Test Generators** | Standard target sets (C1–C4), environments (open/urban/weather/EW), predefined scenarios |
| **Flight Plans** | 5 flight patterns: straight line, orbit, racetrack, random walk, approach-retreat |
| **Asset Management** | Full CRUD: save/load sensors, targets, environments, scenarios, evaluation criteria as JSON |
| **Metrics** | Pd, Pfa, track continuity, Pi, IFF accuracy, latency, CEP, P95 |
| **Track2KML** | Export flight paths, detections, tracks to Google Earth KML |
| **HTML Reports** | Self-contained evaluation reports with metrics tables and requirement compliance |
| **Interactive Map** | 2D canvas map with zoom/pan, mouse coords + elevation, right-click context menu, sensor coverage sectors |
| **Terrain Analysis** | DTED/SRTM elevation data, line-of-sight terrain masking, editable mask parameters, obstacle generation |
| **Sensor Library** | 16 editable sensor templates (radar, RF, RF+, multispectral) with EW sensitivity, elevation, azimuth |
| **Criteria Editor** | Adjustable scoring weights, threshold list editor, save/load evaluation profiles |
| **UML Diagrams** | 6 PlantUML diagrams documenting system architecture and data flow |

## Requirements Mapping (CWA 18150)

- **Detection domain**: FR01, FR02, FR09, FR15, PR08, PR09, TP_D01–TP_D16
- **Tracking domain**: FR04, FR17, FR18, PR19, PR20, TP_T01–TP_T09
- **Identification domain**: FR06, FR14, FR20, PR13, PR14, TP_I01–TP_I08

## Tech Stack

- Java 21, JavaFX 21.0.2
- Jackson 2.17 (JSON), Apache Commons Math 3.6.1 (statistics)
- JAXB 4.0 (KML), SLF4J + Logback (logging)
- Maven 3.9.6 (via wrapper)

## Building for Distribution

```bash
mvnw.cmd clean package
# Creates: target/cuas-eval-1.0.1-SNAPSHOT.jar (fat JAR)
```

## Usage Guide

### Running Evaluations

1. Launch the application: `mvnw.cmd javafx:run`
2. Select **Scenarios** tab and configure test parameters
3. Choose scenario type (Generic or COURAGEOUS S1–S10)
4. Set weather conditions and EW environment
5. Click **Run Evaluation**
6. View results in **Results** tab, export reports from **Report** tab

### Map Interactions

- **Pan**: Click and drag the map canvas
- **Zoom**: Mouse wheel (in/out)
- **Coordinates**: Mouse position shown in info overlay with elevation
- **Add Node**: Right-click → "Add Node Here" to manually add sensor sites
- **Copy Coordinates**: Right-click → "Copy Coordinates" to clipboard

### Sensor Management

1. Navigate to **Sensors** tab
2. Select sensor from library (16 templates)
3. View specifications: type, range, Pd, azimuth, elevation, EW sensitivity
4. Edit sensor parameters and click **Update in Library** to persist changes
5. Use **Add to Scenario** to place sensor at current map center
6. Configure DTI node associations for multi-sensor fusion

### Terrain Analysis

1. Go to **Terrain** tab
2. Click **Sync from Map** to capture current viewport center and radius
3. Enter cache name and click **Build Elevation Cache** (DTED/SRTM required)
4. Load cache to overlay terrain elevation on map
5. Adjust terrain mask parameters (azimuth count, samples, antenna height)
6. Generate obstacles over terrain (buildings, towers, tree lines) for realistic scenarios

### Asset Management

- **Save Assets**: Use AssetManager to persist sensors, targets, environments, scenarios, and evaluation criteria
  - Files stored in `./assets/` directory as JSON
  - Organized into subdirectories by type
- **Load Assets**: Select from saved asset lists, edit, and re-save
- **Evaluation Profiles**: Save custom scoring weights and thresholds for different evaluation standards

---

*Part of the COURAGEOUS CWA 18150 Counter-UAS Testing Methodology.*
