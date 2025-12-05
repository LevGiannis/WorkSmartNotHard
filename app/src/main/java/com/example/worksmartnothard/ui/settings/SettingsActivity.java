package com.example.worksmartnothard.ui.settings;

import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.worksmartnothard.R;
import com.example.worksmartnothard.data.AppPreferences;

public class SettingsActivity extends AppCompatActivity {

    private EditText editName, editSurname, editEmail, editNickname, editStore, editReportEmail;
    private Button buttonSave;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        editName = findViewById(R.id.editName);
        editSurname = findViewById(R.id.editSurname);
        editEmail = findViewById(R.id.editEmail);
        editNickname = findViewById(R.id.editNickname);
        editStore = findViewById(R.id.editStore);
        editReportEmail = findViewById(R.id.editReportEmail);
        buttonSave = findViewById(R.id.buttonSave);

        // Φόρτωση τιμών
        editName.setText(AppPreferences.getFirstName(this));
        editSurname.setText(AppPreferences.getLastName(this));
        editEmail.setText(AppPreferences.getEmail(this));
        editNickname.setText(AppPreferences.getNickname(this));
        editStore.setText(AppPreferences.getStoreCode(this));
        editReportEmail.setText(AppPreferences.getReportEmail(this));

        buttonSave.setOnClickListener(v -> {

            String firstName = editName.getText().toString().trim();
            String lastName = editSurname.getText().toString().trim();
            String email = editEmail.getText().toString().trim();
            String nickname = editNickname.getText().toString().trim();
            String store = editStore.getText().toString().trim();
            String reportEmail = editReportEmail.getText().toString().trim();

            // Προαιρετικό validation
            if (!email.isEmpty() && !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                editEmail.setError("Μη έγκυρο email");
                editEmail.requestFocus();
                return;
            }

            if (!reportEmail.isEmpty() && !Patterns.EMAIL_ADDRESS.matcher(reportEmail).matches()) {
                editReportEmail.setError("Μη έγκυρο email παραλήπτη");
                editReportEmail.requestFocus();
                return;
            }

            AppPreferences.setFirstName(this, firstName);
            AppPreferences.setLastName(this, lastName);
            AppPreferences.setEmail(this, email);
            AppPreferences.setNickname(this, nickname);
            AppPreferences.setStoreCode(this, store);
            AppPreferences.setReportEmail(this, reportEmail);

            Toast.makeText(this, "Οι αλλαγές αποθηκεύτηκαν!", Toast.LENGTH_SHORT).show();
            finish();
        });
    }
}
