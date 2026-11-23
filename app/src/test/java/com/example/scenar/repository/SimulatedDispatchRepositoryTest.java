package com.example.scenar.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import com.example.scenar.model.DispatchRecord;
import com.example.scenar.model.DispatchStatus;
import com.example.scenar.model.IncidentReport;
import com.example.scenar.model.SharingConsent;

import org.junit.After;
import org.junit.Test;

public class SimulatedDispatchRepositoryTest {
    private final SimulatedDispatchRepository repository =
            SimulatedDispatchRepository.getInstance();

    @After
    public void cleanUp() {
        repository.clear();
    }

    @Test
    public void followsTheSimulatedDispatchStateMachine() {
        IncidentReport report = new IncidentReport();
        report.setSummary("Practice incident");
        DispatchRecord submitted = repository.submit(
                report,
                SharingConsent.shareNothing()
        );

        assertEquals(DispatchStatus.SUBMITTED, submitted.getStatus());
        DispatchRecord reviewing = repository.updateStatus(
                submitted.getIncidentId(),
                DispatchStatus.REVIEWING
        );
        DispatchRecord dispatched = repository.updateStatus(
                submitted.getIncidentId(),
                DispatchStatus.DISPATCHED_SIMULATION
        );
        DispatchRecord closed = repository.updateStatus(
                submitted.getIncidentId(),
                DispatchStatus.CLOSED
        );

        assertEquals(DispatchStatus.REVIEWING, reviewing.getStatus());
        assertEquals(DispatchStatus.DISPATCHED_SIMULATION, dispatched.getStatus());
        assertEquals(DispatchStatus.CLOSED, closed.getStatus());
    }

    @Test
    public void rejectsAnUnsafeStatusJump() {
        IncidentReport report = new IncidentReport();
        report.setSummary("Practice incident");
        DispatchRecord submitted = repository.submit(
                report,
                SharingConsent.shareNothing()
        );

        assertThrows(IllegalStateException.class, () -> repository.updateStatus(
                submitted.getIncidentId(),
                DispatchStatus.DISPATCHED_SIMULATION
        ));
    }
}
