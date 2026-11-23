package com.example.scenar.ui.responder;

import com.example.scenar.model.DispatchRecord;
import com.example.scenar.model.DispatchStatus;
import com.example.scenar.model.IncidentReport;
import com.example.scenar.model.SharingConsent;
import com.example.scenar.repository.SimulatedDispatchRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Keeps the responder's local state (timeline, notes, simulated notification log)
 * for the records held by the shared {@link SimulatedDispatchRepository}.
 */
public final class ResponderQueue {
    /**
     * Consent used when the victim screen hands a report over. The victim screen does
     * not collect SharingConsent yet, so this shares the location the reporter chose to
     * add but withholds medical details and does not allow contacting anyone. Replace
     * it with the reporter's own choices once the victim screen collects them.
     */
    public static final SharingConsent VICTIM_HANDOFF_CONSENT =
            new SharingConsent(true, false, false);

    private static final ResponderQueue INSTANCE = new ResponderQueue();

    private final Map<String, ResponderIncident> incidents = new LinkedHashMap<>();
    private final Set<String> demoIds = new HashSet<>();
    private boolean demoSeeded;
    private IncidentReport lastHandedOverReport;

    ResponderQueue() {
    }

    public static ResponderQueue getInstance() {
        return INSTANCE;
    }

    /** Submits the fictional demo incidents once per app process. */
    public synchronized void ensureDemoIncidents(SimulatedDispatchRepository repository) {
        if (demoSeeded) return;
        demoSeeded = true;
        for (DemoIncidents.Demo demo : DemoIncidents.all()) {
            DispatchRecord record = repository.submit(demo.report, demo.consent);
            demoIds.add(record.getIncidentId());
            ResponderIncident incident = track(record);
            incident.addTimeline("Demo incident", ResponderCopy.DEMO_INCIDENT);
            for (DispatchStatus step : demo.advanceTo) {
                updateStatus(repository, incident, step);
            }
        }
    }

    /**
     * Submits the report the victim screen left in IncidentRepository, once.
     *
     * @return the new incident, or null when there was nothing new to hand over
     */
    public synchronized ResponderIncident ingestVictimHandoff(
            IncidentReport current,
            SimulatedDispatchRepository repository
    ) {
        if (current == null || current == lastHandedOverReport) return null;
        lastHandedOverReport = current;
        DispatchRecord record = repository.submit(current, VICTIM_HANDOFF_CONSENT);
        ResponderIncident incident = track(record);
        incident.addTimeline("Victim report", ResponderCopy.VICTIM_HANDOFF);
        return incident;
    }

    /** Submits a report with the given consent and starts tracking it. */
    public synchronized ResponderIncident submit(
            IncidentReport report,
            SharingConsent consent,
            SimulatedDispatchRepository repository
    ) {
        return track(repository.submit(report, consent));
    }

    /** All incidents in the shared repository, newest first. */
    public synchronized List<ResponderIncident> sync(SimulatedDispatchRepository repository) {
        List<ResponderIncident> result = new ArrayList<>();
        for (DispatchRecord record : repository.getIncidents()) {
            ResponderIncident incident = incidents.get(record.getIncidentId());
            if (incident == null) {
                incident = track(record);
            } else if (incident.getRecord() != record) {
                incident.applyRecord(record);
            }
            result.add(incident);
        }
        Collections.reverse(result);
        return result;
    }

    /**
     * Moves an incident to {@code next} through the shared repository.
     *
     * @return false when DispatchStatus does not allow the change
     */
    public synchronized boolean updateStatus(
            SimulatedDispatchRepository repository,
            ResponderIncident incident,
            DispatchStatus next
    ) {
        DispatchStatus current = incident.getStatus();
        if (next == null || current == next || !current.canTransitionTo(next)) return false;
        incident.applyRecord(repository.updateStatus(incident.getIncidentId(), next));
        return true;
    }

    public synchronized void reset() {
        incidents.clear();
        demoIds.clear();
        demoSeeded = false;
        lastHandedOverReport = null;
    }

    private ResponderIncident track(DispatchRecord record) {
        ResponderIncident incident =
                new ResponderIncident(record, demoIds.contains(record.getIncidentId()));
        incidents.put(record.getIncidentId(), incident);
        return incident;
    }
}
