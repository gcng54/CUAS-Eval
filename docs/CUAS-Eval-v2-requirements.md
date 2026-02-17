# CUAS-Eval v2.0 — Enhanced Capabilities Requirements

**Document ID:** CUAS-EVAL-REQ-v2.0  
**Date:** 2026-02-17  
**Baseline:** CWA 18150 (COURAGEOUS) + CUAS-Eval Upper-Level Extensions

---

## 1. Overview

CUAS-Eval extends the CWA 18150 (COURAGEOUS) baseline to be a comprehensive, adjustable 
Counter-UAS evaluation tool with full asset management, interactive map features, 
electronic warfare (EW) modelling, obstacle-aware terrain analysis, and configurable 
evaluation metrics. COURAGEOUS requirements serve as the *default baseline*; CUAS-Eval 
adds upper-level capabilities for flexible scenario construction and evaluation.

---

## 2. UI / Layout Requirements

| ID | Requirement | Priority | Status |
|------|-----------------------------------------------|----------|--------|
| UI-01 | Move Evaluation Results and Report panels into left TabPane alongside Scenarios, Sensors, Terrain tabs. Map occupies full remaining width. | HIGH | NEW |
| UI-02 | Display mouse cursor coordinates (lat, lon) on the map below the centre coordinate overlay. Include elevation from loaded DTED/SRTM if available. | HIGH | NEW |
| UI-03 | Right-click context menu on map with "Add Node Here" action that populates Sensor Panel node lat/lon fields with the clicked coordinate. | HIGH | NEW |
| UI-04 | Terrain Panel Centre Lat, Lon, and Radius fields are updated by the current map viewport (centre + visible radius). Bidirectional sync. | MEDIUM | NEW |
| UI-05 | Elevation cache build dialog requires a user-specified name for easy identification when loading. Cache files are listed by name. | MEDIUM | NEW |

---

## 3. Terrain / Obstacle Requirements

| ID | Requirement | Priority | Status |
|------|-----------------------------------------------|----------|--------|
| TR-01 | Obstacle Generation panel in Terrain tab: create, edit, remove obstacles with position (lat/lon), height, width, and description. | HIGH | NEW |
| TR-02 | Obstacles supplement elevation data for coverage calculation. Obstacles act as virtual terrain that blocks line-of-sight even when DTED/SRTM has no data at that point. | HIGH | NEW |
| TR-03 | Terrain Mask parameters are exposed for editing: number of azimuths, samples per profile, antenna height AGL, Earth curvature correction toggle, max range override. | MEDIUM | NEW |
| TR-04 | Obstacle list is part of `TestEnvironment` and saved/loaded with scenarios. | MEDIUM | NEW |

### Terrain Mask Explanation

A **Terrain Mask** is the result of line-of-sight analysis from a sensor position. For each azimuth direction, the calculator samples terrain elevation along a radial profile and computes the maximum *mask angle* — the minimum elevation angle a target must be above to be visible from the sensor. Parameters used:

- **Sensor Position** (lat, lon) — origin of radial profiles
- **Sensor Altitude MSL** — antenna height above mean sea level (terrain elevation + antenna height AGL, default 10m)
- **Max Range** — maximum detection range in metres; profiles extend to this distance
- **Number of Azimuths** — angular resolution (default 72 = 5° steps; 360 = 1° steps)
- **Samples per Profile** — distance resolution along each radial (default 100)
- **Earth Curvature Correction** — subtracts `d²/(2R)` from effective terrain elevation at distance `d`

---

## 4. Sensor Requirements

| ID | Requirement | Priority | Status |
|------|-----------------------------------------------|----------|--------|
| SN-01 | Sensors can be added to an existing loaded scenario without regenerating the scenario. Added sensors appear in the environment's sensor sites. | HIGH | NEW |
| SN-02 | Sensor Library is editable at runtime: add new sensor templates, modify existing templates, delete custom templates. Baseline (16 built-in) templates are read-only but copyable. | HIGH | NEW |
| SN-03 | Sensor coverage display includes min/max elevation angle and azimuth coverage. Map visualizes coverage sector (not full circle) when azimuth < 360° and within defined elevation angles. | MEDIUM | NEW |
| SN-04 | Sensor coverage on map rendered at a user-defined minimum elevation angle. Only area where target would be above min elevation is drawn as covered. | MEDIUM | NEW |
| SN-05 | Sensor library and custom sensors support save/load to JSON files. | MEDIUM | NEW |

---

## 5. Target / Flight Plan Requirements

| ID | Requirement | Priority | Status |
|------|-----------------------------------------------|----------|--------|
| TG-01 | Targets can be added to an existing scenario with UAS class selection and flight plan assignment. | HIGH | NEW |
| TG-02 | Flight Plan Generation: generate flight plans from waypoints or patterns (straight line, orbit, racetrack, random walk, approach-retreat). | HIGH | NEW |
| TG-03 | Flight plan waypoints can be added by clicking on the map (right-click → "Add Waypoint"). | MEDIUM | NEW |
| TG-04 | Targets and flight plans support save/load to JSON files. | MEDIUM | NEW |

---

## 6. Environment Requirements

