package com.example.worksmartnothard;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.Calendar;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private ProgressViewModel viewModel;
    private ProgressAdapter adapter;
    private AppDatabase db;
    private TextView taskBadge;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = AppDatabase.getDatabase(getApplicationContext());

        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        adapter = new ProgressAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        viewModel = new ViewModelProvider(this).get(ProgressViewModel.class);
        viewModel.getProgressList().observe(this, adapter::setData);

        // FABs
        findViewById(R.id.fabAddEntry).setOnClickListener(v ->
                startActivity(new Intent(this, AddEntryActivity.class)));

        findViewById(R.id.fabAddGoal).setOnClickListener(v ->
                startActivity(new Intent(this, AddGoalActivity.class)));

        findViewById(R.id.fabDailyHistory).setOnClickListener(v -> showDayPickerDialog());
        findViewById(R.id.fabMonthlyHistory).setOnClickListener(v -> showMonthPickerDialog());

        findViewById(R.id.fabTasks).setOnClickListener(v ->
                startActivity(new Intent(this, TasksActivity.class)));

        // Badge
        taskBadge = findViewById(R.id.taskBadge);

        viewModel.loadProgressForCurrentMonth();
        updateTaskBadge();
    }

    @Override
    protected void onResume() {
        super.onResume();
        viewModel.loadProgressForCurrentMonth();
        updateTaskBadge();
    }

    private void showDayPickerDialog() {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog picker = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            String date = year + "-" +
                    String.format("%02d", month + 1) + "-" +
                    String.format("%02d", dayOfMonth);

            Intent intent = new Intent(this, HistoryActivity.class);
            intent.putExtra(HistoryActivity.EXTRA_DATE, date);
            startActivity(intent);
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));

        picker.setTitle("Επιλογή ημέρας");
        picker.show();
    }

    private void showMonthPickerDialog() {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog picker = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            Intent intent = new Intent(this, MonthHistoryActivity.class);
            intent.putExtra(MonthHistoryActivity.EXTRA_YEAR, year);
            intent.putExtra(MonthHistoryActivity.EXTRA_MONTH, month + 1);
            startActivity(intent);
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), 1);

        picker.setTitle("Επιλογή μήνα");
        picker.show();
    }

    private void updateTaskBadge() {
        new Thread(() -> {
            List<Task> tasks = db.taskDao().getAllTasks();
            int pendingCount = (int) tasks.stream().filter(task -> !task.done).count();

            runOnUiThread(() -> {
                if (pendingCount > 0) {
                    taskBadge.setVisibility(View.VISIBLE);
                    taskBadge.setText(String.valueOf(pendingCount));
                } else {
                    taskBadge.setVisibility(View.GONE);
                }
            });
        }).start();
    }
}
