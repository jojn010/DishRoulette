package com.example.dishroulette;

import android.content.ActivityNotFoundException;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputFilter;
import android.view.View;
import android.widget.*;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.*;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Owns the camera/picker and review flow; rotation never resends a paid request automatically.
 */
final class PhotoIngredientsController {
    interface Analyzer {
        okhttp3.Call analyze(String base64, PhotoIngredientApi.Callback callback);
    }

    Analyzer analyzer;
    private final MainActivity activity;
    private final ActivityResultLauncher<Uri> camera;
    private final ActivityResultLauncher<PickVisualMediaRequest> picker;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private File capture, photo;
    private Uri preparing;
    private AlertDialog dialog;
    private EditText review;
    private String reviewText;
    private String stage = "";
    private okhttp3.Call request;
    private boolean disposed;
    private int generation;

    PhotoIngredientsController(MainActivity activity, Bundle state) {
        this.activity = activity;
        analyzer = (image, callback) -> PhotoIngredientApi.analyze(activity, image, callback);
        camera = activity.registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
            if (success && capture != null)
                acceptPhoto(Uri.fromFile(capture));
            else {
                clear();
            }
        });
        picker = activity.registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
            if (uri != null)
                acceptPhoto(uri);
            else
                clear();
        });
        if (state != null) {
            capture = restoredFile(state.getString("photo_capture"));
            photo = restoredFile(state.getString("photo_ready"));
            reviewText = state.getString("photo_review");
            stage = state.getString("photo_stage", "");
            String source = state.getString("photo_source");
            if (source != null)
                preparing = Uri.parse(source);
        }
    }

    void restore() {
        if ("review".equals(stage) && reviewText != null)
            showReview(reviewText);
        else if (photo != null)
            showPreview("analyzing".equals(stage) ? R.string.photo_interrupted : 0);
        else if (preparing != null)
            acceptPhoto(preparing);
            // Android may evict cache files while the Activity is in the background.
            // Preserve outstanding external capture/picker results, but reset unusable previews.
        else if (!"camera".equals(stage) && !"picker".equals(stage))
            clear();
    }

    void open() {
        if (!stage.isEmpty()) {
            restore();
            if (!stage.isEmpty())
                return;
        }
        dialog = new MaterialAlertDialogBuilder(activity).setTitle(R.string.photo_title)
            .setItems(new String[]{activity.getString(R.string.photo_take), activity.getString(R.string.photo_choose)},
                (d, which) -> {
                    if (which == 0)
                        takePhoto();
                    else {
                        stage = "picker";
                        try {
                            picker.launch(new PickVisualMediaRequest.Builder().setMediaType(
                                ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE).build());
                        } catch (ActivityNotFoundException e) {
                            clear();
                            message(R.string.photo_no_picker);
                        }
                    }
                }).setNegativeButton(R.string.cancel, null).show();
        AdaptiveDialogs.fit(activity, dialog);
    }

    private void takePhoto() {
        try {
            File directory = directory();
            capture = File.createTempFile("camera-", ".jpg", directory);
            Uri uri = FileProvider.getUriForFile(activity, activity.getPackageName() + ".ingredientphotos", capture);
            stage = "camera";
            camera.launch(uri);
        } catch (ActivityNotFoundException e) {
            clear();
            message(R.string.photo_no_camera);
        } catch (IOException | IllegalArgumentException | SecurityException e) {
            clear();
            message(R.string.photo_read_error);
        }
    }

    void acceptPhoto(Uri uri) {
        preparing = uri;
        stage = "preparing";
        final int token = ++generation;
        showProgress(R.string.photo_preparing);
        worker.execute(() -> {
            File prepared = null;
            try {
                prepared = IngredientPhoto.prepare(activity.getApplicationContext(), uri);
                final File ready = prepared;
                activity.runOnUiThread(() -> {
                    if (disposed || token != generation) {
                        ready.delete();
                        return;
                    }
                    photo = ready;
                    preparing = null;
                    if (capture != null) {
                        capture.delete();
                        capture = null;
                    }
                    showPreview(0);
                });
            } catch (IOException | RuntimeException | OutOfMemoryError e) {
                if (prepared != null)
                    prepared.delete();
                activity.runOnUiThread(() -> {
                    if (!disposed && token == generation) {
                        clear();
                        message(R.string.photo_read_error);
                    }
                });
            }
        });
    }

    private void showPreview(int error) {
        dismiss();
        stage = "preview";
        LinearLayout content = column();
        ImageView image = new ImageView(activity);
        image.setAdjustViewBounds(true);
        image.setMaxHeight(dp(240));
        image.setScaleType(ImageView.ScaleType.FIT_CENTER);
        image.setImageBitmap(photo == null ? null : BitmapFactory.decodeFile(photo.getAbsolutePath()));
        image.setContentDescription(activity.getString(R.string.photo_preview));
        content.addView(image, new LinearLayout.LayoutParams(-1, -2));
        label(content, R.string.photo_send_detail);
        if (error != 0)
            label(content, error);
        dialog = new MaterialAlertDialogBuilder(activity).setTitle(R.string.photo_preview).setView(scroll(content))
            .setPositiveButton(R.string.photo_analyze, null).setNegativeButton(R.string.cancel, (d, w) -> clear())
            .setNeutralButton(R.string.photo_change, (d, w) -> {
                clear();
                open();
            }).create();
        dialog.setOnCancelListener(d -> clear());
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> analyze()));
        dialog.show();
        AdaptiveDialogs.fit(activity, dialog);
    }

    private void analyze() {
        if (photo == null || !photo.isFile()) {
            clear();
            message(R.string.photo_read_error);
            return;
        }
        if (!new GeminiCredentials(activity).configured()) {
            // Keep the prepared image for retry after the connection is configured.
            dismiss();
            activity.showGeminiSettings();
            return;
        }
        stage = "analyzing";
        final int token = ++generation;
        final File selected = photo;
        showProgress(R.string.photo_analyzing);
        worker.execute(() -> {
            try {
                byte[] bytes;
                try (InputStream in = new FileInputStream(
                    selected); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                    byte[] buffer = new byte[8192];
                    int size;
                    while ((size = in.read(buffer)) != -1) {
                        out.write(buffer, 0, size);
                        if (out.size() > 3_000_000)
                            throw new IOException();
                    }
                    bytes = out.toByteArray();
                }
                String encoded = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP);
                activity.runOnUiThread(() -> {
                    if (disposed || token != generation)
                        return;
                    request = analyzer.analyze(encoded, new PhotoIngredientApi.Callback() {
                        public void success(List<String> ingredients) {
                            activity.runOnUiThread(() -> {
                                if (disposed || token != generation)
                                    return;
                                request = null;
                                if (ingredients.isEmpty())
                                    showPreview(R.string.photo_empty);
                                else
                                    showReview(android.text.TextUtils.join("\n", ingredients));
                            });
                        }

                        public void failure(int message) {
                            activity.runOnUiThread(() -> {
                                if (!disposed && token == generation) {
                                    request = null;
                                    showPreview(message);
                                }
                            });
                        }
                    });
                });
            } catch (IOException | RuntimeException e) {
                activity.runOnUiThread(() -> {
                    if (!disposed && token == generation)
                        showPreview(R.string.photo_read_error);
                });
            }
        });
    }

    void showReview(String text) {
        dismiss();
        stage = "review";
        reviewText = text;
        LinearLayout content = column();
        label(content, R.string.photo_review_detail);
        review = new androidx.appcompat.widget.AppCompatEditText(activity);
        review.setId(R.id.photoIngredientReview);
        review.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE |
            android.text.InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        review.setMinLines(3);
        review.setMaxLines(10);
        review.setFilters(new InputFilter[]{new InputFilter.LengthFilter(2000)});
        review.setText(text);
        review.setContentDescription(activity.getString(R.string.photo_review));
        UiTypography.apply(review);
        content.addView(review, new LinearLayout.LayoutParams(-1, -2));
        dialog = new MaterialAlertDialogBuilder(activity).setTitle(R.string.photo_review).setView(scroll(content))
            .setPositiveButton(R.string.photo_add, null).setNegativeButton(R.string.cancel, (d, w) -> clear()).create();
        dialog.setOnCancelListener(d -> clear());
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String error = activity.addPhotoIngredients(review.getText().toString());
            if (error != null) {
                review.setError(error);
                return;
            }
            clear();
            message(R.string.photo_added);
        }));
        dialog.show();
        AdaptiveDialogs.fit(activity, dialog);
    }

    private void showProgress(int text) {
        dismiss();
        LinearLayout content = column();
        label(content, text);
        content.addView(new ProgressBar(activity));
        dialog = new MaterialAlertDialogBuilder(activity).setTitle(R.string.photo_title).setView(scroll(content))
            .setNegativeButton(R.string.cancel, (d, w) -> clear()).create();
        dialog.setOnCancelListener(d -> clear());
        dialog.show();
        AdaptiveDialogs.fit(activity, dialog);
    }

    private LinearLayout column() {
        LinearLayout l = new LinearLayout(activity);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(dp(24), dp(12), dp(24), dp(12));
        return l;
    }

    private ScrollView scroll(View content) {
        ScrollView s = new ScrollView(activity);
        s.addView(content);
        return s;
    }

    private void label(LinearLayout parent, int text) {
        TextView t = new TextView(activity);
        t.setText(text);
        t.setTextSize(15);
        t.setPadding(0, dp(12), 0, dp(12));
        UiTypography.apply(t);
        parent.addView(t);
    }

    private int dp(int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }

    private void message(int text) {
        Toast.makeText(activity, text, Toast.LENGTH_LONG).show();
    }

    private File directory() throws IOException {
        File d = new File(activity.getCacheDir(), "ingredient-photos");
        if (!d.isDirectory() && !d.mkdirs())
            throw new IOException();
        return d;
    }

    private File restoredFile(String path) {
        if (path == null)
            return null;
        try {
            File f = new File(path);
            return f.isFile() && directory().getCanonicalFile().equals(f.getCanonicalFile().getParentFile()) ? f : null;
        } catch (IOException e) {
            return null;
        }
    }

    private void dismiss() {
        if (dialog != null) {
            dialog.dismiss();
            dialog = null;
        }
        review = null;
    }

    private void clear() {
        ++generation;
        if (request != null) {
            request.cancel();
            request = null;
        }
        dismiss();
        if (capture != null)
            capture.delete();
        if (photo != null)
            photo.delete();
        capture = photo = null;
        preparing = null;
        reviewText = null;
        stage = "";
    }

    void save(Bundle state) {
        state.putString("photo_stage", stage);
        if (capture != null)
            state.putString("photo_capture", capture.getAbsolutePath());
        if (photo != null)
            state.putString("photo_ready", photo.getAbsolutePath());
        if (preparing != null)
            state.putString("photo_source", preparing.toString());
        state.putString("photo_review", review != null ? review.getText().toString() : reviewText);
    }

    void dispose() {
        disposed = true;
        ++generation;
        if (request != null)
            request.cancel();
        worker.shutdownNow();
        dismiss();
        if (!activity.isChangingConfigurations())
            clear();
    }
}
