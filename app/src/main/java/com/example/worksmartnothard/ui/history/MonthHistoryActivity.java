package com.example.worksmartnothard.ui.history;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.worksmartnothard.R;
import com.example.worksmartnothard.data.AppDatabase;
import com.example.worksmartnothard.data.DailyEntry;
import com.example.worksmartnothard.ui.main.ProgressAdapter;
import com.example.worksmartnothard.util.BonusCalculator;
import com.example.worksmartnothard.viewmodel.ProgressViewModel;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class MonthHistoryActivity extends AppCompatActivity {

    public static final String EXTRA_YEAR = "extra_year";
    public static final String EXTRA_MONTH = "extra_month";

    private ProgressViewModel viewModel;
    private ProgressAdapter adapter;
    private TextView titleText;
    private TextView monthBonusText;
    private AppDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_month_history);

        titleText = findViewById(R.id.textMonthTitle);
        monthBonusText = findViewById(R.id.textMonthBonus);

        RecyclerView recyclerView = findViewById(R.id.recyclerMonthHistory);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ProgressAdapter();
        recyclerView.setAdapter(adapter);

        db = AppDatabase.getDatabase(getApplicationContext());

        viewModel = new ViewModelProvider(this).get(ProgressViewModel.class);
        viewModel.getProgressList().observe(this, progressList -> {
            // μόνο για τα ποσοστά προόδου
            adapter.setData(progressList);
        });

        int year = getIntent().getIntExtra(EXTRA_YEAR, Calendar.getInstance().get(Calendar.YEAR));
        int month = getIntent().getIntExtra(EXTRA_MONTH, Calendar.getInstance().get(Calendar.MONTH) + 1);

        titleText.setText(String.format(Locale.getDefault(),
                "Ιστορικό: %02d/%d", month, year));

        // Φόρτωση στόχων/ποσοστών για τον μήνα
        viewModel.loadProgressForMonth(year, month);

        // Υπολογισμός BONUS από τις DailyEntry εγγραφές του μήνα
        String yearMonth = year + "-" + (month < 10 ? "0" + month : month);

        new Thread(() -> {
            List<DailyEntry> entries = db.dailyEntryDao().getEntriesForMonth(yearMonth);
            double totalBonus = BonusCalculator.computeBonusForMonth(entries);

            runOnUiThread(() -> {
                String bonusText = String.format(Locale.getDefault(),
                        "Bonus: %.2f€", totalBonus);
                monthBonusText.setText(bonusText);
            });
        }).start();
    }
}
