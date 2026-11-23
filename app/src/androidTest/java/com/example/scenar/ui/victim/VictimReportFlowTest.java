package com.example.scenar.ui.victim;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.Intents.intending;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasAction;
import static androidx.test.espresso.matcher.ViewMatchers.isChecked;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withHint;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
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
import com.example.scenar.R;
import com.example.scenar.model.SharingConsent;
import com.example.scenar.profile.EmergencyProfile;
import com.example.scenar.profile.EmergencyProfileRepository;
import com.example.scenar.repository.IncidentRepository;
import com.example.scenar.repository.SimulatedDispatchRepository;
import com.example.scenar.ui.responder.ResponderIncident;
import com.example.scenar.ui.responder.ResponderQueue;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.regex.Pattern;
import java.util.stream.Collectors;

@RunWith(AndroidJUnit4.class)
public class VictimReportFlowTest {
    private static final long TIMEOUT_MS = 15_000;
    private static final String DESCRIPTION = "Two people are near smoke after part of a ceiling collapsed.";
    private final UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
    private final EmergencyProfileRepository profiles = EmergencyProfileRepository.getInstance(
            InstrumentationRegistry.getInstrumentation().getTargetContext());
    private EmergencyProfile originalProfile;

    @Rule
    public final IntentsRule intents = new IntentsRule();

    @After
    public void restoreProfile() {
        if (originalProfile == null) return;
        if (originalProfile.isEmpty()) profiles.clear();
        else profiles.save(originalProfile);
    }

    private void useProfile(EmergencyProfile profile) {
        originalProfile = profiles.load();
        if (profile.isEmpty()) profiles.clear();
        else profiles.save(profile);
    }

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
            onView(withId(R.id.report_emergency_button)).perform(click());
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
            onView(withHint("Leave blank if location is unknown"))
                    .perform(scrollTo(), replaceText("Practice Hall"), closeSoftKeyboard());
            onView(withHint("One medical flag per line"))
                    .perform(scrollTo(), replaceText("Allergies: Latex"), closeSoftKeyboard());

            tap("Save report as PDF");
            intended(hasAction(Intent.ACTION_CREATE_DOCUMENT));
            onView(withText("Save cancelled. Your report is still open.")).perform(scrollTo());
            onView(withText("Save report as PDF")).check(matches(isEnabled()));

