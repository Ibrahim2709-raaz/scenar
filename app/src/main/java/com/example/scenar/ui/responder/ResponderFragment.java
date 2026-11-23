package com.example.scenar.ui.responder;

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
import com.example.scenar.model.IncidentReport;
import com.example.scenar.repository.IncidentRepository;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.util.List;
import java.util.Locale;

public class ResponderFragment extends Fragment {

    private List<ResponderIncident> incidentList;
    private ResponderIncident activeIncident;
    private NearbyFacility selectedFacility;

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

        IncidentReport currentReport = IncidentRepository.getInstance().getCurrentReport();
        incidentList = SampleIncidentProvider.createDemoIncidents(currentReport);
        activeIncident = incidentList.get(0);

        List<NearbyFacility> facilities = SampleIncidentProvider.getNearbyFacilities();
        selectedFacility = facilities.isEmpty() ? null : facilities.get(0);

        setupQueueTabs(view);
        bindActiveIncident(view);
        setupActionListeners(view);
    }

    private void setupQueueTabs(View root) {
        LinearLayout queueContainer = root.findViewById(R.id.incident_queue_container);
        queueContainer.removeAllViews();

        for (int i = 0; i < incidentList.size(); i++) {
            final ResponderIncident incident = incidentList.get(i);
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

            if (isSelected) {
                card.setCardBackgroundColor(0xFF0F172A);
                card.setStrokeColor(0xFF38BDF8);
            } else {
                card.setCardBackgroundColor(0xFFFFFFFF);
                card.setStrokeColor(0xFFCBD5E1);
            }

            LinearLayout content = new LinearLayout(requireContext());
            content.setOrientation(LinearLayout.VERTICAL);
            content.setPadding(dpToPx(12), dpToPx(8), dpToPx(12), dpToPx(8));

            TextView idText = new TextView(requireContext());
            idText.setText(incident.getId());
            idText.setTextSize(12f);
            idText.setTypeface(idText.getTypeface(), Typeface.BOLD);
            idText.setTextColor(isSelected ? 0xFF38BDF8 : 0xFF64748B);
            content.addView(idText);

            TextView typeText = new TextView(requireContext());
            String title = incident.getReport().getIncidentType() != null
                    ? incident.getReport().getIncidentType()
                    : "Incident";
            typeText.setText(title);
            typeText.setTextSize(13f);
            typeText.setTypeface(typeText.getTypeface(), Typeface.BOLD);
            typeText.setTextColor(isSelected ? Color.WHITE : 0xFF0F172A);
            content.addView(typeText);

            TextView statusText = new TextView(requireContext());
            statusText.setText(incident.getStatus().getLabel());
            statusText.setTextSize(11f);
            statusText.setTextColor(incident.getStatus().getColor());
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

        // Banner and IDs
        setText(root, R.id.responder_incident_id, activeIncident.getId() + " • Dispatched Unit: " + activeIncident.getAssignedUnit());
        TextView priorityBadge = root.findViewById(R.id.responder_priority_badge);
        priorityBadge.setText(activeIncident.getPriority().getLabel());
        priorityBadge.setTextColor(activeIncident.getPriority().getColor());
        priorityBadge.setBackgroundColor(activeIncident.getPriority().getBackgroundColor());

        setText(root, R.id.responder_incident_type, report.getIncidentType());
        setText(root, R.id.responder_summary, isBlank(report.getSummary()) ? "No incident summary provided." : report.getSummary());

        String peopleStr = report.getPeopleAffected() == null
                ? "Unknown count"
                : report.getPeopleAffected() + " " + (report.getPeopleAffected() == 1 ? "person" : "people");
        setText(root, R.id.responder_people, peopleStr);
        setText(root, R.id.responder_time, activeIncident.getReceivedTime());

        String locationStr = isBlank(report.getLocationDescription())
                ? "Location not specified / Unknown"
                : report.getLocationDescription();
        setText(root, R.id.responder_location, locationStr);

        // Structured alerts (Critical conditions, hazards, consented medical flags, missing info, uncertainties)
        LinearLayout details = root.findViewById(R.id.responder_structured_details);
        details.removeAllViews();

        if (report.getLatitude() != null && report.getLongitude() != null) {
            addDetailCard(details, "GPS Coordinates",
                    String.format(Locale.ROOT, "%.5f, %.5f", report.getLatitude(), report.getLongitude()),
                    0xFF0284C7, 0xFFE0F2FE);
        }

        if (!report.getCriticalConditions().isEmpty()) {
            addDetailCard(details, "🚨 Critical Conditions",
                    String.join("\n• ", report.getCriticalConditions()),
                    0xFFDC2626, 0xFFFEE2E2);
        } else {
            addDetailCard(details, "Critical Conditions",
                    "No critical medical trauma or triage alerts flagged.",
                    0xFF64748B, 0xFFF8FAFC);
        }

        if (!report.getHazards().isEmpty()) {
            addDetailCard(details, "⚠️ Scene Hazards",
                    String.join("\n• ", report.getHazards()),
                    0xFFD97706, 0xFFFEF3C7);
        } else {
            addDetailCard(details, "Scene Hazards",
                    "No environmental or structural hazards reported.",
                    0xFF64748B, 0xFFF8FAFC);
        }

        // V3 Requirement: Show only consented medical information
        if (!report.getMedicalFlags().isEmpty()) {
            addDetailCard(details, "🛡️ Medical Profile (Patient-Consented Only)",
                    String.join("\n• ", report.getMedicalFlags()) + "\n\n✓ User consent verified prior to dispatch transmission.",
                    0xFF0D9488, 0xFFCCFBF1);
        } else {
            addDetailCard(details, "🛡️ Medical Profile",
                    "No consented medical information shared by caller.\n(Protected by patient consent controls)",
                    0xFF64748B, 0xFFF8FAFC);
        }

        if (!report.getMissingInformation().isEmpty()) {
            addDetailCard(details, "❓ Missing Information (Verification Required)",
                    String.join("\n• ", report.getMissingInformation()),
                    0xFF64748B, 0xFFF1F5F9);
        }

        if (!report.getUncertainties().isEmpty()) {
            addDetailCard(details, "ℹ️ AI Uncertainties (Ground Truth Needed)",
                    String.join("\n• ", report.getUncertainties()),
                    0xFF7C3AED, 0xFFEDE9FE);
        }

        // Status Stepper & Pill
        updateStatusViews(root);

        // V4 Requirement: Incident-location map & missing-coordinate fallback
        bindMapAndLocation(root, report);

        // Simulated Contact Notification Preview
        updateNotificationPreview(root);

        // V4 Requirement: Facility discovery, destination selection & routing
        double baseLat = report.getLatitude() != null ? report.getLatitude() : 37.77492;
        double baseLng = report.getLongitude() != null ? report.getLongitude() : -122.41941;
        bindRoutingFacilities(root, baseLat, baseLng);

        // Interoperability JSON View
        String json = SampleIncidentProvider.toJson(report);
        setText(root, R.id.json_preview_text, json);
    }

    private void bindMapAndLocation(View root, IncidentReport report) {
        View missingCard = root.findViewById(R.id.missing_coordinates_card);
        TacticalMapView mapView = root.findViewById(R.id.tactical_map_view);
        View mapActions = root.findViewById(R.id.map_actions_container);
        TextView coordText = root.findViewById(R.id.map_coordinates_text);

        if (report.getLatitude() != null && report.getLongitude() != null) {
            double lat = report.getLatitude();
            double lng = report.getLongitude();

            missingCard.setVisibility(View.GONE);
            mapView.setVisibility(View.VISIBLE);
            mapActions.setVisibility(View.VISIBLE);

            mapView.setLocation(lat, lng, report.getLocationDescription() != null ? report.getLocationDescription() : "Scene");
            mapView.setDistanceKm(1.4);
            coordText.setText(String.format(Locale.ROOT, "GPS Target Lock: %.5f, %.5f", lat, lng));
        } else {
            // Safe missing-coordinate fallback (V4 Requirement)
            mapView.setVisibility(View.GONE);
            mapActions.setVisibility(View.GONE);
            missingCard.setVisibility(View.VISIBLE);

            String desc = isBlank(report.getLocationDescription())
                    ? "No location description provided."
                    : report.getLocationDescription();
            TextView missingDesc = root.findViewById(R.id.missing_coordinates_desc);
            missingDesc.setText("⚠️ Coordinates Unavailable: Exact GPS coordinates were not transmitted. Location is provided as text only: \"" + desc + "\"");
            coordText.setText("GPS Status: Coordinates Missing (Fallback Mode)");
        }
    }

    private void updateStatusViews(View root) {
        TextView statusPill = root.findViewById(R.id.responder_status_pill);
        statusPill.setText(activeIncident.getStatus().getLabel().toUpperCase(Locale.ROOT));
        statusPill.setTextColor(activeIncident.getStatus().getColor());
        statusPill.setBackgroundColor(activeIncident.getStatus().getBackgroundColor());

        // Update Stepper Bar
        LinearLayout stepperContainer = root.findViewById(R.id.status_stepper_container);
        stepperContainer.removeAllViews();

        IncidentStatus[] allStatuses = IncidentStatus.values();
        for (int i = 0; i < allStatuses.length; i++) {
            IncidentStatus s = allStatuses[i];
            boolean isReached = s.ordinal() <= activeIncident.getStatus().ordinal();
            boolean isCurrent = s == activeIncident.getStatus();

            TextView step = new TextView(requireContext());
            step.setText(s.getLabel());
            step.setTextSize(11f);
            step.setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4));

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            lp.setMarginEnd(dpToPx(4));
            lp.rightMargin = dpToPx(4);
            step.setLayoutParams(lp);

            if (isCurrent) {
                step.setBackgroundColor(s.getColor());
                step.setTextColor(Color.WHITE);
                step.setTypeface(step.getTypeface(), Typeface.BOLD);
            } else if (isReached) {
                step.setBackgroundColor(0xFFE2E8F0);
                step.setTextColor(0xFF334155);
            } else {
                step.setBackgroundColor(0xFFF1F5F9);
                step.setTextColor(0xFF94A3B8);
            }
            stepperContainer.addView(step);
        }

        // Timeline
        bindTimeline(root);
    }

    private void bindTimeline(View root) {
        LinearLayout timelineContainer = root.findViewById(R.id.timeline_container);
        timelineContainer.removeAllViews();

        List<TimelineEntry> timeline = activeIncident.getTimeline();
        for (TimelineEntry entry : timeline) {
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

    private void updateNotificationPreview(View root) {
        TextView preview = root.findViewById(R.id.notification_preview_text);
        String location = activeIncident.getReport().getLocationDescription() != null
                ? activeIncident.getReport().getLocationDescription()
                : "Reported Scene";
        String status = activeIncident.getStatus().getLabel();
        String message = String.format(Locale.ROOT,
                "[SCENAR SIMULATED ALERT] Emergency status updated to %s for %s. Dispatched unit: %s. Stand by for updates.",
                status, location, activeIncident.getAssignedUnit());
        preview.setText(message);

        TextView badge = root.findViewById(R.id.notification_status_badge);
        if (activeIncident.isContactNotificationSent()) {
            badge.setVisibility(View.VISIBLE);
            badge.setText("✓ Simulated Alert Logged to Emergency Contacts (Internal Simulation Only)");
        } else {
            badge.setVisibility(View.GONE);
        }

        LinearLayout logContainer = root.findViewById(R.id.notification_log_container);
        logContainer.removeAllViews();
        for (String log : activeIncident.getNotificationLog()) {
            TextView t = new TextView(requireContext());
            t.setText(log);
            t.setTextSize(11f);
            t.setTextColor(0xFF16A34A);
            t.setPadding(0, dpToPx(2), 0, dpToPx(2));
            logContainer.addView(t);
        }
    }

    private void bindRoutingFacilities(View root, double lat, double lng) {
        LinearLayout container = root.findViewById(R.id.routing_facilities_container);
        container.removeAllViews();

        List<NearbyFacility> facilities = SampleIncidentProvider.getNearbyFacilities();
        if (selectedFacility == null && !facilities.isEmpty()) {
            selectedFacility = facilities.get(0);
        }

        updateSelectedDestinationBanner(root);

        for (NearbyFacility facility : facilities) {
            boolean isSelected = facility == selectedFacility;

            MaterialCardView card = new MaterialCardView(requireContext());
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            lp.bottomMargin = dpToPx(8);
            card.setLayoutParams(lp);
            card.setRadius(dpToPx(8));
            card.setStrokeWidth(dpToPx(1));

            if (isSelected) {
                card.setCardBackgroundColor(0xFFF0FDF4);
                card.setStrokeColor(0xFF16A34A);
            } else {
                card.setCardBackgroundColor(0xFFF8FAFC);
                card.setStrokeColor(0xFFE2E8F0);
            }

            LinearLayout content = new LinearLayout(requireContext());
            content.setOrientation(LinearLayout.VERTICAL);
            content.setPadding(dpToPx(12), dpToPx(10), dpToPx(12), dpToPx(10));

            TextView name = new TextView(requireContext());
            name.setText((isSelected ? "◉ " : "○ ") + facility.getName() + " (" + facility.getType() + ")");
            name.setTextSize(13f);
            name.setTypeface(name.getTypeface(), Typeface.BOLD);
            name.setTextColor(isSelected ? 0xFF166534 : 0xFF0F172A);
            content.addView(name);

            TextView dist = new TextView(requireContext());
            dist.setText(String.format(Locale.ROOT, "Approx. Distance: %s • Est. Transit: ~%d min • Emergency Phone: %s",
                    facility.getFormattedDistance(lat, lng),
                    facility.etaMinutes(lat, lng),
                    facility.getPhone()));
            dist.setTextSize(12f);
            dist.setTextColor(isSelected ? 0xFF15803D : 0xFF0284C7);
            dist.setPadding(0, dpToPx(2), 0, 0);
            content.addView(dist);

            card.addView(content);
            card.setOnClickListener(v -> {
                selectedFacility = facility;
                bindRoutingFacilities(root, lat, lng);
            });
            container.addView(card);
        }
    }

    private void updateSelectedDestinationBanner(View root) {
        TextView destLabel = root.findViewById(R.id.selected_destination_label);
        if (destLabel != null && selectedFacility != null) {
            destLabel.setText("Selected Destination: " + selectedFacility.getName() + " (" + selectedFacility.getType() + ")");
        }
    }

    private void setupActionListeners(View root) {
        // Direct Status Controls (V3 Requirement: simulated review, dispatch, and closure controls)
        root.findViewById(R.id.btn_status_dispatch).setOnClickListener(v -> updateIncidentStatus(root, IncidentStatus.DISPATCHED));
        root.findViewById(R.id.btn_status_en_route).setOnClickListener(v -> updateIncidentStatus(root, IncidentStatus.EN_ROUTE));
        root.findViewById(R.id.btn_status_on_scene).setOnClickListener(v -> updateIncidentStatus(root, IncidentStatus.ON_SCENE));
        root.findViewById(R.id.btn_status_close).setOnClickListener(v -> updateIncidentStatus(root, IncidentStatus.RESOLVED));

        // Advance Status Button
        root.findViewById(R.id.btn_advance_status).setOnClickListener(v -> {
            activeIncident.advanceStatus();
            setupQueueTabs(root);
            updateStatusViews(root);
            updateNotificationPreview(root);
            Toast.makeText(requireContext(),
                    "Incident status advanced to: " + activeIncident.getStatus().getLabel(),
                    Toast.LENGTH_SHORT).show();
        });

        // Add Note Button
        EditText noteInput = root.findViewById(R.id.input_responder_note);
        root.findViewById(R.id.btn_add_note).setOnClickListener(v -> {
            String text = noteInput.getText() != null ? noteInput.getText().toString().trim() : "";
            if (!text.isEmpty()) {
                activeIncident.addNote(text);
                noteInput.setText("");
                bindTimeline(root);
                Toast.makeText(requireContext(), "Field note recorded.", Toast.LENGTH_SHORT).show();
            }
        });

        // Open External Directions to Incident Location (V4 Requirement)
        root.findViewById(R.id.btn_open_navigation).setOnClickListener(v -> {
            IncidentReport report = activeIncident.getReport();
            if (report.getLatitude() != null && report.getLongitude() != null) {
                String label = report.getLocationDescription() != null ? report.getLocationDescription() : "Incident Scene";
                openNavigation(report.getLatitude(), report.getLongitude(), label);
            }
        });

        // Search Address in External Maps (Missing-Coordinate Fallback)
        root.findViewById(R.id.btn_search_address_external).setOnClickListener(v -> {
            String location = activeIncident.getReport().getLocationDescription();
            if (!isBlank(location)) {
                searchAddressExternal(location);
            } else {
                Toast.makeText(requireContext(), "No location text available to search.", Toast.LENGTH_SHORT).show();
            }
        });

        // Route to Selected Facility (V4 Requirement)
        root.findViewById(R.id.btn_route_to_selected_facility).setOnClickListener(v -> {
            if (selectedFacility != null) {
                openNavigation(selectedFacility.getLatitude(), selectedFacility.getLongitude(), selectedFacility.getName());
            } else {
                Toast.makeText(requireContext(), "Select a facility from the list below.", Toast.LENGTH_SHORT).show();
            }
        });

        // Copy Coordinates Button
        root.findViewById(R.id.btn_copy_coordinates).setOnClickListener(v -> {
            IncidentReport report = activeIncident.getReport();
            if (report.getLatitude() != null && report.getLongitude() != null) {
                copyToClipboard("Coordinates", String.format(Locale.ROOT, "%.5f, %.5f", report.getLatitude(), report.getLongitude()));
                Toast.makeText(requireContext(), R.string.coordinates_copied, Toast.LENGTH_SHORT).show();
            }
        });

        // Simulate Notification Broadcast Button (V3 Requirement)
        root.findViewById(R.id.btn_simulate_notification).setOnClickListener(v -> {
            TextView preview = root.findViewById(R.id.notification_preview_text);
            String message = preview.getText().toString();

            activeIncident.recordNotification("Emergency Contact (Jane Doe)", message);
            activeIncident.recordNotification("Mutual Aid Agency Dispatch", message);

            updateNotificationPreview(root);
            bindTimeline(root);
            Toast.makeText(requireContext(), R.string.simulated_notification_success, Toast.LENGTH_LONG).show();
        });

        // Copy JSON Button
        root.findViewById(R.id.btn_copy_json).setOnClickListener(v -> {
            String json = SampleIncidentProvider.toJson(activeIncident.getReport());
            copyToClipboard("Incident JSON", json);
            Toast.makeText(requireContext(), R.string.json_copied, Toast.LENGTH_SHORT).show();
        });

        // Load Canonical Sample JSON Button
        root.findViewById(R.id.btn_load_sample_json).setOnClickListener(v -> {
            IncidentReport sample = SampleIncidentProvider.getCanonicalSampleReport();
            IncidentRepository.getInstance().setCurrentReport(sample);
            activeIncident = new ResponderIncident("INC-SAMPLE-CANONICAL", sample, "16:42");
            incidentList.add(0, activeIncident);

            setupQueueTabs(root);
            bindActiveIncident(root);
            Toast.makeText(requireContext(), "Loaded canonical sample incident report.", Toast.LENGTH_SHORT).show();
        });

        // Reset & Return to Home Button
        root.findViewById(R.id.start_new_report_button).setOnClickListener(ignored -> {
            IncidentRepository.getInstance().clear();
            requireActivity().getSupportFragmentManager().popBackStack(
                    null,
                    androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE
            );
            ((MainActivity) requireActivity()).showHome();
        });
    }

    private void updateIncidentStatus(View root, IncidentStatus status) {
        activeIncident.setStatus(status);
        setupQueueTabs(root);
        updateStatusViews(root);
        updateNotificationPreview(root);
        Toast.makeText(requireContext(), "Status updated: " + status.getLabel(), Toast.LENGTH_SHORT).show();
    }

    private void openNavigation(double lat, double lng, String label) {
        String uriStr = String.format(Locale.ROOT, "geo:%.5f,%.5f?q=%.5f,%.5f(%s)", lat, lng, lat, lng, Uri.encode(label));
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(uriStr));
        try {
            startActivity(mapIntent);
        } catch (Exception e) {
            String webUri = String.format(Locale.ROOT, "https://www.google.com/maps/search/?api=1&query=%.5f,%.5f", lat, lng);
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(webUri)));
            } catch (Exception ex) {
                Toast.makeText(requireContext(), "Navigation target: " + lat + ", " + lng, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void searchAddressExternal(String query) {
        String uriStr = "geo:0,0?q=" + Uri.encode(query);
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(uriStr));
        try {
            startActivity(mapIntent);
        } catch (Exception e) {
            String webUri = "https://www.google.com/maps/search/?api=1&query=" + Uri.encode(query);
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(webUri)));
            } catch (Exception ex) {
                Toast.makeText(requireContext(), "Location: " + query, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void copyToClipboard(String label, String text) {
        ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(label, text);
        clipboard.setPrimaryClip(clip);
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
