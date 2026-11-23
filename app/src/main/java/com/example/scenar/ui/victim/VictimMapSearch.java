package com.example.scenar.ui.victim;

record VictimMapSearch(String center, String query, boolean usesIncidentLocation) {
    static VictimMapSearch forHospitals(IncidentReview.Location location) {
        if (location.latitude() != null) {
            return new VictimMapSearch(coordinates(location), "hospital", true);
        }
        if (location.address() != null) {
            return new VictimMapSearch("0,0", "hospital near " + location.address(), true);
        }
        return new VictimMapSearch("0,0", "hospital", false);
    }

    static VictimMapSearch forIncident(IncidentReview.Location location) {
        if (location.latitude() != null) {
            return new VictimMapSearch("0,0", coordinates(location), true);
        }
        if (location.address() != null) {
            return new VictimMapSearch("0,0", location.address(), true);
        }
        return null;
    }

    private static String coordinates(IncidentReview.Location location) {
        return location.latitude() + "," + location.longitude();
    }
}
