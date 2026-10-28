package com.swornima.petcare;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

/**
 * Receives scheduled pet care reminder alarms and displays a local
 * notification. The alarm is created by ReminderScheduler when a pet routine
 * is saved.
 */
public class ReminderReceiver extends BroadcastReceiver {

    public static final String CHANNEL_ID = "petcare_reminders";
    public static final String EXTRA_PET_ID = "pet_id";
    public static final String EXTRA_PET_NAME = "pet_name";
    public static final String EXTRA_TASK_NAME = "task_name";
    public static final String EXTRA_CATEGORY = "category";
    public static final String EXTRA_NOTES = "notes";
    public static final String EXTRA_NOTIFICATION_ID = "notification_id";

    @Override
    public void onReceive(Context context, Intent intent) {
        createNotificationChannel(context);

        String petId = intent.getStringExtra(EXTRA_PET_ID);
        String petName = intent.getStringExtra(EXTRA_PET_NAME);
        String taskName = intent.getStringExtra(EXTRA_TASK_NAME);
        String category = intent.getStringExtra(EXTRA_CATEGORY);
        String notes = intent.getStringExtra(EXTRA_NOTES);
        int notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 0);

        Intent openIntent = new Intent(context, PetDetailActivity.class);
        openIntent.putExtra("pet_id", petId);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent contentIntent = PendingIntent.getActivity(
                context,
                notificationId,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        String title = petName == null || petName.trim().isEmpty()
                ? "Pet care reminder"
                : petName + ": " + safeText(taskName, "Care task");

        String body = safeText(category, "Scheduled care");
        if (notes != null && !notes.trim().isEmpty()) {
            body = body + " - " + notes;
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_care_tasks)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(contentIntent);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                        == PackageManager.PERMISSION_GRANTED) {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build());
        }
    }

    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Pet care reminders",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Feeding, grooming, medication and healthcare reminders");

            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private static String safeText(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }
}
