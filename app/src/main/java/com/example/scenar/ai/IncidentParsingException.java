package com.example.scenar.ai;

public class IncidentParsingException extends Exception {

    public IncidentParsingException(String message) {
        super(message);
    }

    public IncidentParsingException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
}