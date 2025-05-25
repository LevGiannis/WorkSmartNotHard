package com.example.worksmartnothard.ui.history;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.worksmartnothard.R;
import com.example.worksmartnothard.data.AppDatabase;
import com.example.worksmartnothard.data.DailyEntry;

import java.util.List;

public class HistoryActivity extends AppCompatActivity {

    public static final String EXTRA_DATE = "selected_date";
    private AppDatabase db;
    private RecyclerView recyclerView;
    private HistoryAdapter adapter;
    private TextView textTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        textTitle = findViewById(R.id.textDateTitle);
        recyclerView = findViewById(R.id.recyclerHistory);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new HistoryAdapter();
        recyclerView.setAdapter(adapter);

        String date = getIntent().getStringExtra(EXTRA_DATE);
        if (date != null) {
            textTitle.setText("Καταχωρήσεις για: " + date);
            db = AppDatabase.getDatabase(getApplicationContext());

            new Thread(() -> {
                List<DailyEntry> entries = db.dailyEntryDao().getEntriesForDate(date);
                runOnUiThread(() -> adapter.setData(entries));
            }).start();
        }
    }
}
