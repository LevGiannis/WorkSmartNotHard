package com.example.worksmartnothard.ui.history;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.worksmartnothard.viewmodel.ProgressViewModel;
import com.example.worksmartnothard.R;
import com.example.worksmartnothard.ui.main.ProgressAdapter;

import java.util.Calendar;

public class MonthHistoryActivity extends AppCompatActivity {

    public static final String EXTRA_YEAR = "extra_year";
    public static final String EXTRA_MONTH = "extra_month";

    private ProgressViewModel viewModel;
    private ProgressAdapter adapter;
    private TextView titleText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_month_history);

        titleText = findViewById(R.id.textMonthTitle);
        RecyclerView recyclerView = findViewById(R.id.recyclerMonthHistory);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ProgressAdapter();
        recyclerView.setAdapter(adapter);

        viewModel = new ViewModelProvider(this).get(ProgressViewModel.class);
        viewModel.getProgressList().observe(this, adapter::setData);

        int year = getIntent().getIntExtra(EXTRA_YEAR, Calendar.getInstance().get(Calendar.YEAR));
        int month = getIntent().getIntExtra(EXTRA_MONTH, Calendar.getInstance().get(Calendar.MONTH) + 1);

        titleText.setText("Ιστορικό: " + String.format("%02d/%d", month, year));

        viewModel.loadProgressForMonth(year, month);
    }
}
