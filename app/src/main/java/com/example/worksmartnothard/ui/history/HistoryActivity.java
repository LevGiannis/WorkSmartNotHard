package com.example.worksmartnothard.ui.history;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.worksmartnothard.R;
import com.example.worksmartnothard.data.AppDatabase;
import com.example.worksmartnothard.data.DailyEntry;
import com.example.worksmartnothard.util.BonusCalculator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HistoryActivity extends AppCompatActivity {

    public static final String EXTRA_DATE = "selected_date";

    private AppDatabase db;
    private RecyclerView recyclerView;
    private HistoryAdapter adapter;
    private TextView textTitle;
    private TextView textDaySummary;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        textTitle = findViewById(R.id.textDateTitle);
        textDaySummary = findViewById(R.id.textDaySummary);
        recyclerView = findViewById(R.id.recyclerHistory);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new HistoryAdapter();
        recyclerView.setAdapter(adapter);

        String date = getIntent().getStringExtra(EXTRA_DATE);
        if (date != null) {
            // Τίτλος με ημερομηνία
            textTitle.setText("Ημερομηνία: " + date);

            db = AppDatabase.getDatabase(getApplicationContext());

            new Thread(() -> {
                List<DailyEntry> entries = db.dailyEntryDao().getEntriesForDate(date);
                if (entries == null) {
                    entries = Collections.emptyList();
                }

                // 🔹 ΟΜΑΔΟΠΟΙΗΣΗ ΑΝΑ ΚΑΤΗΓΟΡΙΑ & ΑΘΡΟΙΣΜΑ ΠΟΣΟΤΗΤΑΣ
                // Χρησιμοποιούμε LinkedHashMap για να κρατήσουμε τη σειρά εισαγωγής
                Map<String, Double> sumByCategory = new LinkedHashMap<>();
                for (DailyEntry e : entries) {
                    double value = e.count;  // πεδίο count από DailyEntry
                    Double current = sumByCategory.get(e.category);
                    if (current == null) current = 0.0;
                    sumByCategory.put(e.category, current + value);
                }

                // 🔹 Δημιουργούμε "συγχωνευμένες" DailyEntry, μία ανά κατηγορία
                List<DailyEntry> aggregated = new ArrayList<>();
                for (Map.Entry<String, Double> entry : sumByCategory.entrySet()) {
                    String category = entry.getKey();
                    double totalCount = entry.getValue();

                    // homeSubtype δεν μας νοιάζει εδώ → null
                    aggregated.add(new DailyEntry(category, date, totalCount, null));
                }

                // 🔹 Σύνοψη ημέρας
                double totalQty = 0.0;
                for (Double v : sumByCategory.values()) {
                    totalQty += v;
                }

                // Bonus με βάση ΟΛΕΣ τις αρχικές καταχωρήσεις της μέρας
                double dailyBonus = BonusCalculator.calculateMonthlyBonus(entries);

                final String summaryText = String.format(
                        Locale.getDefault(),
                        "Κατηγορίες: %d  • Bonus: %.2f€",
                        aggregated.size(),
                        dailyBonus
                );

                runOnUiThread(() -> {
                    // Δείχνουμε στη λίστα ΜΟΝΟ τις aggregated εγγραφές
                    adapter.setData(aggregated);

                    if (textDaySummary != null) {
                        textDaySummary.setText(summaryText);
                    }
                });
            }).start();
        }
    }
}
