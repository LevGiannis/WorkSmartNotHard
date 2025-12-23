package com.example.worksmartnothard.ui.tasks;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.worksmartnothard.R;
import com.example.worksmartnothard.data.AppDatabase;
import com.example.worksmartnothard.data.Task;
import com.example.worksmartnothard.ui.common.PhotoViewerActivity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.ViewHolder> {

    public interface OnTaskClickListener {
        void onEditRequested(Task task);
    }

    private List<Task> allTasks = new ArrayList<>();
    private List<Task> visibleTasks = new ArrayList<>();
    private String query = "";
    private final AppDatabase db;
    private final OnTaskClickListener listener;

    public TaskAdapter(AppDatabase db, OnTaskClickListener listener) {
        this.db = db;
        this.listener = listener;
    }

    public void setTasks(List<Task> newTasks) {
        this.allTasks = newTasks != null ? new ArrayList<>(newTasks) : new ArrayList<>();
        applyFilterAndSort();
    }

    public void setQuery(String query) {
        this.query = query == null ? "" : query.trim();
        applyFilterAndSort();
    }

    // Χρησιμοποιείται από το export για να πάρουμε τις τρέχουσες εκκρεμότητες
    public List<Task> getTasks() {
        return new ArrayList<>(allTasks);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        CheckBox checkDone;
        TextView textName, textPhone, textAfm, textDescription, textDueDate, textPhoto;

        public ViewHolder(View view) {
            super(view);
            checkDone = view.findViewById(R.id.checkDone);
            textName = view.findViewById(R.id.textName);
            textPhone = view.findViewById(R.id.textPhone);
            textAfm = view.findViewById(R.id.textAfm);
            textDescription = view.findViewById(R.id.textDescription);
            textDueDate = view.findViewById(R.id.textDueDate);
            textPhoto = view.findViewById(R.id.textTaskPhoto);
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
        Task task = visibleTasks.get(position);

        // Γεμίζουμε τα πεδία
        String header = safe(task.name);
        if (!TextUtils.isEmpty(task.type)) {
            header = TextUtils.isEmpty(header) ? task.type : (header + " • " + task.type);
        }
        holder.textName.setText(header);
        String desc = safe(task.description);
        holder.textDescription.setText(TextUtils.isEmpty(desc) ? "—" : desc);
        String due = safe(task.dueDate);
        bindDueDate(holder, due, task.done);

        String phoneRaw = safe(task.phone);
        holder.textPhone.setText("Κινητό: " + phoneRaw);

        String afmRaw = safe(task.afm);
        holder.textAfm.setText("ΑΦΜ: " + afmRaw);

        holder.textPhoto.setVisibility(task.photoUri == null || task.photoUri.trim().isEmpty()
                ? View.GONE
                : View.VISIBLE);
        if (holder.textPhoto.getVisibility() == View.VISIBLE) {
            holder.textPhoto.setText("Φωτογραφία: Προβολή");
        }

        holder.textPhoto.setOnClickListener(v -> {
            if (task.photoUri == null || task.photoUri.trim().isEmpty())
                return;
            Intent i = new Intent(v.getContext(), PhotoViewerActivity.class);
            i.putExtra(PhotoViewerActivity.EXTRA_PHOTO_URI, task.photoUri);
            v.getContext().startActivity(i);
        });

        // Quick actions: tap to copy, long-press to call (όπου υπάρχει)
        holder.textPhone.setOnClickListener(v -> {
            if (TextUtils.isEmpty(phoneRaw))
                return;
            copyToClipboard(v.getContext(), "Κινητό", phoneRaw);
            Toast.makeText(v.getContext(), "Αντιγράφηκε κινητό", Toast.LENGTH_SHORT).show();
        });
        holder.textPhone.setOnLongClickListener(v -> {
            if (TextUtils.isEmpty(phoneRaw))
                return true;
            Intent dial = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phoneRaw));
            v.getContext().startActivity(dial);
            return true;
        });

        holder.textAfm.setOnClickListener(v -> {
            if (TextUtils.isEmpty(afmRaw))
                return;
            copyToClipboard(v.getContext(), "ΑΦΜ", afmRaw);
            Toast.makeText(v.getContext(), "Αντιγράφηκε ΑΦΜ", Toast.LENGTH_SHORT).show();
        });

        // Για να μην “πετάγεται” το listener όταν κάνουμε setChecked
        holder.checkDone.setOnCheckedChangeListener(null);
        holder.checkDone.setChecked(task.done);

        // Διαγραφή με παρατεταμένο πάτημα
        holder.itemView.setOnLongClickListener(v -> {
            TaskReminderScheduler.cancel(v.getContext().getApplicationContext(), task.id);
            new Thread(() -> {
                db.taskDao().deleteTask(task);
                int adapterPosition = holder.getBindingAdapterPosition();
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    int removedTaskId = task.id;
                    holder.itemView.post(() -> {
                        removeById(allTasks, removedTaskId);
                        removeById(visibleTasks, removedTaskId);
                        notifyItemRemoved(adapterPosition);
                    });
                }
            }).start();
            return true;
        });

        // Επεξεργασία με απλό πάτημα
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEditRequested(task);
            }
        });

        // Ενημέρωση done flag
        holder.checkDone.setOnCheckedChangeListener((buttonView, isChecked) -> {
            task.done = isChecked;
            if (isChecked) {
                TaskReminderScheduler.cancel(buttonView.getContext().getApplicationContext(), task.id);
            } else {
                // Re-enable reminders, but don't fallback-spam if due date is already past.
                TaskReminderScheduler.schedule(buttonView.getContext().getApplicationContext(), task, false);
            }

            new Thread(() -> db.taskDao().updateTask(task)).start();
            applyFilterAndSort();
        });
    }

    private String safe(String text) {
        return text == null ? "" : text.trim();
    }

    @Override
    public int getItemCount() {
        return visibleTasks.size();
    }

    private void applyFilterAndSort() {
        String q = query == null ? "" : query.trim().toLowerCase(Locale.getDefault());
        List<Task> filtered = new ArrayList<>();
        for (Task t : allTasks) {
            if (t == null)
                continue;
            if (TextUtils.isEmpty(q) || matchesQuery(t, q)) {
                filtered.add(t);
            }
        }

        // done last, then by due date asc (yyyy-MM-dd sorts lexicographically)
        Collections.sort(filtered, new Comparator<Task>() {
            @Override
            public int compare(Task a, Task b) {
                if (a == null && b == null)
                    return 0;
                if (a == null)
                    return 1;
                if (b == null)
                    return -1;
                if (a.done != b.done)
                    return a.done ? 1 : -1;
                String da = safe(a.dueDate);
                String db = safe(b.dueDate);
                int c = da.compareTo(db);
                if (c != 0)
                    return c;
                return safe(a.name).compareToIgnoreCase(safe(b.name));
            }
        });

        visibleTasks = filtered;
        notifyDataSetChanged();
    }

    private boolean matchesQuery(Task task, String q) {
        return contains(safe(task.name), q)
                || contains(safe(task.description), q)
                || contains(safe(task.phone), q)
                || contains(safe(task.afm), q)
                || contains(safe(task.type), q)
                || contains(safe(task.dueDate), q);
    }

    private boolean contains(String value, String q) {
        if (TextUtils.isEmpty(q))
            return true;
        if (value == null)
            return false;
        return value.toLowerCase(Locale.getDefault()).contains(q);
    }

    private void bindDueDate(ViewHolder holder, String due, boolean done) {
        holder.textDueDate.setTypeface(Typeface.DEFAULT);
        holder.textDueDate.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.text_secondary));

        String base = "Προθεσμία: " + due;
        if (done || TextUtils.isEmpty(due)) {
            holder.textDueDate.setText(base);
            return;
        }

        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        int cmp = due.compareTo(today);
        if (cmp < 0) {
            holder.textDueDate.setText(base + "  •  ΛΗΓΜΕΝΟ");
            holder.textDueDate.setTypeface(Typeface.DEFAULT_BOLD);
            holder.textDueDate.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.text_primary));
        } else if (cmp == 0) {
            holder.textDueDate.setText(base + "  •  Σήμερα");
            holder.textDueDate.setTypeface(Typeface.DEFAULT_BOLD);
            holder.textDueDate.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.accent_blue));
        } else {
            holder.textDueDate.setText(base);
        }
    }

    private void copyToClipboard(Context context, String label, String value) {
        ClipboardManager cm = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm == null)
            return;
        cm.setPrimaryClip(ClipData.newPlainText(label, value));
    }

    private void removeById(List<Task> list, int id) {
        if (list == null)
            return;
        for (int i = list.size() - 1; i >= 0; i--) {
            Task t = list.get(i);
            if (t != null && t.id == id) {
                list.remove(i);
                return;
            }
        }
    }
}
