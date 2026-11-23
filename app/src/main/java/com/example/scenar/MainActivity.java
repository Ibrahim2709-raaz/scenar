package com.example.scenar;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.example.scenar.ui.beacon.BeaconDemoActivity;
import com.example.scenar.ui.home.HomeFragment;
import com.example.scenar.ui.responder.ResponderFragment;
import com.example.scenar.ui.review.ReviewFragment;
import com.example.scenar.ui.victim.VictimReportActivity;

public class MainActivity extends AppCompatActivity {
    private final ActivityResultLauncher<Intent> victimReporter =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK) {
                            Intent data = result.getData();
                            showResponderDashboard(data == null ? null
                                    : data.getStringExtra(VictimReportActivity.EXTRA_DISPATCH_ID));
                        }
                    }
            );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.enableEdgeToEdge(getWindow());
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.main),
                (view, insets) -> {
                    Insets systemBars = insets.getInsets(
                            WindowInsetsCompat.Type.systemBars()
                    );

                    view.setPadding(
                            systemBars.left,
                            systemBars.top,
                            systemBars.right,
                            systemBars.bottom
                    );

                    return insets;
                }
        );

        if (savedInstanceState == null) {
            showHome();
        }
    }

    public void showHome() {
        navigateTo(new HomeFragment(), false);
    }

    public void showReport() {
        victimReporter.launch(
                new Intent(this, VictimReportActivity.class)
        );
    }

    public void showReview() {
        navigateTo(new ReviewFragment(), true);
    }

    public void showResponderDashboard() {
        showResponderDashboard(null);
    }

    public void showResponderDashboard(String incidentId) {
        navigateTo(ResponderFragment.forIncident(incidentId), true);
    }

    public void showBeaconDemo() {
        startActivity(
                new Intent(this, BeaconDemoActivity.class)
        );
    }

    private void navigateTo(
            Fragment fragment,
            boolean addToBackStack
    ) {
        androidx.fragment.app.FragmentTransaction transaction =
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, fragment);

        if (addToBackStack) {
            transaction.addToBackStack(null);
        }

        transaction.commit();
    }
}
