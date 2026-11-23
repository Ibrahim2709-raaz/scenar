package com.example.scenar.ui.responder;

import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.scenar.MainActivity;
import com.example.scenar.R;
import com.example.scenar.model.DispatchStatus;
import com.example.scenar.model.EmergencyFacility;
import com.example.scenar.model.IncidentReport;
import com.example.scenar.model.SharingConsent;
import com.example.scenar.repository.FacilityRepository;
import com.example.scenar.repository.IncidentRepository;
import com.example.scenar.repository.SimulatedDispatchRepository;
import com.google.android.material.card.MaterialCardView;

import java.util.List;
import java.util.Locale;

public class ResponderFragment extends Fragment {
    private static final int NEARBY_FACILITY_LIMIT = 3;
    private static final String ARG_INCIDENT_ID = "incident_id";

    private final SimulatedDispatchRepository dispatch = SimulatedDispatchRepository.getInstance();
    private final ResponderQueue queue = ResponderQueue.getInstance();
    private final FacilityRepository facilityRepository = DemoFacilities.repository();

    private List<ResponderIncident> incidentList;
    private ResponderIncident activeIncident;
    private EmergencyFacility selectedFacility;

    public static ResponderFragment forIncident(@Nullable String incidentId) {
        ResponderFragment fragment = new ResponderFragment();
        Bundle arguments = new Bundle();
        arguments.putString(ARG_INCIDENT_ID, incidentId);
        fragment.setArguments(arguments);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.fragment_responder, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        queue.ensureDemoIncidents(dispatch);
        incidentList = queue.sync(dispatch);
        activeIncident = incidentList.isEmpty() ? null : incidentList.get(0);
        String incidentId = getArguments() == null ? null : getArguments().getString(ARG_INCIDENT_ID);
        for (ResponderIncident incident : incidentList) {
            if (incident.getIncidentId().equals(incidentId)) {
                activeIncident = incident;
                break;
            }
        }

        setupQueueTabs(view);
        bindActiveIncident(view);
        setupActionListeners(view);
    }

    private void refreshQueue() {
        incidentList = queue.sync(dispatch);
    }

    private void setupQueueTabs(View root) {
        LinearLayout queueContainer = root.findViewById(R.id.incident_queue_container);
        queueContainer.removeAllViews();

        for (ResponderIncident incident : incidentList) {
            final boolean isSelected = incident == activeIncident;

            MaterialCardView card = new MaterialCardView(requireContext());
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            lp.rightMargin = dpToPx(8);
            card.setLayoutParams(lp);
            card.setRadius(dpToPx(8));
            card.setStrokeWidth(dpToPx(1));
            card.setCardBackgroundColor(isSelected ? 0xFF0F172A : 0xFFFFFFFF);
            card.setStrokeColor(isSelected ? 0xFF38BDF8 : 0xFFCBD5E1);

            LinearLayout content = new LinearLayout(requireContext());
            content.setOrientation(LinearLayout.VERTICAL);
            content.setPadding(dpToPx(12), dpToPx(8), dpToPx(12), dpToPx(8));

            TextView idText = new TextView(requireContext());
            idText.setText(incident.getDisplayId() + (incident.isDemo() ? " (demo)" : ""));
            idText.setTextSize(12f);
            idText.setTypeface(idText.getTypeface(), Typeface.BOLD);
            idText.setTextColor(isSelected ? 0xFF38BDF8 : 0xFF64748B);
            content.addView(idText);

            TextView typeText = new TextView(requireContext());
            String type = incident.getReport().getIncidentType();
            typeText.setText(isBlank(type) ? "Incident type unknown" : type);
            typeText.setTextSize(13f);
            typeText.setTypeface(typeText.getTypeface(), Typeface.BOLD);
            typeText.setTextColor(isSelected ? Color.WHITE : 0xFF0F172A);
            content.addView(typeText);

            TextView statusText = new TextView(requireContext());
            statusText.setText(ResponderCopy.statusLabel(incident.getStatus()));
            statusText.setTextSize(11f);
            statusText.setTextColor(DispatchStatusStyle.color(incident.getStatus()));
            content.addView(statusText);

            card.addView(content);
            card.setOnClickListener(v -> {
                activeIncident = incident;
                setupQueueTabs(root);
                bindActiveIncident(root);
            });

            queueContainer.addView(card);
        }
    }

