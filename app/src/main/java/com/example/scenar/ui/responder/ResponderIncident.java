package com.example.scenar.ui.responder;

import com.example.scenar.model.DispatchRecord;
import com.example.scenar.model.DispatchStatus;
import com.example.scenar.model.IncidentReport;
import com.example.scenar.model.SharingConsent;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Responder-side view of one shared {@link DispatchRecord}.
 *
 * The report, consent and status always come from the record held by
 * SimulatedDispatchRepository, so the responder only ever sees the copy that
 * IncidentSharingPolicy filtered. This class only adds what the responder keeps
 * locally: a timeline, notes and the simulated notification log.
 */
public class ResponderIncident {
    private static final DateTimeFormatter CLOCK =
            DateTimeFormatter.ofPattern("HH:mm:ss", Locale.ROOT);

    private DispatchRecord record;
    private final boolean demo;
    private final List<TimelineEntry> timeline = new ArrayList<>();
    private final List<String> responderNotes = new ArrayList<>();
    private final List<String> notificationLog = new ArrayList<>();

    public ResponderIncident(DispatchRecord record, boolean demo) {
        this.record = Objects.requireNonNull(record, "record");
        this.demo = demo;
        addTimeline(ResponderCopy.statusLabel(record.getStatus()), ResponderCopy.QUEUE_ADDED);
    }

    public DispatchRecord getRecord() {
        return record;
    }

    public String getIncidentId() {
        return record.getIncidentId();
    }

    /** Short display form of the shared incident ID, for example "INC-1A2B3C4D". */
    public String getDisplayId() {
        String id = record.getIncidentId().replace("-", "");
        String shortId = id.length() > 8 ? id.substring(0, 8) : id;
        return "INC-" + shortId.toUpperCase(Locale.ROOT);
    }

    public boolean isDemo() {
        return demo;
    }

    /** The filtered report from the dispatch record (a copy). */
    public IncidentReport getReport() {
        return record.getReport();
    }

    public SharingConsent getConsent() {
        return record.getConsent();
    }

    public DispatchStatus getStatus() {
        return record.getStatus();
    }

    public PriorityLevel getPriority() {
        return PriorityLevel.calculate(record.getReport());
    }

    public String getReceivedTime() {
        return CLOCK.withZone(ZoneId.systemDefault()).format(record.getSubmittedAt());
    }

    public IncidentLocation getLocation() {
        return IncidentLocation.of(record.getReport());
    }

    /** Medical flags from the filtered record only. Empty when consent was withheld. */
    public List<String> getMedicalFlagsToShow() {
        return record.getReport().getMedicalFlags();
    }

    public String getMedicalEmptyMessage() {
        return record.getConsent().shareMedicalFlags()
                ? ResponderCopy.MEDICAL_NONE_REPORTED
                : ResponderCopy.MEDICAL_NOT_SHARED;
    }

    /** Replaces the record after the shared repository accepted a status change. */
    void applyRecord(DispatchRecord updated) {
        Objects.requireNonNull(updated, "updated");
        if (!updated.getIncidentId().equals(record.getIncidentId())) {
            throw new IllegalArgumentException("Record belongs to another incident");
        }
        DispatchStatus previous = record.getStatus();
        record = updated;
        if (previous != updated.getStatus()) {
            addTimeline(ResponderCopy.STATUS_CHANGE_TITLE_PREFIX
                            + ResponderCopy.statusLabel(updated.getStatus()),
                    ResponderCopy.STATUS_CHANGE_DETAIL);
        }
    }

    public List<TimelineEntry> getTimeline() {
        return new ArrayList<>(timeline);
    }

    public List<String> getResponderNotes() {
        return new ArrayList<>(responderNotes);
    }

    public void addNote(String note) {
        if (note != null && !note.trim().isEmpty()) {
            responderNotes.add(note.trim());
            addTimeline(ResponderCopy.NOTE_TITLE, note.trim());
        }
    }

    public boolean canSimulateNotification() {
        return record.getConsent().notifyEmergencyContacts();
    }

    public boolean hasSimulatedNotifications() {
        return !notificationLog.isEmpty();
    }

    public List<String> getNotificationLog() {
        return new ArrayList<>(notificationLog);
    }

    /**
     * Records a simulated notification. Nothing is delivered anywhere.
     *
     * @throws IllegalStateException when the reporter did not consent to contacting
     *                               emergency contacts
     */
    public void logSimulatedNotification(String recipient, String message) {
        if (!canSimulateNotification()) {
            throw new IllegalStateException(ResponderCopy.NOTIFICATION_NO_CONSENT);
        }
        notificationLog.add(String.format(Locale.ROOT, ResponderCopy.NOTIFICATION_LOG_FORMAT,
                currentTimestamp(), recipient, message));
        addTimeline(ResponderCopy.NOTIFICATION_TIMELINE_TITLE,
                ResponderCopy.NOTIFICATION_TIMELINE_DETAIL_PREFIX + recipient);
    }

    public void addTimeline(String title, String detail) {
        timeline.add(0, new TimelineEntry(currentTimestamp(), title, detail));
    }

    private static String currentTimestamp() {
        return CLOCK.withZone(ZoneId.systemDefault()).format(java.time.Instant.now());
    }
}
