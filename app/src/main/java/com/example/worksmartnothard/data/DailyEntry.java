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

    // 🔹 Επιπλέον στοιχεία καταχώρησης (π.χ. Exprepay)
    // Μπορούν να είναι null/κενά αν δεν ισχύουν για την κατηγορία.
    public String orderNumber;
    public String customerFullName;
    public String referenceNumber;
    public boolean hasPending;

    // Προαιρετική επισύναψη φωτογραφίας (content://... ή fileprovider uri)
    public String photoUri;

    // 🔹 Constructor που ΔΕΝ θα χρησιμοποιεί το Room (μόνο για δικό σου κώδικα)
    @Ignore
    public DailyEntry(String category, String date, double count) {
        this(category, date, count, null);
    }

    @Ignore
    public DailyEntry(String category, String date, double count, String homeType) {
        this(category, date, count, homeType, null, null, null, false, null);
    }

    // 🔹 Constructor που θα χρησιμοποιεί το Room
    public DailyEntry(
            String category,
            String date,
            double count,
            String homeType,
            String orderNumber,
            String customerFullName,
            String referenceNumber,
            boolean hasPending,
            String photoUri) {
        this.category = category;
        this.date = date;
        this.count = count;
        this.homeType = homeType;
        this.orderNumber = orderNumber;
        this.customerFullName = customerFullName;
        this.referenceNumber = referenceNumber;
        this.hasPending = hasPending;
        this.photoUri = photoUri;
    }
}