            tap("Prepare practice handoff");
            visible("Practice handoff status");
            tap("Open responder demo");
            visible("Responder incident view");
            var incident = victimIncident();
            var report = incident.getReport();
            assertEquals(SharingConsent.shareNothing(), incident.getConsent());
            assertNull(report.getLocationDescription());
            assertEquals(Integer.valueOf(1), report.getPeopleAffected());
            assertTrue(report.getCriticalConditions().isEmpty());
            assertTrue(report.getMedicalFlags().isEmpty());
            assertEquals(java.util.Collections.singletonList("Blocked exit"), report.getHazards());
            assertTrue(report.getMissingInformation().contains("Confirm the exact location"));
            assertTrue(report.getUncertainties().contains("Number of affected people requires confirmation"));
            onView(withText(containsString("Blocked exit"))).perform(scrollTo());
            onView(withId(R.id.btn_simulate_notification)).check(matches(not(isEnabled())));
        } finally {
            clearHandoff();
        }
    }

    @Test
    public void selectedVictimConsentReachesResponderAndEnablesContactSimulation() {
        useProfile(new EmergencyProfile(
                "Jordan", "", "O+", "", "", "", "Needs large print", "Casey", "", "+1 250 555 0100"));
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            onView(withId(R.id.report_emergency_button)).perform(click());
            enterDescription();
            tap("Continue");
            tap("Generate practice report");
            visible("Review your report");
            onView(withHint("Leave blank if location is unknown"))
                    .perform(scrollTo(), replaceText("Practice Hall"), closeSoftKeyboard());
            onView(withHint("One medical flag per line"))
                    .perform(scrollTo(), replaceText("Allergies: Latex"), closeSoftKeyboard());

            tap("Share current location");
            tap("Share medical information");
            tap("Notify emergency contact");
            tap("Prepare practice handoff");
            tap("Open responder demo");
            visible("Responder incident view");

            var incident = victimIncident();
            assertEquals(new SharingConsent(true, true, true), incident.getConsent());
            assertEquals("Practice Hall", incident.getReport().getLocationDescription());
            assertEquals(java.util.List.of("Allergies: Latex", "Blood type: O+",
                            "Accessibility needs: Needs large print"),
                    incident.getMedicalFlagsToShow());
            onView(withText(containsString("Allergies: Latex"))).perform(scrollTo());
            onView(withId(R.id.btn_simulate_notification)).check(matches(isEnabled()));
        } finally {
            clearHandoff();
        }
    }

    @Test
    public void regeneratingEditedReportClearsAllHandoffConsent() {
        String[] consentLabels = {
                "Share current location",
                "Share medical information",
                "Notify emergency contact"
        };
        try (ActivityScenario<VictimReportActivity> scenario =
                     ActivityScenario.launch(VictimReportActivity.class)) {
            enterDescription();
            tap("Continue");
            tap("Generate practice report");
            visible("Review your report");
            for (String label : consentLabels) {
                tap(label);
                onView(withText(label)).check(matches(isChecked()));
            }

            tap("Edit incident details");
            onView(withHint("Street, room, or nearby landmark"))
                    .perform(scrollTo(), replaceText("Updated Practice Hall"), closeSoftKeyboard());
            tap("Generate practice report");
            visible("Review your report");
            for (String label : consentLabels) {
                onView(withText(label)).perform(scrollTo()).check(matches(not(isChecked())));
            }
        }
    }

    @Test
    public void missingProfileAllowsHandoffButMissingContactBlocksNotification() {
        useProfile(EmergencyProfile.empty());
        try (ActivityScenario<VictimReportActivity> scenario =
                     ActivityScenario.launch(VictimReportActivity.class)) {
            generateReportForProfileReview();
            onView(withText("No emergency profile saved.")).perform(scrollTo())
                    .check(matches(isDisplayed()));
            tap("Notify emergency contact");
            tap("Prepare practice handoff");
            visible("Emergency contact unavailable");
            onView(withText("Edit saved profile")).check(matches(isDisplayed()));
            onView(withText("Back to review")).perform(click());
            tap("Notify emergency contact");
            tap("Prepare practice handoff");
            visible("Practice handoff status");
        }
    }

    @Test
    public void savedProfileChangesReloadAfterReturningAndSurviveRecreation() {
        useProfile(new EmergencyProfile("Original name", "", "", "", "", "", "", "", "", ""));
        try (ActivityScenario<VictimReportActivity> scenario =
                     ActivityScenario.launch(VictimReportActivity.class)) {
            generateReportForProfileReview();
            onView(withText("Original name")).perform(scrollTo()).check(matches(isDisplayed()));
            tap("Share medical information");
            tap("Edit profile");
            onView(withId(R.id.preferred_name_input))
                    .perform(scrollTo(), replaceText("Updated name"), closeSoftKeyboard());
            onView(withId(R.id.save_profile_button)).perform(scrollTo(), click());
            pressBack();
            visible("Review your report");
            onView(withText("Updated name")).perform(scrollTo()).check(matches(isDisplayed()));
            onView(withText("Share medical information")).perform(scrollTo())
                    .check(matches(not(isChecked())));
            tap("Share medical information");
            scenario.recreate();
            onView(withText("Updated name")).perform(scrollTo()).check(matches(isDisplayed()));
            onView(withText("Share medical information")).perform(scrollTo())
                    .check(matches(isChecked()));
        }
    }

    private void generateReportForProfileReview() {
        enterDescription();
        tap("Continue");
        tap("Generate practice report");
        visible("Review your report");
    }

    private static ResponderIncident victimIncident() {
        var incidents = ResponderQueue.getInstance().sync(SimulatedDispatchRepository.getInstance())
                .stream().filter(incident -> !incident.isDemo()).collect(Collectors.toList());
        assertEquals(1, incidents.size());
        return incidents.get(0);
    }

    private static void clearHandoff() {
        IncidentRepository.getInstance().clear();
        SimulatedDispatchRepository.getInstance().clear();
        ResponderQueue.getInstance().reset();
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
