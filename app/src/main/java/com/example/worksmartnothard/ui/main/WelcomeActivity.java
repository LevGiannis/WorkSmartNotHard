package com.example.worksmartnothard.ui.main;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import com.example.worksmartnothard.R;
import com.example.worksmartnothard.data.AppPreferences;
import com.example.worksmartnothard.ui.main.MainActivity;

public class WelcomeActivity extends AppCompatActivity {

    private EditText etName, etSurname, etEmail, etNickname, etStoreCode;
    private Button btnContinue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ✅ Αν ο χρήστης έχει ήδη ολοκληρώσει την εγγραφή, πήγαινε κατευθείαν στη MainActivity
        if (AppPreferences.isOnboardingCompleted(this)) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_welcome);

        etName = findViewById(R.id.etName);
        etSurname = findViewById(R.id.etSurname);
        etEmail = findViewById(R.id.etEmail);
        etNickname = findViewById(R.id.etNickname);
        etStoreCode = findViewById(R.id.etStoreCode);
        btnContinue = findViewById(R.id.btnContinue);

        btnContinue.setOnClickListener(v -> {
            String nickname = etNickname.getText().toString().trim();
            String storeCode = etStoreCode.getText().toString().trim();

            if (TextUtils.isEmpty(nickname)) {
                etNickname.setError("Το ψευδώνυμο είναι υποχρεωτικό");
                return;
            }
            if (TextUtils.isEmpty(storeCode)) {
                etStoreCode.setError("Ο κωδικός καταστήματος είναι υποχρεωτικός");
                return;
            }

            // Αποθήκευση στοιχείων
            AppPreferences.saveUserInfo(this, nickname, storeCode);
            // Δηλώνουμε ότι ολοκληρώθηκε το onboarding
            AppPreferences.setOnboardingCompleted(this, true);

            // Μετάβαση στη MainActivity
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });
    }
}
