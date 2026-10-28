package com.swornima.petcare;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Schedules local Android alarms for pet care tasks. It uses the app's
 * existing task time/frequency fields, so reminders are created whenever a
 * pet routine is saved.
 */
public final class ReminderScheduler {

    private static final long DAY = TimeUnit.DAYS.toMillis(1);
    private static final long WEEK = TimeUnit.DAYS.toMillis(7);
    private static final long MONTH = TimeUnit.DAYS.toMillis(30);

    private ReminderScheduler() {
    }

    public static void schedulePetReminders(Context context, Pet pet) {
        if (pet == null || pet.getCareTasks() == null) return;

        ReminderReceiver.createNotificationChannel(context);
        for (CareTask task : pet.getCareTasks()) {
            scheduleTaskReminder(context, pet, task);
        }
    }

    public static void cancelPetReminders(Context context, String petId, List<CareTask> tasks) {
        if (TextUtils.isEmpty(petId) || tasks == null) return;

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        for (CareTask task : tasks) {
            PendingIntent pendingIntent = buildPendingIntent(context, petId, task, PendingIntent.FLAG_NO_CREATE);
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent);
            }
        }
    }

    private static void scheduleTaskReminder(Context context, Pet pet, CareTask task) {
        if (task == null || TextUtils.isEmpty(task.getTime())) return;

        long triggerAt = nextTriggerMillis(task.getTime());
        if (triggerAt <= 0) return;

        long interval = repeatInterval(task.getFrequency());
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        Intent fillIntent = new Intent(context, ReminderReceiver.class);
        fillIntent.putExtra(ReminderReceiver.EXTRA_PET_ID, pet.getId());
        fillIntent.putExtra(ReminderReceiver.EXTRA_PET_NAME, pet.getName());
        fillIntent.putExtra(ReminderReceiver.EXTRA_TASK_NAME, task.getName());
        fillIntent.putExtra(ReminderReceiver.EXTRA_CATEGORY, task.getCategory());
        fillIntent.putExtra(ReminderReceiver.EXTRA_NOTES, task.getNotes());
        fillIntent.putExtra(ReminderReceiver.EXTRA_NOTIFICATION_ID, requestCode(pet.getId(), task));

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode(pet.getId(), task),
                fillIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        if (interval > 0) {
            alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, triggerAt, interval, pendingIntent);
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent);
        }
    }

    private static PendingIntent buildPendingIntent(Context context, String petId, CareTask task, int flag) {
        Intent intent = new Intent(context, ReminderReceiver.class);
        return PendingIntent.getBroadcast(
                context,
                requestCode(petId, task),
                intent,
                flag | PendingIntent.FLAG_IMMUTABLE
        );
    }

    private static int requestCode(String petId, CareTask task) {
        String taskId = task != null && !TextUtils.isEmpty(task.getId()) ? task.getId() : task.getName();
        return Math.abs((petId + ":" + taskId).hashCode());
    }

    private static long nextTriggerMillis(String time) {
        try {
            SimpleDateFormat format = new SimpleDateFormat("hh:mm a", Locale.US);
            Calendar parsed = Calendar.getInstance();
            parsed.setTime(format.parse(time.trim()));

            Calendar trigger = Calendar.getInstance();
            trigger.set(Calendar.HOUR_OF_DAY, parsed.get(Calendar.HOUR_OF_DAY));
            trigger.set(Calendar.MINUTE, parsed.get(Calendar.MINUTE));
            trigger.set(Calendar.SECOND, 0);
            trigger.set(Calendar.MILLISECOND, 0);

            if (trigger.getTimeInMillis() <= System.currentTimeMillis()) {
                trigger.add(Calendar.DAY_OF_YEAR, 1);
            }
            return trigger.getTimeInMillis();
        } catch (ParseException | NullPointerException e) {
            return -1;
        }
    }

    private static long repeatInterval(String frequency) {
        if (frequency == null) return DAY;
        String value = frequency.trim().toLowerCase(Locale.US);
        if ("daily".equals(value)) return DAY;
        if ("weekly".equals(value)) return WEEK;
        if ("monthly".equals(value)) return MONTH;
        return 0;
    }
}
