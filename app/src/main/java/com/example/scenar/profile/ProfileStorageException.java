package com.example.scenar.profile;

public final class ProfileStorageException
        extends RuntimeException {

    public ProfileStorageException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }

    public ProfileStorageException(String message) {
        super(message);
    }
}