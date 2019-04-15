package com.jonkimbel.calendarboy;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.google.common.util.concurrent.ListenableFuture;
import com.jonkimbel.calendarboy.input.AccountSelectionController;
import com.jonkimbel.calendarboy.input.CalendarSelectionController;
import com.jonkimbel.calendarboy.input.EventDataController;
import com.jonkimbel.calendarboy.input.api.SelectionController;
import com.jonkimbel.calendarboy.input.concurrent.UiThreadExecutor;
import com.jonkimbel.calendarboy.model.Event;
import com.jonkimbel.calendarboy.view.CalendarView;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    private List<SelectionController> selectionControllers = new ArrayList<>();
    private EventDataController eventDataController;
    private CalendarView calendarView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        CalendarSelectionController calendarSelectionController =
                new CalendarSelectionController(this);
        calendarSelectionController.setPermissionDeniedBehavior(() -> {
            String message = getResources().getString(R.string.calendar_permission_denied);
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        });
        selectionControllers.add(calendarSelectionController);

        AccountSelectionController accountSelectionController =
                new AccountSelectionController(this);
        accountSelectionController.setPermissionDeniedBehavior(() -> {
            String message = getResources().getString(R.string.accounts_permission_denied);
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        });
        selectionControllers.add(accountSelectionController);

        eventDataController = new EventDataController(
                calendarSelectionController, getContentResolver(), accountSelectionController);

        calendarView = findViewById(R.id.calendar_view);
    }

    @Override
    protected void onStart() {
        super.onStart();
        ListenableFuture<List<Event>> calendarDataFuture = eventDataController.getData();
        calendarDataFuture.addListener(
                () -> {
                    try {
                        calendarView.setData(calendarDataFuture.get());
                    } catch (ExecutionException | InterruptedException e) {
                        e.printStackTrace();
                    }
                },
                UiThreadExecutor.INSTANCE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        for (SelectionController controller : selectionControllers) {
            boolean handled = controller.onRequestPermissionsResult(
                    requestCode, permissions, grantResults);
            if (handled) {
                break;
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        for (SelectionController controller : selectionControllers) {
            boolean handled = controller.onActivityResult(requestCode, resultCode, data);
            if (handled) {
                break;
            }
        }
    }
}
