package com.example.scenar.ui.responder;

import com.example.scenar.model.DispatchStatus;

/**
 * User-facing wording for the responder dashboard that is built in code.
 *
 * Everything on this screen is a simulation. The wording must never claim that a
 * message went out, that anything was checked or confirmed, that a radio is
 * transmitting, or that data is real-time. ResponderSimulationLabelsTest enforces this.
 */
public final class ResponderCopy {
    private ResponderCopy() {
    }

    // Location
    public static final String LOCATION_UNAVAILABLE = "Location unavailable";
    public static final String LOCATION_UNAVAILABLE_DETAIL =
            "No GPS location in this report. Navigation, coordinate copying, facility"
                    + " distance and ETA, and location in the BLE preview are turned off.";
    public static final String LOCATION_TEXT_ONLY_PREFIX = "Reported address (text only): ";
    public static final String MAP_COORDINATES_PREFIX = "Reported GPS (demo map, not tracked): ";
    public static final String FACILITY_DISTANCE_UNAVAILABLE =
            "Distance unavailable (no GPS location)";
    public static final String FACILITY_DISTANCE_FORMAT =
            "%.1f km straight-line estimate (demo data)";
    public static final String FACILITY_ETA_UNAVAILABLE = "ETA unavailable (no GPS location)";
    public static final String FACILITY_ETA_FORMAT =
            "ETA about %d min (rough demo estimate at 35 km/h, straight line)";
    public static final String NAVIGATION_UNAVAILABLE =
            "Navigation is off because this report has no GPS location.";

    // Medical
    public static final String MEDICAL_TITLE = "Medical details (shared with consent)";
    public static final String MEDICAL_NOT_SHARED =
            "Medical details not shared: the reporter did not consent.";
    public static final String MEDICAL_NONE_REPORTED = "No medical details reported.";

    // Dispatch and status
    public static final String DEMO_UNIT = "Demo unit (simulated)";
    public static final String QUEUE_ADDED =
            "Added to the simulated dispatch queue. No emergency service is involved.";
    public static final String VICTIM_HANDOFF =
            "Handed over from the victim screen on this device (simulation).";
    public static final String DEMO_INCIDENT = "Demo incident with fictional details.";
    public static final String STATUS_CHANGE_TITLE_PREFIX = "Simulated status change: ";
    public static final String STATUS_CHANGE_DETAIL =
            "Changed on this device only. No responders were contacted.";
    public static final String STATUS_CHANGE_NOT_ALLOWED_PREFIX = "Not allowed in the simulation: ";
    public static final String STATUS_FINAL = "This simulated incident is closed.";
    public static final String NOTE_TITLE = "Responder note (simulation)";

    // Notifications
    public static final String NOTIFICATION_PREVIEW_PREFIX = "[SCENAR DEMO - SIMULATED, NOT DELIVERED] ";
    public static final String NOTIFICATION_LOG_FORMAT =
            "[%s] Simulated only, not delivered. Recipient: %s. Text: %s";
    public static final String NOTIFICATION_TIMELINE_TITLE = "Notification simulated";
    public static final String NOTIFICATION_TIMELINE_DETAIL_PREFIX =
            "Nothing left this device. Simulated recipient: ";
    public static final String NOTIFICATION_LOGGED_BADGE =
            "Simulation log updated. No message left this device.";
    public static final String NOTIFICATION_NO_CONSENT =
            "Notification simulation off: the reporter did not consent to contacting"
                    + " emergency contacts.";
    public static final String NOTIFICATION_DONE_TOAST =
            "Simulation log updated. No message left this device.";
    public static final String DEMO_CONTACT = "Demo Contact A (fictional, 555-0199)";
    public static final String DEMO_AGENCY = "Demo mutual aid desk (fictional)";

    // BLE
    public static final String BLE_PREVIEW_NOTE =
            "Preview only. Bluetooth is not used and nothing is transmitted.";
    public static final String BLE_LOCATION_LEFT_OUT =
            "Location left out of the preview: no GPS location in this report.";
    public static final String BLE_SIZE_FORMAT = "%d bytes (UTF-8)";

    // Facilities
    public static final String FACILITY_SOURCE_PREFIX = "Source: ";
    public static final String DIRECTIONS_DISCLAIMER =
            "Opens your maps app. SCENAR does not route or track units (demo).";

    public static String statusLabel(DispatchStatus status) {
        if (status == null) return "Unknown";
        switch (status) {
            case DRAFT:
                return "Draft";
            case SUBMITTED:
                return "Submitted (simulation)";
            case REVIEWING:
                return "Under review (simulation)";
            case DISPATCHED_SIMULATION:
                return "Dispatched (simulation only)";
            case CLOSED:
                return "Closed (simulation)";
            default:
                return status.name();
        }
    }
}
