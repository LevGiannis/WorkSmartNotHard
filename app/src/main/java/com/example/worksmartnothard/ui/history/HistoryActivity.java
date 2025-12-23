package com.example.worksmartnothard.ui.history;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.worksmartnothard.R;
import com.example.worksmartnothard.data.AppDatabase;
import com.example.worksmartnothard.data.DailyEntry;
import com.example.worksmartnothard.ui.common.PhotoAttachmentHelper;
import com.example.worksmartnothard.ui.common.PhotoViewerActivity;
import com.example.worksmartnothard.util.BonusCalculator;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class HistoryActivity extends AppCompatActivity {

    public static final String EXTRA_DATE = "selected_date";

    private AppDatabase db;
    private RecyclerView recyclerView;
    private HistoryAdapter adapter;
    private TextView textTitle;
    private TextView textDaySummary;
    private EditText editHistorySearch;

    private PhotoAttachmentHelper photoHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        db = AppDatabase.getDatabase(getApplicationContext());

        textTitle = findViewById(R.id.textDateTitle);
        textDaySummary = findViewById(R.id.textDaySummary);
        editHistorySearch = findViewById(R.id.editHistorySearch);
        recyclerView = findViewById(R.id.recyclerHistory);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        photoHelper = new PhotoAttachmentHelper(this);

        adapter = new HistoryAdapter(db, this::showEditEntryDialog);
        recyclerView.setAdapter(adapter);

        if (editHistorySearch != null) {
            editHistorySearch.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    adapter.setQuery(s == null ? "" : s.toString());
                }

                @Override
                public void afterTextChanged(Editable s) {
                }
            });
        }

        String date = getIntent().getStringExtra(EXTRA_DATE);
        if (date != null) {
            // Τίτλος με ημερομηνία
            textTitle.setText("Ημερομηνία: " + date);

            new Thread(() -> {
                List<DailyEntry> entries = db.dailyEntryDao().getEntriesForDate(date);
                if (entries == null) {
                    entries = Collections.emptyList();
                }

                final List<DailyEntry> entriesFinal = entries;

                // Bonus με βάση ΟΛΕΣ τις αρχικές καταχωρήσεις της μέρας
                double dailyBonus = BonusCalculator.calculateDailyBonus(entries);

                final String summaryText = String.format(
                        Locale.getDefault(),
                        "Καταχωρήσεις: %d  • Money: %.2f€",
                        entriesFinal.size(),
                        dailyBonus);

                runOnUiThread(() -> {
                    // Δείχνουμε στη λίστα ΚΑΘΕ καταχώρηση ως ξεχωριστό αντικείμενο
                    adapter.setData(entriesFinal);

                    if (textDaySummary != null) {
                        textDaySummary.setText(summaryText);
                    }
                });
            }).start();
        }
    }

    private void showEditEntryDialog(DailyEntry entry) {
        if (entry == null) {
            return;
        }

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_entry_details, null);

        EditText editOrderNumber = dialogView.findViewById(R.id.editOrderNumber);
        EditText editCustomerFullName = dialogView.findViewById(R.id.editCustomerFullName);
        EditText editReferenceNumber = dialogView.findViewById(R.id.editReferenceNumber);
        CheckBox checkHasPending = dialogView.findViewById(R.id.checkHasPending);

        Button buttonAttachPhoto = dialogView.findViewById(R.id.buttonAttachPhoto);
        Button buttonRemovePhoto = dialogView.findViewById(R.id.buttonRemovePhoto);
        TextView textPhotoStatus = dialogView.findViewById(R.id.textPhotoStatus);
        ImageView imagePhotoPreview = dialogView.findViewById(R.id.imagePhotoPreview);

        editOrderNumber.setText(entry.orderNumber == null ? "" : entry.orderNumber);
        editCustomerFullName.setText(entry.customerFullName == null ? "" : entry.customerFullName);
        editReferenceNumber.setText(entry.referenceNumber == null ? "" : entry.referenceNumber);
        checkHasPending.setChecked(entry.hasPending);

        final String[] photoUriHolder = new String[] { entry.photoUri };
        updatePhotoPreview(textPhotoStatus, imagePhotoPreview, buttonAttachPhoto, buttonRemovePhoto, photoUriHolder[0]);

        buttonAttachPhoto.setOnClickListener(v -> photoHelper.showChooser(uri -> {
            photoUriHolder[0] = uri == null ? null : uri.toString();
            updatePhotoPreview(textPhotoStatus, imagePhotoPreview, buttonAttachPhoto, buttonRemovePhoto,
                    photoUriHolder[0]);
        }, false));

        buttonRemovePhoto.setOnClickListener(v -> {
            photoUriHolder[0] = null;
            updatePhotoPreview(textPhotoStatus, imagePhotoPreview, buttonAttachPhoto, buttonRemovePhoto, null);
        });

        imagePhotoPreview.setOnClickListener(v -> {
            if (TextUtils.isEmpty(photoUriHolder[0]))
                return;
            Intent i = new Intent(this, PhotoViewerActivity.class);
            i.putExtra(PhotoViewerActivity.EXTRA_PHOTO_URI, photoUriHolder[0]);
            startActivity(i);
        });

        new AlertDialog.Builder(this)
                .setTitle("Επεξεργασία Καταχώρησης")
                .setView(dialogView)
                .setPositiveButton("Αποθήκευση", (d, which) -> {
                    String orderNumber = editOrderNumber.getText().toString().trim();
                    String customerFullName = editCustomerFullName.getText().toString().trim();
                    String referenceNumber = editReferenceNumber.getText().toString().trim();

                    entry.orderNumber = orderNumber.isEmpty() ? null : orderNumber;
                    entry.customerFullName = customerFullName.isEmpty() ? null : customerFullName;
                    entry.referenceNumber = referenceNumber.isEmpty() ? null : referenceNumber;
                    entry.hasPending = checkHasPending.isChecked();
                    entry.photoUri = TextUtils.isEmpty(photoUriHolder[0]) ? null : photoUriHolder[0];

                    new Thread(() -> {
                        db.dailyEntryDao().updateDailyEntry(entry);
                        runOnUiThread(() -> {
                            adapter.notifyDataSetChanged();
                            Toast.makeText(this, "Αποθηκεύτηκε", Toast.LENGTH_SHORT).show();
                        });
                    }).start();
                })
                .setNegativeButton("Άκυρο", null)
                .show();
    }

    private void updatePhotoPreview(TextView status, ImageView preview, Button attach, Button remove, String photoUri) {
        if (TextUtils.isEmpty(photoUri)) {
            status.setText("Καμία φωτογραφία");
            preview.setVisibility(View.GONE);
            preview.setImageDrawable(null);
            if (attach != null)
                attach.setText("Επισύναψη φωτογραφίας");
            if (remove != null)
                remove.setVisibility(View.GONE);
            return;
        }

        status.setText("Φωτογραφία: ΟΚ (πάτησε για προβολή)");
        preview.setVisibility(View.VISIBLE);
        if (attach != null)
            attach.setText("Αλλαγή φωτογραφίας");
        if (remove != null)
            remove.setVisibility(View.VISIBLE);
        try {
            preview.setImageURI(Uri.parse(photoUri));
        } catch (Exception ignored) {
        }
    }
}
