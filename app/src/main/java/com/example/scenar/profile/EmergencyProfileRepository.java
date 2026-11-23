package com.example.scenar.profile;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

public final class EmergencyProfileRepository {
    private static final String PREFERENCE_FILE =
            "scenar_secure_profile";

    private static final String KEY_CIPHERTEXT =
            "profile_ciphertext";

    private static final String KEY_INITIALIZATION_VECTOR =
            "profile_initialization_vector";

    private static final String KEY_STORAGE_VERSION =
            "profile_storage_version";

    private static final int STORAGE_VERSION = 1;

    private static final String KEYSTORE_PROVIDER =
            "AndroidKeyStore";

    private static final String KEY_ALIAS =
            "scenar_emergency_profile_key_v1";

    private static final String CIPHER_TRANSFORMATION =
            "AES/GCM/NoPadding";

    private static final int AUTHENTICATION_TAG_BITS = 128;

    private static final byte[] ADDITIONAL_AUTHENTICATED_DATA =
            PREFERENCE_FILE.getBytes(StandardCharsets.UTF_8);

    private static final Gson GSON = new Gson();

    private static volatile EmergencyProfileRepository instance;

    private final SharedPreferences preferences;

    private EmergencyProfileRepository(Context context) {
        if (context == null) {
            throw new IllegalArgumentException(
                    "An Android context is required."
            );
        }

        preferences = context.getApplicationContext()
                .getSharedPreferences(
                        PREFERENCE_FILE,
                        Context.MODE_PRIVATE
                );
    }

    public static EmergencyProfileRepository getInstance(
            Context context
    ) {
        if (instance == null) {
            synchronized (EmergencyProfileRepository.class) {
                if (instance == null) {
                    instance =
                            new EmergencyProfileRepository(context);
                }
            }
        }

        return instance;
    }

    public synchronized EmergencyProfile load() {
        String encodedCiphertext =
                preferences.getString(
                        KEY_CIPHERTEXT,
                        null
                );

        String encodedInitializationVector =
                preferences.getString(
                        KEY_INITIALIZATION_VECTOR,
                        null
                );

        if (encodedCiphertext == null
                && encodedInitializationVector == null) {
            return EmergencyProfile.empty();
        }

        if (encodedCiphertext == null
                || encodedInitializationVector == null) {
            throw new ProfileStorageException(
                    "The saved emergency profile is incomplete."
            );
        }

        int version = preferences.getInt(
                KEY_STORAGE_VERSION,
                0
        );

        if (version != STORAGE_VERSION) {
            throw new ProfileStorageException(
                    "The saved emergency profile uses an unsupported version."
            );
        }

        try {
            byte[] ciphertext = Base64.decode(
                    encodedCiphertext,
                    Base64.NO_WRAP
            );

            byte[] initializationVector = Base64.decode(
                    encodedInitializationVector,
                    Base64.NO_WRAP
            );

            Cipher cipher = Cipher.getInstance(
                    CIPHER_TRANSFORMATION
            );

            cipher.init(
                    Cipher.DECRYPT_MODE,
                    getOrCreateSecretKey(),
                    new GCMParameterSpec(
                            AUTHENTICATION_TAG_BITS,
                            initializationVector
                    )
            );

            cipher.updateAAD(
                    ADDITIONAL_AUTHENTICATED_DATA
            );

            byte[] plaintext = cipher.doFinal(ciphertext);

            EmergencyProfile profile = GSON.fromJson(
                    new String(
                            plaintext,
                            StandardCharsets.UTF_8
                    ),
                    EmergencyProfile.class
            );

            return profile == null
                    ? EmergencyProfile.empty()
                    : profile;

        } catch (GeneralSecurityException
                 | IOException
                 | IllegalArgumentException
                 | JsonSyntaxException exception) {

            throw new ProfileStorageException(
                    "The emergency profile could not be read securely.",
                    exception
            );
        }
    }

    public synchronized void save(
            EmergencyProfile profile
    ) {
        if (profile == null) {
            throw new IllegalArgumentException(
                    "An emergency profile is required."
            );
        }

        try {
            Cipher cipher = Cipher.getInstance(
                    CIPHER_TRANSFORMATION
            );

            cipher.init(
                    Cipher.ENCRYPT_MODE,
                    getOrCreateSecretKey()
            );

            cipher.updateAAD(
                    ADDITIONAL_AUTHENTICATED_DATA
            );

            byte[] plaintext = GSON.toJson(profile)
                    .getBytes(StandardCharsets.UTF_8);

            byte[] ciphertext =
                    cipher.doFinal(plaintext);

            byte[] initializationVector =
                    cipher.getIV();

            boolean saved = preferences.edit()
                    .putString(
                            KEY_CIPHERTEXT,
                            Base64.encodeToString(
                                    ciphertext,
                                    Base64.NO_WRAP
                            )
                    )
                    .putString(
                            KEY_INITIALIZATION_VECTOR,
                            Base64.encodeToString(
                                    initializationVector,
                                    Base64.NO_WRAP
                            )
                    )
                    .putInt(
                            KEY_STORAGE_VERSION,
                            STORAGE_VERSION
                    )
                    .commit();

            if (!saved) {
                throw new ProfileStorageException(
                        "The emergency profile could not be saved."
                );
            }

        } catch (GeneralSecurityException
                 | IOException exception) {

            throw new ProfileStorageException(
                    "The emergency profile could not be encrypted.",
                    exception
            );
        }
    }

    public synchronized boolean hasProfile() {
        return preferences.contains(KEY_CIPHERTEXT)
                && preferences.contains(
                KEY_INITIALIZATION_VECTOR
        );
    }

    public synchronized void clear() {
        boolean cleared = preferences.edit()
                .clear()
                .commit();

        if (!cleared) {
            throw new ProfileStorageException(
                    "The emergency profile could not be cleared."
            );
        }

        try {
            KeyStore keyStore = KeyStore.getInstance(
                    KEYSTORE_PROVIDER
            );

            keyStore.load(null);

            if (keyStore.containsAlias(KEY_ALIAS)) {
                keyStore.deleteEntry(KEY_ALIAS);
            }

        } catch (GeneralSecurityException
                 | IOException exception) {

            throw new ProfileStorageException(
                    "The emergency profile encryption key could not be cleared.",
                    exception
            );
        }
    }

    private SecretKey getOrCreateSecretKey()
            throws GeneralSecurityException, IOException {

        KeyStore keyStore = KeyStore.getInstance(
                KEYSTORE_PROVIDER
        );

        keyStore.load(null);

        Key existingKey = keyStore.getKey(
                KEY_ALIAS,
                null
        );

        if (existingKey instanceof SecretKey secretKey) {
            return secretKey;
        }

        KeyGenerator keyGenerator =
                KeyGenerator.getInstance(
                        KeyProperties.KEY_ALGORITHM_AES,
                        KEYSTORE_PROVIDER
                );

        KeyGenParameterSpec specification =
                new KeyGenParameterSpec.Builder(
                        KEY_ALIAS,
                        KeyProperties.PURPOSE_ENCRYPT
                                | KeyProperties.PURPOSE_DECRYPT
                )
                        .setBlockModes(
                                KeyProperties.BLOCK_MODE_GCM
                        )
                        .setEncryptionPaddings(
                                KeyProperties.ENCRYPTION_PADDING_NONE
                        )
                        .setRandomizedEncryptionRequired(true)
                        .build();

        keyGenerator.init(specification);

        return keyGenerator.generateKey();
    }
}