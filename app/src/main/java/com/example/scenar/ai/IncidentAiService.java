package com.example.scenar.ai;

import com.example.scenar.model.IncidentReport;

/**
 * Boundary for incident structuring.
 *
 * Implementations may generate a report using the configured AI service or a
 * safe local fallback. The callback tells the UI which path produced the
 * result so the user is never misled.
 */
public interface IncidentAiService {

    void structureIncident(
            String description,
            IncidentCallback callback
    );

    interface IncidentCallback {

        void onSuccess(IncidentReport report);

        /**
         * Called when the remote AI was unavailable or returned an unusable
         * result, but a local fallback successfully generated a report.
         *
         * The default implementation preserves compatibility with callbacks
         * that do not need to distinguish between the two result sources.
         */
        default void onFallback(
                IncidentReport report,
                String userMessage
        ) {
            onSuccess(report);
        }

        /**
         * Called only when neither AI nor the local fallback can produce a
         * usable report.
         */
        void onFailure(String message);
    }
}