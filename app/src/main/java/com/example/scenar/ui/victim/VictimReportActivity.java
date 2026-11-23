package com.example.scenar.ui.victim;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognizerIntent;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.core.location.LocationManagerCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.scenar.ai.IncidentAiService;
import com.example.scenar.ai.IncidentAiServiceFactory;
import com.example.scenar.model.DispatchRecord;
import com.example.scenar.model.IncidentReport;
import com.example.scenar.model.SharingConsent;
import com.example.scenar.profile.EmergencyProfile;
import com.example.scenar.profile.EmergencyProfileRepository;
import com.example.scenar.profile.ProfileStorageException;
import com.example.scenar.repository.IncidentRepository;
import com.example.scenar.repository.SimulatedDispatchRepository;
import com.example.scenar.ui.profile.ProfileActivity;

import org.json.JSONException;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Consumer;
import android.widget.ProgressBar;


public final class VictimReportActivity extends Activity {
    public static final String EXTRA_DISPATCH_ID = "com.example.scenar.extra.DISPATCH_ID";
    private static final int INK = 0xff101828;
    private static final int GREEN = 0xff1976d2;
    private static final int MUTED = 0xff526071;
    private static final int PAPER = 0xfff4f7fb;
    private static final int SOFT = 0xffe8f2ff;
    private static final int RED = 0xffd92d20;
    private static final int PEACH = 0xfffeeceb;
    private static final int LINE = 0xffd4dce7;
    private static final int SPEECH_REQUEST = 1;
    private static final int LOCATION_REQUEST = 2;
    private static final int EXPORT_REQUEST = 3;
    private static final int PROFILE_REQUEST = 4;
    private static final int DESCRIPTION_LIMIT = 2500;
    private static final int SUMMARY_LIMIT = 4000;
    private static final String[] CATEGORIES = {
            "unspecified", "medical", "injury", "fire", "personal-safety", "other"
    };
    private static final String[] CATEGORY_LABELS = {
            "Unsure or not specified", "Medical incident", "Injury or accident",
            "Fire or smoke", "Personal safety", "Other"
    };

    private enum Screen { HOME, DETAILS, CONTEXT, REVIEW, DISPATCH }

    private enum ExportFormat {
        PDF("application/pdf", "pdf"), JSON("application/json", "json");

        final String mimeType;
        final String extension;

        ExportFormat(String mimeType, String extension) {
            this.mimeType = mimeType;
            this.extension = extension;
        }
    }

    private record Export(ExportFormat format, VictimReportDraft.Report report, IncidentReport reviewed,
                          List<ReportPdf.Field> fields) {
        void writeTo(OutputStream output) throws IOException, JSONException {
            if (format == ExportFormat.PDF) {
                ReportPdf.write(output, report.generatedAt(), reviewed.getSummary(), fields);
            } else {
                String json = report.toJson(reviewed).toString(2) + "\n";
                output.write(json.getBytes(StandardCharsets.UTF_8));
            }
        }
    }

    private static final class Draft {
        Screen screen = Screen.HOME;
        int category;
        String description = "";
        String address = "";
        String allergies = "";
        String medications = "";
        String conditions = "";
        IncidentReview review;
        EmergencyProfile profile = EmergencyProfile.empty();
        boolean profileUnavailable;
        boolean shareLocation;
        boolean shareMedicalFlags;
        boolean simulateContactNotification;
        PracticeDispatch practiceDispatch;
        String voiceNotice = "";
        String locationNotice = "";
        String generationNotice = "";
        boolean generationCanRetry;
        String exportNotice = "";
        VictimReportDraft.Coordinates coordinates;
        VictimReportDraft.Report report;
        CompletableFuture<VictimReportDraft.Report> generation;
        CompletableFuture<Void> export;
        Export pendingExport;
    }