    private void bindActiveIncident(View root) {
        if (activeIncident == null) return;
        IncidentReport report = activeIncident.getReport();
        IncidentLocation location = activeIncident.getLocation();

        setText(root, R.id.responder_incident_id, activeIncident.getDisplayId() + " • "
                + ResponderCopy.DEMO_UNIT
                + (activeIncident.isDemo() ? " • Demo data" : ""));
        TextView priorityBadge = root.findViewById(R.id.responder_priority_badge);
        priorityBadge.setText(activeIncident.getPriority().getLabel());
        priorityBadge.setTextColor(activeIncident.getPriority().getColor());
        priorityBadge.setBackgroundColor(activeIncident.getPriority().getBackgroundColor());

        setText(root, R.id.responder_incident_type, report.getIncidentType());
        setText(root, R.id.responder_summary, isBlank(report.getSummary())
                ? "No incident summary provided."
                : report.getSummary());

        String people = report.getPeopleAffected() == null
                ? "Unknown"
                : report.getPeopleAffected() + " " + (report.getPeopleAffected() == 1 ? "person" : "people");
        setText(root, R.id.responder_people, people);
        setText(root, R.id.responder_time, activeIncident.getReceivedTime());
        setText(root, R.id.responder_location, isBlank(report.getLocationDescription())
                ? ResponderCopy.LOCATION_UNAVAILABLE
                : report.getLocationDescription());

        LinearLayout details = root.findViewById(R.id.responder_structured_details);
        details.removeAllViews();

        if (location.isAvailable()) {
            addDetailCard(details, "GPS coordinates (as reported)",
                    location.coordinatesText(), 0xFF0284C7, 0xFFE0F2FE);
        } else {
            addDetailCard(details, "GPS location",
                    ResponderCopy.LOCATION_UNAVAILABLE + "\n" + ResponderCopy.LOCATION_UNAVAILABLE_DETAIL,
                    0xFFB45309, 0xFFFEF3C7);
        }

        if (!report.getCriticalConditions().isEmpty()) {
            addDetailCard(details, "Critical conditions",
                    bullets(report.getCriticalConditions()), 0xFFDC2626, 0xFFFEE2E2);
        } else {
            addDetailCard(details, "Critical conditions", "None reported.",
                    0xFF64748B, 0xFFF8FAFC);
        }

        if (!report.getHazards().isEmpty()) {
            addDetailCard(details, "Scene hazards",
                    bullets(report.getHazards()), 0xFFD97706, 0xFFFEF3C7);
        } else {
            addDetailCard(details, "Scene hazards", "None reported.",
                    0xFF64748B, 0xFFF8FAFC);
        }

        // Medical flags come only from the filtered dispatch record.
        List<String> medicalFlags = activeIncident.getMedicalFlagsToShow();
        if (!medicalFlags.isEmpty()) {
            addDetailCard(details, ResponderCopy.MEDICAL_TITLE,
                    bullets(medicalFlags), 0xFF0D9488, 0xFFCCFBF1);
        } else {
            addDetailCard(details, "Medical details",
                    activeIncident.getMedicalEmptyMessage(), 0xFF64748B, 0xFFF8FAFC);
        }

        SharingConsent consent = activeIncident.getConsent();
        addDetailCard(details, "Sharing consent (simulation)",
                "Location: " + (consent.shareLocation() ? "shared" : "not shared")
                        + "\nMedical details: " + (consent.shareMedicalFlags() ? "shared" : "not shared")
                        + "\nEmergency contacts: " + (consent.notifyEmergencyContacts() ? "may be contacted" : "not to be contacted"),
                0xFF475569, 0xFFF1F5F9);

        if (!report.getMissingInformation().isEmpty()) {
            addDetailCard(details, "Missing information (needs confirmation)",
                    bullets(report.getMissingInformation()), 0xFF64748B, 0xFFF1F5F9);
        }

        if (!report.getUncertainties().isEmpty()) {
            addDetailCard(details, "Uncertainties (needs confirmation)",
                    bullets(report.getUncertainties()), 0xFF7C3AED, 0xFFEDE9FE);
        }

        updateStatusViews(root);
        bindMapAndLocation(root, report, location);
        updateNotificationPreview(root);
        bindFacilities(root, location);
        bindBlePreview(root, report);

        setText(root, R.id.json_preview_text, SampleIncidentProvider.toJson(report));
    }

