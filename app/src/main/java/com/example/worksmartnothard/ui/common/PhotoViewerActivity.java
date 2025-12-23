package com.example.worksmartnothard.ui.common;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.worksmartnothard.R;

import java.io.InputStream;

public class PhotoViewerActivity extends AppCompatActivity {

    public static final String EXTRA_PHOTO_URI = "photo_uri";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_viewer);

        ImageView imageView = findViewById(R.id.imagePhoto);

        String uriString = getIntent() != null ? getIntent().getStringExtra(EXTRA_PHOTO_URI) : null;
        if (TextUtils.isEmpty(uriString)) {
            Toast.makeText(this, "Δεν υπάρχει φωτογραφία", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Uri uri = Uri.parse(uriString);

        // Fast path for many content/file uris.
        try {
            imageView.setImageURI(uri);
        } catch (Exception ignored) {
        }

        // Fallback decode (helps some providers).
        if (imageView.getDrawable() == null) {
            try (InputStream in = getContentResolver().openInputStream(uri)) {
                if (in == null) {
                    Toast.makeText(this, "Αποτυχία φόρτωσης φωτογραφίας", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }
                Bitmap bitmap = BitmapFactory.decodeStream(in);
                if (bitmap != null) {
                    imageView.setImageBitmap(bitmap);
                } else {
                    Toast.makeText(this, "Αποτυχία φόρτωσης φωτογραφίας", Toast.LENGTH_SHORT).show();
                    finish();
                }
            } catch (Exception e) {
                Toast.makeText(this, "Αποτυχία φόρτωσης φωτογραφίας", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }
}
