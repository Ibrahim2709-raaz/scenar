package com.example.scenar.ui.responder;

import com.example.scenar.model.IncidentReport;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ResponderIncident implements Serializable {
    private final String id;
    private final IncidentReport report;
    private IncidentStatus status;
    private PriorityLevel priority;
    private final String receivedTime;
    private final List<TimelineEntry> timeline = new ArrayList<>();
    private final List<String> responderNotes = new ArrayList<>();
    private boolean contactNotificationSent = false;
    private final List<String> notificationLog = new ArrayList<>();
    private String assignedUnit;

    public ResponderIncident(String id, IncidentReport report, String receivedTime) {
        this.id = id;
        this.report = report;
        this.receivedTime = receivedTime;
        this.status = IncidentStatus.RECEIVED;
        this.priority = PriorityLevel.calculate(report);
        this.assignedUnit = "Medic Unit 12 & Engine 4";

        addTimeline("Report Ingested", "AI-verified incident report received into dispatch queue.");
    }

    public String getId() {
        return id;
    }

    public IncidentReport getReport() {
        return report;
    }

    public IncidentStatus getStatus() {
        return status;
    }

    public void setStatus(IncidentStatus newStatus) {
        if (this.status != newStatus) {
            this.status = newStatus;
            addTimeline("Status Updated: " + newStatus.getLabel(),
                    "Incident status transitioned to " + newStatus.getLabel() + " by responder console.");
        }
    }

    public void advanceStatus() {
        setStatus(status.next());
    }

    public PriorityLevel getPriority() {
        return priority;
    }

    public void setPriority(PriorityLevel priority) {
        this.priority = priority;
    }

    public String getReceivedTime() {
        return receivedTime;
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
            addTimeline("Field Note Added", note.trim());
        }
    }

    public boolean isContactNotificationSent() {
        return contactNotificationSent;
    }

    public List<String> getNotificationLog() {
        return new ArrayList<>(notificationLog);
    }

    public void recordNotification(String recipient, String message) {
        contactNotificationSent = true;
        String logEntry = String.format(Locale.ROOT, "[%s] Sent to %s: \"%s\"",
                currentTimestamp(), recipient, message);
        notificationLog.add(logEntry);
        addTimeline("Automated Alert Dispatched", "Emergency notification sent to " + recipient);
    }

    public String getAssignedUnit() {
        return assignedUnit;
    }

    public void setAssignedUnit(String assignedUnit) {
        this.assignedUnit = assignedUnit;
    }

    public void addTimeline(String title, String detail) {
        timeline.add(0, new TimelineEntry(currentTimestamp(), title, detail));
    }

    private static String currentTimestamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date());
    }
}
