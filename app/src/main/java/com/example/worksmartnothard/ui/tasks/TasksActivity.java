package com.example.worksmartnothard.ui.tasks;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
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
import com.example.worksmartnothard.ui.common.PhotoAttachmentHelper;
import com.example.worksmartnothard.ui.common.PhotoViewerActivity;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Calendar;

public class TasksActivity extends AppCompatActivity {

    private RecyclerView recyclerTasks;
    private TaskAdapter taskAdapter;
    private Button buttonExportTasks;
    private Button buttonAddTask;
    private EditText editTaskSearch;
    private AppDatabase db;

    private int pendingEditTaskId = -1;

    private PhotoAttachmentHelper photoHelper;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 👉 Χρησιμοποιεί το activity_tasks.xml που σου έδωσα
        setContentView(R.layout.activity_tasks);

        db = AppDatabase.getDatabase(getApplicationContext());

        photoHelper = new PhotoAttachmentHelper(this);

        recyclerTasks = findViewById(R.id.recyclerTasks);
        buttonExportTasks = findViewById(R.id.buttonExportTasks);
        buttonAddTask = findViewById(R.id.buttonAddTask);
        editTaskSearch = findViewById(R.id.editTaskSearch);

        taskAdapter = new TaskAdapter(db, this::showEditTaskDialog);
        recyclerTasks.setLayoutManager(new LinearLayoutManager(this));
        recyclerTasks.setAdapter(taskAdapter);

        if (editTaskSearch != null) {
            editTaskSearch.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    taskAdapter.setQuery(s == null ? "" : s.toString());
                }

