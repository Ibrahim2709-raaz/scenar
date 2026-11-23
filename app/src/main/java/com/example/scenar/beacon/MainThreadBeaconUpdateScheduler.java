package com.example.scenar.beacon;

import android.os.Handler;
import android.os.Looper;

public final class MainThreadBeaconUpdateScheduler
        implements BeaconUpdateScheduler {

    private final Handler handler;

    public MainThreadBeaconUpdateScheduler() {
        this(new Handler(Looper.getMainLooper()));
    }

    MainThreadBeaconUpdateScheduler(Handler handler) {
        if (handler == null) {
            throw new IllegalArgumentException(
                    "A main-thread handler is required."
            );
        }

        this.handler = handler;
    }

    @Override
    public ScheduledTask schedule(
            Runnable task,
            long delayMillis
    ) {
        if (task == null) {
            throw new IllegalArgumentException(
                    "A scheduled beacon task is required."
            );
        }

        if (delayMillis < 0) {
            throw new IllegalArgumentException(
                    "Beacon delay cannot be negative."
            );
        }

        handler.postDelayed(task, delayMillis);

        return () -> handler.removeCallbacks(task);
    }
}