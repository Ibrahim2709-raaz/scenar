package com.example.scenar.beacon;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class BeaconBroadcastControllerTest {
    @Test
    public void startsAndRefreshesPayloadAfterFiveSeconds() {
        FakeAdvertiser advertiser = new FakeAdvertiser();
        ManualScheduler scheduler = new ManualScheduler();
        AtomicLong clock = new AtomicLong(1_000);
        RecordingListener listener = new RecordingListener();

        BeaconBroadcastController controller = controller(
                advertiser,
                scheduler,
                clock,
                new AtomicInteger(100)
        );

        controller.start(
                31.520370,
                74.358749,
                listener
        );

        assertEquals(
                BeaconBroadcastController.State.BROADCASTING,
                controller.getState()
        );
        assertEquals(1, listener.started.size());
        assertEquals(1, advertiser.startedPayloads.size());
        assertTrue(scheduler.hasPendingTask());

        clock.set(1_005);
        scheduler.runPendingTask();

        assertEquals(2, advertiser.startedPayloads.size());
        assertEquals(1, listener.updated.size());
        assertEquals(
                1_005,
                listener.updated.get(0)
                        .generatedAtEpochSeconds()
        );

        assertEquals(
                listener.started.get(0).anonymousBeaconId(),
                listener.updated.get(0).anonymousBeaconId()
        );
    }

    @Test
    public void rotatesAnonymousIdAfterFiveMinutes() {
        FakeAdvertiser advertiser = new FakeAdvertiser();
        ManualScheduler scheduler = new ManualScheduler();
        AtomicLong clock = new AtomicLong(1_000);
        AtomicInteger ids = new AtomicInteger(200);
        RecordingListener listener = new RecordingListener();

        BeaconBroadcastController controller = controller(
                advertiser,
                scheduler,
                clock,
                ids
        );

        controller.start(null, null, listener);
        long originalId =
                listener.started.get(0).anonymousBeaconId();

        clock.set(1_300);
        scheduler.runPendingTask();

        long rotatedId =
                listener.updated.get(0).anonymousBeaconId();

        assertNotEquals(originalId, rotatedId);
    }

    @Test
    public void stopCancelsUpdateAndNotifiesListener() {
        FakeAdvertiser advertiser = new FakeAdvertiser();
        ManualScheduler scheduler = new ManualScheduler();
        AtomicLong clock = new AtomicLong(1_000);
        RecordingListener listener = new RecordingListener();

        BeaconBroadcastController controller = controller(
                advertiser,
                scheduler,
                clock,
                new AtomicInteger(300)
        );

        controller.start(null, null, listener);
        controller.stop();

        assertEquals(
                BeaconBroadcastController.State.STOPPED,
                controller.getState()
        );
        assertFalse(controller.isActive());
        assertEquals(1, listener.stoppedCount);
        assertFalse(scheduler.hasPendingTask());
    }

    @Test
    public void rejectsPartialCoordinates() {
        FakeAdvertiser advertiser = new FakeAdvertiser();
        ManualScheduler scheduler = new ManualScheduler();
        AtomicLong clock = new AtomicLong(1_000);
        RecordingListener listener = new RecordingListener();

        BeaconBroadcastController controller = controller(
                advertiser,
                scheduler,
                clock,
                new AtomicInteger(400)
        );

        controller.start(31.5, null, listener);

        assertEquals(1, listener.failures.size());
        assertEquals(
                BeaconBroadcastController.State.STOPPED,
                controller.getState()
        );
        assertTrue(advertiser.startedPayloads.isEmpty());
    }

    @Test
    public void advertiserFailureStopsController() {
        FakeAdvertiser advertiser = new FakeAdvertiser();
        advertiser.failureMessage =
                "Advertising is unsupported.";

        ManualScheduler scheduler = new ManualScheduler();
        AtomicLong clock = new AtomicLong(1_000);
        RecordingListener listener = new RecordingListener();

        BeaconBroadcastController controller = controller(
                advertiser,
                scheduler,
                clock,
                new AtomicInteger(500)
        );

        controller.start(null, null, listener);

        assertEquals(
                BeaconBroadcastController.State.STOPPED,
                controller.getState()
        );
        assertEquals(1, listener.failures.size());
        assertTrue(
                listener.failures.get(0)
                        .contains("unsupported")
        );
    }

    @Test
    public void lastPayloadIsAvailableWhileActive() {
        FakeAdvertiser advertiser = new FakeAdvertiser();
        ManualScheduler scheduler = new ManualScheduler();
        AtomicLong clock = new AtomicLong(1_000);
        RecordingListener listener = new RecordingListener();

        BeaconBroadcastController controller = controller(
                advertiser,
                scheduler,
                clock,
                new AtomicInteger(600)
        );

        controller.start(null, null, listener);

        assertNotNull(controller.getLastPayload());
    }

    private static BeaconBroadcastController controller(
            FakeAdvertiser advertiser,
            ManualScheduler scheduler,
            AtomicLong clock,
            AtomicInteger ids
    ) {
        BeaconSession session = new BeaconSession(
                new BeaconIdGenerator(ids::getAndIncrement),
                300
        );

        return new BeaconBroadcastController(
                advertiser,
                session,
                scheduler,
                clock::get
        );
    }

    private static final class FakeAdvertiser
            implements BeaconAdvertiser {

        private final List<BeaconPayload> startedPayloads =
                new ArrayList<>();

        private State state = State.STOPPED;
        private Callback callback;
        private String failureMessage;

        @Override
        public void start(
                BeaconPayload payload,
                Callback callback
        ) {
            if (failureMessage != null) {
                callback.onFailure(failureMessage);
                return;
            }

            this.callback = callback;
            startedPayloads.add(payload);
            state = State.ADVERTISING;
            callback.onStarted();
        }

        @Override
        public void stop() {
            Callback previousCallback = callback;
            callback = null;
            state = State.STOPPED;

            if (previousCallback != null) {
                previousCallback.onStopped();
            }
        }

        @Override
        public State getState() {
            return state;
        }
    }

    private static final class ManualScheduler
            implements BeaconUpdateScheduler {

        private Runnable pendingTask;
        private boolean cancelled;

        @Override
        public ScheduledTask schedule(
                Runnable task,
                long delayMillis
        ) {
            assertEquals(
                    SimulatedBeaconService
                            .BROADCAST_INTERVAL_MILLIS,
                    delayMillis
            );

            pendingTask = task;
            cancelled = false;

            return () -> {
                cancelled = true;
                pendingTask = null;
            };
        }

        boolean hasPendingTask() {
            return pendingTask != null && !cancelled;
        }

        void runPendingTask() {
            Runnable task = pendingTask;
            pendingTask = null;

            if (task != null && !cancelled) {
                task.run();
            }
        }
    }

    private static final class RecordingListener
            implements BeaconBroadcastController.Listener {

        private final List<BeaconPayload> started =
                new ArrayList<>();

        private final List<BeaconPayload> updated =
                new ArrayList<>();

        private final List<String> failures =
                new ArrayList<>();

        private int stoppedCount;

        @Override
        public void onBroadcastStarted(
                BeaconPayload payload
        ) {
            started.add(payload);
        }

        @Override
        public void onPayloadUpdated(
                BeaconPayload payload
        ) {
            updated.add(payload);
        }

        @Override
        public void onBroadcastStopped() {
            stoppedCount++;
        }

        @Override
        public void onBroadcastFailure(
                String message
        ) {
            failures.add(message);
        }
    }
}