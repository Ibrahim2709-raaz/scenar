package com.example.scenar.ui.responder;

import com.example.scenar.model.EmergencyFacility;
import com.example.scenar.repository.CuratedFacilityRepository;
import com.example.scenar.repository.FacilityRepository;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Fictional facilities for demos, served through the shared FacilityRepository. */
public final class DemoFacilities {
    static final String SOURCE = "Fictional demo data, not a real facility";
    private static final LocalDate REVIEWED = LocalDate.of(2026, 11, 23);

    private static final List<EmergencyFacility> FACILITIES = Collections.unmodifiableList(
            Arrays.asList(
                    new EmergencyFacility("demo-trauma-center", "Demo Trauma Center",
                            "750 Example Way (fictional)", 37.78120, -122.41150,
                            SOURCE, REVIEWED),
                    new EmergencyFacility("demo-fire-station-4", "Demo Fire Station 4",
                            "220 Sample Blvd (fictional)", 37.77210, -122.42500,
                            SOURCE, REVIEWED),
                    new EmergencyFacility("demo-urgent-care", "Demo Urgent Care",
                            "410 Placeholder Ave (fictional)", 37.77800, -122.42800,
                            SOURCE, REVIEWED)
            ));

    private DemoFacilities() {
    }

    /** All demo facilities, in a fixed order, for when there is no GPS to sort by. */
    public static List<EmergencyFacility> all() {
        return FACILITIES;
    }

    public static FacilityRepository repository() {
        return new CuratedFacilityRepository(FACILITIES);
    }
}