    private void bindMapAndLocation(View root, IncidentReport report, IncidentLocation location) {
        View missingCard = root.findViewById(R.id.missing_coordinates_card);
        TacticalMapView mapView = root.findViewById(R.id.tactical_map_view);
        TextView coordText = root.findViewById(R.id.map_coordinates_text);
        View navigateButton = root.findViewById(R.id.btn_open_navigation);
        View copyButton = root.findViewById(R.id.btn_copy_coordinates);

        navigateButton.setEnabled(location.canNavigate());
        copyButton.setEnabled(location.canCopyCoordinates());

        if (location.isAvailable()) {
            missingCard.setVisibility(View.GONE);
            mapView.setVisibility(View.VISIBLE);
            mapView.setLocation(location.getLatitude(), location.getLongitude(),
                    isBlank(report.getLocationDescription()) ? "Incident" : report.getLocationDescription());
            coordText.setText(ResponderCopy.MAP_COORDINATES_PREFIX + location.coordinatesText());
        } else {
            mapView.clearLocation();
            mapView.setVisibility(View.GONE);
            missingCard.setVisibility(View.VISIBLE);
            TextView missingDesc = root.findViewById(R.id.missing_coordinates_desc);
            String detail = ResponderCopy.LOCATION_UNAVAILABLE_DETAIL;
            if (!isBlank(report.getLocationDescription())) {
                detail += "\n" + ResponderCopy.LOCATION_TEXT_ONLY_PREFIX + report.getLocationDescription();
            }
            missingDesc.setText(detail);
            coordText.setText(ResponderCopy.LOCATION_UNAVAILABLE);
        }
    }

    private void updateStatusViews(View root) {
        DispatchStatus current = activeIncident.getStatus();

        TextView statusPill = root.findViewById(R.id.responder_status_pill);
        statusPill.setText(ResponderCopy.statusLabel(current).toUpperCase(Locale.ROOT));
        statusPill.setTextColor(DispatchStatusStyle.color(current));
        statusPill.setBackgroundColor(DispatchStatusStyle.backgroundColor(current));

        LinearLayout stepperContainer = root.findViewById(R.id.status_stepper_container);
        stepperContainer.removeAllViews();
        int currentIndex = stepIndex(current);
        for (int i = 0; i < DispatchStatusStyle.RESPONDER_STEPS.length; i++) {
            DispatchStatus step = DispatchStatusStyle.RESPONDER_STEPS[i];

            TextView stepView = new TextView(requireContext());
            stepView.setText(ResponderCopy.statusLabel(step));
            stepView.setTextSize(11f);
            stepView.setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            lp.setMarginEnd(dpToPx(4));
            stepView.setLayoutParams(lp);

            if (i == currentIndex) {
                stepView.setBackgroundColor(DispatchStatusStyle.color(step));
                stepView.setTextColor(Color.WHITE);
                stepView.setTypeface(stepView.getTypeface(), Typeface.BOLD);
            } else if (i < currentIndex) {
                stepView.setBackgroundColor(0xFFE2E8F0);
                stepView.setTextColor(0xFF334155);
            } else {
                stepView.setBackgroundColor(0xFFF1F5F9);
                stepView.setTextColor(0xFF94A3B8);
            }
            stepperContainer.addView(stepView);
        }

        setStatusButtonEnabled(root, R.id.btn_status_review, DispatchStatus.REVIEWING);
        setStatusButtonEnabled(root, R.id.btn_status_dispatch, DispatchStatus.DISPATCHED_SIMULATION);
        setStatusButtonEnabled(root, R.id.btn_status_close, DispatchStatus.CLOSED);
        root.findViewById(R.id.btn_advance_status)
                .setEnabled(DispatchStatusStyle.next(current) != null);

        bindTimeline(root);
    }

    private void setStatusButtonEnabled(View root, int buttonId, DispatchStatus target) {
        DispatchStatus current = activeIncident.getStatus();
        root.findViewById(buttonId).setEnabled(current != target && current.canTransitionTo(target));
    }

    private static int stepIndex(DispatchStatus status) {
        for (int i = 0; i < DispatchStatusStyle.RESPONDER_STEPS.length; i++) {
            if (DispatchStatusStyle.RESPONDER_STEPS[i] == status) return i;
        }
        return -1;
    }

