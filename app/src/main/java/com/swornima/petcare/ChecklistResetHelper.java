package com.swornima.petcare;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Resets completed checklist tasks when their care period has expired. This
 * keeps daily, weekly and monthly routines reusable instead of permanently
 * completed after the first check-off.
 */
public final class ChecklistResetHelper {

    private ChecklistResetHelper() {
    }

    public static boolean resetExpiredTasks(List<CareTask> tasks) {
        if (tasks == null) return false;

        boolean changed = false;
        long now = System.currentTimeMillis();
        long todayStart = startOfTodayMillis();

        for (CareTask task : tasks) {
            if (task == null || !task.isCompleted()) continue;

            long completedAt = task.getCompletedAtMillis();
            if (completedAt <= 0 || shouldReset(task.getFrequency(), completedAt, now, todayStart)) {
                task.setCompleted(false);
                task.setCompletedAtMillis(0);
                changed = true;
            }
        }
        return changed;
    }

    private static boolean shouldReset(String frequency, long completedAt, long now, long todayStart) {
        String value = frequency == null ? "daily" : frequency.trim().toLowerCase(Locale.US);
        if ("weekly".equals(value)) {
            return now - completedAt >= TimeUnit.DAYS.toMillis(7);
        }
        if ("monthly".equals(value)) {
            return now - completedAt >= TimeUnit.DAYS.toMillis(30);
        }
        if ("one-off".equals(value)) {
            return false;
        }
        return completedAt < todayStart;
    }

    private static long startOfTodayMillis() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }
}
