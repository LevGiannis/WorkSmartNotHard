package com.example.worksmartnothard;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

public class ProgressViewModel extends AndroidViewModel {

    private final AppDatabase db;
    private final MutableLiveData<List<CategoryProgress>> progressList = new MutableLiveData<>();

    public ProgressViewModel(@NonNull Application application) {
        super(application);
        db = AppDatabase.getDatabase(application);
    }

    public LiveData<List<CategoryProgress>> getProgressList() {
        return progressList;
    }

    public void loadProgressForCurrentMonth() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH) + 1; // +1 γιατί ο μήνας είναι 0-based
        loadProgressForMonth(year, month);
    }

    public void loadProgressForMonth(int year, int month) {
        new Thread(() -> {
            String yearMonth = year + "-" + (month < 10 ? "0" + month : month);

            List<Goal> goals = db.goalDao().getGoalsForMonth(year, month);
            HashMap<String, Double> totals = new HashMap<>();

            for (Goal goal : goals) {
                List<DailyEntry> entries = db.dailyEntryDao().getEntriesForCategoryInMonth(goal.category, yearMonth);
                double total = 0;
                for (DailyEntry e : entries) {
                    total += e.count;
                }
                totals.put(goal.category, total);
            }

            List<CategoryProgress> result = new ArrayList<>();
            for (Goal goal : goals) {
                double achieved = totals.getOrDefault(goal.category, 0.0);
                result.add(new CategoryProgress(goal.category, goal.target, achieved, goal.month, goal.year));

            }

            progressList.postValue(result);
        }).start();
    }
}
