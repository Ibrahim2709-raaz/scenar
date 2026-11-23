package com.example.scenar.repository;

import com.example.scenar.model.EmergencyFacility;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public final class CuratedFacilityRepository implements FacilityRepository {
    private final List<EmergencyFacility> facilities;

    public CuratedFacilityRepository(List<EmergencyFacility> facilities) {
        this.facilities = facilities == null
                ? new ArrayList<>()
                : new ArrayList<>(facilities);
    }

    @Override
    public List<EmergencyFacility> findNearby(
            double latitude,
            double longitude,
            int limit
    ) {
        if (limit < 1) return new ArrayList<>();
        return facilities.stream()
                .sorted(Comparator.comparingDouble(
                        facility -> facility.distanceKmTo(latitude, longitude)
                ))
                .limit(limit)
                .collect(Collectors.toList());
    }
}
