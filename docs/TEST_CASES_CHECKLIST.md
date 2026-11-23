# SCENAR — Test-Case Verification Checklist
**Owner:** Teammate 2 (Responder Side and Product Delivery)  
**Target Milestone:** V0 — V5 (Weekend MVP & Release Gates)  
**Verification Command:** `.\gradlew.bat testDebugUnitTest assembleDebug`

---

## 1. Automated Unit Tests

| ID | Test Suite | Test Case | Expected Result | Status |
|---|---|---|---|---|
| UT-01 | `ResponderUnitTest` | `testIncidentStatusProgression` | Status cycles linearly `RECEIVED` → `DISPATCHED` → `EN_ROUTE` → `ON_SCENE` → `RESOLVED` and halts at `RESOLVED` | PASS |
| UT-02 | `ResponderUnitTest` | `testPriorityCalculation` | Critical conditions trigger `CRITICAL PRIORITY`; hazards (fire/smoke) trigger `HIGH PRIORITY` | PASS |
| UT-03 | `ResponderUnitTest` | `testCanonicalSampleJsonParsing` | Ingests canonical sample JSON without loss; verifies all 11 model fields | PASS |
| UT-04 | `ResponderUnitTest` | `testJsonRoundTrip` | Serialized report matches parsed report identically | PASS |
| UT-05 | `ResponderUnitTest` | `testResponderIncidentLifecycleAndTimeline` | Status changes, field notes, and notifications generate chronological audit events | PASS |
| UT-06 | `ResponderUnitTest` | `testNearbyFacilityHaversineDistanceAndEta` | Calculates spherical distance within ±0.05 km and realistic urban travel ETA | PASS |
| UT-07 | `ResponderUnitTest` | `testBleRescuePacket` | Generates compact JSON beacon payload under 128 bytes with service UUID | PASS |
| UT-08 | `FakeIncidentAiServiceTest` | Deterministic AI output | Fake AI returns stable sample report for victim reporting flow | PASS |
| UT-09 | `VictimReportDraftTest` | Review screen edits | User corrections override AI output; unknown fields are preserved | PASS |

---

## 2. Interactive Responder Console & Dashboard (Manual / UI Verification)

### TC-01: Direct Responder Dashboard Access
- [ ] Launch application on emulator / device.
- [ ] Verify "Open Responder Dashboard" button is prominent on Home screen.
- [ ] Tap "Open Responder Dashboard".
- [ ] Confirm `ResponderFragment` loads immediately without crash or lag.

### TC-02: Incident Queue & Triage Feed
- [ ] Verify horizontal queue shows active incidents:
  - `INC-2026-LIVE` (if submitted from victim flow)
  - `INC-2026-8842` (Structural Emergency — Building B)
  - `INC-2026-8839` (Chemical Hazard — Chemistry Hall)
  - `INC-2026-8825` (Medical Trauma — Campus Quad)
- [ ] Tap each incident card and verify active incident switches smoothly.
- [ ] Verify priority badges render in high-contrast color codes:
  - `CRITICAL PRIORITY`: Red (`#DC2626`)
  - `HIGH PRIORITY`: Amber (`#EA580C`)
  - `MEDIUM PRIORITY`: Orange (`#E65100`)

### TC-03: Incident Structured Details
- [ ] Confirm incident type, executive summary, people affected count, and time elapsed are displayed.
- [ ] Verify **Critical Conditions** card displays bulleted items with alert icon (e.g., "One person is unresponsive").
- [ ] Verify **Scene Hazards** card displays warnings (e.g., "Smoke in hallway", "Falling debris").
- [ ] Verify **Consented Medical Flags** card explicitly displays patient consent badge (e.g., "Consented: Penicillin allergy").
- [ ] Verify **Missing Information** displays unverified items (e.g., "Confirm floor accessibility").
- [ ] Verify **AI Uncertainties** displays caution banner for first responders.

### TC-04: Status Progression Lifecycle
- [ ] Tap "Advance Status" button.
- [ ] Verify status advances from `DISPATCHED` → `EN ROUTE`.
- [ ] Verify status pill color updates (`#6A1B9A` Purple).
- [ ] Verify Stepper Bar highlights current stage.
- [ ] Verify Dispatch Timeline appends a timestamped audit log:
  `"[HH:mm:ss] Status Updated: En Route — Incident status transitioned to En Route"`
- [ ] Tap "Advance Status" again to verify `ON SCENE` and `RESOLVED` states.

### TC-05: Responder Field Notes
- [ ] Enter text in "Enter responder on-scene note...":
  `"Perimeter secured. Hallway smoke clearing. Secondary medic requested."`
