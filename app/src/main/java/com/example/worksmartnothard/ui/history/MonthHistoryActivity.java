package com.example.worksmartnothard.ui.history;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.worksmartnothard.R;
import com.example.worksmartnothard.data.AppDatabase;
import com.example.worksmartnothard.data.AppPreferences;
import com.example.worksmartnothard.data.DailyEntry;
import com.example.worksmartnothard.model.CategoryProgress;
import com.example.worksmartnothard.ui.main.ProgressAdapter;
import com.example.worksmartnothard.util.BonusCalculator;
import com.example.worksmartnothard.viewmodel.ProgressViewModel;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MonthHistoryActivity extends AppCompatActivity {

    public static final String EXTRA_YEAR = "extra_year";
    public static final String EXTRA_MONTH = "extra_month";

    private ProgressViewModel viewModel;
    private ProgressAdapter adapter;

    private TextView titleText;
    private TextView monthBonusText;
    private Button buttonExportExcel;

    private AppDatabase db;
    private List<CategoryProgress> currentProgressList = new ArrayList<>();

    private int selectedYear;
    private int selectedMonth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_month_history);

        db = AppDatabase.getDatabase(getApplicationContext());

        titleText = findViewById(R.id.textMonthTitle);
        monthBonusText = findViewById(R.id.textMonthBonus);
        buttonExportExcel = findViewById(R.id.buttonExportExcel);

        RecyclerView recyclerView = findViewById(R.id.recyclerMonthHistory);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ProgressAdapter();
        recyclerView.setAdapter(adapter);

        viewModel = new ViewModelProvider(this).get(ProgressViewModel.class);

        selectedYear = getIntent().getIntExtra(EXTRA_YEAR,
                Calendar.getInstance().get(Calendar.YEAR));
        selectedMonth = getIntent().getIntExtra(EXTRA_MONTH,
                Calendar.getInstance().get(Calendar.MONTH) + 1);

        titleText.setText(
                String.format(Locale.getDefault(), "Ιστορικό: %02d/%d", selectedMonth, selectedYear));

        viewModel.getProgressList().observe(this, progressList -> {
            currentProgressList = progressList;
            adapter.setData(progressList);
        });

        viewModel.loadProgressForMonth(selectedYear, selectedMonth);

        updateMonthBonus();

        buttonExportExcel.setOnClickListener(v -> exportMonthToCsv());
    }

    // Υπολογίζει και εμφανίζει το συνολικό bonus του μήνα
    private void updateMonthBonus() {
        new Thread(() -> {
            String yearMonth = String.format(Locale.getDefault(), "%04d-%02d",
                    selectedYear, selectedMonth);
            List<DailyEntry> entries = db.dailyEntryDao().getEntriesForMonth(yearMonth);

            double totalBonus = BonusCalculator.calculateMonthlyBonus(entries);

            runOnUiThread(() -> monthBonusText.setText(
                    String.format(Locale.getDefault(), "Money: %.2f€", totalBonus)));
        }).start();
    }

    // Ετοιμάζει το CSV για τον συγκεκριμένο μήνα και το περνάει για αποθήκευση.
    private void exportMonthToCsv() {
        if (currentProgressList == null || currentProgressList.isEmpty()) {
            Toast.makeText(this, "Δεν υπάρχουν δεδομένα για εξαγωγή", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            String yearMonth = String.format(Locale.getDefault(), "%04d-%02d",
                    selectedYear, selectedMonth);
            List<DailyEntry> entries = db.dailyEntryDao().getEntriesForMonth(yearMonth);

            double totalBonus = BonusCalculator.calculateMonthlyBonus(entries);

            // Ομαδοποίηση εγγραφών ανά κατηγορία (όπως κάνουμε και στα Tasks)
            Map<String, List<DailyEntry>> entriesByCategory = new HashMap<>();
            for (DailyEntry e : entries) {
                List<DailyEntry> list = entriesByCategory.get(e.category);
                if (list == null) {
                    list = new ArrayList<>();
                    entriesByCategory.put(e.category, list);
                }
                list.add(e);
            }

            // Φτιάχνουμε CSV
            StringBuilder sb = new StringBuilder();
            sb.append("ΑΠΟΤΕΛΕΣΜΑΤΑ ΜΗΝΑ;")
                    .append(String.format(Locale.getDefault(), "%02d/%04d",
                            selectedMonth, selectedYear))
                    .append("\n");
            sb.append(String.format(Locale.US,
                    "Συνολικό Money Μήνα;%.2f€\n\n", totalBonus));
            sb.append("Κατηγορία;Στόχος;Επίτευξη;Success %;Money (€)\n");

            for (CategoryProgress p : currentProgressList) {
                List<DailyEntry> catEntries = entriesByCategory.getOrDefault(p.category, new ArrayList<>());

                double categoryBonus = BonusCalculator.calculateMonthlyBonus(catEntries);
                int percent = p.getPercentage();

                sb.append(String.format(Locale.US,
                        "%s;%.0f;%.2f;%d%%;%.2f\n",
                        p.category, p.target, p.achieved, percent, categoryBonus));
            }

            sb.append("\n")
                    .append(String.format(Locale.US,
                            "ΣΥΝΟΛΙΚΟ MONEY ΜΗΝΑ;;;;%.2f\n", totalBonus));

            saveMonthCsvToDownloads(sb.toString());
        }).start();
    }

    // 🔹 ΙΔΙΑ λογική με saveTasksCsvToDownloads (TasksActivity)
    private void saveMonthCsvToDownloads(String csvContent) {

        ContentResolver resolver = getContentResolver();
        Uri collection;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            collection = MediaStore.Downloads.getContentUri(
                    MediaStore.VOLUME_EXTERNAL_PRIMARY);
        } else {
            collection = MediaStore.Files.getContentUri("external");
        }

        // Μοναδικό όνομα με timestamp (όπως στα Tasks)
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault())
                .format(new Date());
        String fileName = String.format(Locale.getDefault(),
                "results_%04d_%02d_%s.csv", selectedYear, selectedMonth, timestamp);

        ContentValues values = new ContentValues();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
            values.put(MediaStore.Downloads.MIME_TYPE, "text/csv");
            values.put(MediaStore.Downloads.IS_PENDING, 1);
        } else {
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
            values.put(MediaStore.MediaColumns.MIME_TYPE, "text/csv");
        }

        Uri item;
        try {
            item = resolver.insert(collection, values);
        } catch (Exception e) {
            runOnUiThread(() -> Toast.makeText(this,
                    "Σφάλμα δημιουργίας αρχείου στις Λήψεις",
                    Toast.LENGTH_SHORT).show());
            return;
        }

        if (item == null) {
            runOnUiThread(() -> Toast.makeText(this,
                    "Σφάλμα δημιουργίας αρχείου στις Λήψεις",
                    Toast.LENGTH_SHORT).show());
            return;
        }

        try (OutputStream out = resolver.openOutputStream(item)) {
            if (out == null) {
                runOnUiThread(() -> Toast.makeText(this,
                        "Σφάλμα ανοίγματος αρχείου",
                        Toast.LENGTH_SHORT).show());
                return;
            }
            // BOM UTF-8 για σωστά ελληνικά στο Excel
            out.write(new byte[] { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF });
            out.write(csvContent.getBytes(StandardCharsets.UTF_8));
            out.flush();
        } catch (IOException e) {
            runOnUiThread(() -> Toast.makeText(this,
                    "Σφάλμα κατά το export",
                    Toast.LENGTH_SHORT).show());
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentValues done = new ContentValues();
            done.put(MediaStore.Downloads.IS_PENDING, 0);
            try {
                resolver.update(item, done, null, null);
            } catch (Exception e) {
                // Αν κάτι πάει στραβά εδώ, απλά συνεχίζουμε – το αρχείο ήδη υπάρχει
            }
        }

        Uri finalItem = item;
        runOnUiThread(() -> {
            Toast.makeText(this,
                    "Το αρχείο αποθηκεύτηκε στις Λήψεις:\n" + fileName,
                    Toast.LENGTH_LONG).show();
            sendEmailWithAttachment(finalItem, fileName);
        });
    }

    // Ανοίγει email με το αρχείο ως συνημμένο (ίδιο pattern με TasksActivity)
    private void sendEmailWithAttachment(Uri fileUri, String filename) {

        String savedEmail = AppPreferences.getEffectiveReportEmail(this);

        Intent emailIntent = new Intent(Intent.ACTION_SEND);
        emailIntent.setType("text/csv");

        if (savedEmail != null && !savedEmail.trim().isEmpty()) {
            emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[] { savedEmail });
        }

        emailIntent.putExtra(Intent.EXTRA_SUBJECT,
                "Μηνιαίο Report " + String.format(Locale.getDefault(),
                        "%02d/%04d", selectedMonth, selectedYear));
        emailIntent.putExtra(Intent.EXTRA_TEXT,
                "Σας επισυνάπτω το μηνιαίο report.");
        emailIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
        emailIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        try {
            startActivity(Intent.createChooser(emailIntent, "Αποστολή Report"));
        } catch (android.content.ActivityNotFoundException e) {
            Toast.makeText(this,
                    "Δεν βρέθηκε εφαρμογή email για αποστολή.",
                    Toast.LENGTH_LONG).show();
        }
    }
}
