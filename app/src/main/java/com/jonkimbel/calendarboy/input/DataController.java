package com.jonkimbel.calendarboy.input;

import com.google.common.util.concurrent.ListenableFuture;

public interface DataController<T> {
    ListenableFuture<T> getData();
}
