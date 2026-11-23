package com.example.scenar.ui.beacon;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.not;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.scenar.R;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public final class BeaconDemoActivityTest {

    @Test
    public void screenShowsBeaconAndScannerControls() {
        try (ActivityScenario<BeaconDemoActivity> scenario =
                     ActivityScenario.launch(BeaconDemoActivity.class)) {

            onView(withText(R.string.offline_beacon_title))
                    .check(matches(isDisplayed()));

            onView(withText(R.string.beacon_simulation_warning))
                    .check(matches(isDisplayed()));

            onView(withId(R.id.start_beacon_button))
                    .check(matches(isDisplayed()))
                    .check(matches(isEnabled()));

            onView(withId(R.id.stop_beacon_button))
                    .check(matches(isDisplayed()))
                    .check(matches(not(isEnabled())));

            onView(withId(R.id.start_scan_button))
                    .check(matches(isDisplayed()))
                    .check(matches(isEnabled()));

            onView(withId(R.id.stop_scan_button))
                    .check(matches(isDisplayed()))
                    .check(matches(not(isEnabled())));
        }
    }

    @Test
    public void controlsRemainVisibleAfterRecreation() {
        try (ActivityScenario<BeaconDemoActivity> scenario =
                     ActivityScenario.launch(BeaconDemoActivity.class)) {

            scenario.recreate();

            onView(withId(R.id.start_beacon_button))
                    .check(matches(isDisplayed()));

            onView(withId(R.id.start_scan_button))
                    .check(matches(isDisplayed()));

            onView(withText(R.string.beacon_simulation_warning))
                    .check(matches(isDisplayed()));
        }
    }
}