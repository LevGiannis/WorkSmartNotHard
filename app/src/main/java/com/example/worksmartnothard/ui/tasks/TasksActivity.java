package com.example.worksmartnothard.ui.tasks;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.worksmartnothard.R;
import com.example.worksmartnothard.data.AppDatabase;
import com.example.worksmartnothard.data.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.time.LocalDate;
import java.util.List;

public class TasksActivity extends AppCompatActivity {

    private AppDatabase db;
    private TaskAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tasks);

        db = AppDatabase.getDatabase(getApplicationContext());

        RecyclerView recyclerView = findViewById(R.id.recyclerTasks);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TaskAdapter(db);
        recyclerView.setAdapter(adapter);

        loadTasks();

        FloatingActionButton fabAddTask = findViewById(R.id.fabAddTask);
        fabAddTask.setOnClickListener(v -> showAddTaskDialog());
    }

    private void loadTasks() {
        new Thread(() -> {
            List<Task> tasks = db.taskDao().getAllTasks();
            runOnUiThread(() -> adapter.setTasks(tasks));
        }).start();
    }

    private void showAddTaskDialog() {
        View formView = getLayoutInflater().inflate(R.layout.dialog_add_task, null);

        EditText inputName = formView.findViewById(R.id.inputName);
        EditText inputPhone = formView.findViewById(R.id.inputPhone);
        EditText inputAfm = formView.findViewById(R.id.inputAfm);
        EditText inputDescription = formView.findViewById(R.id.inputDescription);

        new AlertDialog.Builder(this)
                .setTitle("Νέα Εκκρεμότητα")
                .setView(formView)
                .setPositiveButton("Καταχώριση", (dialog, which) -> {
                    String name = inputName.getText().toString().trim();
                    String phone = inputPhone.getText().toString().trim();
                    String afm = inputAfm.getText().toString().trim();
                    String description = inputDescription.getText().toString().trim();

                    if (!name.isEmpty() && !phone.isEmpty() && !afm.isEmpty() && !description.isEmpty()) {
                        Task task = new Task(name, phone, afm, description, false, LocalDate.now().toString());
                        new Thread(() -> {
                            db.taskDao().insertTask(task);
                            loadTasks();
                        }).start();
                    } else {
                        Toast.makeText(this, "Συμπλήρωσε όλα τα πεδία", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Άκυρο", null)
                .show();
    }
}
