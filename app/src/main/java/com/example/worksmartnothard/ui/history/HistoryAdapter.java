package com.example.worksmartnothard.ui.history;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.worksmartnothard.R;
import com.example.worksmartnothard.data.DailyEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {

    private final List<DailyEntry> items = new ArrayList<>();

    public void setData(List<DailyEntry> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DailyEntry entry = items.get(position);

        // Αν είναι Vodafone Home W/F και έχουμε υποτύπο, τον εμφανίζουμε
        String title = entry.category;
        if ("Vodafone Home W/F".equals(entry.category)
                && entry.homeSubtype != null
                && !entry.homeSubtype.trim().isEmpty()) {
            title = entry.category + " - " + entry.homeSubtype.trim();
        }

        holder.textCategory.setText(title);
        holder.textCount.setText(formatCount(entry));
    }

    private String formatCount(DailyEntry entry) {
        // Ραντεβού = ποσό σε €, όλα τα άλλα = τεμάχια
        if ("Ραντεβού".equals(entry.category)) {
            return String.format(Locale.getDefault(), "%.2f €", entry.count);
        } else {
            return String.format(Locale.getDefault(), "%.0f τεμ.", entry.count);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        final TextView textCategory;
        final TextView textCount;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            textCategory = itemView.findViewById(R.id.textHistoryCategory);
            textCount = itemView.findViewById(R.id.textHistoryCount);
        }
    }
}
