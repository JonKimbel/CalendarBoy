package com.jonkimbel.calendarboy.input;

import android.app.Activity;
import android.content.Intent;

import com.jonkimbel.calendarboy.input.api.SelectionController;
import com.jonkimbel.calendarboy.input.calendar.CalendarSelectionActivity;
import com.jonkimbel.calendarboy.input.concurrent.UiThreadExecutor;

import androidx.annotation.GuardedBy;
import androidx.core.app.ActivityCompat;

import static android.Manifest.permission.READ_CALENDAR;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static com.jonkimbel.calendarboy.input.api.RequestCodes.READ_CALENDAR_RQ;
import static com.jonkimbel.calendarboy.input.api.RequestCodes.SELECT_CALENDAR_RQ;
import static com.jonkimbel.calendarboy.input.calendar.CalendarSelectionActivity.KEY_INPUT_ACCOUNT_NAME;
import static com.jonkimbel.calendarboy.input.calendar.CalendarSelectionActivity.KEY_INPUT_ACCOUNT_TYPE;
import static com.jonkimbel.calendarboy.input.calendar.CalendarSelectionActivity.KEY_OUTPUT_CALENDAR_ID;

public class CalendarSelectionController implements SelectionController {
    private final static String TAG = "CalendarPermissionsCtrl";

    private final Activity activity;

    @GuardedBy("this")
    private Callback successCallback;
    @GuardedBy("this")
    private Runnable failureRunnable;
    @GuardedBy("this")
    private Long calendarId;
    @GuardedBy("this")
    private AccountIdentifier accountToSelectCalendarFor;

    public CalendarSelectionController(Activity activity) {
        this.activity = activity;
    }

    @Override
    public synchronized boolean onRequestPermissionsResult(
            int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode != READ_CALENDAR_RQ) {
            return false;
        }

        if (grantResults[0] == PERMISSION_GRANTED && accountToSelectCalendarFor != null) {
            showCalendarSelectionDialog(accountToSelectCalendarFor);
            accountToSelectCalendarFor = null;
        } else {
            successCallback = null;
            UiThreadExecutor.INSTANCE.execute(failureRunnable);
        }
        return true;
    }

    @Override
    public synchronized boolean onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != SELECT_CALENDAR_RQ) {
            return false;
        }

        if (resultCode == Activity.RESULT_OK && data.hasExtra(KEY_OUTPUT_CALENDAR_ID)) {
            calendarId = data.getLongExtra(KEY_OUTPUT_CALENDAR_ID, 0);
            if (successCallback != null) {
                Callback callback = successCallback;
                UiThreadExecutor.INSTANCE.execute(() -> callback.onCalendarSelected(calendarId));
            }
        }
        successCallback = null;
        return true;
    }

    public void setPermissionDeniedBehavior(Runnable runnable) {
        failureRunnable = runnable;
    }

    public void getSelectionThenRun(Callback callback,
                                    AccountIdentifier accountToSelectCalendarFor) {
        if (calendarId == null) {
            askForSelectionThenRun(callback, accountToSelectCalendarFor);
        } else if (callback != null) {
            UiThreadExecutor.INSTANCE.execute(() -> callback.onCalendarSelected(calendarId));
        }
    }

    public void askForSelectionThenRun(Callback callback,
                                       AccountIdentifier accountToSelectCalendarFor) {
        successCallback = callback;
        if (ActivityCompat.checkSelfPermission(activity, READ_CALENDAR) != PERMISSION_GRANTED) {
            this.accountToSelectCalendarFor = accountToSelectCalendarFor;
            ActivityCompat.requestPermissions(
                    activity, new String[]{READ_CALENDAR}, READ_CALENDAR_RQ);
        } else if (callback != null) {
            showCalendarSelectionDialog(accountToSelectCalendarFor);
        }
    }

    private void showCalendarSelectionDialog(AccountIdentifier accountToSelectCalendarFor) {
        Intent intent = new Intent(activity, CalendarSelectionActivity.class);
        intent.putExtra(KEY_INPUT_ACCOUNT_NAME, accountToSelectCalendarFor.getAccountName());
        intent.putExtra(KEY_INPUT_ACCOUNT_TYPE, accountToSelectCalendarFor.getAccountType());
        activity.startActivityForResult(intent, SELECT_CALENDAR_RQ);
    }

    public interface Callback {
        void onCalendarSelected(Long calendarId);
    }

    public static class AccountIdentifier {
        private final String accountName;
        private final String accountType;

        public AccountIdentifier(String accountName, String accountType) {
            this.accountName = accountName;
            this.accountType = accountType;
        }

        String getAccountName() {
            return accountName;
        }

        String getAccountType() {
            return accountType;
        }
    }
}
