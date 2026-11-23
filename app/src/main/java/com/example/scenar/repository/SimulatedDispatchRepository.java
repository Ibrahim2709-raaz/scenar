package com.example.scenar.repository;

import com.example.scenar.model.DispatchRecord;
import com.example.scenar.model.DispatchStatus;
import com.example.scenar.model.IncidentReport;
import com.example.scenar.model.SharingConsent;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class SimulatedDispatchRepository {
    private static final SimulatedDispatchRepository INSTANCE =
            new SimulatedDispatchRepository();

    private final List<DispatchRecord> incidents = new ArrayList<>();

    private SimulatedDispatchRepository() {
    }

    public static SimulatedDispatchRepository getInstance() {
        return INSTANCE;
    }

    public synchronized DispatchRecord submit(
            IncidentReport report,
            SharingConsent consent
    ) {
        IncidentReport shared = IncidentSharingPolicy.filterForResponder(report, consent);
        DispatchRecord record = new DispatchRecord(
                UUID.randomUUID().toString(),
                shared,
                consent,
                DispatchStatus.SUBMITTED,
                Instant.now()
        );
        incidents.add(record);
        return record;
    }

    public synchronized List<DispatchRecord> getIncidents() {
        return new ArrayList<>(incidents);
    }

    public synchronized DispatchRecord updateStatus(
            String incidentId,
            DispatchStatus next
    ) {
        for (int index = 0; index < incidents.size(); index++) {
            DispatchRecord record = incidents.get(index);
            if (record.getIncidentId().equals(incidentId)) {
                DispatchRecord updated = record.transitionTo(next);
                incidents.set(index, updated);
                return updated;
            }
        }
        throw new IllegalArgumentException("Unknown incident ID");
    }

    public synchronized void clear() {
        incidents.clear();
    }
}
