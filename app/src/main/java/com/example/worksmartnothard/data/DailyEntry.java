package com.example.worksmartnothard.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "DailyEntry")
public class DailyEntry {

    @PrimaryKey(autoGenerate = true)
    public int id;

    @NonNull
    public String category;      // π.χ. "Vodafone Home W/F", "PortIN mobile", κλπ.

    @NonNull
    public String date;          // YYYY-MM-DD

    public double count;         // ποσότητα ή ποσό (για Ραντεβού)

    // 🔹 Υποκατηγορία για Vodafone Home W/F (ADSL 24, VDSL, FWA κτλ)
    // Για τις άλλες κατηγορίες μπορεί να είναι null.
    public String homeSubtype;

    public DailyEntry(@NonNull String category,
                      @NonNull String date,
                      double count,
                      String homeSubtype) {
        this.category = category;
        this.date = date;
        this.count = count;
        this.homeSubtype = homeSubtype;
    }
}