                @Override
                public void afterTextChanged(Editable s) {
                }
            });
        }

        pendingEditTaskId = getIntent() != null
            ? getIntent().getIntExtra(TaskReminderScheduler.EXTRA_TASK_ID, -1)
            : -1;

        loadTasksFromDb();

        buttonAddTask.setOnClickListener(v -> showAddTaskDialog());
        buttonExportTasks.setOnClickListener(v -> exportTasksToCsv());

    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        pendingEditTaskId = intent != null ? intent.getIntExtra(TaskReminderScheduler.EXTRA_TASK_ID, -1) : -1;
        // Refresh list so the task exists and then open edit.
        loadTasksFromDb();
    }

    private void loadTasksFromDb() {
        new Thread(() -> {
            List<Task> tasks = db.taskDao().getAllTasks();
            runOnUiThread(() -> {
                taskAdapter.setTasks(tasks);
                if (pendingEditTaskId > 0) {
                    int taskId = pendingEditTaskId;
                    pendingEditTaskId = -1;
                    Task match = null;
                    if (tasks != null) {
                        for (Task t : tasks) {
                            if (t != null && t.id == taskId) {
                                match = t;
                                break;
                            }
                        }
                    }
                    if (match != null) {
                        showEditTaskDialog(match);
                    } else {
                        // Fallback: load from DB and edit.
                        new Thread(() -> {
                            Task t = db.taskDao().getTaskById(taskId);
                            if (t != null) {
                                runOnUiThread(() -> showEditTaskDialog(t));
                            }
                        }).start();
                    }
                }
            });
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

        Button buttonAttachPhoto = dialogView.findViewById(R.id.buttonAttachPhoto);
        Button buttonRemovePhoto = dialogView.findViewById(R.id.buttonRemovePhoto);
        TextView textPhotoStatus = dialogView.findViewById(R.id.textPhotoStatus);
        ImageView imagePhotoPreview = dialogView.findViewById(R.id.imagePhotoPreview);

        final String[] photoUriHolder = new String[] { null };
        updatePhotoPreview(textPhotoStatus, imagePhotoPreview, buttonAttachPhoto, buttonRemovePhoto, photoUriHolder[0]);
        buttonAttachPhoto.setOnClickListener(v -> photoHelper.showChooser(uri -> {
            photoUriHolder[0] = uri == null ? null : uri.toString();
            updatePhotoPreview(textPhotoStatus, imagePhotoPreview, buttonAttachPhoto, buttonRemovePhoto, photoUriHolder[0]);
        }, false));

        buttonRemovePhoto.setOnClickListener(v -> {
            photoUriHolder[0] = null;
            updatePhotoPreview(textPhotoStatus, imagePhotoPreview, buttonAttachPhoto, buttonRemovePhoto, null);
        });

        imagePhotoPreview.setOnClickListener(v -> {
            if (TextUtils.isEmpty(photoUriHolder[0])) return;
            Intent i = new Intent(this, PhotoViewerActivity.class);
            i.putExtra(PhotoViewerActivity.EXTRA_PHOTO_URI, photoUriHolder[0]);
            startActivity(i);
        });

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
                    year, month, day).show();
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
                            photoUriHolder[0],
                            false);

                    // Εισαγωγή στη βάση
                    new Thread(() -> {
                        long insertedId = db.taskDao().insertTask(newTask);
                        newTask.id = (int) insertedId;

                        // Προγραμματισμός ειδοποίησης για την προθεσμία (ακυρώσιμο με βάση το task id)
                        TaskReminderScheduler.ScheduleResult result = TaskReminderScheduler
                                .schedule(getApplicationContext(), newTask, true);

                        List<Task> updated = db.taskDao().getAllTasks();
                        runOnUiThread(() -> {
                            taskAdapter.setTasks(updated);
                            showScheduleToast(result);
                        });
                    }).start();

                })
                .setNegativeButton("Άκυρο", null)
                .show();
    }

    private void showEditTaskDialog(Task task) {
        if (task == null) {
            return;
        }

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_task, null);

        EditText editTaskName = dialogView.findViewById(R.id.editTaskName);
        EditText editTaskPhone = dialogView.findViewById(R.id.editTaskPhone);
        EditText editTaskAfm = dialogView.findViewById(R.id.editTaskAfm);
        EditText editTaskType = dialogView.findViewById(R.id.editTaskType);
        EditText editTaskDescription = dialogView.findViewById(R.id.editTaskDescription);
        TextView textTaskDueDate = dialogView.findViewById(R.id.textTaskDueDate);

        Button buttonAttachPhoto = dialogView.findViewById(R.id.buttonAttachPhoto);
        Button buttonRemovePhoto = dialogView.findViewById(R.id.buttonRemovePhoto);
        TextView textPhotoStatus = dialogView.findViewById(R.id.textPhotoStatus);
        ImageView imagePhotoPreview = dialogView.findViewById(R.id.imagePhotoPreview);

        editTaskName.setText(safe(task.name));
        editTaskPhone.setText(safe(task.phone));
        editTaskAfm.setText(safe(task.afm));
        editTaskType.setText(safe(task.type));
        editTaskDescription.setText(safe(task.description));

        Calendar cal = Calendar.getInstance();
        String[] dueDateHolder = new String[1];
        dueDateHolder[0] = TextUtils.isEmpty(task.dueDate)
                ? new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.getTime())
                : task.dueDate;
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
                    year, month, day).show();
        });

        final String[] photoUriHolder = new String[] { task.photoUri };
        updatePhotoPreview(textPhotoStatus, imagePhotoPreview, buttonAttachPhoto, buttonRemovePhoto, photoUriHolder[0]);
        buttonAttachPhoto.setOnClickListener(v -> photoHelper.showChooser(uri -> {
            photoUriHolder[0] = uri == null ? null : uri.toString();
            updatePhotoPreview(textPhotoStatus, imagePhotoPreview, buttonAttachPhoto, buttonRemovePhoto, photoUriHolder[0]);
        }, false));

        buttonRemovePhoto.setOnClickListener(v -> {
            photoUriHolder[0] = null;
            updatePhotoPreview(textPhotoStatus, imagePhotoPreview, buttonAttachPhoto, buttonRemovePhoto, null);
        });

        imagePhotoPreview.setOnClickListener(v -> {
            if (TextUtils.isEmpty(photoUriHolder[0])) return;
            Intent i = new Intent(this, PhotoViewerActivity.class);
            i.putExtra(PhotoViewerActivity.EXTRA_PHOTO_URI, photoUriHolder[0]);
            startActivity(i);
        });

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Επεξεργασία Εκκρεμότητας")
                .setView(dialogView)
                .setPositiveButton("Αποθήκευση", null)
                .setNegativeButton("Άκυρο", null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(btn -> {
            String name = safe(editTaskName.getText().toString());
            String phone = safe(editTaskPhone.getText().toString());
            String afm = safe(editTaskAfm.getText().toString());
            String type = safe(editTaskType.getText().toString());
            String description = safe(editTaskDescription.getText().toString());
            String dueDate = safe(dueDateHolder[0]);

            if (TextUtils.isEmpty(name) && TextUtils.isEmpty(description)) {
                Toast.makeText(this, "Συμπλήρωσε τουλάχιστον όνομα ή περιγραφή", Toast.LENGTH_SHORT).show();
                return;
            }

            if (TextUtils.isEmpty(type)) {
                type = "Γενικό";
            }

            task.name = name;
            task.phone = phone;
            task.afm = afm;
            task.type = type;
            task.description = description;
            task.dueDate = dueDate;
            task.photoUri = TextUtils.isEmpty(photoUriHolder[0]) ? null : photoUriHolder[0];

            new Thread(() -> {
                db.taskDao().updateTask(task);

                // Keep reminders consistent with latest data.
                TaskReminderScheduler.cancel(getApplicationContext(), task.id);
                if (!task.done) {
                    TaskReminderScheduler.schedule(getApplicationContext(), task, false);
                }

                runOnUiThread(() -> {
                    loadTasksFromDb();
                    Toast.makeText(this, "Αποθηκεύτηκε", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                });
            }).start();
        }));

        dialog.show();
    }

    private void sendTestNotificationNow() {
        Intent intent = new Intent(this, TaskReminderReceiver.class);
        intent.putExtra("task_name", "TEST Task");
        intent.putExtra("task_description", "Αν βλέπεις αυτή την ειδοποίηση, όλα δουλεύουν!");

        // Στέλνουμε το broadcast αμέσως, ΧΩΡΙΣ AlarmManager
        sendBroadcast(intent);
    }

    private void showScheduleToast(TaskReminderScheduler.ScheduleResult result) {
        if (result == null || result.triggerTimes == null || result.triggerTimes.isEmpty()) {
            return;
        }

        if (result.usedFallback) {
            String formatted = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
                    .format(new Date(result.triggerTimes.get(0)));
            Toast.makeText(
                    this,
                    "Η προθεσμία έχει περάσει.\nΈβαλα υπενθύμιση σε 10 δευτερόλεπτα:\n" + formatted,
                    Toast.LENGTH_LONG).show();
            return;
        }

        StringBuilder msg = new StringBuilder("Η υπενθύμιση ορίστηκε για:\n");
        SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        for (long t : result.triggerTimes) {
            msg.append("• ").append(df.format(new Date(t))).append("\n");
        }
        Toast.makeText(this, msg.toString().trim(), Toast.LENGTH_LONG).show();
    }

    // 🔹 Export εκκρεμοτήτων (done == false) -> σε CSV στο Downloads + email, ΟΠΩΣ
    // στον μήνα
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
            runOnUiThread(
                    () -> Toast.makeText(this, "Σφάλμα δημιουργίας αρχείου στις Λήψεις", Toast.LENGTH_SHORT).show());
            return;
        }

        try (OutputStream out = resolver.openOutputStream(item)) {
            if (out == null) {
                runOnUiThread(() -> Toast.makeText(this, "Σφάλμα ανοίγματος αρχείου", Toast.LENGTH_SHORT).show());
                return;
            }

            // BOM για ελληνικά σε Excel
            out.write(new byte[] { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF });
            out.write(csvContent.getBytes(StandardCharsets.UTF_8));
            out.flush();

        } catch (IOException e) {
            e.printStackTrace();
            runOnUiThread(() -> Toast.makeText(this, "Σφάλμα κατά το export", Toast.LENGTH_SHORT).show());
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
        emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[] { savedEmail });
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Export Εκκρεμοτήτων");
        emailIntent.putExtra(Intent.EXTRA_TEXT, "Σας επισυνάπτω το αρχείο με τις εκκρεμότητες.");
        emailIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
        emailIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        startActivity(Intent.createChooser(emailIntent, "Αποστολή Εκκρεμοτήτων"));
    }

    private String safe(String text) {
        return text == null ? "" : text.trim();
    }

    private void updatePhotoPreview(TextView status, ImageView preview, Button attach, Button remove, String photoUri) {
        if (TextUtils.isEmpty(photoUri)) {
            status.setText("Καμία φωτογραφία");
            preview.setVisibility(View.GONE);
            preview.setImageDrawable(null);
            if (attach != null) attach.setText("Επισύναψη φωτογραφίας");
            if (remove != null) remove.setVisibility(View.GONE);
            return;
        }

        status.setText("Φωτογραφία: ΟΚ (πάτησε για προβολή)");
        preview.setVisibility(View.VISIBLE);
        if (attach != null) attach.setText("Αλλαγή φωτογραφίας");
        if (remove != null) remove.setVisibility(View.VISIBLE);
        try {
            preview.setImageURI(Uri.parse(photoUri));
        } catch (Exception ignored) {
        }
    }
}
