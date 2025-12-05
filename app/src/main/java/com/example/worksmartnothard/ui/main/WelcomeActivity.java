package com.example.worksmartnothard.ui.main;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;

import android.Manifest;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.os.Build;
import androidx.appcompat.app.AppCompatActivity;

import com.example.worksmartnothard.R;
import com.example.worksmartnothard.data.AppPreferences;

public class WelcomeActivity extends AppCompatActivity {

    private EditText etName, etSurname, etEmail, etNickname, etStoreCode;
    private Button btnContinue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Αν ολοκληρώθηκε το onboarding, πήγαινε Main
        if (AppPreferences.isOnboardingCompleted(this)) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_welcome);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        1001
                );
            }
        }

        etName = findViewById(R.id.etName);
        etSurname = findViewById(R.id.etSurname);
        etEmail = findViewById(R.id.etEmail);
        etNickname = findViewById(R.id.etNickname);
        etStoreCode = findViewById(R.id.etStoreCode);
        btnContinue = findViewById(R.id.btnContinue);

        btnContinue.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String surname = etSurname.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String nickname = etNickname.getText().toString().trim();
            String storeCode = etStoreCode.getText().toString().trim();

            if (TextUtils.isEmpty(name)) {
                etName.setError("Το όνομα είναι υποχρεωτικό");
                return;
            }

            if (TextUtils.isEmpty(surname)) {
                etSurname.setError("Το επώνυμο είναι υποχρεωτικό");
                return;
            }

            if (TextUtils.isEmpty(email)) {
                etEmail.setError("Το email είναι υποχρεωτικό");
                return;
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                etEmail.setError("Μη έγκυρο email");
                return;
            }

            if (TextUtils.isEmpty(nickname)) {
                etNickname.setError("Το ψευδώνυμο είναι υποχρεωτικό");
                return;
            }

            if (TextUtils.isEmpty(storeCode)) {
                etStoreCode.setError("Ο κωδικός καταστήματος είναι υποχρεωτικός");
                return;
            }

            // Αποθήκευση onboarding στοιχείων
            AppPreferences.setFirstName(this, name);
            AppPreferences.setLastName(this, surname);
            AppPreferences.setEmail(this, email);
            AppPreferences.setNickname(this, nickname);
            AppPreferences.setStoreCode(this, storeCode);

            // Default report email = προσωπικό email (αν δεν υπάρχει ήδη)
            if (TextUtils.isEmpty(AppPreferences.getReportEmail(this))) {
                AppPreferences.setReportEmail(this, email);
            }

            AppPreferences.setOnboardingCompleted(this, true);

            startActivity(new Intent(this, MainActivity.class));
            finish();
        });
    }
}
