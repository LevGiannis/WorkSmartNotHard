package com.example.worksmartnothard.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.Ignore;

@Entity
public class DailyEntry {

    @PrimaryKey(autoGenerate = true)
    public int id;

    // Κύρια κατηγορία (PortIN mobile, Vodafone Home W/F, Ραντεβού κλπ)
    public String category;

    // Ημερομηνία μορφής YYYY-MM-DD
    public String date;

    // Ποσότητα (τεμ. ή ποσό € για Ραντεβού)
    public double count;

    // Υποκατηγορία για Vodafone Home W/F (ADSL 24, VDSL κλπ)
    // Μπορεί να είναι null για άλλες κατηγορίες
    public String homeType;

    // 🔹 Constructor που ΔΕΝ θα χρησιμοποιεί το Room (μόνο για δικό σου κώδικα)
    @Ignore
    public DailyEntry(String category, String date, double count) {
        this(category, date, count, null);
    }

    // 🔹 Constructor που θα χρησιμοποιεί το Room
    public DailyEntry(String category, String date, double count, String homeType) {
        this.category = category;
        this.date = date;
        this.count = count;
        this.homeType = homeType;
    }
}
