package com.example.scenar.ui.profile;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.scenar.R;
import com.example.scenar.profile.EmergencyProfile;
import com.example.scenar.profile.EmergencyProfileRepository;
import com.example.scenar.profile.ProfileStorageException;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.appbar.MaterialToolbar;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public final class ProfileActivity extends AppCompatActivity {
    private static final String[] BLOOD_TYPES = {
            "Unknown",
            "A+",
            "A-",
            "B+",
            "B-",
            "AB+",
            "AB-",
            "O+",
            "O-"
    };

    private EmergencyProfileRepository repository;

    private TextInputEditText preferredName;
    private TextInputEditText dateOfBirth;
    private MaterialAutoCompleteTextView bloodType;
    private TextInputEditText allergies;
    private TextInputEditText medications;
    private TextInputEditText medicalConditions;
    private TextInputEditText accessibilityNeeds;
    private TextInputEditText contactName;
    private TextInputEditText contactRelationship;
    private TextInputEditText contactPhone;

    private TextInputLayout dateOfBirthLayout;
    private TextInputLayout contactNameLayout;
    private TextInputLayout contactPhoneLayout;

    private LinearProgressIndicator completionBar;
    private TextView completionText;
    private TextView profileStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.enableEdgeToEdge(getWindow());
        setContentView(R.layout.activity_profile);

        View root = findViewById(R.id.profile_root);
        ViewCompat.setOnApplyWindowInsetsListener(
                root,
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

        repository =
                EmergencyProfileRepository.getInstance(this);

        bindViews();
        configureToolbar();
        configureDatePicker();
        configureBloodTypes();
        configureActions();
        configureCompletionUpdates();
        loadProfile();
    }

    private void bindViews() {
        preferredName =
                findViewById(R.id.preferred_name_input);

        dateOfBirth =
                findViewById(R.id.date_of_birth_input);

        bloodType =
                findViewById(R.id.blood_type_input);

        allergies =
                findViewById(R.id.allergies_input);

        medications =
                findViewById(R.id.medications_input);

        medicalConditions =
                findViewById(R.id.medical_conditions_input);

        accessibilityNeeds =
                findViewById(R.id.accessibility_needs_input);

        contactName =
                findViewById(R.id.contact_name_input);

        contactRelationship =
                findViewById(R.id.contact_relationship_input);

        contactPhone =
                findViewById(R.id.contact_phone_input);

        dateOfBirthLayout =
                findViewById(R.id.date_of_birth_layout);

        contactNameLayout =
                findViewById(R.id.contact_name_layout);

        contactPhoneLayout =
                findViewById(R.id.contact_phone_layout);

        completionBar =
                findViewById(R.id.profile_completion_bar);

        completionText =
                findViewById(R.id.profile_completion_text);

        profileStatus =
                findViewById(R.id.profile_status);
    }

    private void configureToolbar() {
        MaterialToolbar toolbar =
                findViewById(R.id.profile_toolbar);

        toolbar.setNavigationOnClickListener(
                ignored -> finish()
        );
    }

    private void configureDatePicker() {
        dateOfBirth.setInputType(InputType.TYPE_NULL);
        dateOfBirth.setFocusable(false);
        dateOfBirth.setClickable(true);

        View.OnClickListener openPicker =
                ignored -> showDatePicker();

        dateOfBirth.setOnClickListener(openPicker);
        dateOfBirthLayout.setEndIconOnClickListener(openPicker);
    }

    private void configureBloodTypes() {
        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_dropdown_item_1line,
                        BLOOD_TYPES
                );

        bloodType.setAdapter(adapter);
    }

    private void configureActions() {
        MaterialButton saveButton =
                findViewById(R.id.save_profile_button);

        MaterialButton clearButton =
                findViewById(R.id.clear_profile_button);

        saveButton.setOnClickListener(
                ignored -> saveProfile()
        );

        clearButton.setOnClickListener(
                ignored -> confirmClearProfile()
        );
    }

    private void configureCompletionUpdates() {
        TextWatcher watcher = new TextWatcher() {
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
                clearValidationErrors();
                updateCompletion();
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        };

        preferredName.addTextChangedListener(watcher);
        dateOfBirth.addTextChangedListener(watcher);
        bloodType.addTextChangedListener(watcher);
        allergies.addTextChangedListener(watcher);
        medications.addTextChangedListener(watcher);
        medicalConditions.addTextChangedListener(watcher);
        accessibilityNeeds.addTextChangedListener(watcher);
        contactName.addTextChangedListener(watcher);
        contactRelationship.addTextChangedListener(watcher);
        contactPhone.addTextChangedListener(watcher);
    }

    private void loadProfile() {
        try {
            EmergencyProfile profile = repository.load();
            populate(profile);
            profileStatus.setText("");

        } catch (ProfileStorageException exception) {
            populate(EmergencyProfile.empty());
            profileStatus.setText(
                    R.string.profile_load_failed
            );
        }
    }

    private void populate(EmergencyProfile profile) {
        preferredName.setText(profile.preferredName());
        dateOfBirth.setText(profile.dateOfBirth());

        bloodType.setText(
                profile.bloodType(),
                false
        );

        allergies.setText(profile.allergies());
        medications.setText(profile.medications());
        medicalConditions.setText(
                profile.medicalConditions()
        );
        accessibilityNeeds.setText(
                profile.accessibilityNeeds()
        );
        contactName.setText(
                profile.emergencyContactName()
        );
        contactRelationship.setText(
                profile.emergencyContactRelationship()
        );
        contactPhone.setText(
                profile.emergencyContactPhone()
        );

        updateCompletion();
    }

    private EmergencyProfile profileFromFields() {
        return new EmergencyProfile(
                value(preferredName),
                value(dateOfBirth),
                value(bloodType),
                value(allergies),
                value(medications),
                value(medicalConditions),
                value(accessibilityNeeds),
                value(contactName),
                value(contactRelationship),
                value(contactPhone)
        );
    }

    private void updateCompletion() {
        EmergencyProfile profile =
                profileFromFields();

        int completion =
                profile.completionPercentage();

        completionBar.setProgressCompat(
                completion,
                true
        );

        completionText.setText(
                getString(
                        R.string.profile_completion_format,
                        completion
                )
        );
    }

    private void saveProfile() {
        clearValidationErrors();

        EmergencyProfile profile =
                profileFromFields();

        if (!validate(profile)) {
            return;
        }

        try {
            repository.save(profile);
            profileStatus.setText("");
            setResult(RESULT_OK);

            Snackbar.make(
                    findViewById(R.id.profile_root),
                    R.string.profile_saved,
                    Snackbar.LENGTH_SHORT
            ).show();

        } catch (ProfileStorageException exception) {
            profileStatus.setText(
                    R.string.profile_save_failed
            );
        }
    }

    private boolean validate(EmergencyProfile profile) {
        boolean valid = true;

        if (!profile.dateOfBirth().isEmpty()) {
            try {
                LocalDate date =
                        LocalDate.parse(
                                profile.dateOfBirth()
                        );

                if (date.isAfter(LocalDate.now())) {
                    dateOfBirthLayout.setError(
                            getString(
                                    R.string.future_date_error
                            )
                    );
                    valid = false;
                }

            } catch (DateTimeParseException exception) {
                dateOfBirthLayout.setError(
                        getString(
                                R.string.invalid_date_error
                        )
                );
                valid = false;
            }
        }

        boolean hasContactName =
                !profile.emergencyContactName().isEmpty();

        boolean hasContactPhone =
                !profile.emergencyContactPhone().isEmpty();

        if (hasContactName && !hasContactPhone) {
            contactPhoneLayout.setError(
                    getString(
                            R.string.contact_phone_required
                    )
            );
            valid = false;
        }

        if (hasContactPhone && !hasContactName) {
            contactNameLayout.setError(
                    getString(
                            R.string.contact_name_required
                    )
            );
            valid = false;
        }

        if (hasContactPhone
                && countPhoneDigits(
                profile.emergencyContactPhone()
        ) < 7) {
            contactPhoneLayout.setError(
                    getString(
                            R.string.invalid_phone_error
                    )
            );
            valid = false;
        }

        return valid;
    }

    private void showDatePicker() {
        LocalDate selectedDate =
                parseOrDefaultDate(
                        value(dateOfBirth)
                );

        DatePickerDialog dialog =
                new DatePickerDialog(
                        this,
                        (picker, year, month, day) -> {
                            LocalDate chosen =
                                    LocalDate.of(
                                            year,
                                            month + 1,
                                            day
                                    );

                            dateOfBirth.setText(
                                    chosen.toString()
                            );
                        },
                        selectedDate.getYear(),
                        selectedDate.getMonthValue() - 1,
                        selectedDate.getDayOfMonth()
                );

        dialog.getDatePicker().setMaxDate(
                System.currentTimeMillis()
        );

        dialog.show();
    }

    private LocalDate parseOrDefaultDate(String value) {
        try {
            if (!value.isEmpty()) {
                return LocalDate.parse(value);
            }
        } catch (DateTimeParseException ignored) {
        }

        return LocalDate.now().minusYears(18);
    }

    private void confirmClearProfile() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.clear_profile_title)
                .setMessage(R.string.clear_profile_message)
                .setNegativeButton(
                        android.R.string.cancel,
                        null
                )
                .setPositiveButton(
                        R.string.clear_profile_action,
                        (dialog, which) -> clearProfile()
                )
                .show();
    }

    private void clearProfile() {
        try {
            repository.clear();
            populate(EmergencyProfile.empty());
            profileStatus.setText("");
            setResult(RESULT_OK);

            Snackbar.make(
                    findViewById(R.id.profile_root),
                    R.string.profile_cleared,
                    Snackbar.LENGTH_SHORT
            ).show();

        } catch (ProfileStorageException exception) {
            profileStatus.setText(
                    R.string.profile_clear_failed
            );
        }
    }

    private void clearValidationErrors() {
        dateOfBirthLayout.setError(null);
        contactNameLayout.setError(null);
        contactPhoneLayout.setError(null);
    }

    private static String value(TextView view) {
        CharSequence text = view.getText();
        return text == null ? "" : text.toString();
    }

    private static int countPhoneDigits(String phone) {
        int digits = 0;

        for (int index = 0;
             index < phone.length();
             index++) {

            if (Character.isDigit(
                    phone.charAt(index)
            )) {
                digits++;
            }
        }

        return digits;
    }
}