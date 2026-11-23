package com.example.scenar.repository;

import static org.junit.Assert.assertEquals;

import com.example.scenar.model.EmergencyFacility;

import org.junit.Test;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

public class CuratedFacilityRepositoryTest {
    @Test
    public void returnsFacilitiesNearestFirstAndHonoursLimit() {
        EmergencyFacility near = facility("near", 25.01, 55.01);
        EmergencyFacility far = facility("far", 26.0, 56.0);
        CuratedFacilityRepository repository =
                new CuratedFacilityRepository(Arrays.asList(far, near));

        List<EmergencyFacility> result = repository.findNearby(25.0, 55.0, 1);

        assertEquals(1, result.size());
        assertEquals("near", result.get(0).id());
    }

    private EmergencyFacility facility(String id, double latitude, double longitude) {
        return new EmergencyFacility(
                id,
                "Practice Hospital " + id,
                "Practice address",
                latitude,
                longitude,
                "Curated hackathon dataset",
                LocalDate.of(2026, 11, 23)
        );
    }
}
