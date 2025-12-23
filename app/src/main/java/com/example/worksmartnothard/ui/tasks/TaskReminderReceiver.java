package com.example.worksmartnothard.ui.tasks;

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

import com.example.worksmartnothard.R;
import com.example.worksmartnothard.data.AppDatabase;
import com.example.worksmartnothard.data.Task;

public class TaskReminderReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "tasks_channel";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (context
                    .checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
        }

        String name = null;
        String description = null;

        int taskId = intent.getIntExtra(TaskReminderScheduler.EXTRA_TASK_ID, -1);
        if (taskId > 0) {
            AppDatabase db = AppDatabase.getDatabase(context.getApplicationContext());
            Task task = db.taskDao().getTaskById(taskId);
            if (task == null || task.done) {
                return;
            }
            name = task.name;
            description = task.description;
        } else {
            // Backward compatibility for alarms created before stable scheduling.
            name = intent.getStringExtra("task_name");
            description = intent.getStringExtra("task_description");
        }

        if (name == null)
            name = "Εκκρεμότητα";
        if (description == null)
            description = "";

        createNotificationChannel(context);

        Intent openIntent = new Intent(context, TasksActivity.class);
        openIntent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        if (taskId > 0) {
            openIntent.putExtra(TaskReminderScheduler.EXTRA_TASK_ID, taskId);
        }

        PendingIntent contentIntent = PendingIntent.getActivity(
                context,
                taskId > 0 ? taskId : 0,
                openIntent,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                        ? PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                        : PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                // 🔹 Icon της εφαρμογής αντί για το default Android
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Υπενθύμιση Eκκρεμότητας: " + name)
                .setContentText(description)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(description))
                .setContentIntent(contentIntent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        int notificationId = (int) System.currentTimeMillis();
        NotificationManagerCompat.from(context).notify(notificationId, builder.build());
    }

    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Εκκρεμότητες",
                    NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("Υπενθυμίσεις για Eκκρεμότητες");

            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
}
