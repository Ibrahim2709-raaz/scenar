# SCENAR — Presentation Deck & Pitch Script
**Slide Deck Draft — Hackathon Final Presentation**  
**Presenter:** SCENAR Team (Teammate 1, Technical Lead, Teammate 2)  
**Format:** 10 Slides (3 to 5 minutes delivery)

---

## Slide 1: Title & Hook
- **Slide Title:** SCENAR
- **Subtitle:** AI-Assisted Incident Ingestion & Dispatch for High-Stress Emergencies
- **Team:**
  - *Victim App Lead:* Intake UI, Voice & Accessibility
  - *Technical Lead:* Architecture, AI Pipeline & Interoperability Schema
  - *Product Delivery & Responder Lead:* Dispatcher Dashboard, Map, Contact Simulation & Release Gates
- **Speaker (Teammate 2):**  
  > *"When an emergency strikes, seconds dictate survival. Yet today, emergency dispatch relies on panicked, fragmented phone calls where vital details are lost. Today, we introduce SCENAR: an end-to-end emergency communication platform that turns distressed descriptions into structured incident reports that the caller reviews before a simulated hand-off to responders."*

---

## Slide 2: The Critical Problem
- **Headline:** Emergency Ingestion is Broken During High Panic
- **Bullet Points:**
  - **Cognitive Overload:** Injured or frightened callers struggle to provide coherent answers.
  - **Critical Omissions:** Hazards (downed power lines, toxic smoke, chemical leaks) are frequently omitted.
  - **Missing Location Precision:** Callers say "near the stairs" or "building B" without room numbers or GPS locks.
  - **911 Call Center Congestion:** Dispatchers spend 2 to 4 minutes per call attempting to extract basics.
- **Speaker (Teammate 1):**  
  > *"During panic, human memory and verbal clarity degrade. 911 dispatchers must manually de-escalate callers while frantically typing notes. Crucial hazards—like smoke in the stairwell—often get missed until firefighters are already inside."*

---

## Slide 3: The SCENAR Solution
- **Headline:** Guided Ingestion + Controlled AI + Tactical Dispatch
- **Visual Diagram:**
  `Distressed Speech/Text` ➔ `AI Extraction Pipeline` ➔ `Human Verification Gate` ➔ `Tactical Dispatch Console`
- **Key Differentiators:**
  1. **Low-Stress Intake:** Multimodal voice/text input designed for motor-impaired and panicked users.
  2. **Strict Guardrails:** Unknown information stays unknown; zero medical diagnosis or hallucinations.
  3. **Human-in-the-Loop Review:** The victim or bystander verifies and corrects AI interpretations before transmission.
  4. **Instant First-Responder Triage:** Prioritized dashboard with simulated status changes, a schematic demo map, and a simulated contact notification log.
- **Speaker (Technical Lead):**  
  > *"SCENAR doesn't replace the human—it supercharges both caller and dispatcher. Our AI pipeline structures raw descriptions into data fields, but enforces a strict human-in-the-loop gate before dispatch."*

---

## Slide 4: System Architecture & Data Contract
- **Headline:** Robust, Offline-Ready Architecture
- **Architecture Highlights:**
  - **Unified Data Contract:** Standardized `IncidentReport` JSON schema shared across all app components.
  - **Deterministic AI Boundary:** Pluggable `IncidentAiService` with local fallback when connectivity drops.
  - **Privacy by Design:** Patient medical flags (allergies, pacemaker, asthma) reach the responder view **only** with the reporter's consent.
  - **Zero-Dependency Demo Map:** A schematic Canvas map that needs no API key. It is not a real map and is labelled as a demo.
- **Speaker (Technical Lead):**  
  > *"Our architecture relies on an interoperable JSON schema. Whether parsed by cloud LLMs, local on-device models, or deterministic fallbacks, the contract remains rock-solid."*

---

## Slide 5: Live Demo — Victim Emergency Ingestion
- **Headline:** From Panic to Precision in Under 30 Seconds
- **Live Demo Action:**
  - Open SCENAR > "Report emergency".
  - Distressed input: *"Ceiling collapsed in Building B room 204, smoke in the hall, two people hurt, one not responding."*
  - Show AI structured extraction:
    - Incident Type: Structural Emergency
    - People Affected: 2
    - Hazards: Smoke, Debris
    - Critical Conditions: Unresponsive victim
  - Victim corrects room detail and consents to penicillin allergy flag.
- **Speaker (Teammate 1):**  
  > *"Notice how the intake adapts. The victim simply speaks what they observe. The pipeline extracts critical conditions and hazards, but leaves room for immediate correction before the report is submitted."*

