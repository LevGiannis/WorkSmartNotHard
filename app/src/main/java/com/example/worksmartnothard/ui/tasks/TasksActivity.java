package com.example.worksmartnothard.ui.tasks;

import android.app.AlertDialog;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.DatePickerDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.TextView;


import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.worksmartnothard.R;
import com.example.worksmartnothard.data.AppDatabase;
import com.example.worksmartnothard.data.AppPreferences;
import com.example.worksmartnothard.data.Task;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.text.ParseException;
import java.util.Calendar;

public class TasksActivity extends AppCompatActivity {

    private RecyclerView recyclerTasks;
    private TaskAdapter taskAdapter;
    private Button buttonExportTasks;
    private Button buttonAddTask;
    private AppDatabase db;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 👉 Χρησιμοποιεί το activity_tasks.xml που σου έδωσα
        setContentView(R.layout.activity_tasks);

        db = AppDatabase.getDatabase(getApplicationContext());

        recyclerTasks = findViewById(R.id.recyclerTasks);
        buttonExportTasks = findViewById(R.id.buttonExportTasks);
        buttonAddTask = findViewById(R.id.buttonAddTask);

        taskAdapter = new TaskAdapter(db);
        recyclerTasks.setLayoutManager(new LinearLayoutManager(this));
        recyclerTasks.setAdapter(taskAdapter);

        loadTasksFromDb();