    private void bindTimeline(View root) {
        LinearLayout timelineContainer = root.findViewById(R.id.timeline_container);
        timelineContainer.removeAllViews();

        for (TimelineEntry entry : activeIncident.getTimeline()) {
            LinearLayout row = new LinearLayout(requireContext());
            row.setOrientation(LinearLayout.VERTICAL);
            row.setPadding(0, dpToPx(4), 0, dpToPx(4));

            TextView header = new TextView(requireContext());
            header.setText(String.format(Locale.ROOT, "[%s] %s", entry.getTimestamp(), entry.getTitle()));
            header.setTextSize(12f);
            header.setTypeface(header.getTypeface(), Typeface.BOLD);
            header.setTextColor(0xFF0F172A);
            row.addView(header);

            TextView detail = new TextView(requireContext());
            detail.setText(entry.getDetail());
            detail.setTextSize(12f);
            detail.setTextColor(0xFF475569);
            row.addView(detail);

            timelineContainer.addView(row);
        }
    }

    private String notificationMessage() {
        IncidentReport report = activeIncident.getReport();
        StringBuilder message = new StringBuilder(ResponderCopy.NOTIFICATION_PREVIEW_PREFIX)
                .append("Incident status: ")
                .append(ResponderCopy.statusLabel(activeIncident.getStatus()))
                .append('.');
        if (!isBlank(report.getLocationDescription())) {
            message.append(" Reported location: ").append(report.getLocationDescription()).append('.');
        }
        return message.toString();
    }

    private void updateNotificationPreview(View root) {
        TextView preview = root.findViewById(R.id.notification_preview_text);
        preview.setText(notificationMessage());

        root.findViewById(R.id.btn_simulate_notification)
                .setEnabled(activeIncident.canSimulateNotification());

        TextView badge = root.findViewById(R.id.notification_status_badge);
        if (!activeIncident.canSimulateNotification()) {
            badge.setVisibility(View.VISIBLE);
            badge.setText(ResponderCopy.NOTIFICATION_NO_CONSENT);
        } else if (activeIncident.hasSimulatedNotifications()) {
            badge.setVisibility(View.VISIBLE);
            badge.setText(ResponderCopy.NOTIFICATION_LOGGED_BADGE);
        } else {
            badge.setVisibility(View.GONE);
        }

        LinearLayout logContainer = root.findViewById(R.id.notification_log_container);
        logContainer.removeAllViews();
        for (String log : activeIncident.getNotificationLog()) {
            TextView t = new TextView(requireContext());
            t.setText(log);
            t.setTextSize(11f);
            t.setTextColor(0xFF475569);
            t.setPadding(0, dpToPx(2), 0, dpToPx(2));
            logContainer.addView(t);
        }
    }

    private void bindFacilities(View root, IncidentLocation location) {
        LinearLayout container = root.findViewById(R.id.routing_facilities_container);
        container.removeAllViews();

        // Without GPS there is nothing to sort by and no distance to show.
        List<EmergencyFacility> facilities = location.canCalculateFacilityDistance()
                ? facilityRepository.findNearby(location.getLatitude(), location.getLongitude(),
                        NEARBY_FACILITY_LIMIT)
                : DemoFacilities.all();

        if (selectedFacility == null || !containsFacility(facilities, selectedFacility)) {
            selectedFacility = facilities.isEmpty() ? null : facilities.get(0);
        }
        updateSelectedDestination(root, location);

        for (EmergencyFacility facility : facilities) {
            boolean isSelected = selectedFacility != null && facility.id().equals(selectedFacility.id());

            MaterialCardView card = new MaterialCardView(requireContext());
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            lp.bottomMargin = dpToPx(8);
            card.setLayoutParams(lp);
            card.setRadius(dpToPx(8));
            card.setStrokeWidth(dpToPx(1));
            card.setCardBackgroundColor(isSelected ? 0xFFF0FDF4 : 0xFFF8FAFC);
            card.setStrokeColor(isSelected ? 0xFF16A34A : 0xFFE2E8F0);

            LinearLayout content = new LinearLayout(requireContext());
            content.setOrientation(LinearLayout.VERTICAL);
            content.setPadding(dpToPx(12), dpToPx(10), dpToPx(12), dpToPx(10));

            TextView name = new TextView(requireContext());
            name.setText((isSelected ? "◉ " : "○ ") + facility.name());
            name.setTextSize(13f);
            name.setTypeface(name.getTypeface(), Typeface.BOLD);
            name.setTextColor(isSelected ? 0xFF166534 : 0xFF0F172A);
            content.addView(name);

            TextView info = new TextView(requireContext());
            info.setText(facility.address()
                    + "\n" + location.facilityDistanceText(facility)
                    + "\n" + location.facilityEtaText(facility)
                    + "\n" + ResponderCopy.FACILITY_SOURCE_PREFIX + facility.source()
                    + " (reviewed " + facility.lastReviewed() + ")");
            info.setTextSize(12f);
            info.setTextColor(isSelected ? 0xFF15803D : 0xFF475569);
            info.setPadding(0, dpToPx(2), 0, 0);
            content.addView(info);

            card.addView(content);
            card.setOnClickListener(v -> {
                selectedFacility = facility;
                bindFacilities(root, location);
            });
            container.addView(card);
        }
    }

