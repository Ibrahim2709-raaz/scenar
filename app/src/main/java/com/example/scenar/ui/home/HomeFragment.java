package com.example.scenar.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.scenar.MainActivity;
import com.example.scenar.R;

public class HomeFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        return inflater.inflate(
                R.layout.fragment_home,
                container,
                false
        );
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);

        MainActivity activity =
                (MainActivity) requireActivity();

        view.findViewById(R.id.report_emergency_button)
                .setOnClickListener(
                        ignored -> activity.showReport()
                );

        view.findViewById(R.id.open_responder_button)
                .setOnClickListener(
                        ignored -> activity.showResponderDashboard()
                );

        view.findViewById(R.id.open_beacon_demo_button)
                .setOnClickListener(
                        ignored -> activity.showBeaconDemo()
                );
    }
}