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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class DailyTasksSummaryReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "daily_tasks_channel";
    private static final int NOTIFICATION_ID = 2001;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (context
                    .checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
        }

        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getDatabase(context.getApplicationContext());
            List<Task> allTasks = db.taskDao().getAllTasks();

            if (allTasks == null) {
                return;
            }

            // Μόνο όσες ΔΕΝ είναι done
            List<Task> pendingTasks = new ArrayList<>();
            for (Task t : allTasks) {
                if (t != null && !t.done) {
                    pendingTasks.add(t);
                }
            }

            int count = pendingTasks.size();
            if (count == 0) {
                // Καμία ανοιχτή εκκρεμότητα → δεν στέλνουμε ειδοποίηση
                return;
            }

            // Φτιάχνουμε κείμενο με τις πρώτες 5 εκκρεμότητες
            StringBuilder content = new StringBuilder();
            int maxShown = Math.min(5, count);
            for (int i = 0; i < maxShown; i++) {
                Task t = pendingTasks.get(i);
                content.append("• ")
                        .append(safe(t.name)) // πελάτης
                        .append(" – ")
                        .append(safe(t.type)) // τύπος (PortIN, Home κτλ)
                        .append("\n");
            }
            if (count > maxShown) {
                content.append("... +").append(count - maxShown).append(" ακόμα");
            }

            createNotificationChannel(context);

            // Όταν πατάς την ειδοποίηση → ανοίγει η λίστα εκκρεμοτήτων
            Intent openIntent = new Intent(context, TasksActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    context,
                    0,
                    openIntent,
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                            ? PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                            : PendingIntent.FLAG_UPDATE_CURRENT);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle("Εκκρεμότητες σήμερα: " + count)
                    .setContentText("Πάτησε για να δεις αναλυτικά.")
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(content.toString()))
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT);

            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build());
        });
    }

    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Καθημερινές εκκρεμότητες",
                    NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("Καθημερινή σύνοψη με όλες τις ανοιχτές εκκρεμότητες");

            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private static String safe(String text) {
        return text == null ? "" : text.trim();
    }
}
