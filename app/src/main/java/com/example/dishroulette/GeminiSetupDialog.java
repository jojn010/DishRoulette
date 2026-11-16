package com.example.dishroulette;

import android.content.Intent;
import android.net.Uri;
import android.text.InputType;
import android.view.View;
import android.view.WindowManager;
import android.widget.*;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import okhttp3.*;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Personal connection editor. Never pre-fills, displays, logs or backs up the saved secret.
 */
final class GeminiSetupDialog {
    private GeminiSetupDialog() {
    }

    static void show(MainActivity activity) {
        GeminiCredentials store = new GeminiCredentials(activity);
        GeminiCredentials.Value saved = store.read();
        LinearLayout content = new LinearLayout(activity);
        content.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(activity, 24);
        content.setPadding(pad, dp(activity, 8), pad, dp(activity, 12));
        TextView intro = text(activity, content, activity.getString(R.string.gemini_intro));
        intro.setTextSize(15);
        EditText key = new androidx.appcompat.widget.AppCompatEditText(activity);
        key.setId(R.id.geminiKey);
        key.setHint(saved == null ? R.string.gemini_key_hint : R.string.gemini_key_replace);
        key.setContentDescription(activity.getString(R.string.gemini_key_hint));
        key.setSingleLine(true);
        key.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        key.setSaveEnabled(false);
        key.setTextDirection(View.TEXT_DIRECTION_LTR);
        if (android.os.Build.VERSION.SDK_INT >= 26) {
            key.setImeOptions(android.view.inputmethod.EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING);
            key.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS);
        }
        content.addView(key, new LinearLayout.LayoutParams(-1, dp(activity, 56)));
        text(activity, content, activity.getString(R.string.gemini_model));
        EditText model = new androidx.appcompat.widget.AppCompatEditText(activity);
        model.setId(R.id.geminiModel);
        model.setSingleLine(true);
        model.setText(saved == null ? GeminiCredentials.DEFAULT_MODEL : saved.model);
        model.setTextSize(14);
        model.setTextDirection(View.TEXT_DIRECTION_LTR);
        model.setContentDescription(activity.getString(R.string.gemini_model));
        content.addView(model, new LinearLayout.LayoutParams(-1, dp(activity, 52)));
        TextView status = text(activity, content,
            activity.getString(saved == null ? R.string.gemini_not_connected : R.string.gemini_key_saved));
        status.setId(R.id.geminiStatus);
        status.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        TextView create = text(activity, content, activity.getString(R.string.gemini_get_key));
        create.setTextColor(UiColors.color(activity, R.color.accent));
        create.setMinHeight(dp(activity, 48));
        create.setGravity(android.view.Gravity.CENTER_VERTICAL);
        create.setOnClickListener(v -> activity.startActivity(
            new Intent(Intent.ACTION_VIEW, Uri.parse("https://aistudio.google.com/api-keys"))));
        ScrollView scroll = new ScrollView(activity);
        scroll.addView(content);
        UiTypography.apply(content);
        androidx.appcompat.app.AlertDialog dialog =
            new MaterialAlertDialogBuilder(activity).setTitle(R.string.gemini_settings).setView(scroll)
                .setPositiveButton(R.string.gemini_connect, null).setNegativeButton(android.R.string.cancel, null)
                .setNeutralButton(R.string.gemini_remove, null).create();
        final okhttp3.Call[] pending = {null};
        dialog.setOnShowListener(ignored -> {
            if (dialog.getWindow() != null)
                dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
            Button connect = dialog.getButton(-1);
            connect.setId(R.id.geminiConnect);
            Button remove = dialog.getButton(-3);
            remove.setEnabled(saved != null);
            remove.setOnClickListener(v -> {
                if (pending[0] != null)
                    pending[0].cancel();
                store.clear();
                dialog.dismiss();
                Toast.makeText(activity, R.string.gemini_removed, Toast.LENGTH_SHORT).show();
            });
            connect.setOnClickListener(v -> {
                GeminiCredentials.Value current = store.read();
                String secret = key.getText().toString().trim();
                if (secret.isEmpty() && current != null)
                    secret = current.key;
                String selected = model.getText().toString().trim();
                if (!GeminiKeyFormat.accepts(secret)) {
                    key.setError(activity.getString(R.string.gemini_invalid_key));
                    return;
                }
                if (!selected.matches("gemini-[A-Za-z0-9.-]{1,70}")) {
                    model.setError(activity.getString(R.string.gemini_invalid_model));
                    return;
                }
                final String checkedKey = secret;
                connect.setEnabled(false);
                remove.setEnabled(false);
                key.setEnabled(false);
                model.setEnabled(false);
                status.setText(R.string.gemini_checking);
                OkHttpClient client =
                    new OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS).callTimeout(20, TimeUnit.SECONDS)
                        .followRedirects(false).followSslRedirects(false).build();
                pending[0] = client.newCall(
                    new Request.Builder().url("https://generativelanguage.googleapis.com/v1beta/models/" + selected)
                        .header("x-goog-api-key", secret).build());
                pending[0].enqueue(new Callback() {
                    private void finish(boolean ok) {
                        activity.runOnUiThread(() -> {
                            if (!dialog.isShowing())
                                return;
                            connect.setEnabled(true);
                            remove.setEnabled(store.configured());
                            key.setEnabled(true);
                            model.setEnabled(true);
                            if (ok) {
                                try {
                                    store.save(checkedKey, selected);
                                    key.setText("");
                                    status.setText(R.string.gemini_connected);
                                    Toast.makeText(activity, R.string.gemini_connected, Toast.LENGTH_LONG).show();
                                    dialog.dismiss();
                                } catch (Exception e) {
                                    status.setText(R.string.gemini_storage_error);
                                }
                            } else
                                status.setText(R.string.gemini_check_failed);
                        });
                    }

                    public void onFailure(okhttp3.Call call, IOException e) {
                        finish(false);
                    }

                    public void onResponse(okhttp3.Call call, Response response) {
                        try (Response close = response) {
                            finish(response.isSuccessful());
                        }
                    }
                });
            });
        });
        dialog.setOnDismissListener(ignored -> {
            if (pending[0] != null)
                pending[0].cancel();
            key.setText("");
            activity.refreshConnection();
        });
        dialog.show();
    }

    private static int dp(MainActivity a, int value) {
        return Math.round(value * a.getResources().getDisplayMetrics().density);
    }

    private static TextView text(MainActivity a, LinearLayout parent, String value) {
        TextView t = new TextView(a);
        t.setText(value);
        t.setTextSize(14);
        t.setTextColor(UiColors.color(a, R.color.muted));
        t.setPadding(0, dp(a, 10), 0, dp(a, 6));
        parent.addView(t, new LinearLayout.LayoutParams(-1, -2));
        return t;
    }
}
