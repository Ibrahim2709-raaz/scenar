package com.example.scenar.ai;

public final class TestIncidentScenarios {

    private TestIncidentScenarios() {
        // Prevent this test-data class from being instantiated.
    }

    public static final String COMPLETE_INCIDENT =
            "Part of the ceiling collapsed in Building B, room 204. "
                    + "Two people are here. One is conscious with a leg "
                    + "injury, and the other is not responding. "
                    + "There is smoke in the hallway.";

    public static final String INCOMPLETE_INCIDENT =
            "Please help, someone is hurt and I don't know what to do.";

    public static final String CONTRADICTORY_INCIDENT =
            "There are two people, maybe three. "
                    + "I think this is Building A, "
                    + "but someone said Building C.";
}