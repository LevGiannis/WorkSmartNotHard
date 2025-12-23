package com.example.worksmartnothard.ui.tasks;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.example.worksmartnothard.data.Task;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class TaskReminderScheduler {

    public static final String EXTRA_TASK_ID = "task_id";
    public static final String EXTRA_ALARM_SLOT = "alarm_slot";

    private static final int SLOT_MORNING = 1;
    private static final int SLOT_AFTERNOON = 2;
    private static final int SLOT_FALLBACK = 3;

    private static final int REQUEST_CODE_BASE = 9_000_000;

    private TaskReminderScheduler() {
    }

    public static final class ScheduleResult {
        public final List<Long> triggerTimes;
        public final boolean usedFallback;

        private ScheduleResult(List<Long> triggerTimes, boolean usedFallback) {
            this.triggerTimes = triggerTimes;
            this.usedFallback = usedFallback;
        }
    }

    public static ScheduleResult schedule(Context context, Task task, boolean allowFallbackIfPast) {
        if (task == null || task.id <= 0) {
            return new ScheduleResult(new ArrayList<>(), false);
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Date date;
        try {
            date = sdf.parse(task.dueDate);
            if (date == null) {
                return new ScheduleResult(new ArrayList<>(), false);
            }
        } catch (ParseException e) {
            return new ScheduleResult(new ArrayList<>(), false);
        }

        long nowMillis = System.currentTimeMillis();

        Calendar calMorning = Calendar.getInstance();
        calMorning.setTime(date);
        calMorning.set(Calendar.HOUR_OF_DAY, 10);
        calMorning.set(Calendar.MINUTE, 0);
        calMorning.set(Calendar.SECOND, 0);
        calMorning.set(Calendar.MILLISECOND, 0);

        Calendar calAfternoon = Calendar.getInstance();
        calAfternoon.setTime(date);
        calAfternoon.set(Calendar.HOUR_OF_DAY, 17);
        calAfternoon.set(Calendar.MINUTE, 30);
        calAfternoon.set(Calendar.SECOND, 0);
        calAfternoon.set(Calendar.MILLISECOND, 0);

        List<Long> scheduled = new ArrayList<>();

        if (calMorning.getTimeInMillis() > nowMillis) {
            scheduleSingleAlarm(context, task.id, SLOT_MORNING, calMorning.getTimeInMillis());
            scheduled.add(calMorning.getTimeInMillis());
        }

        if (calAfternoon.getTimeInMillis() > nowMillis) {
            scheduleSingleAlarm(context, task.id, SLOT_AFTERNOON, calAfternoon.getTimeInMillis());
            scheduled.add(calAfternoon.getTimeInMillis());
        }

        boolean usedFallback = false;
        if (scheduled.isEmpty() && allowFallbackIfPast) {
            long triggerFallback = nowMillis + 10_000L;
            scheduleSingleAlarm(context, task.id, SLOT_FALLBACK, triggerFallback);
            scheduled.add(triggerFallback);
            usedFallback = true;
        }

        return new ScheduleResult(scheduled, usedFallback);
    }

    public static void cancel(Context context, int taskId) {
        if (taskId <= 0)
            return;

        cancelSlot(context, taskId, SLOT_MORNING);
        cancelSlot(context, taskId, SLOT_AFTERNOON);
        cancelSlot(context, taskId, SLOT_FALLBACK);
    }

    private static void scheduleSingleAlarm(Context context, int taskId, int slot, long triggerAt) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null)
            return;

        PendingIntent pendingIntent = buildPendingIntent(context, taskId, slot, false);
        if (pendingIntent == null)
            return;

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent);
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent);
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent);
            }
        } catch (SecurityException ignored) {
            // If exact alarms are restricted, task is still saved; just skip scheduling.
        }
    }

    private static void cancelSlot(Context context, int taskId, int slot) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null)
            return;

        PendingIntent pendingIntent = buildPendingIntent(context, taskId, slot, true);
        if (pendingIntent == null)
            return;

        alarmManager.cancel(pendingIntent);
        pendingIntent.cancel();
    }

    private static PendingIntent buildPendingIntent(Context context, int taskId, int slot, boolean noCreate) {
        Intent intent = new Intent(context, TaskReminderReceiver.class);
        intent.setAction("com.example.worksmartnothard.TASK_REMINDER_" + slot);
        intent.putExtra(EXTRA_TASK_ID, taskId);
        intent.putExtra(EXTRA_ALARM_SLOT, slot);

        int requestCode = requestCode(taskId, slot);

        int flags;
        if (noCreate) {
            flags = PendingIntent.FLAG_NO_CREATE;
        } else {
            flags = PendingIntent.FLAG_UPDATE_CURRENT;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        return PendingIntent.getBroadcast(context, requestCode, intent, flags);
    }

    private static int requestCode(int taskId, int slot) {
        long code = REQUEST_CODE_BASE + (taskId * 10L) + slot;
        return (int) (code % Integer.MAX_VALUE);
    }
}
