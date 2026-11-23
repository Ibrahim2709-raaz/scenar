package com.example.scenar.beacon;

public interface BeaconUpdateScheduler {
    interface ScheduledTask {
        void cancel();
    }

    ScheduledTask schedule(
            Runnable task,
            long delayMillis
    );
}