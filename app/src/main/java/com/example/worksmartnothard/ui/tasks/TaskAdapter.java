package com.example.worksmartnothard.ui.tasks;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.worksmartnothard.R;
import com.example.worksmartnothard.data.AppDatabase;
import com.example.worksmartnothard.data.Task;

import java.util.ArrayList;
import java.util.List;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.ViewHolder> {

    private List<Task> tasks = new ArrayList<>();
    private final AppDatabase db;

    public TaskAdapter(AppDatabase db) {
        this.db = db;
    }

    public void setTasks(List<Task> newTasks) {
        this.tasks = newTasks != null ? newTasks : new ArrayList<>();
        notifyDataSetChanged();
    }

    // 🔹 Χρησιμοποιείται από το export για να πάρουμε τις τρέχουσες εκκρεμότητες
    public List<Task> getTasks() {
        return new ArrayList<>(tasks);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        CheckBox checkDone;
        TextView textName, textPhone, textAfm, textDescription;

        public ViewHolder(View view) {
            super(view);
            checkDone = view.findViewById(R.id.checkDone);
            textName = view.findViewById(R.id.textName);
            textPhone = view.findViewById(R.id.textPhone);
            textAfm = view.findViewById(R.id.textAfm);
            textDescription = view.findViewById(R.id.textDescription);
        }
    }

    @NonNull
    @Override
    public TaskAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_task, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskAdapter.ViewHolder holder, int position) {
        Task task = tasks.get(position);

        holder.textName.setText("Πελάτης: " + task.name);
        holder.textPhone.setText("Κινητό: " + task.phone);
        holder.textAfm.setText("ΑΦΜ: " + task.afm);
        holder.textDescription.setText("Εκκρεμότητα: " + task.description);
        holder.checkDone.setChecked(task.done);

        // Διαγραφή με παρατεταμένο πάτημα
        holder.itemView.setOnLongClickListener(v -> {
            new Thread(() -> {
                db.taskDao().deleteTask(task);
                // Ιδανικά notify στο main thread, αλλά το αφήνουμε απλό
                tasks.remove(position);
                holder.itemView.post(() -> notifyItemRemoved(position));
            }).start();
            return true;
        });

        // Ενημέρωση done flag
        holder.checkDone.setOnCheckedChangeListener((buttonView, isChecked) -> {
            task.done = isChecked;
            new Thread(() -> db.taskDao().updateTask(task)).start();
        });
    }

    @Override
    public int getItemCount() {
        return tasks.size();
    }
}