- [ ] Tap "Add Field Note".
- [ ] Verify input clears and note appears at the top of the Dispatch Timeline.
- [ ] Verify empty or whitespace notes are rejected gracefully.

---

## 3. Map & Tactical Location Display

### TC-06: Tactical Map Radar View
- [ ] Verify `TacticalMapView` canvas renders dark navy grid (`#0F172A`) with coordinate lines.
- [ ] Verify pulsing red beacon marker indicates Incident Target.
- [ ] Verify cyan marker indicates Unit 12 (Responder).
- [ ] Verify green cross indicates nearest Emergency Facility / Hospital.
- [ ] Verify bottom HUD telemetry displays live GPS coordinates, distance (km), and transit ETA.

### TC-07: Navigation & Coordinate Actions
- [ ] Tap "Navigate in Google Maps".
- [ ] Verify standard Android `geo:` intent or browser Google Maps launches with exact scene coordinates and location pin.
- [ ] Tap "Copy Coordinates".
- [ ] Verify Toast confirms: `"Coordinates copied to clipboard"`.
- [ ] Paste into another app/browser to verify exact format: `37.77492, -122.41941`.

---

## 4. Automated Contact-Notification Simulation (V3 Milestone)

### TC-08: Notification Preview & Recipients
- [ ] Inspect recipient group:
  - Designated Emergency Contact (Jane Doe — Next of Kin: +1 555-0199)
  - Fire Station 4 Dispatch
  - County EMS Mutual Aid
- [ ] Verify preview text dynamically incorporates current incident location and status:
  `"[SCENAR AUTO-ALERT] Emergency status updated to EN ROUTE for Building B, Room 204..."`

### TC-09: Simulation Broadcast Execution
- [ ] Tap "Simulate Alert Broadcast".
- [ ] Verify green status badge appears:
  `"✓ Automated Alerts Dispatched & Verified via Gateway"`
- [ ] Verify 3 timestamped transmission logs appear under notification log container:
  `"[HH:mm:ss] Sent to Emergency Contact (Jane Doe): ..."`
- [ ] Verify timeline appends: `"Automated Alert Dispatched"`.

---

## 5. Frontier Experiments (V4 Routing & V5 Bluetooth Rescue Mode)

### TC-10: V4 Emergency Facility Routing
- [ ] Verify 3 verified emergency facilities are listed:
  1. Metropolitan Trauma Center & ER (Level 1 Trauma)
  2. Station 4 Fire & Rescue (Heavy Rescue & HazMat)
  3. St. Jude Urgent Care
- [ ] Verify dynamic distance (km) and estimated transit time (mins) are calculated via Haversine formula.
- [ ] Tap on "Metropolitan Trauma Center & ER" card.
- [ ] Verify map navigation intent launches with hospital coordinates.

### TC-11: V5 Offline BLE Rescue Beacon
- [ ] Verify standby HUD displays:
  - `Service UUID: 0000FEAA-0000-1000-8000-00805F9B34FB`
  - Beacon ID, Hop Count (1), RSSI (-68 dBm), Payload size.
- [ ] Tap "Start BLE Distress Beacon Simulation".
- [ ] Verify status updates to active green:
  `"● BLE RESCUE BEACON: BROADCASTING (ACTIVE)"`
- [ ] Verify timeline records BLE Beacon Broadcast Started event.
- [ ] Tap button again to verify clean toggle back to Standby.

---

## 6. End-to-End Interoperability & Data Contract

### TC-12: End-to-End Victim Flow to Responder Console
- [ ] From Home screen, tap "Report emergency".
- [ ] Speak or type incident:
  `"Ceiling collapsed in Building B, room 204. Smoke in the hallway, two people injured."`
- [ ] Proceed through review screen and tap "Confirm and preview dispatch".
- [ ] Verify app transitions automatically into `ResponderFragment`.
- [ ] Verify `INC-2026-LIVE` appears in the incident queue with the exact victim report details.

### TC-13: Canonical Sample Report Reload
- [ ] Tap "Load Sample JSON" in responder header.
- [ ] Verify canonical sample report loads immediately (`INC-SAMPLE-CANONICAL`).
- [ ] Inspect "Interoperability JSON Contract" section at the bottom.
- [ ] Tap "Copy JSON" and verify Toast `"JSON copied to clipboard"`.

---

## 7. Build and Stability Gates

- [ ] Project compiles with zero warnings or errors:
  `.\gradlew.bat testDebugUnitTest assembleDebug`
- [ ] APK installs on emulator (Android 8.0 / API 26 through Android 15 / API 35).
- [ ] No ANR (Application Not Responding) or memory leaks during navigation.
- [ ] Offline operation: Works completely in airplane mode with no internet connection.
