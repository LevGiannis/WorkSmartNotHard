package com.example.worksmartnothard.ui.tasks;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.worksmartnothard.R;

public class TaskReminderReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "tasks_channel";

    @Override
    public void onReceive(Context context, Intent intent) {
        String name = intent.getStringExtra("task_name");
        String description = intent.getStringExtra("task_description");
        if (name == null) name = "Εκκρεμότητα";
        if (description == null) description = "";

        createNotificationChannel(context);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                // 🔹 Icon της εφαρμογής αντί για το default Android
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Υπενθύμιση Eκκρεμότητας: " + name)
                .setContentText(description)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(description))
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
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription("Υπενθυμίσεις για Eκκρεμότητες");

            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
}
