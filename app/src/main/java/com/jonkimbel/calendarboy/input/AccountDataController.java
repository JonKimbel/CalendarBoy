package com.jonkimbel.calendarboy.input;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.annotation.SuppressLint;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;

public class AccountDataController implements DataController<Account[]> {
    private final AccountSelectionController accountPermissionsController;
    private final AccountManager accountManager;

    // Monotonically non-null.
    private Account[] accounts = null;

    public AccountDataController(
            AccountSelectionController accountPermissionsController,
            AccountManager accountManager) {
        this.accountPermissionsController = accountPermissionsController;
        this.accountManager = accountManager;
    }

    @Override
    public ListenableFuture<Account[]> getData() {
        SettableFuture<Account[]> dataAvailableFuture = SettableFuture.create();
        if (accounts != null) {
            dataAvailableFuture.set(accounts);
            return dataAvailableFuture;
        }

        accountPermissionsController.getSelectionThenRun((accountName, accountType) -> {
            updateData();
            dataAvailableFuture.set(accounts);
        });
        return dataAvailableFuture;
    }

    @SuppressLint("MissingPermission") // We've already checked when this method is called.
    private synchronized void updateData() {
        if (accounts != null) {
            return;
        }
        accounts = accountManager.getAccounts();
    }
}