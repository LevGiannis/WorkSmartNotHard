package com.example.worksmartnothard;

import android.os.Bundle;
import android.text.InputType;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

public class AddEntryActivity extends AppCompatActivity {

    private Spinner categorySpinner;
    private EditText countEditText;
    private Button saveButton;
    private AppDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_entry);

        categorySpinner = findViewById(R.id.spinnerCategory);
        countEditText = findViewById(R.id.editCount);
        saveButton = findViewById(R.id.buttonSave);

        List<String> categories = Arrays.asList(
                "PortIn Mobile", "Vodafone Home", "Fisrt", "Exprepay",
                "Ec2Post-Post2Post", "TV", "Migrations Vdsl", "Ραντεβού"
        );

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categories);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(adapter);

        db = AppDatabase.getDatabase(getApplicationContext());

        // Αλλάζουμε hint ανάλογα με την κατηγορία
        categorySpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view, int position, long id) {
                String selected = categorySpinner.getSelectedItem().toString();
                if (selected.equals("Ραντεβού")) {
                    countEditText.setHint("Ποσό σε €");
                    countEditText.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
                } else {
                    countEditText.setHint("Ποσότητα σε τεμ.");
                    countEditText.setInputType(InputType.TYPE_CLASS_NUMBER);
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

            double count = Double.parseDouble(countText); // υποστηρίζει δεκαδικά

            String today = LocalDate.now().toString(); // YYYY-MM-DD

            new Thread(() -> {
                db.dailyEntryDao().insertDailyEntry(new DailyEntry(category, today, count));
                runOnUiThread(() -> {
                    Toast.makeText(this, "Η καταχώριση αποθηκεύτηκε!", Toast.LENGTH_SHORT).show();
                    finish();
                });
            }).start();
        });
    }
}
