package com.example.scenar.ui.responder;

import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.scenar.MainActivity;
import com.example.scenar.R;
import com.example.scenar.model.IncidentReport;
import com.example.scenar.repository.IncidentRepository;

import java.util.Locale;

public class ResponderFragment extends Fragment {
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

        IncidentReport report = IncidentRepository.getInstance().getCurrentReport();
        if (report != null) {
            setText(view, R.id.responder_incident_type, report.getIncidentType());
            setText(view, R.id.responder_summary, report.getSummary());
            setText(view, R.id.responder_people, report.getPeopleAffected() == null
                    ? "Unknown"
                    : String.valueOf(report.getPeopleAffected()));
            setText(view, R.id.responder_location,
                    isBlank(report.getLocationDescription())
                            ? "Not confirmed"
                            : report.getLocationDescription());
            LinearLayout details = view.findViewById(R.id.responder_structured_details);
            if (report.getLatitude() != null && report.getLongitude() != null) {
                addDetail(details, R.string.coordinates, String.format(Locale.ROOT, "%.5f, %.5f",
                        report.getLatitude(), report.getLongitude()));
            }
            addDetail(details, R.string.critical_conditions, String.join("\n", report.getCriticalConditions()));
            addDetail(details, R.string.hazards, String.join("\n", report.getHazards()));
            addDetail(details, R.string.medical_flags, String.join("\n", report.getMedicalFlags()));
            addDetail(details, R.string.missing_information, String.join("\n", report.getMissingInformation()));
            addDetail(details, R.string.uncertainties, String.join("\n", report.getUncertainties()));
        }

        view.findViewById(R.id.start_new_report_button).setOnClickListener(ignored -> {
            IncidentRepository.getInstance().clear();
            requireActivity().getSupportFragmentManager().popBackStack(
                    null,
                    androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE
            );
            ((MainActivity) requireActivity()).showHome();
        });
    }

    private static void setText(View root, int viewId, String value) {
        ((TextView) root.findViewById(viewId)).setText(isBlank(value) ? "Unknown" : value);
    }

    private void addDetail(LinearLayout details, int labelRes, String value) {
        if (isBlank(value)) return;
        TextView label = new TextView(requireContext());
        label.setText(labelRes);
        label.setTypeface(label.getTypeface(), Typeface.BOLD);
        LinearLayout.LayoutParams layout = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        layout.topMargin = Math.round(16 * getResources().getDisplayMetrics().density);
        details.addView(label, layout);
        TextView detail = new TextView(requireContext());
        detail.setText(value);
        details.addView(detail);
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
