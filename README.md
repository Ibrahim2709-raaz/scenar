# SCENAR

AI-assisted emergency incident reporting for victims, witnesses, and first responders.

## The problem

During an emergency, frightened or injured people may give incomplete, disorganized information. SCENAR turns that input into a clear incident report that the user can verify before it is shared with responders.

## Weekend MVP

The submission-ready target is an end-to-end flow:

1. Start an emergency report.
2. Speak or type what happened.
3. Use AI to extract victims, injuries, hazards, location clues, and missing information.
4. Ask short follow-up questions.
5. Let the user correct and confirm the report.
6. Submit it to a simulated responder dashboard.

Unknown information must remain unknown. SCENAR does not diagnose conditions or generate treatment advice.

## Milestones

- **V0 — Interactive concept:** clickable reporting and responder flow.
- **V1 — Working report:** guided questions, GPS, medical profile, editable report, and simulated submission without AI.
- **V2 — AI-powered MVP:** structured extraction, follow-up questions, validation, and offline/API failure handling.
- **V3 — Dispatch experience:** responder dashboard, status updates, consented medical flags, and contact notification simulation.
- **V4 — Routing:** nearby emergency facilities, maps, and static sourced guidance.
- **V5 — Offline rescue experiment:** Bluetooth Low Energy beacon and queued reports.

V2 is the weekend success threshold. Later versions begin only after the current version is tested, recorded, and stable.

## Team ownership

- **Technical lead:** architecture, report schema, AI pipeline, validation, integration, releases.
- **Victim app:** reporting flow, voice/text UI, GPS, review screen, accessibility, error states.
- **Responder experience:** dashboard, status updates, map, testing, demo video, and presentation assets.

## Safety boundaries

- The user reviews and corrects AI output before submission.
- AI uncertainty and missing data are shown explicitly.
- A deterministic form remains available if AI or internet access fails.
- Medical information is shared only with consent.
- Authority dispatch is simulated unless an approved real integration exists.
- Bluetooth rescue mode is experimental until validated on physical devices.

## Current status

The repository contains a native Android skeleton built with Java, XML layouts,
Material 3 and a single-activity fragment flow. A deterministic fake AI service
supports the complete prototype path without requiring an API key or network:

```text
Home → Report emergency → Review and correct → Responder incident view
```

### Shared contracts

- `IncidentReport` is the common model used by every feature area.
- `IncidentAiService` is the boundary between the interface and AI provider.
- `FakeIncidentAiService` supplies stable sample output during parallel work.
- `IncidentRepository` holds the current prototype report in memory.

### Build

Open the repository root in Android Studio, allow Gradle to sync, then run the
`app` configuration on an API 26 or newer emulator or Android device.

From a Windows terminal, the verification command is:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
```

The responder screen and dispatch status are simulations. The current build
does not contact emergency services.
