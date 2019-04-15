package com.jonkimbel.calendarboy.input.calendar;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.CalendarContract.Calendars;

import com.jonkimbel.calendarboy.R;
import com.jonkimbel.calendarboy.model.Calendar;

import java.util.ArrayList;
import java.util.List;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class CalendarSelectionActivity extends AppCompatActivity {
    private static final String[] CALENDAR_PROJECTION = new String[]{
            Calendars._ID,                           // 0
            Calendars.CALENDAR_DISPLAY_NAME,         // 1
    };

    private static final int CALENDAR_PROJECTION_ID = 0;
    private static final int CALENDAR_PROJECTION_DISPLAY_NAME = 1;

    // Activity API.
    public static final String KEY_OUTPUT_CALENDAR_ID = "calendar_id";
    public static final String KEY_INPUT_ACCOUNT_NAME = "account_name";
    public static final String KEY_INPUT_ACCOUNT_TYPE = "account_type";

    // View.
    private CalendarListAdapter calendarListAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar_selection);

        calendarListAdapter = new CalendarListAdapter();

        RecyclerView calendarList = findViewById(R.id.calendar_list);
        calendarList.setLayoutManager(new LinearLayoutManager(this));
        calendarList.setAdapter(calendarListAdapter);

        calendarListAdapter.setCallback(calendarId -> {
            Intent intent = new Intent();
            intent.putExtra(KEY_OUTPUT_CALENDAR_ID, calendarId);
            setResult(RESULT_OK, intent);
            finish();
        });

        Intent intent = getIntent();
        calendarListAdapter.updateData(getCalendars(
                intent.getStringExtra(KEY_INPUT_ACCOUNT_NAME),
                intent.getStringExtra(KEY_INPUT_ACCOUNT_TYPE)));
    }

    @Override
    public void onBackPressed() {
        setResult(RESULT_CANCELED, new Intent());
        super.onBackPressed();
    }

    @SuppressLint("MissingPermission") // We've already checked when this method is called.
    private List<Calendar> getCalendars(String accountName, String accountType) {
        String selection = "("
                + Calendars.ACCOUNT_NAME + " = ?) AND ("
                + Calendars.ACCOUNT_TYPE + " = ?)";
        String[] selectionArgs = new String[]{accountName, accountType};

        Cursor cursor = getContentResolver().query(
                Calendars.CONTENT_URI, CALENDAR_PROJECTION, selection, selectionArgs, null);

        List<Calendar> calendars = new ArrayList<>();
        while (cursor.moveToNext()) {
            calendars.add(new Calendar(cursor.getString(CALENDAR_PROJECTION_DISPLAY_NAME),
                    cursor.getLong(CALENDAR_PROJECTION_ID)));
        }
        return calendars;
    }
}
