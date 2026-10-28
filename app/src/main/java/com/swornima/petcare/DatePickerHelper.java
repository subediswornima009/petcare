package com.swornima.petcare;

import android.app.DatePickerDialog;
import android.widget.EditText;

import java.util.Calendar;

public final class DatePickerHelper {

    private DatePickerHelper() {
    }

    public static void show(EditText target) {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog dialog = new DatePickerDialog(
                target.getContext(),
                (view, year, month, dayOfMonth) ->
                        target.setText(String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)),
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        dialog.show();
    }
}
