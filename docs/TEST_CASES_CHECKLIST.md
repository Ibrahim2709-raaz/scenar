# SCENAR — Test-Case Verification Checklist
**Owner:** Teammate 2 (Responder Side and Product Delivery)  
**Verification commands:** `.\gradlew.bat testDebugUnitTest` then `.\gradlew.bat assembleDebug`

Everything on the responder dashboard is a simulation. No message, call, SMS, Bluetooth signal or dispatch leaves the device.

---

## 1. Automated Unit Tests (responder)

Fill in the Status column after running the Gradle commands above.

| ID | Test class | What it checks | Status |
|---|---|---|---|
| UT-01 | `ResponderUnitTest` | Advance button follows the shared `DispatchStatus` order; invalid transitions are refused; every status change is stored in and loaded from `SimulatedDispatchRepository` | |
| UT-02 | `ResponderUnitTest` | Victim handoff is submitted once; demo incidents are seeded once and marked as demo | |
| UT-03 | `ResponderUnitTest` | Priority calculation; notes; facility distance uses the shared `EmergencyFacility` | |
| UT-04 | `ResponderUnitTest` | Sample JSON parsing and round trip | |
| UT-05 | `MissingCoordinatesTest` | No GPS shows "Location unavailable"; navigation, coordinate copying, facility distance and ETA, and BLE location are all off; no fallback coordinates are used | |
| UT-06 | `MedicalConsentTest` | Withheld medical consent keeps medical flags out of the dashboard and BLE preview; only the filtered dispatch record is used | |
| UT-07 | `ResponderSimulationLabelsTest` | Status labels, timeline, notification log, facilities and BLE preview are marked as simulated or fictional; responder text never claims anything was sent, verified, broadcasting or live | |
| UT-08 | `BlePayloadPreviewTest` | BLE preview JSON is built with Gson: quotes, newlines and backslashes are escaped; size is measured in UTF-8; unknown values are left out | |

---

## 2. Responder Dashboard (manual)

### TC-01: Open the dashboard
- [ ] On Home, tap "Open Responder Dashboard".
- [ ] The simulation banner is visible at the top.
- [ ] The queue shows four demo incidents, each marked "(demo)".

### TC-02: Incident details
- [ ] Incident type, summary, people affected ("Unknown" when not given) and received time are shown.
- [ ] Critical conditions and hazards show "None reported." when empty.
- [ ] The "Sharing consent (simulation)" card lists what the reporter shared.

### TC-03: Medical consent
- [ ] Open the demo "Structural emergency": medical details are shown.
- [ ] Open the demo "Medical incident": "Medical details not shared: the reporter did not consent." No medical flags appear anywhere, including the JSON and BLE preview.

### TC-04: Simulated status changes
- [ ] Status steps are: Submitted, Under review, Dispatched (simulation only), Closed, each labelled as simulation.
- [ ] Only allowed changes are enabled (for example, Review is disabled once dispatched).
- [ ] Each change adds "Simulated status change: …" with "No responders were contacted." to the timeline.
- [ ] After Closed, the advance button is disabled.

### TC-05: Notes
- [ ] Add a note; it appears at the top of the timeline. Blank notes are ignored.

---

## 3. Location and Map

### TC-06: Incident with GPS
- [ ] The schematic map shows "SCHEMATIC DEMO MAP — SIMULATED, NOT REAL-TIME" and the reported coordinates.
- [ ] "Open reported location in maps" opens a maps app (or the browser) at those coordinates.
- [ ] "Copy Coordinates" copies the same coordinates.

### TC-07: Incident without GPS
- [ ] Open the demo "Fire or smoke" (no GPS).
- [ ] "Location unavailable" is shown; the map is hidden.
- [ ] Navigation and copy buttons are disabled.
- [ ] Facilities show "Distance unavailable (no GPS location)" and "ETA unavailable (no GPS location)", and the directions button is disabled.
- [ ] The BLE preview has no `lat`/`lon` and says location was left out.

---

## 4. Simulated Contact Notification

### TC-08: With consent
- [ ] Open the demo "Structural emergency" and tap "Add to simulation log".
- [ ] Two log entries appear, each "Simulated only, not delivered", for the fictional recipients.

### TC-09: Without consent
- [ ] Open the demo "Chemical hazard": the button is disabled and the screen says the reporter did not consent to contacting emergency contacts.

---

## 5. Facilities and BLE Preview

### TC-10: Demo facilities
- [ ] Three fictional facilities are listed with "Source: Fictional demo data, not a real facility".
- [ ] With GPS, each shows a straight-line distance and a rough demo ETA; tapping one selects it.

### TC-11: BLE payload preview
- [ ] The card says "Preview only. Bluetooth is not used and nothing is transmitted."
- [ ] The size is shown in bytes (UTF-8).

---

## 6. End to End

### TC-12: Victim report to responder
- [ ] On Home, tap "Report emergency", describe an incident, review and submit.
- [ ] The dashboard opens with the new incident selected (not marked "(demo)").
- [ ] Medical details from the victim screen are not shown (the victim screen does not collect medical consent yet).

### TC-13: Sample JSON
- [ ] Tap "Load Sample JSON": a new incident appears in the queue.
- [ ] "Copy JSON" copies the filtered report.

---

## 7. Build and Stability Gates

- [ ] `.\gradlew.bat testDebugUnitTest` passes.
- [ ] `.\gradlew.bat assembleDebug` succeeds.
- [ ] The APK installs and runs on an API 26+ emulator.
- [ ] The responder dashboard works in airplane mode.
