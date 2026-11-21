package com.example.scenar.ui.responder;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.scenar.MainActivity;
import com.example.scenar.R;
import com.example.scenar.model.IncidentReport;
import com.example.scenar.repository.IncidentRepository;

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

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
