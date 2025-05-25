package com.example.worksmartnothard;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

public class AddGoalActivity extends AppCompatActivity {

    private Spinner spinnerCategory;
    private EditText editTarget;
    private Button buttonSave;
    private AppDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_goal);

        spinnerCategory = findViewById(R.id.spinnerGoalCategory);
        editTarget = findViewById(R.id.editTarget);
        buttonSave = findViewById(R.id.buttonSaveGoal);

        List<String> categories = Arrays.asList(
                "PortIn Mobile",
                "Vodafone Home",
                "Fisrt",
                "Exprepay",
                "Ec2Post-Post2Post",
                "TV",
                "Migrations Vdsl",
                "Ραντεβού"
        );

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categories);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);

        db = AppDatabase.getDatabase(getApplicationContext());

        buttonSave.setOnClickListener(v -> {
            String category = spinnerCategory.getSelectedItem().toString();
            String targetStr = editTarget.getText().toString().trim();

            if (targetStr.isEmpty()) {
                Toast.makeText(this, "Βάλε στόχο", Toast.LENGTH_SHORT).show();
                return;
            }

            double target = Double.parseDouble(targetStr);

            LocalDate now = LocalDate.now();
            int year = now.getYear();
            int month = now.getMonthValue();

            new Thread(() -> {
                db.goalDao().insertGoal(new Goal(category, year, month, target));
                runOnUiThread(() -> {
                    Toast.makeText(this, "Ο στόχος αποθηκεύτηκε!", Toast.LENGTH_SHORT).show();
                    finish();
                });
            }).start();
        });
    }
}
