package com.example.worksmartnothard.ui.goal;

import android.app.AlertDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.worksmartnothard.R;
import com.example.worksmartnothard.data.AppDatabase;
import com.example.worksmartnothard.data.Category;
import com.example.worksmartnothard.data.Goal;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class AddGoalActivity extends AppCompatActivity {

    private RecyclerView recycler;
    private Button buttonAddCategory;
    private Button buttonSave;
    private GoalCategoryTargetAdapter adapter;
    private AppDatabase db;
    private int year;
    private int month;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_goal);

        recycler = findViewById(R.id.recyclerGoalCategories);
        buttonAddCategory = findViewById(R.id.buttonAddCategory);
        buttonSave = findViewById(R.id.buttonSaveGoals);

        adapter = new GoalCategoryTargetAdapter();
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(adapter);

        db = AppDatabase.getDatabase(getApplicationContext());

        LocalDate now = LocalDate.now();
        year = now.getYear();
        month = now.getMonthValue();

        seedCategoriesIfNeeded();

        buttonAddCategory.setOnClickListener(v -> showAddCategoryDialog());

        buttonSave.setOnClickListener(v -> saveGoals());

        loadRows();
    }

    private void seedCategoriesIfNeeded() {
        new Thread(() -> {
            try {
                if (db.categoryDao().count() > 0)
                    return;

                String[] defaults = getResources().getStringArray(R.array.categories);
                for (String name : defaults) {
                    if (name == null)
                        continue;
                    String trimmed = name.trim();
                    if (trimmed.isEmpty())
                        continue;
                    db.categoryDao().insertCategory(new Category(trimmed));
                }
            } catch (Exception ignored) {
            }
        }).start();
    }

    private void loadRows() {
        new Thread(() -> {
            List<String> categories = db.categoryDao().getAllNames();
            if (categories == null || categories.isEmpty()) {
                String[] defaults = getResources().getStringArray(R.array.categories);
                categories = new ArrayList<>();
                for (String c : defaults)
                    categories.add(c);
            }

            List<Goal> goals = db.goalDao().getGoalsForMonth(year, month);
            HashMap<String, Goal> byCategory = new HashMap<>();
            if (goals != null) {
                for (Goal g : goals) {
                    if (g != null && g.category != null)
                        byCategory.put(g.category, g);
                }
            }

            List<GoalCategoryTargetAdapter.Row> rows = new ArrayList<>();
            for (String category : categories) {
                if (category == null)
                    continue;
                String trimmed = category.trim();
                if (trimmed.isEmpty())
                    continue;

                Goal g = byCategory.get(trimmed);
                boolean checked = g != null;
                String targetText = "";
                if (g != null) {
                    boolean isMoney = "Ραντεβού".equals(trimmed);
                    targetText = isMoney
                            ? String.format(Locale.getDefault(), "%.2f", g.target)
                            : String.valueOf((int) Math.round(g.target));
                }
                rows.add(new GoalCategoryTargetAdapter.Row(trimmed, checked, targetText));
            }

            runOnUiThread(() -> adapter.setRows(rows));
        }).start();
    }

    private void showAddCategoryDialog() {
        EditText input = new EditText(this);
        input.setHint("π.χ. Νέα Κατηγορία");

        new AlertDialog.Builder(this)
                .setTitle("Νέα κατηγορία")
                .setView(input)
                .setPositiveButton("Προσθήκη", (dialog, which) -> {
                    String name = input.getText().toString().trim();
                    if (name.isEmpty()) {
                        Toast.makeText(this, "Βάλε όνομα κατηγορίας", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    new Thread(() -> {
                        db.categoryDao().insertCategory(new Category(name));
                        runOnUiThread(this::loadRows);
                    }).start();
                })
                .setNegativeButton("Άκυρο", null)
                .show();
    }

    private void saveGoals() {
        List<GoalCategoryTargetAdapter.Row> rows = adapter.getRows();

        for (GoalCategoryTargetAdapter.Row row : rows) {
            if (!row.checked)
                continue;
            String text = row.targetText == null ? "" : row.targetText.trim();
            if (text.isEmpty()) {
                Toast.makeText(this, "Βάλε στόχο για: " + row.category, Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                Double.parseDouble(text.replace(',', '.'));
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Μη έγκυρος αριθμός: " + row.category, Toast.LENGTH_SHORT).show();
                return;
            }
        }

        new Thread(() -> {
            for (GoalCategoryTargetAdapter.Row row : rows) {
                if (row == null || row.category == null)
                    continue;

                if (!row.checked) {
                    db.goalDao().deleteGoal(row.category, month, year);
                    continue;
                }

                String text = row.targetText == null ? "" : row.targetText.trim();
                double target = Double.parseDouble(text.replace(',', '.'));

                Goal existing = db.goalDao().findGoal(row.category, month, year);
                if (existing == null) {
                    db.goalDao().insertGoal(new Goal(row.category, year, month, target));
                } else {
                    existing.target = target;
                    db.goalDao().updateGoal(existing);
                }
            }

            runOnUiThread(() -> {
                Toast.makeText(this, "Οι στόχοι αποθηκεύτηκαν!", Toast.LENGTH_SHORT).show();
                finish();
            });
        }).start();
    }
}
