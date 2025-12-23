package com.example.worksmartnothard.ui.goal;

import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.worksmartnothard.R;

import java.util.ArrayList;
import java.util.List;

public class GoalCategoryTargetAdapter extends RecyclerView.Adapter<GoalCategoryTargetAdapter.ViewHolder> {

    public static class Row {
        public final String category;
        public boolean checked;
        public String targetText;

        public Row(String category, boolean checked, String targetText) {
            this.category = category;
            this.checked = checked;
            this.targetText = targetText;
        }
    }

    private final List<Row> rows = new ArrayList<>();

    public void setRows(List<Row> newRows) {
        rows.clear();
        if (newRows != null)
            rows.addAll(newRows);
        notifyDataSetChanged();
    }

    public List<Row> getRows() {
        return new ArrayList<>(rows);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_goal_category_target, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Row row = rows.get(position);

        holder.checkBox.setOnCheckedChangeListener(null);
        holder.checkBox.setChecked(row.checked);
        holder.checkBox.setText(row.category);

        boolean isMoney = "Ραντεβού".equals(row.category);
        holder.targetEdit.setInputType(isMoney
                ? (InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL)
                : InputType.TYPE_CLASS_NUMBER);

        if (holder.watcher != null) {
            holder.targetEdit.removeTextChangedListener(holder.watcher);
        }

        holder.targetEdit.setText(row.targetText == null ? "" : row.targetText);
        holder.targetEdit.setEnabled(row.checked);

        holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            row.checked = isChecked;
            holder.targetEdit.setEnabled(isChecked);
        });

        holder.watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                row.targetText = s == null ? "" : s.toString();
            }
        };
        holder.targetEdit.addTextChangedListener(holder.watcher);
    }

    @Override
    public int getItemCount() {
        return rows.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final CheckBox checkBox;
        final EditText targetEdit;
        TextWatcher watcher;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            checkBox = itemView.findViewById(R.id.checkCategory);
            targetEdit = itemView.findViewById(R.id.editCategoryTarget);
        }
    }
}
