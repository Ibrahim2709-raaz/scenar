# SCENAR — Presentation Timing & Rehearsal Guide
**Owner:** Teammate 2 (Responder Side and Product Delivery)  
**Formats:** 3-Minute Standard Pitch & 5-Minute Extended Pitch  
**Rehearsal Target Date:** Prior to Monday Final Pitch Session

---

## 1. 3-Minute Strict Pitch Timing (Standard Hackathon Format)

| Time Window | Speaker | Section | Key Visual / Action | Word Count Target |
|---|---|---|---|---|
| **0:00 - 0:25** (25s) | **Teammate 2** | Problem & Hook | Slide 1 & 2: Panicked 911 calls, lost information | ~50 words |
| **0:25 - 0:55** (30s) | **Teammate 1** | Victim Intake Demo | Live App: Type/Speak emergency report, show AI extraction | ~65 words |
| **0:55 - 1:15** (20s) | **Technical Lead** | Verification Gate & Schema | Review screen: human correction, consent check, JSON schema | ~40 words |
| **1:15 - 2:05** (50s) | **Teammate 2** | Responder Console & Radar | Live App: Triage queue, Critical badges, Stepper advance, Tactical Radar, Navigation launch | ~110 words |
| **2:05 - 2:35** (30s) | **Teammate 2** | Notification & BLE Experiments | Live App: Simulate SMS alerts, V4 Routing, Toggle V5 BLE beacon | ~65 words |
| **2:35 - 3:00** (25s) | **All / Teammate 2** | Safety Guardrails & Closing | Slide 9 & 10: Human-in-loop, zero hallucinations, wrap-up | ~50 words |

**Total Words:** ~380 words (~130 words/minute — comfortable, clear speaking pace).

---

## 2. 5-Minute Pitch Timing (Extended / Q&A Friendly Format)

| Time Window | Speaker | Section | Detailed Content |
|---|---|---|---|
| **0:00 - 0:45** (45s) | **Teammate 2** | Introduction & Problem Deep Dive | Cognitive overload in crisis, real statistics on dispatch call delays, danger of omitted scene hazards. |
| **0:45 - 1:30** (45s) | **Teammate 1** | Victim Experience Demo | Walk through accessible intake, voice transcription, low-stress UI, offline draft preservation. |
| **1:30 - 2:15** (45s) | **Technical Lead** | AI Architecture & Safety Guardrails | Negative prompting rules, why unknown values stay unknown, zero medical advice policy, data schema interoperability. |
| **2:15 - 3:30** (75s) | **Teammate 2** | Responder Console Live Demo | Triage feed, critical priority calculation, interactive status stepper (`Dispatched` → `On Scene`), real-time audit logging, field notes, custom Canvas Tactical Radar. |
| **3:30 - 4:20** (50s) | **Teammate 2** | V3, V4 & V5 Milestones | Contact notification simulation, nearest trauma center routing, offline BLE rescue mesh beacon demonstration. |
| **4:20 - 5:00** (40s) | **Team** | Impact, NG911 Roadmap & Closing | NENA i3 / CAD integration roadmap, open-source safety commitment, thank you and open for Q&A. |

---

## 3. Team Rehearsal Schedule & Run-Through Protocol

### Session 1: Individual Dry Runs (30 minutes)
- Each team member reads through their section with a stopwatch.
- Cut filler words (*"um"*, *"like"*, *"basically"*, *"as you can see"*).
- Ensure smooth device-to-speaker handoffs.

### Session 2: Technical & Mirroring Test (20 minutes)
- Connect Android device/emulator to presentation display (scrcpy or HDMI).
- Test audio microphone levels and live slide clicker.
- Verify screen brightness at 100% and text size readable from 10 feet away.

### Session 3: Full Team Timed Rehearsals (40 minutes)
- **Run 1:** Baseline run (no stopping). Review time overrun.
- **Run 2:** Refine transitions between Teammate 1, Tech Lead, and Teammate 2.
- **Run 3:** Strict 3-minute hard-stop run (ring bell at 2:50).
- **Run 4:** Final polish run. Record video as backup.

---

## 4. Judge Q&A Preparation & Defensible Responses

### Q1: *"How do you prevent the AI from hallucinating injuries or medical advice?"*
**Speaker:** Technical Lead / Teammate 2  
**Answer:**  
> *"Our AI pipeline enforces strict negative constraints: it only extracts explicitly stated facts. Unknown fields are kept strictly null or added to missingInformation—they are never guessed. Furthermore, SCENAR enforces a mandatory Human-in-the-Loop review screen where the caller must visually verify and correct every extracted field before transmission. SCENAR never provides medical diagnoses or treatment recommendations."*

### Q2: *"What happens if the caller has no internet connection?"*
**Speaker:** Teammate 2  
**Answer:**  
> *"SCENAR was built with resilient fallbacks. If internet fails, our deterministic local engine processes the report offline without API calls. In catastrophic disaster scenarios where cellular infrastructure is wiped out, our V5 Bluetooth Low Energy (BLE) Rescue Mode broadcasts compressed distress packets across peer mesh hops to nearby rescuers."*

### Q3: *"How does this integrate with existing 911 Computer-Aided Dispatch (CAD) systems?"*
**Speaker:** Technical Lead  
**Answer:**  
> *"Our core model serializes into standard NENA i3-compliant JSON. Current 911 call centers are transitioning to Next Generation 911 (NG911), which accepts structured digital incident data via REST and WebSockets. SCENAR's IncidentReport contract maps directly to CAD CAD-to-CAD and incident exchange standards."*

### Q4: *"Why do you require explicit consent for medical flags?"*
**Speaker:** Teammate 1 / Teammate 2  
**Answer:**  
> *"Medical privacy is paramount. Even in emergencies, victims must have agency over their health data. Allergies, conditions, and medications stored in user profiles are flagged with an explicit consent toggle during the review screen. If the user unchecks consent, that data is instantly scrubbed from the dispatch payload."*

### Q5: *"Can first responders communicate back to the victim or their family?"*
**Speaker:** Teammate 2  
**Answer:**  
> *"Yes. In our Responder Console, every status advance (such as transitioning from Dispatched to En Route) triggers an automated notification simulation to verified emergency contacts and mutual aid units, keeping families informed and preventing panic calls from jamming 911 phone lines."*
