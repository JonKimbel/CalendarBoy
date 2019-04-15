package com.jonkimbel.calendarboy.input;

import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Intent;
import android.os.Build;

import com.jonkimbel.calendarboy.R;
import com.jonkimbel.calendarboy.input.api.SelectionController;
import com.jonkimbel.calendarboy.input.concurrent.UiThreadExecutor;

import androidx.annotation.GuardedBy;
import androidx.core.app.ActivityCompat;

import static android.Manifest.permission.GET_ACCOUNTS;
import static android.accounts.AccountManager.KEY_ACCOUNT_NAME;
import static android.accounts.AccountManager.KEY_ACCOUNT_TYPE;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static com.jonkimbel.calendarboy.input.api.RequestCodes.ANDROID_M_SELECT_ACCOUNT_RQ;
import static com.jonkimbel.calendarboy.input.api.RequestCodes.GET_ACCOUNTS_RQ;

public class AccountSelectionController implements SelectionController {
    private static final String TAG = "AccountPermissionsCtrl";

    private final Activity activity;
    private final String androidMAccountSelectionDialogString;

    @GuardedBy("this")
    private Callback successCallback;
    @GuardedBy("this")
    private Runnable failureRunnable;
    @GuardedBy("this")
    private String accountName;
    @GuardedBy("this")
    private String accountType;

    public AccountSelectionController(Activity activity) {
        this.activity = activity;
        this.androidMAccountSelectionDialogString =
                activity.getResources().getString(R.string.android_m_account_selection_dialog);
    }

    @Override
    public synchronized boolean onRequestPermissionsResult(
            int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode != GET_ACCOUNTS_RQ) {
            return false;
        }

        if (grantResults[0] == PERMISSION_GRANTED) {
            // TODO: show a dialog so pre-M phones can select an account.
            successCallback = null;
        } else {
            successCallback = null;
            UiThreadExecutor.INSTANCE.execute(failureRunnable);
        }
        return true;
    }

    @Override
    public synchronized boolean onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != ANDROID_M_SELECT_ACCOUNT_RQ) {
            return false;
        }

        if (resultCode == Activity.RESULT_OK) {
            accountName = data.getStringExtra(KEY_ACCOUNT_NAME);
            accountType = data.getStringExtra(KEY_ACCOUNT_TYPE);
            if (successCallback != null) {
                Callback callback = successCallback;
                UiThreadExecutor.INSTANCE.execute(() -> callback.onAccountSelected(
                        accountName, accountType));
            }
        }
        successCallback = null;
        return true;
    }

    public synchronized void setPermissionDeniedBehavior(Runnable runnable) {
        this.failureRunnable = runnable;
    }

    public synchronized void getSelectionThenRun(Callback callback) {
        if (accountName == null || accountType == null) {
            askForSelectionThenRun(callback);
        } else if (callback != null) {
            UiThreadExecutor.INSTANCE.execute(() -> callback.onAccountSelected(accountName,
                    accountType));
        }
    }

    public synchronized void askForSelectionThenRun(Callback callback) {
        this.successCallback = callback;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent chooseAccountIntent = AccountManager.newChooseAccountIntent(
                    null, null, null, androidMAccountSelectionDialogString, null, null, null);
            activity.startActivityForResult(chooseAccountIntent, ANDROID_M_SELECT_ACCOUNT_RQ);
        } else {
            ActivityCompat.requestPermissions(activity, new String[]{GET_ACCOUNTS},
                    GET_ACCOUNTS_RQ);
        }
    }

    public interface Callback {
        void onAccountSelected(String accountName, String accountType);
    }
}
