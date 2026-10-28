package com.swornima.petcare;

import android.text.TextUtils;
import android.widget.EditText;

import androidx.fragment.app.FragmentManager;

import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Opens Material's clock-face time picker for a Care Task's "Time" field
 * (used by both MyItemsActivity and CreatePetActivity's Add/Edit Care Task
 * dialog) and writes the result back in the same "hh:mm a" string format
 * the app already stores (e.g. "07:00 AM"), so existing CareTask data stays
 * compatible.
 */
public final class TimePickerHelper {

    private TimePickerHelper() {
    }

    public static void show(FragmentManager fragmentManager, EditText etTime) {
        int initialHour = 9;
        int initialMinute = 0;

        Calendar parsed = parseTimeToCalendar(etTime.getText().toString().trim());
        if (parsed != null) {
            initialHour = parsed.get(Calendar.HOUR_OF_DAY);
            initialMinute = parsed.get(Calendar.MINUTE);
        }

        MaterialTimePicker picker = new MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_12H)
                .setHour(initialHour)
                .setMinute(initialMinute)
                .setTitleText("Select Time")
                .build();

        picker.addOnPositiveButtonClickListener(v -> etTime.setText(String.format(Locale.US, "%02d:%02d %s",
                to12Hour(picker.getHour()), picker.getMinute(), picker.getHour() < 12 ? "AM" : "PM")));

        picker.show(fragmentManager, "time_picker");
    }

    private static int to12Hour(int hour24) {
        int h = hour24 % 12;
        return h == 0 ? 12 : h;
    }

    private static Calendar parseTimeToCalendar(String time) {
        if (TextUtils.isEmpty(time)) return null;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.US);
            Date date = sdf.parse(time);
            if (date == null) return null;
            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            return cal;
        } catch (ParseException e) {
            return null;
        }
    }
}
