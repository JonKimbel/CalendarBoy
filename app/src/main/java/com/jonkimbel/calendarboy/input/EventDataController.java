package com.jonkimbel.calendarboy.input;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract.Instances;
import android.util.Log;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.jonkimbel.calendarboy.input.calendar.CalendarSelectionController;
import com.jonkimbel.calendarboy.model.Event;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

public class EventDataController {
    public static final String[] INSTANCE_PROJECTION = new String[]{
            Instances.BEGIN,         // 0
            Instances.END,           // 1
            Instances.TITLE,         // 2
    };

    private static final int INSTANCE_PROJECTION_BEGIN = 0;
    private static final int INSTANCE_PROJECTION_END = 1;
    private static final int INSTANCE_PROJECTION_TITLE = 2;

    private final CalendarSelectionController calendarSelectionController;
    private final AccountSelectionController accountSelectionController;
    private final ContentResolver contentResolver;

    // Monotonically non-null.
    private List<Event> eventData = null;

    public EventDataController(
            CalendarSelectionController calendarSelectionController,
            ContentResolver contentResolver,
            AccountSelectionController accountSelectionController) {
        this.calendarSelectionController = calendarSelectionController;
        this.accountSelectionController = accountSelectionController;
        this.contentResolver = contentResolver;
    }

    public ListenableFuture<List<Event>> getData() {
        SettableFuture<List<Event>> dataAvailableFuture = SettableFuture.create();
        accountSelectionController.getSelectionThenRun((accountName, accountType) -> {
            getDataInternal(dataAvailableFuture, accountName, accountType);
        });
        return dataAvailableFuture;
    }

    private void getDataInternal(SettableFuture<List<Event>> future, String accountName,
                                 String accountType) {
        if (eventData != null) {
            future.set(eventData);
        } else {
            calendarSelectionController.getSelectionThenRun(calendarId -> {
                updateData(accountName, accountType, calendarId);
                future.set(eventData);
            }, new CalendarSelectionController.AccountIdentifier(accountName, accountType));
        }
    }

    @SuppressLint("MissingPermission") // We've already checked when this method is called.
    private synchronized void updateData(String accountName, String accountType, Long calendarId) {
        if (eventData != null) {
            return;
        }

        eventData = getInstances(accountName, accountType, calendarId);
    }

    private List<Event> getInstances(String accountName, String accountType, Long calendarId) {
        // TODO: ask the user which day they want to look at.
        // TODO: use backwards-compatible time representations.
        ZonedDateTime now = ZonedDateTime.ofInstant(Instant.now(), ZoneId.systemDefault());
        ZonedDateTime todayStart = now.toLocalDate().atStartOfDay(ZoneId.systemDefault());
        ZonedDateTime todayEnd = todayStart.plusDays(1).minusNanos(1000);

        String selection = Instances.CALENDAR_ID + " = ?";
        String[] selectionArgs = new String[]{Long.toString(calendarId)};

        Uri.Builder uriBuilder = Instances.CONTENT_URI.buildUpon();
        ContentUris.appendId(uriBuilder, todayStart.toEpochSecond() * 1000);
        ContentUris.appendId(uriBuilder, todayEnd.toEpochSecond() * 1000);

        Log.i("EventDataController", Long.toString(calendarId));
        Log.i("EventDataController", todayStart.format(DateTimeFormatter.ISO_DATE_TIME));
        Log.i("EventDataController", todayEnd.format(DateTimeFormatter.ISO_DATE_TIME));

        Cursor cursor = contentResolver.query(
                uriBuilder.build(),
                INSTANCE_PROJECTION,
                selection,
                selectionArgs,
                null);

        List<Event> data = new ArrayList<>();
        while (cursor.moveToNext()) {
            String title = cursor.getString(INSTANCE_PROJECTION_TITLE);
            long startTimeMillis = cursor.getLong(INSTANCE_PROJECTION_BEGIN);
            long endTimeMillis = cursor.getLong(INSTANCE_PROJECTION_END);

            Calendar startTime = Calendar.getInstance();
            Calendar endTime = Calendar.getInstance();
            startTime.setTimeInMillis(startTimeMillis);
            endTime.setTimeInMillis(endTimeMillis);

            DateFormat formatter = SimpleDateFormat.getDateTimeInstance();
            Log.i("EventDataController",
                    String.format("%s\t%s through %s",
                            title,
                            formatter.format(startTime.getTime()),
                            formatter.format(endTime.getTime())));
            data.add(new Event(startTimeMillis, endTimeMillis, title));
        }
        Collections.sort(data, Event::compare);
        return data;
    }
}
