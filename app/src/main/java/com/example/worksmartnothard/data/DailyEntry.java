package com.example.worksmartnothard.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "daily_entries")
public class DailyEntry {

    @PrimaryKey(autoGenerate = true)
    public int id;

    @NonNull
    public String category;

    @NonNull
    public String date; // YYYY-MM-DD

    public double count; // Τώρα δέχεται και δεκαδικά (π.χ. €)

    public DailyEntry(@NonNull String category, @NonNull String date, double count) {
        this.category = category;
        this.date = date;
        this.count = count;
    }
}
