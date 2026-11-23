package com.example.scenar.ui.home;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.location.LocationManagerCompat;
import androidx.fragment.app.Fragment;

import com.example.scenar.MainActivity;
import com.example.scenar.R;
import com.example.scenar.profile.EmergencyProfile;
import com.example.scenar.profile.EmergencyProfileRepository;
import com.example.scenar.profile.ProfileStorageException;
import com.example.scenar.ui.profile.ProfileActivity;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.time.LocalTime;

public final class HomeFragment extends Fragment {
    private final ActivityResultLauncher<Intent> profileEditor =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> refreshHome()
            );

    private EmergencyProfileRepository profileRepository;

    private TextView greetingText;
    private TextView profileCompletionText;
    private TextView profileActionText;
    private TextView networkStatusText;
    private TextView locationStatusText;
    private TextView homeStatusText;

    private LinearProgressIndicator profileCompletionBar;

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

        profileRepository =
                EmergencyProfileRepository.getInstance(
                        requireContext()
                );

        greetingText =
                view.findViewById(R.id.home_greeting);

        profileCompletionText =
                view.findViewById(
                        R.id.home_profile_completion
                );

        profileActionText =
                view.findViewById(
                        R.id.home_profile_action
                );

        profileCompletionBar =
                view.findViewById(
                        R.id.home_profile_progress
                );

        networkStatusText =
                view.findViewById(
                        R.id.home_network_status
                );

        locationStatusText =
                view.findViewById(
                        R.id.home_location_status
                );

        homeStatusText =
                view.findViewById(
                        R.id.home_status
                );

        view.findViewById(R.id.report_emergency_button)
                .setOnClickListener(
                        ignored ->
                                ((MainActivity) requireActivity())
                                        .showReport()
                );

        View.OnClickListener openProfile =
                ignored -> profileEditor.launch(
                        new Intent(
                                requireContext(),
                                ProfileActivity.class
                        )
                );

        view.findViewById(R.id.open_profile_button)
                .setOnClickListener(openProfile);

        view.findViewById(R.id.home_profile_button)
                .setOnClickListener(openProfile);

        refreshHome();
    }

    @Override
    public void onResume() {
        super.onResume();

        if (getView() != null
                && profileRepository != null) {
            refreshHome();
        }
    }

    private void refreshHome() {
        View view = getView();

        if (view == null
                || profileRepository == null) {
            return;
        }

        refreshProfile();
        refreshConnectivity();
        refreshLocation();
    }

    private void refreshProfile() {
        try {
            EmergencyProfile profile =
                    profileRepository.load();

            int completion =
                    profile.completionPercentage();

            profileCompletionBar.setProgressCompat(
                    completion,
                    true
            );

            profileCompletionText.setText(
                    getString(
                            R.string.home_profile_completion_format,
                            completion
                    )
            );

            profileActionText.setText(
                    completion >= 100
                            ? R.string.review_profile
                            : R.string.complete_profile
            );

            greetingText.setText(
                    greetingFor(profile)
            );

            homeStatusText.setText("");

        } catch (ProfileStorageException exception) {
            profileCompletionBar.setProgressCompat(
                    0,
                    false
            );

            profileCompletionText.setText(
                    R.string.profile_unavailable
            );

            profileActionText.setText(
                    R.string.open_profile
            );

            greetingText.setText(
                    greetingFor(
                            EmergencyProfile.empty()
                    )
            );

            homeStatusText.setText(
                    R.string.home_profile_error
            );
        }
    }

    private void refreshConnectivity() {
        ConnectivityManager manager =
                (ConnectivityManager)
                        requireContext().getSystemService(
                                Context.CONNECTIVITY_SERVICE
                        );

        boolean connected = false;

        if (manager != null) {
            Network network =
                    manager.getActiveNetwork();

            NetworkCapabilities capabilities =
                    network == null
                            ? null
                            : manager.getNetworkCapabilities(
                            network
                    );

            connected = capabilities != null
                    && capabilities.hasCapability(
                    NetworkCapabilities
                            .NET_CAPABILITY_INTERNET
            )
                    && capabilities.hasCapability(
                    NetworkCapabilities
                            .NET_CAPABILITY_VALIDATED
            );
        }

        networkStatusText.setText(
                connected
                        ? R.string.network_ready
                        : R.string.network_offline
        );

        networkStatusText.setTextColor(
                ContextCompat.getColor(
                        requireContext(),
                        connected
                                ? R.color.scenar_success
                                : R.color.scenar_warning
                )
        );
    }

    private void refreshLocation() {
        boolean permissionGranted =
                ContextCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
                        || ContextCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED;

        LocationManager manager =
                (LocationManager)
                        requireContext().getSystemService(
                                Context.LOCATION_SERVICE
                        );

        boolean locationEnabled =
                manager != null
                        && LocationManagerCompat
                        .isLocationEnabled(manager);

        int text;
        int colour;

        if (!locationEnabled) {
            text = R.string.location_disabled;
            colour = R.color.scenar_warning;

        } else if (!permissionGranted) {
            text = R.string.location_permission_later;
            colour = R.color.scenar_text_secondary;

        } else {
            text = R.string.location_ready;
            colour = R.color.scenar_success;
        }

        locationStatusText.setText(text);
        locationStatusText.setTextColor(
                ContextCompat.getColor(
                        requireContext(),
                        colour
                )
        );
    }

    private String greetingFor(
            EmergencyProfile profile
    ) {
        int hour = LocalTime.now().getHour();

        String greeting;

        if (hour < 12) {
            greeting = getString(
                    R.string.good_morning
            );
        } else if (hour < 17) {
            greeting = getString(
                    R.string.good_afternoon
            );
        } else {
            greeting = getString(
                    R.string.good_evening
            );
        }

        if (profile.preferredName().isEmpty()) {
            return greeting;
        }

        return getString(
                R.string.named_greeting_format,
                greeting,
                firstName(profile.preferredName())
        );
    }

    private static String firstName(String name) {
        String trimmed = name.trim();
        int separator = trimmed.indexOf(' ');

        return separator < 0
                ? trimmed
                : trimmed.substring(0, separator);
    }
}