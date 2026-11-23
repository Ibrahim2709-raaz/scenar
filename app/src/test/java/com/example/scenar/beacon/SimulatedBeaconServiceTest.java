package com.example.scenar.beacon;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class SimulatedBeaconServiceTest {
    @Test
    public void serviceClearlyReportsSimulationState() {
        SimulatedBeaconService service =
                new SimulatedBeaconService();

        assertFalse(service.isRunning());
        assertEquals(
                SimulatedBeaconService.State.STOPPED,
                service.getState()
        );

        service.start(
                BeaconPayload.emergencyWithoutLocation(
                        123,
                        1_795_000_000L
                )
        );

        assertTrue(service.isRunning());
        assertEquals(
                SimulatedBeaconService.State.BROADCASTING_SIMULATION,
                service.getState()
        );
        assertTrue(
                service.getStatusLabel()
                        .toLowerCase()
                        .contains("simulation")
        );
        assertEquals(
                BeaconPayloadCodec.PAYLOAD_SIZE_BYTES,
                service.createNextPacket().length
        );

        service.stop();

        assertFalse(service.isRunning());
        assertEquals(
                SimulatedBeaconService.State.STOPPED,
                service.getState()
        );
    }

    @Test
    public void packetCannotBeCreatedBeforeStarting() {
        SimulatedBeaconService service =
                new SimulatedBeaconService();

        assertThrows(
                IllegalStateException.class,
                service::createNextPacket
        );
    }

    @Test
    public void intervalMatchesFiveSecondRequirement() {
        assertEquals(
                5_000L,
                SimulatedBeaconService.BROADCAST_INTERVAL_MILLIS
        );
    }
}