
package com.example.worksmartnothard.ui.common;
import android.view.View;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.View;

import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class PhotoAttachmentHelper {

    public interface Callback {
        void onPhotoUriChanged(@Nullable Uri uri);
    }

    private final ComponentActivity activity;

    @Nullable
    private Callback pendingCallback;

    @Nullable
    private Uri pendingCameraUri;

    private final ActivityResultLauncher<Uri> takePictureLauncher;
    private final ActivityResultLauncher<String[]> openDocumentLauncher;

    public PhotoAttachmentHelper(@NonNull ComponentActivity activity) {
        this.activity = activity;

        takePictureLauncher = activity.registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                resultOk -> {
                    if (pendingCallback == null) {
                        return;
                    }
                    if (resultOk && pendingCameraUri != null) {
                        pendingCallback.onPhotoUriChanged(pendingCameraUri);
                    }
                    pendingCallback = null;
                    pendingCameraUri = null;
                });

        openDocumentLauncher = activity.registerForActivityResult(
                new ActivityResultContracts.OpenDocument(),
                uri -> {
                    if (pendingCallback == null) {
                        return;
                    }
                    if (uri != null) {
                        takePersistableReadPermission(activity, uri);
                        pendingCallback.onPhotoUriChanged(uri);
                    }
                    pendingCallback = null;
                });
    }

    public void showChooser(@NonNull Callback callback, boolean allowRemove) {
        this.pendingCallback = callback;

        View dialogView = activity.getLayoutInflater().inflate(
                com.example.worksmartnothard.R.layout.dialog_photo_source, null);

        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        dialogView.findViewById(com.example.worksmartnothard.R.id.buttonCamera).setOnClickListener(v -> {
            dialog.dismiss();
            Uri cameraUri = createTempImageUri(activity);
            if (cameraUri == null) {
                pendingCallback = null;
                return;
            }
            pendingCameraUri = cameraUri;
            takePictureLauncher.launch(cameraUri);
        });

        dialogView.findViewById(com.example.worksmartnothard.R.id.buttonGallery).setOnClickListener(v -> {
            dialog.dismiss();
            openDocumentLauncher.launch(new String[] { "image/*" });
        });

        if (allowRemove) {
            // Προσθέτουμε κουμπί "Αφαίρεση" στο κάτω μέρος
            com.google.android.material.button.MaterialButton removeBtn = new com.google.android.material.button.MaterialButton(activity, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
            removeBtn.setText("Αφαίρεση");
            removeBtn.setIconResource(android.R.drawable.ic_menu_close_clear_cancel);
            removeBtn.setIconTintResource(com.example.worksmartnothard.R.color.accent_blue_dark);
            removeBtn.setTextColor(activity.getColor(com.example.worksmartnothard.R.color.accent_blue_dark));
            removeBtn.setStrokeColorResource(com.example.worksmartnothard.R.color.accent_blue_dark);
            removeBtn.setLayoutParams(new android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT));
            ((android.widget.LinearLayout) dialogView).addView(removeBtn);
            removeBtn.setOnClickListener(v -> {
                dialog.dismiss();
                if (pendingCallback != null) {
                    pendingCallback.onPhotoUriChanged(null);
                }
                pendingCallback = null;
                pendingCameraUri = null;
            });
        }

        dialog.setOnCancelListener(d -> {
            pendingCallback = null;
            pendingCameraUri = null;
        });
        dialog.show();
    }

    private static void takePersistableReadPermission(Context context, Uri uri) {
        try {
            context.getContentResolver().takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (SecurityException ignored) {
            // Some providers don't allow persistable permissions; ignore.
        }
    }

    @Nullable
    private static Uri createTempImageUri(Context context) {
        try {
            File dir = new File(context.getCacheDir(), "images");
            // noinspection ResultOfMethodCallIgnored
            dir.mkdirs();

            String stamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            File file = new File(dir, "photo_" + stamp + ".jpg");

            return FileProvider.getUriForFile(
                    context,
                    context.getPackageName() + ".fileprovider",
                    file);
        } catch (Exception e) {
            return null;
        }
    }
}
