package com.example.scenar.beacon;

public interface BeaconAdvertiser {
    enum State {
        STOPPED,
        STARTING,
        ADVERTISING
    }

    interface Callback {
        void onStarted();

        void onStopped();

        void onFailure(String message);
    }

    void start(
            BeaconPayload payload,
            Callback callback
    );

    void stop();

    State getState();

    default boolean isAdvertising() {
        return getState() == State.ADVERTISING;
    }
}