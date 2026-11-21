package com.example.scenar.ui.review;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.scenar.MainActivity;
import com.example.scenar.R;
import com.example.scenar.model.IncidentReport;
import com.example.scenar.repository.IncidentRepository;

import java.util.List;

public class ReviewFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.fragment_review, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        IncidentReport report = IncidentRepository.getInstance().getCurrentReport();
        TextView emptyState = view.findViewById(R.id.review_empty_text);
        View reviewContent = view.findViewById(R.id.review_content);

        if (report == null) {
            reviewContent.setVisibility(View.GONE);
            emptyState.setVisibility(View.VISIBLE);
            return;
        }

        EditText incidentType = view.findViewById(R.id.review_incident_type);
        EditText summary = view.findViewById(R.id.review_summary);
        EditText peopleAffected = view.findViewById(R.id.review_people_affected);
        EditText location = view.findViewById(R.id.review_location);
        TextView hazards = view.findViewById(R.id.review_hazards);
        TextView missingInformation = view.findViewById(R.id.review_missing_information);

        incidentType.setText(valueOrEmpty(report.getIncidentType()));
        summary.setText(valueOrEmpty(report.getSummary()));
        peopleAffected.setText(report.getPeopleAffected() == null
                ? ""
                : String.valueOf(report.getPeopleAffected()));
        location.setText(valueOrEmpty(report.getLocationDescription()));
        hazards.setText(formatList(report.getHazards()));
        missingInformation.setText(formatList(report.getMissingInformation()));

        view.findViewById(R.id.confirm_report_button).setOnClickListener(ignored -> {
            report.setIncidentType(incidentType.getText().toString().trim());
            report.setSummary(summary.getText().toString().trim());
            report.setLocationDescription(location.getText().toString().trim());
            report.setPeopleAffected(parseIntegerOrNull(peopleAffected.getText().toString()));
            IncidentRepository.getInstance().setCurrentReport(report);
            ((MainActivity) requireActivity()).showResponderDashboard();
        });
    }

    private static String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }

    private static String formatList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "None reported";
        }
        return "• " + String.join("\n• ", values);
    }

    private static Integer parseIntegerOrNull(String value) {
        try {
            return value.trim().isEmpty() ? null : Integer.parseInt(value.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
