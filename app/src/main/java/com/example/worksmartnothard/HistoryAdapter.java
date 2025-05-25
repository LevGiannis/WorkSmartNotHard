package com.example.worksmartnothard;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {

    private List<DailyEntry> data = new ArrayList<>();

    public void setData(List<DailyEntry> newData) {
        data = newData;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textCategory, textCount;

        public ViewHolder(View itemView) {
            super(itemView);
            textCategory = itemView.findViewById(R.id.textHistoryCategory);
            textCount = itemView.findViewById(R.id.textHistoryCount);
        }
    }

    @NonNull
    @Override
    public HistoryAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryAdapter.ViewHolder holder, int position) {
        DailyEntry entry = data.get(position);

        boolean isMoney = entry.category.equals("Ραντεβού");
        String unit = isMoney ? "€" : "τεμ.";
        String countFormatted = isMoney
                ? String.format("%.2f", entry.count)
                : String.valueOf((int) entry.count);

        holder.textCategory.setText(entry.category);
        holder.textCount.setText(countFormatted + " " + unit);
    }

    @Override
    public int getItemCount() {
        return data.size();
    }
}
