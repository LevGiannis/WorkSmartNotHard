package com.example.worksmartnothard.ui.main;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.worksmartnothard.R;
import com.example.worksmartnothard.data.AppDatabase;
import com.example.worksmartnothard.data.AppPreferences;
import com.example.worksmartnothard.data.DailyEntry;
import com.example.worksmartnothard.data.Task;
import com.example.worksmartnothard.model.CategoryProgress;
import com.example.worksmartnothard.ui.entry.AddEntryActivity;
import com.example.worksmartnothard.ui.goal.AddGoalActivity;
import com.example.worksmartnothard.ui.history.HistoryActivity;
import com.example.worksmartnothard.ui.history.MonthHistoryActivity;
import com.example.worksmartnothard.ui.settings.SettingsActivity;
import com.example.worksmartnothard.ui.tasks.TasksActivity;
import com.example.worksmartnothard.util.BonusCalculator;
import com.example.worksmartnothard.viewmodel.ProgressViewModel;

import java.time.LocalDate;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private ProgressViewModel viewModel;
    private ProgressAdapter adapter;
    private AppDatabase db;

    private TextView taskBadge;
    private TextView overallProgressText;
    private TextView userInfoText;
    private TextView totalBonusText;   // Συνολικό Bonus

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = AppDatabase.getDatabase(getApplicationContext());

        // RecyclerView
        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        adapter = new ProgressAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // 🔧 Ανάκτηση ψευδωνύμου/καταστήματος
        userInfoText = findViewById(R.id.textUserInfo);
        String nickname = AppPreferences.getNickname(this);
        String storeCode = AppPreferences.getStoreCode(this);
        userInfoText.setText("📍 " + storeCode + " | 👤 " + nickname);

        // 🔧 Συνολική πρόοδος και bonus
        overallProgressText = findViewById(R.id.textOverallProgress);
        totalBonusText = findViewById(R.id.textTotalBonus);

        // ▶️ SETTINGS BUTTON
        findViewById(R.id.buttonSettings).setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
        });

        // 🔧 Προβολή στόχων
        viewModel = new ViewModelProvider(this).get(ProgressViewModel.class);
        viewModel.getProgressList().observe(this, progressList -> {
            adapter.setData(progressList);
            updateOverallProgress(progressList);
            updateTotalBonusForCurrentMonth();
        });

        // FABs
        findViewById(R.id.fabAddEntry).setOnClickListener(v ->
                startActivity(new Intent(this, AddEntryActivity.class)));

        findViewById(R.id.fabAddGoal).setOnClickListener(v ->
                startActivity(new Intent(this, AddGoalActivity.class)));

        findViewById(R.id.fabDailyHistory).setOnClickListener(v -> showDayPickerDialog());

        findViewById(R.id.fabMonthlyHistory).setOnClickListener(v -> showMonthPickerDialog());

        findViewById(R.id.fabTasks).setOnClickListener(v ->
                startActivity(new Intent(this, TasksActivity.class)));

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

    private void showDayPickerDialog() {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog picker = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            String date = year + "-" +
                    String.format(Locale.getDefault(), "%02d", month + 1) + "-" +
                    String.format(Locale.getDefault(), "%02d", dayOfMonth);

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

    // 🔹 Υπολογισμός ποσοστιαίας συνολικής επιτυχίας
    private void updateOverallProgress(List<CategoryProgress> progressList) {
        if (progressList == null || progressList.isEmpty()) {
            overallProgressText.setText("Success: 0%");
            overallProgressText.setTextColor(
                    ContextCompat.getColor(this, R.color.text_primary));
            if (totalBonusText != null) {
                totalBonusText.setText("Bonus: 0.00€");
            }
            return;
        }

        double percentageSum = 0.0;
        int count = 0;

        for (CategoryProgress p : progressList) {
            if (p.target > 0) {
                double categoryPercent = (p.achieved * 100.0) / p.target;
                percentageSum += categoryPercent;
                count++;
            }
        }

        int averagePercentage = (count == 0)
                ? 0
                : (int) Math.round(percentageSum / count);

        overallProgressText.setText("Success: " + averagePercentage + "%");

        int color = (averagePercentage >= 95)
                ? ContextCompat.getColor(this, R.color.accent_blue)
                : ContextCompat.getColor(this, R.color.text_primary);

        overallProgressText.setTextColor(color);
    }

    // 🔹 Υπολογισμός bonus τρέχοντος μήνα
    private void updateTotalBonusForCurrentMonth() {
        new Thread(() -> {
            String yearMonth = LocalDate.now().toString().substring(0, 7);
            List<DailyEntry> entries = db.dailyEntryDao().getEntriesForMonth(yearMonth);
            double totalBonus = BonusCalculator.calculateMonthlyBonus(entries);

            runOnUiThread(() -> {
                if (totalBonusText != null) {
                    String bonusText = String.format(
                            Locale.getDefault(),
                            "Bonus: %.2f€",
                            totalBonus
                    );
                    totalBonusText.setText(bonusText);
                }
            });
        }).start();
    }
}
