package com.jonkimbel.calendarboy.model;

public class Event {
    private final long startTimeMillis;
    private final long endTimeMillis;
    private final String title;

    public Event(long startTimeMillis, long endTimeMillis, String title) {
        this.startTimeMillis = startTimeMillis;
        this.endTimeMillis = endTimeMillis;
        this.title = title;
    }

    public long getStartTimeMillis() {
        return startTimeMillis;
    }

    public long getEndTimeMillis() {
        return endTimeMillis;
    }

    public String getTitle() {
        return title;
    }
}
