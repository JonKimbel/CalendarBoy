package com.jonkimbel.calendarboy.input.api;

import android.content.Intent;

public interface SelectionController {
    boolean onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults);

    boolean onActivityResult(int requestCode, int resultCode, Intent data);
}