        buttonAddTask.setOnClickListener(v -> showAddTaskDialog());
        buttonExportTasks.setOnClickListener(v -> exportTasksToCsv());



    }

    private void loadTasksFromDb() {
        new Thread(() -> {
            List<Task> tasks = db.taskDao().getAllTasks();
            runOnUiThread(() -> taskAdapter.setTasks(tasks));
        }).start();
    }

    // 🔹 Dialog προσθήκης νέας εκκρεμότητας
    private void showAddTaskDialog() {
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_add_task, null);

        EditText editTaskName = dialogView.findViewById(R.id.editTaskName);
        EditText editTaskPhone = dialogView.findViewById(R.id.editTaskPhone);
        EditText editTaskAfm = dialogView.findViewById(R.id.editTaskAfm);
        EditText editTaskType = dialogView.findViewById(R.id.editTaskType);
        EditText editTaskDescription = dialogView.findViewById(R.id.editTaskDescription);
        TextView textTaskDueDate = dialogView.findViewById(R.id.textTaskDueDate);

        // default: προθεσμία = σήμερα
        Calendar cal = Calendar.getInstance();
        String[] dueDateHolder = new String[1];
        dueDateHolder[0] = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .format(cal.getTime());
        textTaskDueDate.setText("Προθεσμία: " + dueDateHolder[0]);

        textTaskDueDate.setOnClickListener(v -> {
            int year = cal.get(Calendar.YEAR);
            int month = cal.get(Calendar.MONTH);
            int day = cal.get(Calendar.DAY_OF_MONTH);

            new DatePickerDialog(
                    this,
                    (view, y, m, d) -> {
                        cal.set(Calendar.YEAR, y);
                        cal.set(Calendar.MONTH, m);
                        cal.set(Calendar.DAY_OF_MONTH, d);
                        String selected = String.format(Locale.getDefault(), "%04d-%02d-%02d", y, m + 1, d);
                        dueDateHolder[0] = selected;
                        textTaskDueDate.setText("Προθεσμία: " + selected);
                    },
                    year, month, day
            ).show();
        });

        new AlertDialog.Builder(this)
                .setTitle("Νέα Εκκρεμότητα")
                .setView(dialogView)
                .setPositiveButton("Αποθήκευση", (dialog, which) -> {
                    String name = editTaskName.getText().toString().trim();
                    String phone = editTaskPhone.getText().toString().trim();
                    String afm = editTaskAfm.getText().toString().trim();
                    String type = editTaskType.getText().toString().trim();
                    String description = editTaskDescription.getText().toString().trim();
                    String dueDate = dueDateHolder[0];

                    if (TextUtils.isEmpty(name) && TextUtils.isEmpty(description)) {
                        Toast.makeText(this, "Συμπλήρωσε τουλάχιστον όνομα ή περιγραφή", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (TextUtils.isEmpty(type)) {
                        type = "Γενικό"; // default αν δεν γράψεις τίποτα
                    }

                    String dateCreated = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                            .format(new Date());

                    Task newTask = new Task(
                            name,
                            phone,
                            afm,
                            description,
                            dateCreated,
                            type,
                            dueDate,
                            false
                    );

                    // Εισαγωγή στη βάση
                    new Thread(() -> {
                        db.taskDao().insertTask(newTask);
                        List<Task> updated = db.taskDao().getAllTasks();
                        runOnUiThread(() -> taskAdapter.setTasks(updated));
                    }).start();

                    // Προγραμματισμός ειδοποίησης για την προθεσμία
                    scheduleTaskNotification(name, description, dueDate);

                })
                .setNegativeButton("Άκυρο", null)
                .show();
    }


    private void sendTestNotificationNow() {
        Intent intent = new Intent(this, TaskReminderReceiver.class);
        intent.putExtra("task_name", "TEST Task");
        intent.putExtra("task_description", "Αν βλέπεις αυτή την ειδοποίηση, όλα δουλεύουν!");

        // Στέλνουμε το broadcast αμέσως, ΧΩΡΙΣ AlarmManager
        sendBroadcast(intent);
    }

    private void scheduleTaskNotification(String name, String description, String dueDate) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Calendar now = Calendar.getInstance();
        long nowMillis = now.getTimeInMillis();

        Date date;
        try {
            date = sdf.parse(dueDate);
            if (date == null) return;
        } catch (ParseException e) {
            e.printStackTrace();
            return;
        }

        // 1️⃣ Υπενθύμιση στις 10:00
        Calendar calMorning = Calendar.getInstance();
        calMorning.setTime(date);
        calMorning.set(Calendar.HOUR_OF_DAY, 10);
        calMorning.set(Calendar.MINUTE, 0);
        calMorning.set(Calendar.SECOND, 0);
        long triggerMorning = calMorning.getTimeInMillis();

        // 2️⃣ Υπενθύμιση στις 17:00
        Calendar calAfternoon = Calendar.getInstance();
        calAfternoon.setTime(date);
        calAfternoon.set(Calendar.HOUR_OF_DAY, 17);
        calAfternoon.set(Calendar.MINUTE, 30);
        calAfternoon.set(Calendar.SECOND, 0);
        long triggerAfternoon = calAfternoon.getTimeInMillis();

        List<String> timesScheduled = new ArrayList<>();

        // Αν η ώρα είναι στο μέλλον → ορίζουμε alarm
        if (triggerMorning > nowMillis) {
            scheduleSingleAlarm(triggerMorning, name, description);
            timesScheduled.add(
                    new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                            .format(calMorning.getTime())
            );
        }

        if (triggerAfternoon > nowMillis) {
            scheduleSingleAlarm(triggerAfternoon, name, description);
            timesScheduled.add(
                    new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                            .format(calAfternoon.getTime())
            );
        }

        // Αν δεν προγραμματίστηκε καμία (ημερομηνία & ώρες έχουν περάσει) → σε 10 δευτερόλεπτα
        if (timesScheduled.isEmpty()) {
            Calendar calFallback = Calendar.getInstance();
            calFallback.add(Calendar.SECOND, 10);
            long triggerFallback = calFallback.getTimeInMillis();

            scheduleSingleAlarm(triggerFallback, name, description);

            String formatted = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
                    .format(calFallback.getTime());
            Toast.makeText(
                    this,
                    "Η προθεσμία έχει περάσει.\nΈβαλα υπενθύμιση σε 10 δευτερόλεπτα:\n" + formatted,
                    Toast.LENGTH_LONG
            ).show();
        } else {
            String msg = "Η υπενθύμιση ορίστηκε για:\n";
            for (String t : timesScheduled) {
                msg += "• " + t + "\n";
            }
            Toast.makeText(this, msg.trim(), Toast.LENGTH_LONG).show();
        }
    }


    private void scheduleSingleAlarm(long triggerAt, String name, String description) {
        Intent intent = new Intent(this, TaskReminderReceiver.class);
        intent.putExtra("task_name", name);
        intent.putExtra("task_description", description);

        int requestCode = (int) System.currentTimeMillis();

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                requestCode,
                intent,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                        ? PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                        : PendingIntent.FLAG_UPDATE_CURRENT
        );

        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        if (alarmManager == null) return;

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // Android 12+ → inexact alarm (δεν χρειάζεται SCHEDULE_EXACT_ALARM)
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent);
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAt,
                        pendingIntent
                );
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent);
            }
        } catch (SecurityException e) {
            e.printStackTrace();
            Toast.makeText(
                    this,
                    "Δεν μπόρεσα να ορίσω ακριβή υπενθύμιση, αλλά η εκκρεμότητα αποθηκεύτηκε.",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    // 🔹 Export εκκρεμοτήτων (done == false) -> σε CSV στο Downloads + email, ΟΠΩΣ στον μήνα
    private void exportTasksToCsv() {
        List<Task> allTasks = taskAdapter.getTasks();
        if (allTasks == null) {
            allTasks = new ArrayList<>();
        }

        // ΜΟΝΟ εκκρεμότητες (done == false)
        List<Task> pendingTasks = new ArrayList<>();
        for (Task t : allTasks) {
            if (t != null && !t.done) {
                pendingTasks.add(t);
            }
        }

        if (pendingTasks.isEmpty()) {
            Toast.makeText(this, "Δεν υπάρχουν εκκρεμότητες για export", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            String today = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    .format(new Date());

            StringBuilder sb = new StringBuilder();
            sb.append("ΕΚΚΡΕΜΟΤΗΤΕΣ;Ημερομηνία εξαγωγής;")
                    .append(today)
                    .append("\n\n");

            sb.append("Πελάτης;Κινητό;ΑΦΜ;Τύπος;Εκκρεμότητα;Ημερομηνία Δημιουργίας;Προθεσμία\n");

            for (Task t : pendingTasks) {
                sb.append(safe(t.name)).append(";")
                        .append(safe(t.phone)).append(";")
                        .append(safe(t.afm)).append(";")
                        .append(safe(t.type)).append(";")
                        .append(safe(t.description).replace("\n", " ")).append(";")
                        .append(safe(t.dateCreated)).append(";")
                        .append(safe(t.dueDate))
                        .append("\n");
            }

            saveTasksCsvToDownloads(sb.toString());

        }).start();
    }

    // 🔹 ΙΔΙΑ λογική με saveCsvToDownloads του MonthHistoryActivity
    private void saveTasksCsvToDownloads(String csvContent) {

        ContentResolver resolver = getContentResolver();
        Uri collection;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
        } else {
            collection = MediaStore.Files.getContentUri("external");
        }

        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault())
                .format(new Date());
        String fileName = "tasks_" + timestamp + ".csv";

        ContentValues values = new ContentValues();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
            values.put(MediaStore.Downloads.MIME_TYPE, "text/csv");
            values.put(MediaStore.Downloads.IS_PENDING, 1);
        } else {
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
            values.put(MediaStore.MediaColumns.MIME_TYPE, "text/csv");
        }

        Uri item = resolver.insert(collection, values);
        if (item == null) {
            runOnUiThread(() ->
                    Toast.makeText(this, "Σφάλμα δημιουργίας αρχείου στις Λήψεις", Toast.LENGTH_SHORT).show()
            );
            return;
        }

        try (OutputStream out = resolver.openOutputStream(item)) {
            if (out == null) {
                runOnUiThread(() ->
                        Toast.makeText(this, "Σφάλμα ανοίγματος αρχείου", Toast.LENGTH_SHORT).show()
                );
                return;
            }

            // BOM για ελληνικά σε Excel
            out.write(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});
            out.write(csvContent.getBytes(StandardCharsets.UTF_8));
            out.flush();

        } catch (IOException e) {
            e.printStackTrace();
            runOnUiThread(() ->
                    Toast.makeText(this, "Σφάλμα κατά το export", Toast.LENGTH_SHORT).show()
            );
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentValues done = new ContentValues();
            done.put(MediaStore.Downloads.IS_PENDING, 0);
            resolver.update(item, done, null, null);
        }

        runOnUiThread(() -> {
            Toast.makeText(this, "Το αρχείο αποθηκεύτηκε στις Λήψεις:\n" + fileName, Toast.LENGTH_LONG).show();
            sendEmailWithAttachment(item, fileName);
        });
    }

    // 🔹 Ίδιο pattern με MonthHistoryActivity αλλά για Εκκρεμότητες
    private void sendEmailWithAttachment(Uri fileUri, String filename) {

        String savedEmail = AppPreferences.getEffectiveReportEmail(this);

        Intent emailIntent = new Intent(Intent.ACTION_SEND);
        emailIntent.setType("text/csv");
        emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{savedEmail});
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Export Εκκρεμοτήτων");
        emailIntent.putExtra(Intent.EXTRA_TEXT, "Σας επισυνάπτω το αρχείο με τις εκκρεμότητες.");
        emailIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
        emailIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        startActivity(Intent.createChooser(emailIntent, "Αποστολή Εκκρεμοτήτων"));
    }

    private String safe(String text) {
        return text == null ? "" : text.trim();
    }
}
