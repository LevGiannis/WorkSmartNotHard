package com.example.worksmartnothard;

import android.app.AlertDialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ProgressAdapter extends RecyclerView.Adapter<ProgressAdapter.ViewHolder> {

    private List<CategoryProgress> data = new ArrayList<>();

    public void setData(List<CategoryProgress> newData) {
        data = newData;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView categoryText, percentText;
        ProgressBar progressBar;
        ImageView iconCheck;

        public ViewHolder(View view) {
            super(view);
            categoryText = view.findViewById(R.id.textCategory);
            percentText = view.findViewById(R.id.textPercent);
            progressBar = view.findViewById(R.id.progressBar);
            iconCheck = view.findViewById(R.id.iconCheck); // Vector icon ✔️
        }
    }

    @NonNull
    @Override
    public ProgressAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_progress, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProgressAdapter.ViewHolder holder, int position) {
        CategoryProgress item = data.get(position);

        holder.categoryText.setText(item.category);

        boolean isMoney = item.category.equals("Ραντεβού");
        String unit = isMoney ? "€" : "τεμ.";

        String achievedFormatted = isMoney
                ? String.format("%.2f", item.achieved)
                : String.valueOf((int) item.achieved);

        String targetFormatted = isMoney
                ? String.format("%.2f", item.target)
                : String.valueOf((int) item.target);

        int percentage = item.getPercentage();
        holder.percentText.setText(percentage + "% (" + achievedFormatted + "/" + targetFormatted + " " + unit + ")");
        holder.progressBar.setProgress(percentage);

        if (percentage >= 95) {
            holder.progressBar.setProgressTintList(
                    ColorStateList.valueOf(Color.parseColor("#2196F3")) // μπλε
            );
        } else {
            holder.progressBar.setProgressTintList(null); // default
        }

        if (percentage >= 100) {
            holder.iconCheck.setVisibility(View.VISIBLE);
        } else {
            holder.iconCheck.setVisibility(View.GONE);
        }

        // ✏️ Επεξεργασία στόχου με long press
        holder.itemView.setOnLongClickListener(v -> {
            showEditGoalDialog(v.getContext(), item);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    private void showEditGoalDialog(Context context, CategoryProgress progress) {
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_edit_goal, null);

        Spinner categorySpinner = dialogView.findViewById(R.id.spinnerCategory);
        EditText targetInput = dialogView.findViewById(R.id.inputTarget);

        String[] categories = context.getResources().getStringArray(R.array.categories);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, categories);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(adapter);

        categorySpinner.setSelection(Arrays.asList(categories).indexOf(progress.category));
        targetInput.setText(String.valueOf((int) progress.target));

        new AlertDialog.Builder(context)
                .setTitle("Επεξεργασία Στόχου")
                .setView(dialogView)
                .setPositiveButton("Αποθήκευση", (dialog, which) -> {
                    String newCategory = categorySpinner.getSelectedItem().toString();
                    int newTarget;
                    try {
                        newTarget = Integer.parseInt(targetInput.getText().toString().trim());
                    } catch (NumberFormatException e) {
                        Toast.makeText(context, "Μη έγκυρος αριθμός", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    new Thread(() -> {
                        AppDatabase db = AppDatabase.getDatabase(context);
                        GoalDao dao = db.goalDao();
                        Goal goal = dao.findGoal(progress.category, progress.month, progress.year);
                        if (goal != null) {
                            goal.category = newCategory;
                            goal.target = newTarget;
                            dao.updateGoal(goal);
                        }
                    }).start();
                })
                .setNegativeButton("Άκυρο", null)
                .show();
    }
}
