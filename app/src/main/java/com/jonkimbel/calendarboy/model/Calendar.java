package com.jonkimbel.calendarboy.model;

public class Calendar {
    private final String name;
    private final long id;

    public Calendar(String name, long id) {
        this.name = name;
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public long getId() {
        return id;
    }
}
