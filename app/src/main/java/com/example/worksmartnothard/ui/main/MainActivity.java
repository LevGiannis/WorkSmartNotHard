package com.example.worksmartnothard.ui.main;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
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
import com.example.worksmartnothard.ui.tasks.DailyTasksSummaryReceiver;
import com.example.worksmartnothard.util.BonusCalculator;
import com.example.worksmartnothard.viewmodel.ProgressViewModel;

import java.text.DateFormatSymbols;
import java.time.LocalDate;
import java.util.ArrayList;
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
    private TextView totalBonusText; // Συνολικό Μπόνους

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
        String storeDisplay = "Κατάστημα".equals(storeCode) ? "—" : storeCode;
        String nicknameDisplay = "Χρήστης".equals(nickname) ? "—" : nickname;
        userInfoText.setText(storeDisplay + "  •  " + nicknameDisplay);

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
        findViewById(R.id.fabAddEntry).setOnClickListener(v -> startActivity(new Intent(this, AddEntryActivity.class)));

        // FAB για στόχους (αντι για το "ορφανό" startActivity που είχες)
        findViewById(R.id.fabAddGoal).setOnClickListener(v -> startActivity(new Intent(this, AddGoalActivity.class)));

        findViewById(R.id.fabDailyHistory).setOnClickListener(v -> showDayPickerDialog());

        findViewById(R.id.fabMonthlyHistory).setOnClickListener(v -> showMonthYearDialog());

        findViewById(R.id.fabTasks).setOnClickListener(v -> startActivity(new Intent(this, TasksActivity.class)));

        taskBadge = findViewById(R.id.taskBadge);

        viewModel.loadProgressForCurrentMonth();
        updateTaskBadge();

        // 🔔 Καθημερινή ειδοποίηση με όλες τις εκκρεμότητες στις 10:00
        scheduleDailyTasksSummary();
    }

    @Override
    protected void onResume() {
        super.onResume();
        viewModel.loadProgressForCurrentMonth();
        updateTaskBadge();
    }

    // 🔔 Προγραμματισμός καθημερινής ειδοποίησης στις 10:00
    private void scheduleDailyTasksSummary() {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null)
            return;

        Intent intent = new Intent(this, DailyTasksSummaryReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                0,
                intent,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                        ? PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                        : PendingIntent.FLAG_UPDATE_CURRENT);

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.HOUR_OF_DAY, 10);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        // Αν η ώρα 10:00 για σήμερα έχει ήδη περάσει, προγραμμάτισε από αύριο
        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        // Δεν μας νοιάζει να είναι "απόλυτα" ακριβές, οπότε inexact για να παίζει
        // παντού χωρίς permissions
        alarmManager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.getTimeInMillis(),
                AlarmManager.INTERVAL_DAY,
                pendingIntent);
    }

    private void updateTaskBadge() {
        new Thread(() -> {
            List<Task> tasks = db.taskDao().getAllTasks();
            int pendingCount = 0;
            if (tasks != null) {
                for (Task t : tasks) {
                    if (t != null && !t.done) {
                        pendingCount++;
                    }
                }
            }

            int finalPendingCount = pendingCount;
            runOnUiThread(() -> {
                if (finalPendingCount > 0) {
                    taskBadge.setVisibility(View.VISIBLE);
                    taskBadge.setText(String.valueOf(finalPendingCount));
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

    private void showMonthYearDialog() {
        // Τρέχων μήνας/έτος
        Calendar cal = Calendar.getInstance();
        int currentYear = cal.get(Calendar.YEAR);
        int currentMonth = cal.get(Calendar.MONTH); // 0–11

        // Inflate το custom layout του dialog
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_month_year_picker, null);

        Spinner spinnerMonth = dialogView.findViewById(R.id.spinnerMonth);
        Spinner spinnerYear = dialogView.findViewById(R.id.spinnerYear);

        // Μήνες (Ιαν, Φεβ, ...)
        String[] months = new DateFormatSymbols(Locale.getDefault()).getMonths();
        List<String> monthNames = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            monthNames.add(months[i]); // μόνο οι 12 μήνες
        }

        ArrayAdapter<String> monthAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                monthNames);
        monthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerMonth.setAdapter(monthAdapter);
        spinnerMonth.setSelection(currentMonth);

        // Έτη, π.χ. από currentYear - 5 έως currentYear
        List<Integer> years = new ArrayList<>();
        for (int y = currentYear - 5; y <= currentYear; y++) {
            years.add(y);
        }

        ArrayAdapter<Integer> yearAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                years);
        yearAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerYear.setAdapter(yearAdapter);

        // Επιλέγουμε το currentYear (τελευταίο της λίστας)
        spinnerYear.setSelection(years.indexOf(currentYear));

        new AlertDialog.Builder(this)
                .setTitle("Επιλογή μήνα & έτους")
                .setView(dialogView)
                .setNegativeButton("Άκυρο", null)
                .setPositiveButton("OK", (dialog, which) -> {
                    int monthIndex = spinnerMonth.getSelectedItemPosition(); // 0–11
                    int year = (Integer) spinnerYear.getSelectedItem();

                    Intent intent = new Intent(MainActivity.this, MonthHistoryActivity.class);
                    intent.putExtra(MonthHistoryActivity.EXTRA_YEAR, year);
                    intent.putExtra(MonthHistoryActivity.EXTRA_MONTH, monthIndex + 1); // 1–12
                    startActivity(intent);
                })
                .show();
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
            overallProgressText.setText("Επιτυχία: 0%");
            overallProgressText.setTextColor(
                    ContextCompat.getColor(this, R.color.text_primary));
            if (totalBonusText != null) {
                totalBonusText.setText("Μπόνους: 0,00€");
            }
            return;
        }

        double totalTarget = 0.0;
        double totalAchieved = 0.0;
        for (CategoryProgress p : progressList) {
            if (p != null && p.target > 0) {
                totalTarget += p.target;
                totalAchieved += p.achieved;
            }
        }

        int weightedPercentage = (totalTarget <= 0.0)
                ? 0
                : (int) Math.round((totalAchieved * 100.0) / totalTarget);

        overallProgressText.setText("Επιτυχία: " + weightedPercentage + "%");

        int color = (weightedPercentage >= 95)
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
                            "Μπόνους: %.2f€",
                            totalBonus);
                    totalBonusText.setText(bonusText);
                }
            });
        }).start();
    }
}
