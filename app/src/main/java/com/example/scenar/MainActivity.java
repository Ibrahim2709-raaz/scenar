package com.example.scenar;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.example.scenar.ui.home.HomeFragment;
import com.example.scenar.ui.report.ReportFragment;
import com.example.scenar.ui.responder.ResponderFragment;
import com.example.scenar.ui.review.ReviewFragment;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.enableEdgeToEdge(getWindow());
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        if (savedInstanceState == null) {
            showHome();
        }
    }

    public void showHome() {
        navigateTo(new HomeFragment(), false);
    }

    public void showReport() {
        navigateTo(new ReportFragment(), true);
    }

    public void showReview() {
        navigateTo(new ReviewFragment(), true);
    }

    public void showResponderDashboard() {
        navigateTo(new ResponderFragment(), true);
    }

    private void navigateTo(Fragment fragment, boolean addToBackStack) {
        androidx.fragment.app.FragmentTransaction transaction = getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment);

        if (addToBackStack) {
            transaction.addToBackStack(null);
        }

        transaction.commit();
    }
}