| ID | Requirement | Priority | Status |
|------|-----------------------------------------------|----------|--------|
| EV-01 | Environment includes Electronic Warfare (EW) effects in addition to weather conditions. | HIGH | NEW |
| EV-02 | EW effects degrade RF sensor capabilities (RF detectors, radar) analogous to weather degradation. Initial implementation: EW factor (0.0–1.0) applied as a multiplier to RF sensor Pd and identification probability. | HIGH | NEW |
| EV-03 | EW can be applied by targets (UAS-mounted jammer), by a fixed jammer asset, or as a general environment condition. | MEDIUM | NEW |
| EV-04 | EW parameters: jamming power level (LOW/MEDIUM/HIGH), affected frequency bands, jammer position (if directional). | MEDIUM | NEW |
| EV-05 | Environment assets (weather, EW settings, obstacles, sensor sites) support save/load to JSON. | MEDIUM | NEW |

---

## 7. Scenario / Asset Management Requirements

| ID | Requirement | Priority | Status |
|------|-----------------------------------------------|----------|--------|
| AM-01 | All major assets (Sensors, Targets with flight plans, Environments, Scenarios) support Create, Edit, Save, and Load operations via JSON files. | HIGH | NEW |
| AM-02 | Scenario save includes complete state: environment, sensor sites, obstacles, targets, flight plans, EW settings, linked requirements, evaluation criteria. | HIGH | NEW |
| AM-03 | Asset lists displayed in UI panels with load/save buttons and file chooser dialogs. | MEDIUM | NEW |

---

## 8. Evaluation / Metrics Requirements

| ID | Requirement | Priority | Status |
|------|-----------------------------------------------|----------|--------|
| EM-01 | Evaluation criteria (thresholds per requirement) are editable in a dedicated panel/dialog. User can adjust min/max values, add/remove thresholds. | HIGH | NEW |
| EM-02 | Evaluation criteria profiles support save/load to JSON. Multiple profiles can be maintained (e.g., "CWA 18150 Baseline", "Strict", "Custom"). | HIGH | NEW |
| EM-03 | Evaluation Results panel is now a tab in the left TabPane for a wider map display. | HIGH | NEW |

---

## 9. Requirements Management

| ID | Requirement | Priority | Status |
|------|-----------------------------------------------|----------|--------|
| RM-01 | COURAGEOUS (CWA 18150) requirements are the default baseline and loaded automatically. | EXISTING | — |
| RM-02 | CUAS-Eval acts as upper-level framework: users can add custom requirements beyond COURAGEOUS. | MEDIUM | NEW |
| RM-03 | Requirements can be enabled/disabled per evaluation run. | LOW | NEW |

---

## 10. Updated Class Model Changes

| Domain Change | Description |
|--------------------------------------|--------------------------------------------------------------|
| `TestEnvironment.ewCondition` | New field: `EwCondition` enum (NONE, LOW, MEDIUM, HIGH) |
| `TestEnvironment.ewFactor` | New field: double (0.0–1.0) RF degradation factor |
| `CuasSensor.ewFactor` | New field: EW sensitivity factor per sensor |
| `CuasSensor.computePd()` | Updated to include EW degradation for RF-based sensors |
| `SensorLibrary` | Made mutable: `addTemplate()`, `removeTemplate()`, `updateTemplate()` |
| `Obstacle` | Extended: `lengthM` field, `type` enum (BUILDING, TOWER, TREE_LINE, HILL, WALL) |
| `EvaluationCriteria` | Made serializable: save/load JSON, editable thresholds |
| `FlightPlanGenerator` | New class: generates standard flight patterns |
| `MapView` | Mouse coordinate display, right-click context menu, coverage sectors |

---

## 11. Traceability Matrix

| New Req | Implements | Affects Classes |
|---------|-----------------------------------------------|----------------------------------------------|
| UI-01 | Map bigger, evaluation as tab | `MainView` |
| UI-02 | Mouse coordinates + elevation | `MapView` |
| UI-03 | Right-click add node | `MapView`, `SensorPanel` |
| UI-04 | Map → terrain sync | `MapView`, `DtedPanel` |
| UI-05 | Named elevation caches | `DtedPanel` |
| TR-01 | Obstacle editor | `DtedPanel` |
| TR-02 | Obstacle in LOS calculation | `TerrainMaskCalculator` |
| TR-03 | Terrain mask param edit | `DtedPanel` |
| SN-01 | Add sensor to scenario | `SensorPanel`, `ScenarioPanel` |
| SN-02 | Editable sensor library | `SensorLibrary`, `SensorPanel` |
| SN-03 | Coverage sector display | `MapView`, `CuasSensor` |
| TG-01 | Add target to scenario | `ScenarioPanel` |
| TG-02 | Flight plan generation | `FlightPlanGenerator` |
| EV-01 | EW effects | `TestEnvironment` |
| EV-02 | EW RF degradation | `CuasSensor`, `DetectionSystem` |
| AM-01 | Asset CRUD | All panels, model classes |
| EM-01 | Editable metrics | `EvaluationCriteria`, `EvaluationPanel` |
| EM-02 | Metrics save/load | `EvaluationCriteria` |

---

*End of Requirements Document*
