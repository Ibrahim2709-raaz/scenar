package com.example.scenar.ui.responder;

import java.io.Serializable;

public class TimelineEntry implements Serializable {
    private final String timestamp;
    private final String title;
    private final String detail;

    public TimelineEntry(String timestamp, String title, String detail) {
        this.timestamp = timestamp;
        this.title = title;
        this.detail = detail;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getTitle() {
        return title;
    }

    public String getDetail() {
        return detail;
    }
}
