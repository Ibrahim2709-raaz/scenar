package com.example.scenar.ui.responder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.example.scenar.model.DispatchStatus;
import com.example.scenar.model.EmergencyFacility;
import com.example.scenar.model.IncidentReport;
import com.example.scenar.model.SharingConsent;
import com.example.scenar.repository.SimulatedDispatchRepository;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ResponderSimulationLabelsTest {
    /** Claims the responder screen must never make. */
    private static final Pattern FORBIDDEN = Pattern.compile(
            "\\b(sent|send|sending|verified|verify|broadcast|broadcasting|live)\\b",
            Pattern.CASE_INSENSITIVE);

    private final SimulatedDispatchRepository repository = SimulatedDispatchRepository.getInstance();
    private ResponderQueue queue;

    @Before
    public void setUp() {
        repository.clear();
        queue = new ResponderQueue();
    }

    @After
    public void tearDown() {
        repository.clear();
    }

    @Test
    public void responderStatusLabelsSaySimulation() {
        for (DispatchStatus status : DispatchStatusStyle.RESPONDER_STEPS) {
            String label = ResponderCopy.statusLabel(status).toLowerCase(Locale.ROOT);
            assertTrue(label, label.contains("simulation"));
        }
    }

    @Test
    public void statusChangeIsRecordedAsSimulated() {
        ResponderIncident incident = queue.submit(new IncidentReport(),
                SharingConsent.shareNothing(), repository);

        assertTrue(queue.updateStatus(repository, incident, DispatchStatus.REVIEWING));

        TimelineEntry latest = incident.getTimeline().get(0);
        assertTrue(latest.getTitle().startsWith("Simulated status change"));
        assertTrue(latest.getDetail().contains("No responders were contacted"));
    }

    @Test
    public void notificationLogSaysSimulatedAndNotDelivered() {
        ResponderIncident incident = queue.submit(new IncidentReport(),
                new SharingConsent(false, false, true), repository);

        assertTrue(incident.canSimulateNotification());
        incident.logSimulatedNotification(ResponderCopy.DEMO_CONTACT, "Test message");

        String entry = incident.getNotificationLog().get(0);
        assertTrue(entry.contains("Simulated only, not delivered"));
        assertEquals(ResponderCopy.NOTIFICATION_TIMELINE_TITLE, incident.getTimeline().get(0).getTitle());
        assertFalse(FORBIDDEN.matcher(entry).find());
    }

    @Test
    public void notificationSimulationIsOffWithoutContactConsent() {
        ResponderIncident incident = queue.submit(new IncidentReport(),
                SharingConsent.shareReportDetails(), repository);

        assertFalse(incident.canSimulateNotification());
        try {
            incident.logSimulatedNotification(ResponderCopy.DEMO_CONTACT, "Test message");
            fail("Expected the simulation to refuse without consent");
        } catch (IllegalStateException expected) {
            assertTrue(incident.getNotificationLog().isEmpty());
        }
    }

    @Test
    public void demoFacilitiesAreLabelledFictional() {
        for (EmergencyFacility facility : DemoFacilities.all()) {
            assertTrue(facility.source().toLowerCase(Locale.ROOT).contains("fictional"));
        }
    }

    @Test
    public void blePreviewIsMarkedAsDemo() {
        String json = BlePayloadPreview.from("INC-TEST", new IncidentReport()).getJson();
        assertTrue(json.contains("\"demo\":true"));
        assertTrue(ResponderCopy.BLE_PREVIEW_NOTE.contains("nothing is transmitted"));
    }

    @Test
    public void responderCopyMakesNoRealWorldClaims() throws IllegalAccessException {
        List<String> texts = new ArrayList<>();
        for (Field field : ResponderCopy.class.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) && field.getType() == String.class) {
                texts.add((String) field.get(null));
            }
        }
        for (DispatchStatus status : DispatchStatus.values()) {
            texts.add(ResponderCopy.statusLabel(status));
        }
        assertNoForbiddenWords("ResponderCopy", texts);
    }

    @Test
    public void responderLayoutAndStringsMakeNoRealWorldClaims() throws IOException {
        String layout = read("src/main/res/layout/fragment_responder.xml");
        String strings = read("src/main/res/values/strings.xml");

        List<String> texts = new ArrayList<>();
        Matcher literal = Pattern.compile("android:(?:text|hint)=\"([^\"@][^\"]*)\"").matcher(layout);
        while (literal.find()) texts.add(literal.group(1));

        Set<String> names = new LinkedHashSet<>();
        Matcher ref = Pattern.compile("@string/([a-z0-9_]+)").matcher(layout);
        while (ref.find()) names.add(ref.group(1));
        for (String name : names) {
            Matcher value = Pattern.compile("<string name=\"" + name + "\">(.*?)</string>").matcher(strings);
            assertTrue("Missing string " + name, value.find());
            texts.add(value.group(1));
        }
        assertFalse(texts.isEmpty());
        assertNoForbiddenWords("fragment_responder.xml", texts);
    }

    private static void assertNoForbiddenWords(String where, List<String> texts) {
        for (String text : texts) {
            assertNotNull(text);
            Matcher matcher = FORBIDDEN.matcher(text);
            if (matcher.find()) {
                fail(where + " says \"" + matcher.group() + "\" in: " + text);
            }
        }
    }

    /** Unit tests run from the module directory under Gradle; also allow the repo root. */
    private static String read(String modulePath) throws IOException {
        File file = new File(modulePath);
        if (!file.exists()) file = new File("app", modulePath);
        return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
    }
}
