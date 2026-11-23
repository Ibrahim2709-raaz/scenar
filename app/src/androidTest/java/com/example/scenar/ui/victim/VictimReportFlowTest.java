package com.example.scenar.ui.victim;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.Intents.intending;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasAction;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.withHint;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.Manifest;
import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.speech.RecognizerIntent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.intent.rule.IntentsRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject2;
import androidx.test.uiautomator.Until;

import com.example.scenar.MainActivity;
import com.example.scenar.repository.IncidentRepository;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.regex.Pattern;

@RunWith(AndroidJUnit4.class)
public class VictimReportFlowTest {
    private static final long TIMEOUT_MS = 15_000;
    private static final String DESCRIPTION = "Two people are near smoke after part of a ceiling collapsed.";
    private final UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

    @Rule
    public final IntentsRule intents = new IntentsRule();

    @Test
    public void acceptsAndDeniesLocationAndKeepsManualAddressAvailable() {
        try (ActivityScenario<VictimReportActivity> scenario = ActivityScenario.launch(VictimReportActivity.class)) {
            enterDescription();
            tap("Continue");
            tap("Use device location");
            tapPermission("permission_deny_button");
            onView(withText("Location permission was denied. Enter an address or landmark instead."))
                    .perform(scrollTo());
            onView(withHint("Street, room, or nearby landmark")).perform(replaceText("Practice Hall"), closeSoftKeyboard());

            tap("Use device location");
            tapPermission("permission_allow_foreground_only_button");
            scenario.onActivity(activity -> assertEquals(PackageManager.PERMISSION_GRANTED,
                    activity.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)));
            onView(withText("Practice Hall")).check(matches(isEnabled()));
        }
    }

    @Test
    public void cancelledSpeechAndRotationPreserveTheTypedDescription() throws Exception {
        intending(hasAction(RecognizerIntent.ACTION_RECOGNIZE_SPEECH))
                .respondWith(new Instrumentation.ActivityResult(Activity.RESULT_CANCELED, null));
        try (ActivityScenario<VictimReportActivity> scenario = ActivityScenario.launch(VictimReportActivity.class)) {
            enterDescription();
            tap("Speak description");
            intended(hasAction(RecognizerIntent.ACTION_RECOGNIZE_SPEECH));
            onView(withText("No dictation was added. Try again or type your description."))
                    .perform(scrollTo());
            try {
                device.setOrientationLeft();
                device.waitForIdle();
                scenario.onActivity(activity -> assertEquals(Configuration.ORIENTATION_LANDSCAPE,
                        activity.getResources().getConfiguration().orientation));
                onView(withText(DESCRIPTION)).check(matches(isEnabled()));
            } finally {
                device.unfreezeRotation();
            }
        }
    }

    @Test
    public void cancelledPdfKeepsTheReportAndHandsStructuredDataToResponder() {
        intending(hasAction(Intent.ACTION_CREATE_DOCUMENT))
                .respondWith(new Instrumentation.ActivityResult(Activity.RESULT_CANCELED, null));
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            onView(withText("Report emergency")).perform(click());
            enterDescription();
            tap("Continue");
            tap("Generate practice report");
            visible("Review your report");
            onView(withHint("Number of people, or blank"))
                    .perform(scrollTo(), replaceText("1"), closeSoftKeyboard());
            onView(withHint("One hazard per line"))
                    .perform(scrollTo(), replaceText("Blocked exit"), closeSoftKeyboard());
            onView(withHint("One condition per line"))
                    .perform(scrollTo(), replaceText(""), closeSoftKeyboard());

            tap("Save report as PDF");
            intended(hasAction(Intent.ACTION_CREATE_DOCUMENT));
            onView(withText("Save cancelled. Your report is still open.")).perform(scrollTo());
            onView(withText("Save report as PDF")).check(matches(isEnabled()));

            tap("Open responder demo");
            visible("Responder incident view");
            var report = IncidentRepository.getInstance().getCurrentReport();
            assertNotNull(report);
            assertEquals(Integer.valueOf(1), report.getPeopleAffected());
            assertTrue(report.getCriticalConditions().isEmpty());
            assertEquals(java.util.Collections.singletonList("Blocked exit"), report.getHazards());
            assertTrue(report.getMissingInformation().contains("Confirm the exact location"));
            assertTrue(report.getUncertainties().contains("Number of affected people requires confirmation"));
            onView(withText("Blocked exit")).perform(scrollTo());
        } finally {
            IncidentRepository.getInstance().clear();
        }
    }

    private void enterDescription() {
        visible("Start practice report");
        tap("Start practice report");
        onView(withHint("Type what happened here")).perform(scrollTo(), replaceText(DESCRIPTION), closeSoftKeyboard());
    }

    private static void tap(String label) {
        onView(withText(label)).perform(scrollTo(), click());
    }

    private void tapPermission(String id) {
        UiObject2 button = device.wait(Until.findObject(By.res(Pattern.compile(".*:id/" + id))), TIMEOUT_MS);
        assertNotNull("Missing permission button " + id, button);
        button.click();
    }

    private void visible(String text) {
        assertTrue("Missing text " + text, device.wait(Until.hasObject(By.text(text)), TIMEOUT_MS));
    }
}
