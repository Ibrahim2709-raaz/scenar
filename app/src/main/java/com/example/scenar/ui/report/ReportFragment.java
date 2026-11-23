package com.example.scenar.ui.report;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.scenar.MainActivity;
import com.example.scenar.R;
import com.example.scenar.ai.IncidentAiService;
import com.example.scenar.ai.IncidentAiServiceFactory;
import com.example.scenar.model.IncidentReport;
import com.example.scenar.repository.IncidentRepository;

public class ReportFragment extends Fragment {
    private final IncidentAiService incidentAiService =
            IncidentAiServiceFactory.createConfiguredService();

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.fragment_report, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        EditText descriptionInput = view.findViewById(R.id.incident_description_input);
        TextView errorText = view.findViewById(R.id.report_error_text);
        ProgressBar progressBar = view.findViewById(R.id.report_progress);
        Button continueButton = view.findViewById(R.id.structure_report_button);

        continueButton.setOnClickListener(ignored -> {
            errorText.setVisibility(View.GONE);
            setLoading(true, continueButton, progressBar);

            incidentAiService.structureIncident(
                    descriptionInput.getText().toString(),
                    new IncidentAiService.IncidentCallback() {
                        @Override
                        public void onSuccess(IncidentReport report) {
                            if (!isAdded()) {
                                return;
                            }
                            requireActivity().runOnUiThread(() -> {
                                IncidentRepository.getInstance().setCurrentReport(report);
                                setLoading(false, continueButton, progressBar);
                                ((MainActivity) requireActivity()).showReview();
                            });
                        }

                        @Override
                        public void onFailure(String message) {
                            if (!isAdded()) {
                                return;
                            }
                            requireActivity().runOnUiThread(() -> {
                                setLoading(false, continueButton, progressBar);
                                errorText.setText(message);
                                errorText.setVisibility(View.VISIBLE);
                                descriptionInput.requestFocus();
                            });
                        }
                    }
            );
        });
    }

    private void setLoading(boolean loading, Button button, ProgressBar progressBar) {
        button.setEnabled(!loading);
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
    }
}
