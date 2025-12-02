package com.example.worksmartnothard.ui.entry;

import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.worksmartnothard.R;
import com.example.worksmartnothard.data.AppDatabase;
import com.example.worksmartnothard.data.DailyEntry;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

public class AddEntryActivity extends AppCompatActivity {

    private Spinner categorySpinner;
    private Spinner homeSubtypeSpinner;
    private TextView homeSubtypeLabel;
    private EditText countEditText;
    private Button saveButton;
    private AppDatabase db;

    private static final String CATEGORY_VODAFONE_HOME = "Vodafone Home W/F";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_entry);

        categorySpinner = findViewById(R.id.spinnerCategory);
        homeSubtypeSpinner = findViewById(R.id.spinnerHomeSubtype);
        homeSubtypeLabel = findViewById(R.id.textHomeSubtypeLabel);
        countEditText = findViewById(R.id.editCount);
        saveButton = findViewById(R.id.buttonSave);

        // Κύριες κατηγορίες
        List<String> categories = Arrays.asList(
                "PortIN mobile",
                "Exprepay",
                CATEGORY_VODAFONE_HOME,
                "Migration FTTH",
                "Post2post",
                "Ec2post",
                "First",
                "New Connection",
                "Ραντεβού",
                "Συσκευές",
                "TV",
                "Migration VDSL"
        );

        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                categories
        );
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(categoryAdapter);

        // Υποκατηγορίες Vodafone Home W/F
        List<String> homeSubtypes = Arrays.asList(
                "ADSL 24",
                "ADSL 24 TRIPLE",
                "VDSL",
                "VDSL TRIPLE",
                "DOUBLE 300/500/1000",
                "TRIPLE 300/500/1000",
                "FWA"
        );

        ArrayAdapter<String> subtypeAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                homeSubtypes
        );
        subtypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        homeSubtypeSpinner.setAdapter(subtypeAdapter);

        db = AppDatabase.getDatabase(getApplicationContext());

        // Αλλάζουμε hint / υποκατηγορία ανάλογα με κατηγορία
        categorySpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                String selected = categorySpinner.getSelectedItem().toString();

                if ("Ραντεβού".equals(selected)) {
                    countEditText.setHint("Ποσό σε €");
                    countEditText.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
                } else {
                    countEditText.setHint("Ποσότητα σε τεμ.");
                    countEditText.setInputType(InputType.TYPE_CLASS_NUMBER);
                }

                if (CATEGORY_VODAFONE_HOME.equals(selected)) {
                    // Δείξε spinner υποκατηγορίας
                    homeSubtypeLabel.setVisibility(View.VISIBLE);
                    homeSubtypeSpinner.setVisibility(View.VISIBLE);
                } else {
                    // Κρύψε το
                    homeSubtypeLabel.setVisibility(View.GONE);
                    homeSubtypeSpinner.setVisibility(View.GONE);
                }
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        saveButton.setOnClickListener(v -> {
            String category = categorySpinner.getSelectedItem().toString();
            String countText = countEditText.getText().toString().trim();

            if (countText.isEmpty()) {
                Toast.makeText(this, "Συμπλήρωσε την τιμή", Toast.LENGTH_SHORT).show();
                return;
            }

            // Αν είναι Vodafone Home W/F πρέπει ΥΠΟΧΡΕΩΤΙΚΑ να έχει subtype
            String homeType = null;
            if (CATEGORY_VODAFONE_HOME.equals(category)) {
                if (homeSubtypeSpinner.getSelectedItem() == null) {
                    Toast.makeText(this, "Επίλεξε τύπο Vodafone Home W/F", Toast.LENGTH_SHORT).show();
                    return;
                }
                homeType = homeSubtypeSpinner.getSelectedItem().toString();
            }

            double count = Double.parseDouble(countText); // υποστηρίζει δεκαδικά

            String today = LocalDate.now().toString(); // YYYY-MM-DD

            String finalHomeType = homeType;
            new Thread(() -> {
                db.dailyEntryDao().insertDailyEntry(
                        new DailyEntry(category, today, count, finalHomeType)
                );
                runOnUiThread(() -> {
                    Toast.makeText(this, "Η καταχώριση αποθηκεύτηκε!", Toast.LENGTH_SHORT).show();
                    finish();
                });
            }).start();
        });
    }
}
