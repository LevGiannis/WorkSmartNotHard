package com.example.worksmartnothard.ui.entry;

import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import android.net.Uri;
import android.content.Intent;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.worksmartnothard.R;
import com.example.worksmartnothard.data.AppDatabase;
import com.example.worksmartnothard.data.Category;
import com.example.worksmartnothard.data.DailyEntry;
import com.example.worksmartnothard.ui.common.PhotoAttachmentHelper;
import com.example.worksmartnothard.ui.common.PhotoViewerActivity;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class AddEntryActivity extends AppCompatActivity {

    private Spinner categorySpinner;
    private Spinner homeSubtypeSpinner;
    private TextView homeSubtypeLabel;
    private EditText countEditText;
    private EditText orderNumberEditText;
    private EditText customerFullNameEditText;
    private EditText referenceNumberEditText;
    private CheckBox hasPendingCheckBox;
    private Button attachPhotoButton;
    private Button removePhotoButton;
    private TextView photoStatusText;
    private ImageView photoPreviewImage;
    private Button saveButton;
    private AppDatabase db;

    private PhotoAttachmentHelper photoHelper;
    private Uri selectedPhotoUri;

    private static final String CATEGORY_VODAFONE_HOME = "Vodafone Home W/F";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_entry);

        categorySpinner = findViewById(R.id.spinnerCategory);
        homeSubtypeSpinner = findViewById(R.id.spinnerHomeSubtype);
        homeSubtypeLabel = findViewById(R.id.textHomeSubtypeLabel);
        countEditText = findViewById(R.id.editCount);
        orderNumberEditText = findViewById(R.id.editOrderNumber);
        customerFullNameEditText = findViewById(R.id.editCustomerFullName);
        referenceNumberEditText = findViewById(R.id.editReferenceNumber);
        hasPendingCheckBox = findViewById(R.id.checkHasPending);
        attachPhotoButton = findViewById(R.id.buttonAttachPhoto);
        removePhotoButton = findViewById(R.id.buttonRemovePhoto);
        photoStatusText = findViewById(R.id.textPhotoStatus);
        photoPreviewImage = findViewById(R.id.imagePhotoPreview);
        saveButton = findViewById(R.id.buttonSave);

        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                new ArrayList<>());
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(categoryAdapter);

        ArrayAdapter<String> subtypeAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                getResources().getStringArray(R.array.vodafone_home_types));
        subtypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        homeSubtypeSpinner.setAdapter(subtypeAdapter);

        db = AppDatabase.getDatabase(getApplicationContext());

        new Thread(() -> {
            try {
                if (db.categoryDao().count() == 0) {
                    String[] defaults = getResources().getStringArray(R.array.categories);
                    for (String name : defaults) {
                        if (name == null)
                            continue;
                        String trimmed = name.trim();
                        if (trimmed.isEmpty())
                            continue;
                        db.categoryDao().insertCategory(new Category(trimmed));
                    }
                }
            } catch (Exception ignored) {
            }

            List<String> categories = db.categoryDao().getAllNames();
            if (categories == null || categories.isEmpty()) {
                String[] defaults = getResources().getStringArray(R.array.categories);
                categories = new ArrayList<>();
                for (String c : defaults)
                    categories.add(c);
            }

            List<String> finalCategories = categories;
            runOnUiThread(() -> {
                categoryAdapter.clear();
                categoryAdapter.addAll(finalCategories);
                categoryAdapter.notifyDataSetChanged();
            });
        }).start();

        photoHelper = new PhotoAttachmentHelper(this);
        attachPhotoButton.setOnClickListener(v -> photoHelper.showChooser(uri -> {
            selectedPhotoUri = uri;
            updatePhotoPreview();
        }, false));

        removePhotoButton.setOnClickListener(v -> {
            selectedPhotoUri = null;
            updatePhotoPreview();
        });

        photoPreviewImage.setOnClickListener(v -> {
            if (selectedPhotoUri == null)
                return;
            Intent i = new Intent(this, PhotoViewerActivity.class);
            i.putExtra(PhotoViewerActivity.EXTRA_PHOTO_URI, selectedPhotoUri.toString());
            startActivity(i);
        });

        updatePhotoPreview();

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
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });

        saveButton.setOnClickListener(v -> {
            String category = categorySpinner.getSelectedItem().toString();
            String countText = countEditText.getText().toString().trim();

            String orderNumber = orderNumberEditText.getText().toString().trim();
            String customerFullName = customerFullNameEditText.getText().toString().trim();
            String referenceNumber = referenceNumberEditText.getText().toString().trim();
            boolean hasPending = hasPendingCheckBox.isChecked();

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
            String photoUri = selectedPhotoUri == null ? null : selectedPhotoUri.toString();

            String finalHomeType = homeType;
            new Thread(() -> {
                db.dailyEntryDao().insertDailyEntry(
                        new DailyEntry(
                                category,
                                today,
                                count,
                                finalHomeType,
                                orderNumber.isEmpty() ? null : orderNumber,
                                customerFullName.isEmpty() ? null : customerFullName,
                                referenceNumber.isEmpty() ? null : referenceNumber,
                                hasPending,
                                photoUri));
                runOnUiThread(() -> {
                    Toast.makeText(this, "Η καταχώριση αποθηκεύτηκε!", Toast.LENGTH_SHORT).show();
                    finish();
                });
            }).start();
        });
    }

    private void updatePhotoPreview() {
        if (selectedPhotoUri == null) {
            photoStatusText.setText("Καμία φωτογραφία");
            photoPreviewImage.setVisibility(View.GONE);
            photoPreviewImage.setImageDrawable(null);
            attachPhotoButton.setText("Επισύναψη φωτογραφίας");
            removePhotoButton.setVisibility(View.GONE);
            return;
        }

        photoStatusText.setText("Φωτογραφία: ΟΚ (πάτησε για προβολή)");
        photoPreviewImage.setVisibility(View.VISIBLE);
        attachPhotoButton.setText("Αλλαγή φωτογραφίας");
        removePhotoButton.setVisibility(View.VISIBLE);
        try {
            photoPreviewImage.setImageURI(selectedPhotoUri);
        } catch (Exception ignored) {
        }
    }
}
