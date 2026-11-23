package com.example.scenar.repository;

import com.example.scenar.model.EmergencyFacility;

import java.util.List;

public interface FacilityRepository {
    List<EmergencyFacility> findNearby(
            double latitude,
            double longitude,
            int limit
    );
}
