# SCENAR — Demo Video Recording & Screenshot Guide
**Owner:** Teammate 2 (Responder Side and Product Delivery)  
**Target Video Duration:** 2 minutes 30 seconds (Max 3:00)  
**Deliverables:** High-Definition Demo MP4 + 10 High-Resolution Screenshots for Submission

---

## 1. Recording Setup & Technical Configuration

### Device & Emulator Specs
- **Device Profile:** Pixel 7 or Pixel 8 Pro
- **Resolution:** 1080 x 2400 (or 1440 x 3120), 420 dpi
- **OS Version:** Android 14 / 15 (API 34/35)
- **Theme:** System Light theme for UI contrast, dark navy tactical map view
- **Screen Recorder:** Android Studio Screen Recorder or OBS Studio (60 FPS, Bitrate 12 Mbps)
- **Audio:** High-quality condenser mic, room noise dampened, clear voiceover

### Pre-Recording Checklist
1. Build clean APK: `.\gradlew.bat assembleDebug`
2. Clear app storage: Settings > Apps > SCENAR > Clear Storage & Cache.
3. Set emulator clock to `12:00` or enable Demo Mode:
   ```bash
   adb shell settings put global sysui_demo_allowed 1
   adb shell am broadcast -a com.android.systemui.demo -e command enter
   adb shell am broadcast -a com.android.systemui.demo -e command clock -e hhmm 1200
   adb shell am broadcast -a com.android.systemui.demo -e command battery -e level 100 -e plugged false
   adb shell am broadcast -a com.android.systemui.demo -e command network -e wifi show -e level 4
   ```
4. Verify audio levels: peak at -6 dB, normalize to -14 LUFS.

---

## 2. Scene-by-Scene Storyboard & Voiceover Script

| Time | Scene | On-Screen Action | Voiceover Script | Focus Point |
|---|---|---|---|---|
| **0:00 - 0:20** | **The Problem** | Title slide + Distressed voice simulation graphic | *"In high-stress emergencies, victims and witnesses are in panic. Verbal calls are fragmented, missing critical location data, hazards, and vital medical history. This costs first responders precious minutes."* | Hook: The communication breakdown during disasters |
| **0:20 - 0:45** | **Victim Intake** | Open SCENAR > Tap "Report Emergency" > Type/Speak description | *"Meet SCENAR. SCENAR provides an accessible intake flow where distressed callers can speak or type what happened in their own words, capturing raw input without overwhelming them."* | Clean, accessible Material 3 design |
| **0:45 - 1:10** | **AI Extraction & Review** | Review Screen: AI extracts fields; user corrects room number; consents to medical flags | *"Our AI pipeline extracts incident type, people affected, hazards, and critical conditions into a structured report. Crucially: unknown values stay unknown. The victim reviews, corrects AI assumptions, and gives explicit consent before transmitting."* | Safety & human-in-the-loop review |
| **1:10 - 1:40** | **Responder Dashboard** | Tap "Confirm and Preview Dispatch" > Transition to `ResponderFragment` | *"Instantly, the report arrives at the SCENAR Dispatch Console. Triage priority is calculated automatically as CRITICAL. Responders immediately see: 2 victims affected, ceiling collapse hazards, and patient-consented penicillin allergy flags."* | Triage priority banner, structured cards |
| **1:40 - 2:05** | **Tactical Map & Status Lifecycle** | View Tactical Map > Tap "Advance Status" > Add field note | *"Responders view our custom Tactical Radar display with live GPS lock and estimated transit time. With one tap, units advance status from Dispatched to En Route, automatically logging immutable audit timestamps and field notes."* | Tactical radar canvas + Stepper workflow |
| **2:05 - 2:20** | **Contact Notification & Routing** | Tap "Simulate Alert Broadcast" > Show hospital routing | *"Simultaneously, SCENAR simulates automated SMS dispatch alerts to designated family emergency contacts and mutual aid units, while computing routing to the nearest Level 1 Trauma Center."* | V3 Notification & V4 Routing |
| **2:20 - 2:40** | **Offline BLE Mesh Experiment** | Toggle BLE Rescue Beacon > Show active broadcast HUD | *"When disasters wipe out cell towers, SCENAR's offline rescue mode broadcasts compressed BLE distress packets over local mesh hops to nearby rescuers without internet or cellular connectivity."* | V5 Zero-infrastructure disaster resilience |
| **2:40 - 2:50** | **Conclusion & Call to Action** | Return to summary / GitHub QR | *"SCENAR: Turning distressed voice into verified action, saving lives when every second counts. Thank you."* | Professional closing logo |

---

## 3. High-Resolution Screenshot Shot List (For Submission Deck & Devpost)

Capture and export the following 10 screenshots to `docs/screenshots/`:

1. `01_home_screen.png`: SCENAR welcome home screen with "Report emergency" and "Open Responder Dashboard" buttons.
2. `02_victim_input.png`: Distressed emergency reporting screen with voice and text intake options.
3. `03_victim_review_screen.png`: AI structured review screen showing human-in-the-loop correction and consented medical flags.
4. `04_responder_queue_triage.png`: Responder Console overview showing multi-incident feed with color-coded priority pills (`CRITICAL`, `HIGH`, `MEDIUM`).
5. `05_incident_structured_details.png`: Active incident view with Critical Conditions (Red), Scene Hazards (Amber), and Consented Medical Flags (Teal).
6. `06_status_lifecycle_stepper.png`: Live status stepper (`Received` → `Dispatched` → `En Route` → `On Scene` → `Resolved`) and timestamped dispatch timeline.
7. `07_tactical_radar_map.png`: Custom dark navy canvas radar view showing incident target beacon, unit responder dot, and hospital cross.
8. `08_automated_contact_notification.png`: Contact notification simulation showing SMS alert preview and verified gateway delivery audit logs.
9. `09_facility_routing_experiment.png`: V4 facility routing card displaying Metropolitan Trauma Center, Haversine distance, and transit ETA.
10. `10_offline_ble_beacon_mesh.png`: V5 Bluetooth Low Energy disaster beacon HUD showing active broadcast status, service UUID, and compact JSON payload.

---

## 4. Emergency Backup Demo Contingency
If live emulator mirroring fails during the presentation:
- Have the pre-recorded 1080p MP4 loaded in VLC or Windows Media Player, paused at 0:00 with spacebar ready.
- Store a local copy on a USB flash drive and on your phone.