    private static boolean containsFacility(List<EmergencyFacility> facilities, EmergencyFacility target) {
        for (EmergencyFacility facility : facilities) {
            if (facility.id().equals(target.id())) return true;
        }
        return false;
    }

    private void updateSelectedDestination(View root, IncidentLocation location) {
        TextView destLabel = root.findViewById(R.id.selected_destination_label);
        View routeButton = root.findViewById(R.id.btn_route_to_selected_facility);
        routeButton.setEnabled(location.canNavigate() && selectedFacility != null);
        if (selectedFacility == null) {
            destLabel.setText("No facility selected.");
        } else if (location.canNavigate()) {
            destLabel.setText("Selected: " + selectedFacility.name() + "\n" + ResponderCopy.DIRECTIONS_DISCLAIMER);
        } else {
            destLabel.setText("Selected: " + selectedFacility.name() + "\n" + ResponderCopy.NAVIGATION_UNAVAILABLE);
        }
    }

    private void bindBlePreview(View root, IncidentReport report) {
        BlePayloadPreview preview = BlePayloadPreview.from(activeIncident.getDisplayId(), report);
        setText(root, R.id.ble_preview_text, preview.getJson());
        setText(root, R.id.ble_preview_size,
                String.format(Locale.ROOT, ResponderCopy.BLE_SIZE_FORMAT, preview.getUtf8ByteSize()));
        TextView locationNote = root.findViewById(R.id.ble_location_note);
        locationNote.setVisibility(preview.includesLocation() ? View.GONE : View.VISIBLE);
        locationNote.setText(ResponderCopy.BLE_LOCATION_LEFT_OUT);
    }

    private void setupActionListeners(View root) {
        root.findViewById(R.id.btn_status_review).setOnClickListener(v ->
                updateIncidentStatus(root, DispatchStatus.REVIEWING));
        root.findViewById(R.id.btn_status_dispatch).setOnClickListener(v ->
                updateIncidentStatus(root, DispatchStatus.DISPATCHED_SIMULATION));
        root.findViewById(R.id.btn_status_close).setOnClickListener(v ->
                updateIncidentStatus(root, DispatchStatus.CLOSED));

        root.findViewById(R.id.btn_advance_status).setOnClickListener(v -> {
            DispatchStatus next = DispatchStatusStyle.next(activeIncident.getStatus());
            if (next == null) {
                toast(ResponderCopy.STATUS_FINAL);
                return;
            }
            updateIncidentStatus(root, next);
        });

        EditText noteInput = root.findViewById(R.id.input_responder_note);
        root.findViewById(R.id.btn_add_note).setOnClickListener(v -> {
            String text = noteInput.getText() != null ? noteInput.getText().toString().trim() : "";
            if (!text.isEmpty()) {
                activeIncident.addNote(text);
                noteInput.setText("");
                bindTimeline(root);
                toast("Note added to this device's timeline.");
            }
        });

        root.findViewById(R.id.btn_open_navigation).setOnClickListener(v -> {
            IncidentLocation location = activeIncident.getLocation();
            if (!location.canNavigate()) {
                toast(ResponderCopy.NAVIGATION_UNAVAILABLE);
                return;
            }
            String label = activeIncident.getReport().getLocationDescription();
            openNavigation(location.getLatitude(), location.getLongitude(),
                    isBlank(label) ? "Incident" : label);
        });

        root.findViewById(R.id.btn_route_to_selected_facility).setOnClickListener(v -> {
            if (!activeIncident.getLocation().canNavigate()) {
                toast(ResponderCopy.NAVIGATION_UNAVAILABLE);
            } else if (selectedFacility == null) {
                toast("Select a facility from the list below.");
            } else {
                openNavigation(selectedFacility.latitude(), selectedFacility.longitude(),
                        selectedFacility.name());
            }
        });

        root.findViewById(R.id.btn_copy_coordinates).setOnClickListener(v -> {
            IncidentLocation location = activeIncident.getLocation();
            if (!location.canCopyCoordinates()) {
                toast(ResponderCopy.LOCATION_UNAVAILABLE);
                return;
            }
            copyToClipboard("Coordinates", location.coordinatesText());
            toast(getString(R.string.coordinates_copied));
        });

        root.findViewById(R.id.btn_simulate_notification).setOnClickListener(v -> {
            if (!activeIncident.canSimulateNotification()) {
                toast(ResponderCopy.NOTIFICATION_NO_CONSENT);
                return;
            }
            String message = notificationMessage();
            activeIncident.logSimulatedNotification(ResponderCopy.DEMO_CONTACT, message);
            activeIncident.logSimulatedNotification(ResponderCopy.DEMO_AGENCY, message);
            updateNotificationPreview(root);
            bindTimeline(root);
            toast(ResponderCopy.NOTIFICATION_DONE_TOAST);
        });

        root.findViewById(R.id.btn_copy_json).setOnClickListener(v -> {
            copyToClipboard("Incident JSON", SampleIncidentProvider.toJson(activeIncident.getReport()));
            toast(getString(R.string.json_copied));
        });

        root.findViewById(R.id.btn_load_sample_json).setOnClickListener(v -> {
            IncidentReport sample = SampleIncidentProvider.getCanonicalSampleReport();
            activeIncident = queue.submit(sample, SharingConsent.shareReportDetails(), dispatch);
            activeIncident.addTimeline("Sample report", "Loaded the fictional sample JSON.");
            refreshQueue();
            setupQueueTabs(root);
            bindActiveIncident(root);
            toast("Loaded the fictional sample report.");
        });

        root.findViewById(R.id.start_new_report_button).setOnClickListener(ignored -> {
            IncidentRepository.getInstance().clear();
            requireActivity().getSupportFragmentManager().popBackStack(
                    null,
                    androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE
            );
            ((MainActivity) requireActivity()).showHome();
        });
    }