    private Draft draft;
    private final IncidentAiService incidentAiService =
            IncidentAiServiceFactory.createConfiguredService();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private CancellationSignal locationRequest;
    private LinearLayout content;
    private EditText descriptionField;
    private final EnumMap<IncidentReview.Field, EditText> reviewFields =
            new EnumMap<>(IncidentReview.Field.class);
    private Spinner categoryField;
    private TextView voiceStatus;
    private TextView locationStatus;
    private TextView generationStatus;
    private ProgressBar generationProgress;
    private Button retryGenerationButton;
    private TextView exportStatus;
    private Button locateButton;
    private Button clearLocationButton;
    private Button generateButton;
    private Button pdfButton;
    private Button jsonButton;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        WindowCompat.enableEdgeToEdge(getWindow());
        Object retained = getLastNonConfigurationInstance();
        draft = retained instanceof Draft previous ? previous : new Draft();
        render();
        if (draft.generation != null) observeGeneration(draft.generation);
        if (draft.export != null) observeExport(draft.export);
    }

    @Override
    public Object onRetainNonConfigurationInstance() {
        if (draft.screen == Screen.DETAILS) draft.category = categoryField.getSelectedItemPosition();
        return draft;
    }

    @Override
    protected void onDestroy() {
        if (locationRequest != null) {
            cancelLocation();
            draft.locationNotice = "Location request ended. You can try again.";
        }
        super.onDestroy();
    }

    private void go(Screen next) {
        View focused = getCurrentFocus();
        if (focused != null) {
            getSystemService(InputMethodManager.class)
                    .hideSoftInputFromWindow(focused.getWindowToken(), 0);
        }
        if (draft.screen == Screen.CONTEXT && next != Screen.CONTEXT) {
            if (locationRequest != null) draft.locationNotice = "";
            cancelLocation();
        }
        draft.screen = next;
        render();
    }

    @Override
    public void onBackPressed() {
        switch (draft.screen) {
            case DISPATCH -> go(Screen.REVIEW);
            case REVIEW -> {
                if (draft.export == null && draft.pendingExport == null) go(Screen.CONTEXT);
            }
            case CONTEXT -> backToDetails();
            case DETAILS -> backToHome();
            case HOME -> super.onBackPressed();
        }
    }

    private void render() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(PAPER);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        ViewCompat.setOnApplyWindowInsetsListener(scroll, (view, windowInsets) -> {
            var edges = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout() | WindowInsetsCompat.Type.ime());
            view.setPadding(edges.left, edges.top, edges.right, edges.bottom);
            return windowInsets;
        });

        content = column();
        content.setPadding(dp(20), dp(16), dp(20), dp(28));
        scroll.addView(content);
        setContentView(scroll);
        ViewCompat.requestApplyInsets(scroll);

        add(content, text("SCENAR  ·  Practice mode", 16, GREEN, true), 0);
        switch (draft.screen) {
            case HOME -> showHome();
            case DETAILS -> showDetails();
            case CONTEXT -> showContext();
            case REVIEW -> showReview();
            case DISPATCH -> showDispatchStatus();
        }
    }

    private void showHome() {
        title("Start an incident report");
        LinearLayout help = card(PEACH);
        TextView emergency = text("In an emergency?", 22, RED, true);
        ViewCompat.setAccessibilityHeading(emergency, true);
        add(help, emergency, 0);
        add(help, text("This is a practice flow. In a real emergency, contact your local emergency services.",
                15, INK, false), 6);
        add(content, help, 18);
        add(content, text("Describe what happened, add optional details, and review a draft.",
                17, MUTED, false), 20);
        add(content, primary("Start practice report", () -> go(Screen.DETAILS)), 14);
        add(content, text("Use fictional details. This demo saves drafts only and cannot dispatch help.",
                14, MUTED, false), 12);
    }

    private void showDetails() {
        title("What happened?");
        step("Step 1 of 3  ·  Incident details");
        categoryField = new Spinner(this);
        categoryField.setId(View.generateViewId());
        categoryField.setMinimumHeight(dp(48));
        ArrayAdapter<String> categories = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, CATEGORY_LABELS);
        categories.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categoryField.setAdapter(categories);
        categoryField.setSelection(draft.category);
        label(content, "Incident type", categoryField, 18);
        add(content, categoryField, 2);

        descriptionField = field(content, "Description (required)", draft.description,
                "Type what happened here", 4, DESCRIPTION_LIMIT, value -> draft.description = value);
        add(content, secondary("Speak description", this::startDictation), 12);
        voiceStatus = status(content, draft.voiceNotice.isEmpty()
                ? "Your phone’s speech service may process audio online. Use fictional details."
                : draft.voiceNotice);
        add(content, primary("Continue", () -> {
            draft.category = categoryField.getSelectedItemPosition();
            if (draft.description.trim().isEmpty()) {
                descriptionField.setError("Enter a short description or use dictation.");
                descriptionField.requestFocus();
                return;
            }
            go(Screen.CONTEXT);
        }), 20);
        add(content, secondary("Back", this::backToHome), 8);
    }

    private void showContext() {
        title("Location and medical info");
        step("Step 2 of 3  ·  Both sections are optional");
        LinearLayout place = card(Color.WHITE);
        heading(place, "Incident location");
        field(place, "Address or landmark", draft.address, "Street, room, or nearby landmark",
                1, 300, value -> draft.address = value);
        locateButton = secondary("Use device location", this::requestLocationPermission);
        add(place, locateButton, 12);
        clearLocationButton = secondary("Remove coordinates", () -> {
            cancelLocation();
            draft.coordinates = null;
            draft.locationNotice = "Coordinates removed.";
            updateLocationControls();
        });
        add(place, clearLocationButton, 8);
        locationStatus = status(place, "");
        add(content, place, 16);
        updateLocationControls();

        LinearLayout medical = card(Color.WHITE);
        heading(medical, "Medical profile");
        add(medical, text("Add relevant details for the person affected.", 14, MUTED, false), 4);
        field(medical, "Allergies", draft.allergies, "", 1, 300, value -> draft.allergies = value);
        field(medical, "Medications", draft.medications, "", 1, 300,
                value -> draft.medications = value);
        field(medical, "Medical conditions or access needs", draft.conditions, "", 2, 600,
                value -> draft.conditions = value);
        add(content, medical, 14);

        generateButton = primary(
                "Generate report",
                this::generate
        );
        add(content, generateButton, 16);

        generationProgress = new ProgressBar(this);
        generationProgress.setIndeterminate(true);
        generationProgress.setContentDescription(
                "SCENAR is organizing the incident report"
        );
        add(content, generationProgress, 12);

        generationStatus = status(content, "");

        retryGenerationButton = secondary(
                "Try generating again",
                this::generate
        );
        add(content, retryGenerationButton, 8);

        add(
                content,
                secondary("Back", this::backToDetails),
                8
        );

        setGenerating(draft.generation != null);
    }

    private void showReview() {
        reloadProfile();

        title("Review your report");
        step("Step 3 of 3  ·  Check the draft for accuracy");

        addGenerationSourceBanner();

        LinearLayout facts = card(Color.WHITE);
        heading(facts, "Reported details");
        add(facts, text("Check and correct any simulated AI details before continuing. Clear a field if the information is unknown.",
                14, MUTED, false), 8);
        fact(facts, "Original description", draft.report.input().description());
        reviewFields.clear();
        for (IncidentReview.Field detail : IncidentReview.Field.values()) {
            String value = draft.review.get(detail);
            EditText editor = field(facts, detail.label, value, detail.hint, detail.lines,
                    Math.max(SUMMARY_LIMIT, value.length()), updated -> {
                        draft.review.set(detail, updated);
                        if (draft.export == null && draft.pendingExport == null) showExportStatus("");
                    });
            reviewFields.put(detail, editor);
        }
        add(content, facts, 14);
        add(content, secondary("Maps and nearby hospitals", this::openRouting), 12);

        add(content, text("Save a copy or open the responder demo on this device. The app does not dispatch help.",
                14, MUTED, false), 12);
        pdfButton = primary("Save report as PDF", () -> exportReport(ExportFormat.PDF));
        jsonButton = secondary("Save report as JSON", () -> exportReport(ExportFormat.JSON));
        add(content, pdfButton, 12);
        add(content, jsonButton, 8);
        updateExportButtons();
        exportStatus = status(content, draft.export != null ? "Saving report…" : draft.exportNotice);
        addDispatchOptions();
        add(content, secondary("Edit incident details", () -> {
            if (draft.pendingExport == null && draft.export == null) go(Screen.CONTEXT);
        }), 8);
        add(content, secondary("Clear report and start over", this::confirmClear), 8);
    }

    private void addGenerationSourceBanner() {
        if (draft.report == null) {
            return;
        }

        boolean usedFallback =
                draft.report.usedLocalFallback();

        int backgroundColour = usedFallback
                ? 0xfffff4e5
                : 0xffeaf4ff;

        LinearLayout banner =
                card(backgroundColour);

        heading(
                banner,
                usedFallback
                        ? "Created with local fallback"
                        : "AI-assisted draft"
        );

        String message;

        if (usedFallback) {
            message = draft.report.generationMessage();

            if (message == null
                    || message.isBlank()) {
                message =
                        "The online AI service was unavailable, "
                                + "so SCENAR organized this report "
                                + "on the device.";
            }
        } else {
            message =
                    "SCENAR used the configured AI service to "
                            + "organize the information. Review every "
                            + "field before continuing.";
        }

        add(
                banner,
                text(message, 14, MUTED, false),
                8
        );

        add(
                content,
                banner,
                14
        );
    }

    private void reloadProfile() {
        EmergencyProfile loaded;

        try {
            loaded = EmergencyProfileRepository
                    .getInstance(this)
                    .load();

            draft.profileUnavailable = false;

        } catch (ProfileStorageException
                 | SecurityException
                 | ClassCastException error) {
            loaded = EmergencyProfile.empty();
            draft.profileUnavailable = true;
        }

        if (!loaded.equals(draft.profile)) {
            draft.shareMedicalFlags = false;
            draft.simulateContactNotification = false;
        }

        draft.profile = loaded;
    }

    private void editProfile() {
        if (draft.pendingExport == null
                && draft.export == null) {
            startActivityForResult(
                    new Intent(
                            this,
                            ProfileActivity.class
                    ),
                    PROFILE_REQUEST
            );
        }
    }

    private void addProfileSummary() {
        LinearLayout card =
                card(Color.WHITE);

        heading(
                card,
                "Saved emergency profile"
        );

        if (draft.profileUnavailable) {
            status(
                    card,
                    "The saved profile could not be loaded. "
                            + "You can continue without it or edit "
                            + "the profile."
            );

        } else if (draft.profile.isEmpty()) {
            status(
                    card,
                    "No emergency profile saved."
            );
        }

        EmergencyProfile profile =
                draft.profile;

        fact(
                card,
                "Preferred name",
                provided(profile.preferredName())
        );
        fact(
                card,
                "Blood type",
                provided(profile.bloodType())
        );
        fact(
                card,
                "Allergies",
                provided(profile.allergies())
        );
        fact(
                card,
                "Medications",
                provided(profile.medications())
        );
        fact(
                card,
                "Medical conditions",
                provided(profile.medicalConditions())
        );
        fact(
                card,
                "Accessibility needs",
                provided(profile.accessibilityNeeds())
        );
        fact(
                card,
                "Emergency contact name",
                provided(profile.emergencyContactName())
        );
        fact(
                card,
                "Emergency contact phone",
                provided(profile.emergencyContactPhone())
        );

        add(
                card,
                secondary(
                        "Edit profile",
                        this::editProfile
                ),
                8
        );

        add(content, card, 12);
    }

    private void addDispatchOptions() {
        addProfileSummary();
        LinearLayout options = card(Color.WHITE);
        heading(options, "Practice handoff");
        add(options, text("Medical sharing includes reviewed flags and the saved medical information above. "
                + "Check the other report fields for private details. Contact notification is simulated.",
                14, MUTED, false), 8);
        add(options, checkBox("Share current location", draft.shareLocation,
                checked -> draft.shareLocation = checked), 8);
        add(options, checkBox("Share medical information", draft.shareMedicalFlags,
                checked -> draft.shareMedicalFlags = checked), 8);
        add(options, checkBox("Notify emergency contact", draft.simulateContactNotification,
                checked -> draft.simulateContactNotification = checked), 4);
        add(options, primary("Prepare practice handoff", this::preparePracticeHandoff), 8);
        add(content, options, 12);
    }

    private CheckBox checkBox(String label, boolean checked, Consumer<Boolean> update) {
        CheckBox box = new CheckBox(this);
        box.setText(label);
        box.setTextColor(INK);
        box.setMinHeight(dp(48));
        box.setChecked(checked);
        box.setOnCheckedChangeListener((button, selected) -> update.accept(selected));
        return box;
    }

    private List<ReportPdf.Field> reportFields(IncidentReport reviewed) {
        VictimReportDraft.Input input = draft.report.input();
        String coordinates = reviewed.getLatitude() == null ? ""
                : String.format(Locale.ROOT, "%.5f, %.5f", reviewed.getLatitude(), reviewed.getLongitude());
        return Arrays.asList(
                new ReportPdf.Field("Incident type", valueOrEmpty(reviewed.getIncidentType())),
                new ReportPdf.Field("Original description", input.description()),
                new ReportPdf.Field("People affected", valueOrEmpty(reviewed.getPeopleAffected())),
                new ReportPdf.Field("Critical conditions", String.join("\n", reviewed.getCriticalConditions())),
                new ReportPdf.Field("Hazards", String.join("\n", reviewed.getHazards())),
                new ReportPdf.Field("Address or landmark", valueOrEmpty(reviewed.getLocationDescription())),
                new ReportPdf.Field("Coordinates", coordinates),
                new ReportPdf.Field("Medical flags", String.join("\n", reviewed.getMedicalFlags())),
                new ReportPdf.Field("Missing information", String.join("\n", reviewed.getMissingInformation())),
                new ReportPdf.Field("Uncertainties", String.join("\n", reviewed.getUncertainties())));
    }

    private static String valueOrEmpty(Object value) {
        return value == null ? "" : value.toString();
    }

    private static String provided(Object value) {
        String text = valueOrEmpty(value);
        return text.isBlank() ? "Not provided" : text;
    }

    private void preparePracticeHandoff() {
        if (draft.pendingExport != null || draft.export != null) return;
        IncidentReport reviewed = validatedReport();
        if (reviewed == null) return;
        try {
            draft.practiceDispatch = new PracticeDispatch(reviewed, draft.profile, new SharingConsent(
                    draft.shareLocation, draft.shareMedicalFlags, draft.simulateContactNotification));
        } catch (PracticeDispatch.MissingEmergencyContact error) {
            new AlertDialog.Builder(this)
                    .setTitle("Emergency contact unavailable")
                    .setMessage("Add an emergency contact name and valid phone number in your profile, "
                            + "or turn off contact notification to continue.")
                    .setPositiveButton("Edit saved profile", (dialog, button) -> editProfile())
                    .setNegativeButton("Back to review", null)
                    .show();
            return;
        }
        go(Screen.DISPATCH);
    }

    private void showDispatchStatus() {
        title("Practice handoff status");
        LinearLayout status = card(Color.WHITE);
        heading(status, "Report ready for local responder demo");
        fact(status, "Dispatch", "Simulated only. No emergency services contacted.");
        IncidentReport shared = draft.practiceDispatch.sharedReport();
        fact(status, "Incident type", provided(shared.getIncidentType()));
        fact(status, "Summary", provided(shared.getSummary()));
        fact(status, "People affected", provided(shared.getPeopleAffected()));
        fact(status, "Critical conditions", provided(String.join("\n", shared.getCriticalConditions())));
        fact(status, "Hazards", provided(String.join("\n", shared.getHazards())));
        fact(status, "Address or landmark", provided(shared.getLocationDescription()));
        fact(status, "Coordinates", shared.getLatitude() == null || shared.getLongitude() == null
                ? "None shared"
                : String.format(Locale.ROOT, "%.5f, %.5f", shared.getLatitude(), shared.getLongitude()));
        fact(status, "Medical flags", shared.getMedicalFlags().isEmpty()
                ? "None shared" : String.join("\n", shared.getMedicalFlags()));
        fact(status, "Missing information", provided(String.join("\n", shared.getMissingInformation())));
        fact(status, "Uncertainties", provided(String.join("\n", shared.getUncertainties())));
        fact(status, "Emergency contact", draft.practiceDispatch.allowsContactNotification()
                ? "The responder can simulate a notification. Contact details stay in the saved profile."
                : "No notification requested.");
        add(content, status, 14);
        add(content, primary("Open responder demo", this::openResponderDemo), 12);
        add(content, secondary("Back to review", () -> go(Screen.REVIEW)), 8);
    }

    private void openResponderDemo() {
        if (isFinishing()) return;
        DispatchRecord record = draft.practiceDispatch.submit(SimulatedDispatchRepository.getInstance());
        IncidentRepository.getInstance().setCurrentReport(record.getReport());
        setResult(RESULT_OK, new Intent().putExtra(EXTRA_DISPATCH_ID, record.getIncidentId()));
        finish();
    }

    private void startDictation() {
        Intent speech = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                .putExtra(RecognizerIntent.EXTRA_PROMPT, "Describe the practice incident");
        try {
            startActivityForResult(speech, SPEECH_REQUEST);
        } catch (ActivityNotFoundException | SecurityException error) {
            draft.voiceNotice = "Speech input is unavailable on this device. You can type the description.";
            voiceStatus.setText(draft.voiceNotice);
            descriptionField.requestFocus();
        }
    }

    private void requestLocationPermission() {
        if (hasLocationPermission()) {
            getLocation();
        } else {
            requestPermissions(new String[] {Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_REQUEST);
        }
    }

    private boolean hasLocationPermission() {
        return checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int request, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(request, permissions, results);
        if (request != LOCATION_REQUEST || draft.screen != Screen.CONTEXT) return;
        if (hasLocationPermission()) getLocation();
        else locationMessage("Location permission was denied. Enter an address or landmark instead.");
    }

    private void getLocation() {
        LocationManager manager = getSystemService(LocationManager.class);
        if (manager == null || !LocationManagerCompat.isLocationEnabled(manager)) {
            locationMessage("Device location is off. Turn it on or enter an address or landmark.");
            return;
        }
        boolean precise = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
        String provider = precise && manager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                ? LocationManager.GPS_PROVIDER : LocationManager.NETWORK_PROVIDER;
        if (!manager.isProviderEnabled(provider)) {
            locationMessage("No location provider is available. Enter an address or landmark.");
            return;
        }
        CancellationSignal signal = new CancellationSignal();
        locationRequest = signal;
        locationMessage("Getting device location… You can enter a location manually.");
        handler.postDelayed(() -> {
            if (locationRequest != signal) return;
            cancelLocation();
            locationMessage("Location timed out. You can retry or enter an address or landmark.");
        }, 12000);

        try {
            LocationManagerCompat.getCurrentLocation(manager, provider, signal, handler::post, point -> {
                if (locationRequest != signal) return;
                locationRequest = null;
                if (point == null) {
                    locationMessage("No location was received. You can retry or enter an address or landmark.");
                    return;
                }
                draft.coordinates = new VictimReportDraft.Coordinates(point.getLatitude(),
                        point.getLongitude(), point.hasAccuracy() ? point.getAccuracy() : null);
                locationMessage("Coordinates added.");
            });
        } catch (IllegalArgumentException | SecurityException error) {
            cancelLocation();
            locationMessage("Device location is unavailable. Enter an address or landmark instead.");
        }
    }

    private void cancelLocation() {
        CancellationSignal signal = locationRequest;
        locationRequest = null;
        if (signal != null) signal.cancel();
    }

    private void locationMessage(String message) {
        draft.locationNotice = message;
        updateLocationControls();
    }

    private void updateLocationControls() {
        locateButton.setEnabled(locationRequest == null);
        clearLocationButton.setVisibility(draft.coordinates == null ? View.GONE : View.VISIBLE);
        String message = draft.locationNotice;
        if (draft.coordinates != null) message += "\n" + format(draft.coordinates);
        if (message.trim().isEmpty()) message = "Location is included only if you choose to add it.";
        locationStatus.setText(message.trim());
    }

    private void openRouting() {
        if (draft.pendingExport != null || draft.export != null) return;
        try {
            showRouting(draft.review.location());
        } catch (IncidentReview.InvalidField invalid) {
            showReviewError(invalid);
        }
    }

    private void showRouting(IncidentReview.Location location) {
        ScrollView scroll = new ScrollView(this);
        LinearLayout routing = column();
        routing.setPadding(dp(12), 0, dp(12), dp(12));
        scroll.addView(routing);
        VictimMapSearch incident = VictimMapSearch.forIncident(location);
        VictimMapSearch hospitals = VictimMapSearch.forHospitals(location);
        LinearLayout routes = card(Color.WHITE);
        heading(routes, "Open in a map app");
        add(routes, text(incident == null
                ? "No incident location is set. Your map app may use its own location settings."
                : "Check your reviewed location before passing it to a map app.",
                14, MUTED, false), 8);
        if (incident != null) {
            fact(routes, "Reviewed location", incident.query());
        }
        TextView routingStatus = status(routes, "");
        if (incident != null) {
            add(routes, secondary("View incident location", () -> openMap(incident, routingStatus)), 8);
        }
        add(routes, primary("Search hospitals in Maps", () -> openMap(hospitals, routingStatus)), 8);
        add(routing, routes, 0);

        LinearLayout guidance = card(Color.WHITE);
        heading(guidance, "Before travelling");
        add(guidance, text("Map results may vary. Verify the facility’s address and services with the facility. "
                + "In a real emergency, contact your local emergency services.", 14, INK, false), 8);
        add(guidance, secondary("Source: Google Maps Help", () -> openExternal(
                Uri.parse("https://support.google.com/maps/answer/4610185"), routingStatus,
                "No app is available to open the help page.")), 8);
        add(routing, guidance, 12);
        new AlertDialog.Builder(this).setTitle("Maps and nearby hospitals")
                .setView(scroll).setNegativeButton("Back to review", null).show();
    }

    private void openMap(VictimMapSearch search, TextView routingStatus) {
        Uri uri = Uri.parse("geo:" + search.center() + "?q=" + Uri.encode(search.query()));
        Runnable open = () -> openExternal(uri, routingStatus,
                "No compatible map app is available. Search for a hospital in your preferred map service.");
        if (search.usesIncidentLocation()) {
            new AlertDialog.Builder(this)
                    .setTitle("Share location with Maps?")
                    .setMessage("The map app will receive the reviewed address or coordinates.")
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Open Maps", (dialog, which) -> open.run())
                    .show();
        } else {
            open.run();
        }
    }

    private void openExternal(Uri uri, TextView routingStatus, String unavailableMessage) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, uri));
            routingStatus.setText("");
        } catch (ActivityNotFoundException | SecurityException error) {
            routingStatus.setText(unavailableMessage);
        }
    }

    private String format(VictimReportDraft.Coordinates point) {
        String position = String.format(Locale.ROOT, "%.5f, %.5f",
                point.latitude(), point.longitude());
        return point.accuracyMeters() == null ? position
                : position + " · accuracy about " + Math.round(point.accuracyMeters()) + " m";
    }

    private VictimReportDraft.Input input() {
        return new VictimReportDraft.Input(CATEGORIES[draft.category], draft.description.trim(),
                draft.address.trim(), draft.coordinates,
                new VictimReportDraft.MedicalProfile(draft.allergies.trim(), draft.medications.trim(),
                        draft.conditions.trim()));
    }

    private void backToDetails() {
        draft.generation = null;
        draft.generationNotice = "";
        go(Screen.DETAILS);
    }

    private void backToHome() {
        draft.category = categoryField.getSelectedItemPosition();
        go(Screen.HOME);
    }

    private void generate() {
        if (draft.generation != null) {
            return;
        }

        if (draft.description.trim().isEmpty()) {
            go(Screen.DETAILS);
            descriptionField.setError(
                    "Enter a short description or use dictation."
            );
            descriptionField.requestFocus();
            return;
        }

        if (locationRequest != null) {
            cancelLocation();
            locationMessage(
                    "Location request stopped. "
                            + "Only captured coordinates are included."
            );
        }

        VictimReportDraft.Input input = input();

        if (draft.report != null
                && draft.report.input().equals(input)) {
            go(Screen.REVIEW);
            return;
        }

        draft.generationCanRetry = false;
        draft.generationNotice = "";

        setGenerating(true);

        draft.generation =
                VictimReportDraft.generateIncidentReport(
                        input,
                        incidentAiService
                );

        observeGeneration(draft.generation);
    }

    private void setGenerating(boolean busy) {
        if (generateButton != null) {
            generateButton.setEnabled(!busy);
            generateButton.setText(
                    busy
                            ? "Organizing report…"
                            : "Generate report"
            );
        }

        if (generationProgress != null) {
            generationProgress.setVisibility(
                    busy
                            ? View.VISIBLE
                            : View.GONE
            );
        }

        if (generationStatus != null) {
            generationStatus.setText(
                    busy
                            ? "SCENAR is organizing the details. "
                            + "This may take a few seconds."
                            : draft.generationNotice
            );
        }

        if (retryGenerationButton != null) {
            retryGenerationButton.setVisibility(
                    !busy && draft.generationCanRetry
                            ? View.VISIBLE
                            : View.GONE
            );
            retryGenerationButton.setEnabled(!busy);
        }
    }

    private void observeGeneration(
            CompletableFuture<VictimReportDraft.Report> task
    ) {
        task.whenComplete(
                (report, error) -> handler.post(() -> {
                    if (isDestroyed()
                            || draft.generation != task) {
                        return;
                    }

                    draft.generation = null;

                    boolean invalidReport =
                            report == null
                                    || report.summary() == null
                                    || report.summary()
                                    .trim()
                                    .isEmpty()
                                    || report.summary().length()
                                    > SUMMARY_LIMIT;

                    if (error != null || invalidReport) {
                        draft.generationCanRetry = true;
                        draft.generationNotice =
                                generationFailureMessage(error);
                        setGenerating(false);
                        return;
                    }

                    if (!report.input().equals(input())) {
                        draft.generationCanRetry = true;
                        draft.generationNotice =
                                "Your details changed while the report "
                                        + "was being generated. Generate "
                                        + "again to include the changes.";
                        setGenerating(false);
                        return;
                    }

                    draft.generationCanRetry = false;
                    draft.generationNotice =
                            report.usedLocalFallback()
                                    ? report.generationMessage()
                                    : "";
                    draft.exportNotice = "";
                    draft.report = report;

                    draft.review = new IncidentReview(
                            report.toReviewedIncident(
                                    CATEGORY_LABELS[draft.category],
                                    report.summary()
                            )
                    );

                    draft.shareLocation = false;
                    draft.shareMedicalFlags = false;
                    draft.simulateContactNotification = false;

                    go(Screen.REVIEW);
                })
        );
    }

    private String generationFailureMessage(
            Throwable error
    ) {
        Throwable cause = error;

        while (cause instanceof CompletionException
                && cause.getCause() != null) {
            cause = cause.getCause();
        }

        if (cause instanceof IllegalArgumentException
                && cause.getMessage() != null
                && !cause.getMessage().isBlank()) {
            return cause.getMessage()
                    + " Your entered details are still available.";
        }

        return "The report could not be generated. "
                + "Your entered details are still available. "
                + "Please try again.";
    }
    private IncidentReport validatedReport() {
        try {
            return draft.review.toReport();
        } catch (IncidentReview.InvalidField invalid) {
            showReviewError(invalid);
            return null;
        }
    }

    private void showReviewError(IncidentReview.InvalidField invalid) {
        EditText editor = reviewFields.get(invalid.field);
        editor.requestFocus();
        editor.setError(invalid.getMessage());
    }

    private void exportReport(ExportFormat format) {
        IncidentReport reviewed = validatedReport();
        if (reviewed == null) return;
        try {
            draft.pendingExport = new Export(format, draft.report, reviewed, reportFields(reviewed));
            showExportStatus("");
            startActivityForResult(new Intent(Intent.ACTION_CREATE_DOCUMENT)
                    .addCategory(Intent.CATEGORY_OPENABLE).setType(format.mimeType)
                    .putExtra(Intent.EXTRA_TITLE, "incident-practice-report." + format.extension),
                    EXPORT_REQUEST);
        } catch (ActivityNotFoundException | SecurityException error) {
            draft.pendingExport = null;
            showExportStatus("Could not open a save location. Try again.");
        }
    }

    private void showExportStatus(String message) {
        draft.exportNotice = message;
        if (draft.screen != Screen.REVIEW) return;
        exportStatus.setText(message);
        updateExportButtons();
    }

    private void updateExportButtons() {
        boolean available = draft.pendingExport == null && draft.export == null;
        pdfButton.setEnabled(available);
        jsonButton.setEnabled(available);
    }

    private void saveTo(Uri uri, Export export) {
        var resolver = getApplicationContext().getContentResolver();
        draft.export = CompletableFuture.runAsync(() -> {
            try (OutputStream stream = resolver.openOutputStream(uri, "w")) {
                if (stream == null) throw new IOException("No output stream");
                export.writeTo(stream);
            } catch (IOException | JSONException error) {
                throw new CompletionException(error);
            }
        });
        showExportStatus("Saving report…");
        observeExport(draft.export);
    }

    private void observeExport(CompletableFuture<Void> task) {
        task.whenComplete((unused, error) -> handler.post(() -> {
            if (isDestroyed() || draft.export != task) return;
            draft.export = null;
            showExportStatus(error == null ? "Practice report saved."
                    : "The report could not be saved. Try another file location.");
        }));
    }

    @Override
    protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request == SPEECH_REQUEST && draft.screen == Screen.DETAILS) {
            ArrayList<String> results = data == null ? null
                    : data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (result != RESULT_OK || results == null || results.isEmpty() || results.get(0) == null
                    || results.get(0).trim().isEmpty()) {
                draft.voiceNotice = "No dictation was added. Try again or type your description.";
            } else {
                String existing = draft.description.trim();
                String spoken = results.get(0).trim();
                String combined = existing.isEmpty() ? spoken : existing + " " + spoken;
                descriptionField.setText(combined);
                draft.voiceNotice = combined.length() > DESCRIPTION_LIMIT
                        ? "Description limit reached. Review the captured text."
                        : "Dictation added. Review the captured text.";
            }
            voiceStatus.setText(draft.voiceNotice);
        } else if (request == PROFILE_REQUEST && draft.screen == Screen.REVIEW) {
            render();
        } else if (request == EXPORT_REQUEST && draft.pendingExport != null) {
            Export export = draft.pendingExport;
            draft.pendingExport = null;
            Uri uri = data == null ? null : data.getData();
            if (result == RESULT_OK && uri != null) saveTo(uri, export);
            else showExportStatus("Save cancelled. Your report is still open.");
        }
    }

    private void confirmClear() {
        if (draft.pendingExport != null || draft.export != null) return;
        new AlertDialog.Builder(this).setTitle("Clear this practice report?")
                .setMessage("All details in this draft will be cleared.")
                .setNegativeButton("Keep report", null)
                .setPositiveButton("Clear report", (dialog, button) -> {
                    draft = new Draft();
                    go(Screen.HOME);
                }).show();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private LinearLayout column() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        return layout;
    }

    private void add(LinearLayout parent, View view, int space) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(space);
        parent.addView(view, params);
    }

    private GradientDrawable background(int colour, int radius, boolean border) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(colour);
        shape.setCornerRadius(dp(radius));
        if (border) shape.setStroke(dp(1), LINE);
        return shape;
    }

    private LinearLayout card(int colour) {
        LinearLayout card = column();
        card.setPadding(dp(20), dp(20), dp(20), dp(20));
        card.setBackground(
                background(
                        colour,
                        20,
                        colour == Color.WHITE
                )
        );
        card.setElevation(dp(1));
        return card;
    }

    private TextView text(
            String words,
            int size,
            int colour,
            boolean bold
    ) {
        TextView view = new TextView(this);
        view.setText(words);
        view.setTextSize(size);
        view.setTextColor(colour);
        view.setLineSpacing(0, 1.08f);

        if (bold) {
            view.setTypeface(
                    Typeface.DEFAULT,
                    Typeface.BOLD
            );
        }

        return view;
    }

    private void title(String words) {
        TextView view = text(words, 30, INK, true);
        view.setLineSpacing(0, 1.02f);
        ViewCompat.setAccessibilityHeading(view, true);
        add(content, view, 16);
        ViewCompat.setAccessibilityPaneTitle(content, words);
        setTitle(words + " · SCENAR");
    }

    private void step(String words) {
        TextView step = text(
                words.toUpperCase(Locale.ROOT),
                12,
                GREEN,
                true
        );

        step.setLetterSpacing(0.06f);
        step.setPadding(
                dp(12),
                dp(8),
                dp(12),
                dp(8)
        );
        step.setBackground(
                background(SOFT, 100, false)
        );

        add(content, step, 12);
    }

    private void heading(
            LinearLayout group,
            String words
    ) {
        TextView heading = text(words, 20, INK, true);
        ViewCompat.setAccessibilityHeading(heading, true);
        add(group, heading, 0);
    }

    private TextView status(
            LinearLayout group,
            String words
    ) {
        TextView status = text(words, 14, MUTED, false);
        status.setAccessibilityLiveRegion(
                View.ACCESSIBILITY_LIVE_REGION_POLITE
        );
        add(group, status, 8);
        return status;
    }

    private void fact(
            LinearLayout group,
            String name,
            String value
    ) {
        TextView label = text(
                name.toUpperCase(Locale.ROOT),
                11,
                GREEN,
                true
        );

        label.setLetterSpacing(0.06f);

        add(group, label, 14);
        add(group, text(value, 16, INK, false), 4);
    }

    private void label(
            LinearLayout group,
            String name,
            View view,
            int top
    ) {
        TextView label = text(name, 14, INK, true);
        label.setLabelFor(view.getId());
        add(group, label, top);
    }

    private EditText field(
            LinearLayout group,
            String name,
            String value,
            String hint,
            int lines,
            int limit,
            Consumer<String> update
    ) {
        EditText editor = new EditText(this);
        editor.setId(View.generateViewId());

        editor.setInputType(
                InputType.TYPE_CLASS_TEXT
                        | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                        | (lines > 1
                        ? InputType.TYPE_TEXT_FLAG_MULTI_LINE
                        : 0)
        );

        editor.setHint(hint);
        editor.setFilters(
                new InputFilter[]{
                        new InputFilter.LengthFilter(limit)
                }
        );
        editor.setText(value);
        editor.setTextSize(16);
        editor.setMinLines(lines);

        if (lines > 1) {
            editor.setMaxLines(8);
        }

        editor.setMinimumHeight(dp(54));
        editor.setGravity(android.view.Gravity.TOP);
        editor.setPadding(
                dp(14),
                dp(13),
                dp(14),
                dp(13)
        );
        editor.setTextColor(INK);
        editor.setHintTextColor(MUTED);
        editor.setBackground(
                background(Color.WHITE, 14, true)
        );
        editor.setSaveEnabled(false);

        editor.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(
                    CharSequence text,
                    int start,
                    int count,
                    int after
            ) {
            }

            @Override
            public void onTextChanged(
                    CharSequence text,
                    int start,
                    int before,
                    int count
            ) {
                update.accept(text.toString());
            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (editor.getError() != null) {
                    editor.setError(null);
                }
            }
        });

        label(group, name, editor, 14);
        add(group, editor, 6);

        return editor;
    }

    private Button primary(
            String words,
            Runnable action
    ) {
        return button(
                words,
                GREEN,
                Color.WHITE,
                false,
                action
        );
    }

    private Button secondary(
            String words,
            Runnable action
    ) {
        return button(
                words,
                Color.WHITE,
                GREEN,
                true,
                action
        );
    }

    private Button button(
            String words,
            int colour,
            int foreground,
            boolean border,
            Runnable action
    ) {
        Button control = new Button(this);
        control.setText(words);
        control.setTextSize(16);
        control.setAllCaps(false);
        control.setMinHeight(dp(56));
        control.setPadding(
                dp(16),
                dp(10),
                dp(16),
                dp(10)
        );
        control.setTypeface(
                Typeface.DEFAULT,
                Typeface.BOLD
        );
        control.setTextColor(foreground);
        control.setLetterSpacing(0.01f);
        control.setElevation(dp(1));

        control.setBackground(
                new RippleDrawable(
                        ColorStateList.valueOf(0x22000000),
                        background(colour, 16, border),
                        null
                )
        );

        control.setOnClickListener(
                view -> action.run()
        );

        return control;
    }
}
