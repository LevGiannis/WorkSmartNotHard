package com.example.worksmartnothard.ui.history;

import android.content.Intent;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.worksmartnothard.R;
import com.example.worksmartnothard.data.AppDatabase;
import com.example.worksmartnothard.data.DailyEntry;
import com.example.worksmartnothard.ui.common.PhotoViewerActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {

    public interface OnEntryClickListener {
        void onEditRequested(DailyEntry entry);
    }

    private final List<DailyEntry> allItems = new ArrayList<>();
    private final List<DailyEntry> visibleItems = new ArrayList<>();
    private String query = "";
    private final AppDatabase db;
    private final OnEntryClickListener listener;

    public HistoryAdapter(AppDatabase db, OnEntryClickListener listener) {
        this.db = db;
        this.listener = listener;
    }

    public void setData(List<DailyEntry> newItems) {
        allItems.clear();
        if (newItems != null) {
            allItems.addAll(newItems);
        }
        applyFilter();
    }

    public void setQuery(String query) {
        this.query = query == null ? "" : query.trim();
        applyFilter();
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
        DailyEntry entry = visibleItems.get(position);

        // Αν είναι Vodafone Home W/F και έχουμε υποτύπο, τον εμφανίζουμε
        String title = entry.category;
        if ("Vodafone Home W/F".equals(entry.category)
                && entry.homeType != null
                && !entry.homeType.trim().isEmpty()) {
            title = entry.category + " - " + entry.homeType.trim();
        }

        holder.textCategory.setText(title);
        holder.textCount.setText(formatCount(entry));

        holder.textDetails.setText(buildDetails(entry));

        holder.checkHasPending.setOnCheckedChangeListener(null);
        holder.checkHasPending.setChecked(entry.hasPending);
        holder.checkHasPending.setOnCheckedChangeListener((buttonView, isChecked) -> {
            entry.hasPending = isChecked;
            new Thread(() -> db.dailyEntryDao().updateDailyEntry(entry)).start();
        });

        holder.textPhoto.setVisibility(entry.photoUri == null || entry.photoUri.trim().isEmpty()
                ? View.GONE
                : View.VISIBLE);
        if (holder.textPhoto.getVisibility() == View.VISIBLE) {
            holder.textPhoto.setText("Φωτογραφία: Προβολή");
        }

        holder.textPhoto.setOnClickListener(v -> {
            if (entry.photoUri == null || entry.photoUri.trim().isEmpty())
                return;
            Intent i = new Intent(v.getContext(), PhotoViewerActivity.class);
            i.putExtra(PhotoViewerActivity.EXTRA_PHOTO_URI, entry.photoUri);
            v.getContext().startActivity(i);
        });

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEditRequested(entry);
            }
        });
    }

    private String buildDetails(DailyEntry entry) {
        StringBuilder sb = new StringBuilder();

        if (entry.orderNumber != null && !entry.orderNumber.trim().isEmpty()) {
            sb.append("Παραγγελία: ").append(entry.orderNumber.trim());
        }

        if (entry.customerFullName != null && !entry.customerFullName.trim().isEmpty()) {
            if (sb.length() > 0)
                sb.append("  •  ");
            sb.append("Πελάτης: ").append(entry.customerFullName.trim());
        }

        if (entry.referenceNumber != null && !entry.referenceNumber.trim().isEmpty()) {
            if (sb.length() > 0)
                sb.append("  •  ");
            sb.append("Αναφορά: ").append(entry.referenceNumber.trim());
        }

        if (sb.length() == 0) {
            return "";
        }
        return sb.toString();
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
        return visibleItems.size();
    }

    private void applyFilter() {
        visibleItems.clear();

        String q = query == null ? "" : query.trim().toLowerCase(Locale.getDefault());
        if (TextUtils.isEmpty(q)) {
            visibleItems.addAll(allItems);
            notifyDataSetChanged();
            return;
        }

        for (DailyEntry e : allItems) {
            if (e == null)
                continue;
            if (matchesQuery(e, q)) {
                visibleItems.add(e);
            }
        }

        notifyDataSetChanged();
    }

    private boolean matchesQuery(DailyEntry entry, String q) {
        return contains(buildTitle(entry), q)
                || contains(buildDetails(entry), q)
                || contains(formatCount(entry), q);
    }

    private String buildTitle(DailyEntry entry) {
        if (entry == null)
            return "";
        String title = entry.category == null ? "" : entry.category;
        if ("Vodafone Home W/F".equals(entry.category)
                && entry.homeType != null
                && !entry.homeType.trim().isEmpty()) {
            title = entry.category + " - " + entry.homeType.trim();
        }
        return title;
    }

    private boolean contains(String value, String q) {
        if (TextUtils.isEmpty(q))
            return true;
        if (value == null)
            return false;
        return value.toLowerCase(Locale.getDefault()).contains(q);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        final TextView textCategory;
        final TextView textCount;
        final TextView textDetails;
        final CheckBox checkHasPending;
        final TextView textPhoto;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            textCategory = itemView.findViewById(R.id.textHistoryCategory);
            textCount = itemView.findViewById(R.id.textHistoryCount);
            textDetails = itemView.findViewById(R.id.textHistoryDetails);
            checkHasPending = itemView.findViewById(R.id.checkHistoryHasPending);
            textPhoto = itemView.findViewById(R.id.textHistoryPhoto);
        }
    }
}