    private void updateIncidentStatus(View root, DispatchStatus next) {
        if (!queue.updateStatus(dispatch, activeIncident, next)) {
            toast(ResponderCopy.STATUS_CHANGE_NOT_ALLOWED_PREFIX + ResponderCopy.statusLabel(next));
            return;
        }
        refreshQueue();
        setupQueueTabs(root);
        updateStatusViews(root);
        updateNotificationPreview(root);
        toast(ResponderCopy.STATUS_CHANGE_TITLE_PREFIX + ResponderCopy.statusLabel(next));
    }

    private void openNavigation(double lat, double lng, String label) {
        String uri = String.format(Locale.ROOT, "geo:%.5f,%.5f?q=%.5f,%.5f(%s)",
                lat, lng, lat, lng, Uri.encode(label));
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(uri)));
        } catch (ActivityNotFoundException e) {
            String webUri = String.format(Locale.ROOT,
                    "https://www.google.com/maps/search/?api=1&query=%.5f,%.5f", lat, lng);
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(webUri)));
            } catch (ActivityNotFoundException ex) {
                toast("No maps app or browser is available on this device.");
            }
        }
    }

    private void copyToClipboard(String label, String text) {
        ClipboardManager clipboard =
                (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText(label, text));
    }

    private void addDetailCard(LinearLayout container, String title, String content, int primaryColor, int bgColor) {
        MaterialCardView card = new MaterialCardView(requireContext());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dpToPx(10);
        card.setLayoutParams(lp);
        card.setRadius(dpToPx(8));
        card.setCardBackgroundColor(bgColor);
        card.setStrokeColor(primaryColor);
        card.setStrokeWidth(dpToPx(1));

        LinearLayout inner = new LinearLayout(requireContext());
        inner.setOrientation(LinearLayout.VERTICAL);
        inner.setPadding(dpToPx(12), dpToPx(8), dpToPx(12), dpToPx(8));

        TextView label = new TextView(requireContext());
        label.setText(title);
        label.setTextColor(primaryColor);
        label.setTextSize(12f);
        label.setTypeface(label.getTypeface(), Typeface.BOLD);
        inner.addView(label);

        TextView body = new TextView(requireContext());
        body.setText(content);
        body.setTextColor(0xFF1E293B);
        body.setTextSize(13f);
        body.setPadding(0, dpToPx(4), 0, 0);
        inner.addView(body);

        card.addView(inner);
        container.addView(card);
    }

    private void toast(String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }

    private static String bullets(List<String> items) {
        return "• " + String.join("\n• ", items);
    }

    private static void setText(View root, int viewId, String value) {
        TextView view = root.findViewById(viewId);
        if (view != null) {
            view.setText(isBlank(value) ? "Unknown" : value);
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}