---

## Slide 6: Live Demo — First Responder Dispatch Console
- **Headline:** Triage & Simulated Dispatch
- **Live Demo Action:**
  - Switch to Responder Console (`ResponderFragment`).
  - Active incident card appears immediately with **CRITICAL PRIORITY** badge.
  - Highlights:
    - 🚨 Critical Conditions (Red Card)
    - ⚠️ Scene Hazards (Amber Warning)
    - 🛡️ Medical details, shown only when the reporter consented
    - ❓ Missing Information (Blue Verification Notice)
- **Speaker (Teammate 2):**  
  > *"On the dispatcher and responder side, the report appears in the simulated queue. Priority is computed as CRITICAL. Responders don't read walls of unstructured text—they get high-visibility cards for immediate hazards and vital medical flags."*

---

## Slide 7: Live Demo — Status Stepper, Demo Map & Field Notes
- **Headline:** Simulated Incident Lifecycle
- **Live Demo Action:**
  - Tap "Advance Status" (`Submitted` ➔ `Under review` ➔ `Dispatched (simulation only)` ➔ `Closed`).
  - Demonstrate instant Dispatch Stepper updates and chronological audit logging.
  - Add field note: *"Perimeter secured. Hallway smoke cleared."*
  - Show the **schematic demo map**: show the schematic demo map with the reported coordinates, then open the demo incident without GPS to show "Location unavailable".
  - Tap "Navigate in Google Maps" to launch turn-by-turn navigation intent.
- **Speaker (Teammate 2):**  
  > *"With one tap, the responder moves the incident through simulated statuses, and each change is logged with a timestamp. The schematic map shows the reported location, and when a report has no GPS, the app says so and turns navigation off instead of guessing."*

---

## Slide 8: Automated Notifications & Frontier Experiments
- **Headline:** V3 Notification Simulation, V4 Facilities & V5 BLE Preview
- **Three Core Innovations:**
  1. **Contact Notification Simulation (V3):** Shows what a message to emergency contacts could say, only with the reporter's consent. Nothing is delivered.
  2. **Nearby Facilities (V4):** Straight-line distance to fictional demo facilities, only when the report has GPS.
  3. **BLE Payload Preview (V5):** The compact JSON an offline Bluetooth mode could carry, shown on screen only. Bluetooth is not used.
- **Speaker (Teammate 2):**  
  > *"Beyond the core flow, we prototyped three ideas, all clearly simulated: a preview of what emergency contacts could be told, distances to nearby facilities when GPS is available, and a preview of a compact payload for a future offline Bluetooth mode. None of them contacts anyone or transmits anything."*

---

## Slide 9: Safety, Ethics & Privacy Guardrails
- **Headline:** Built for Life-or-Death Accountability
- **Non-Negotiables:**
  - ❌ **No AI Medical Diagnosis:** SCENAR records reported symptoms; it never guesses clinical diagnoses or treatments.
  - ❌ **No Hallucinated Fields:** Unknown locations or victim counts remain explicitly marked as `"Unknown"`.
  - 🛡️ **Explicit Consent for Health Data:** Medical flags are transmitted only after user opt-in.
  - 🔒 **Simulated Authority Dispatch:** Clearly marked simulated dispatch to prevent accidental emergency services load during testing.
- **Speaker (Technical Lead):**  
  > *"In emergency software, hallucination is a safety hazard. We engineered strict negative constraints: unknown values remain unknown, AI outputs are never authoritative until human-confirmed, and health data is protected by strict consent."*

---

## Slide 10: Milestones & Future Roadmap
- **Headline:** From Hackathon MVP to Next-Gen 911 Standard
- **Accomplished This Weekend:**
  - ✅ V0 — Interactive Concept & Skeleton Flow
  - ✅ V1 — Guided Reporting, Medical Profile & Editable Review
  - ✅ V2 — AI-Powered Extraction & Safe Deterministic Fallback
  - ✅ V3 — Responder Dashboard, Simulated Status Changes & Contact Notification Simulation
  - ✅ V4 — Schematic Demo Map & Nearby Facilities (demo data)
  - ✅ V5 — BLE Payload Preview (simulation only)
- **Next Steps:**
  - Integration with CAD (Computer-Aided Dispatch) standard APIs (NENA i3 / NG911).
  - Prototype a real offline Bluetooth mode and test it on physical devices.
- **Speaker (Teammate 2):**  
  > *"SCENAR bridges the gap between human panic and emergency action. All unit tests pass, the build compiles cleanly, and the complete flow is ready for demonstration today. Thank you!"*
