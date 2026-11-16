package com.example.dishroulette;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.widget.EditText;

import androidx.appcompat.app.AlertDialog;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.platform.app.InstrumentationRegistry;

import java.io.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Deterministic emulator-only tests: no images or credentials leave the device.
 */
public class PhotoIngredientsTest {
    @Test
    public void evictedPreviewCanChooseAnotherPhotoAfterRecreation() throws Exception {
        Context c = ApplicationProvider.getApplicationContext();
        File source = File.createTempFile("evicted-preview-", ".jpg", c.getCacheDir());
        Bitmap bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
        try (OutputStream out = new FileOutputStream(source)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out);
        }
        bitmap.recycle();
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(a -> a.photoIngredients.acceptPhoto(Uri.fromFile(source)));
            awaitButton(scenario, R.string.photo_analyze);
            scenario.onActivity(a -> {
                try {
                    java.lang.reflect.Field field = PhotoIngredientsController.class.getDeclaredField("photo");
                    field.setAccessible(true);
                    assertTrue(((File) field.get(a.photoIngredients)).delete());
                } catch (ReflectiveOperationException e) {
                    throw new AssertionError(e);
                }
            });
            scenario.recreate();
            scenario.onActivity(a -> {
                a.photoIngredients.open();
                assertNotNull("Scan must recover when Android removes the cached preview", dialog(a));
                assertNotNull(dialog(a).getListView());
                assertEquals(2, dialog(a).getListView().getAdapter().getCount());
            });
        } finally {
            source.delete();
        }
    }

    @Before
    public void setup() throws Exception {
        Context c = ApplicationProvider.getApplicationContext();
        c.getSharedPreferences("dishroulette_ui", 0).edit().clear().putString(InterfaceSettings.LANGUAGE, "en")
            .commit();
        new GeminiCredentials(c).save("AIza" + "x".repeat(35), "gemini-test");
    }

    @Test
    public void reviewEditsSurviveRotationAndMergeWithoutDuplicates() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(a -> {
                assertNull(a.addPhotoIngredients("rice"));
                a.photoIngredients.showReview("rice\neggs\ntomato");
            });
            scenario.onActivity(
                a -> ((EditText) dialog(a).findViewById(R.id.photoIngredientReview)).setText("rice\neggs"));
            scenario.recreate();
            scenario.onActivity(a -> {
                assertEquals("rice\neggs",
                    ((EditText) dialog(a).findViewById(R.id.photoIngredientReview)).getText().toString());
                dialog(a).getButton(AlertDialog.BUTTON_POSITIVE).performClick();
            });
            scenario.onActivity(a -> {
                String saved = a.getSharedPreferences("dishroulette_ui", 0).getString("loaded_ammunition", "");
                assertTrue(saved.contains("rice"));
                assertTrue(saved.contains("eggs"));
                assertFalse(saved.contains("tomato"));
                assertEquals(saved.indexOf("rice"), saved.lastIndexOf("rice"));
                assertEquals("pantry", a.profile().mode);
            });
        }
    }

    @Test
    public void invalidReviewDoesNotPartiallyModifyPantry() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(a -> {
                assertNull(a.addPhotoIngredients("rice"));
                String before = a.getSharedPreferences("dishroulette_ui", 0).getString("loaded_ammunition", "");
                assertNotNull(a.addPhotoIngredients("eggs\n" + "x".repeat(81)));
                assertEquals(before, a.getSharedPreferences("dishroulette_ui", 0).getString("loaded_ammunition", ""));
                assertNotNull(a.addPhotoIngredients("  "));
            });
        }
    }

    @Test
    public void photoPreviewAnalysisReviewAndCancellation() throws Exception {
        Context c = ApplicationProvider.getApplicationContext();
        File source = new File(c.getCacheDir(), "test-food.jpg");
        Bitmap bitmap = Bitmap.createBitmap(2200, 1100, Bitmap.Config.ARGB_8888);
        try (OutputStream out = new FileOutputStream(source)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out);
        }
        bitmap.recycle();
        AtomicReference<PhotoIngredientApi.Callback> pending = new AtomicReference<>();
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(a -> {
                a.photoIngredients.analyzer = (image, callback) -> {
                    assertTrue(image.length() > 100);
                    pending.set(callback);
                    return null;
                };
                a.photoIngredients.acceptPhoto(Uri.fromFile(source));
            });
            awaitButton(scenario, R.string.photo_analyze);
            scenario.onActivity(a -> dialog(a).getButton(AlertDialog.BUTTON_POSITIVE).performClick());
            long deadline = System.currentTimeMillis() + 5000;
            while (pending.get() == null && System.currentTimeMillis() < deadline) {
                Thread.sleep(30);
            }
            assertNotNull(pending.get());
            pending.get().failure(R.string.photo_network_error);
            awaitButton(scenario, R.string.photo_analyze);
            pending.set(null);
            scenario.onActivity(a -> dialog(a).getButton(AlertDialog.BUTTON_POSITIVE).performClick());
            deadline = System.currentTimeMillis() + 5000;
            while (pending.get() == null && System.currentTimeMillis() < deadline) {
                Thread.sleep(30);
            }
            assertNotNull(pending.get());
            pending.get().success(List.of());
            awaitButton(scenario, R.string.photo_analyze);
            pending.set(null);
            scenario.onActivity(a -> dialog(a).getButton(AlertDialog.BUTTON_POSITIVE).performClick());
            deadline = System.currentTimeMillis() + 5000;
            while (pending.get() == null && System.currentTimeMillis() < deadline) {
                Thread.sleep(30);
            }
            assertNotNull(pending.get());
            pending.get().success(List.of("tomato", "eggs"));
            awaitButton(scenario, R.string.photo_add);
            scenario.onActivity(a -> {
                assertEquals("tomato\neggs",
                    ((EditText) dialog(a).findViewById(R.id.photoIngredientReview)).getText().toString());
                dialog(a).getButton(AlertDialog.BUTTON_NEGATIVE).performClick();
            });
            scenario.onActivity(a -> assertFalse(
                a.getSharedPreferences("dishroulette_ui", 0).getString("loaded_ammunition", "").contains("tomato")));
            pending.get().success(List.of("late result"));
            scenario.onActivity(a -> assertNull(dialog(a)));
            assertEquals(0, new File(c.getCacheDir(), "ingredient-photos").list().length);
        } finally {
            source.delete();
        }
    }

    @Test
    public void imageIsSampledOrientedAndMetadataRemoved() throws Exception {
        Context c = ApplicationProvider.getApplicationContext();
        File original = new File(c.getCacheDir(), "orientation-test.jpg");
        Bitmap bitmap = Bitmap.createBitmap(2400, 1200, Bitmap.Config.ARGB_8888);
        try (OutputStream out = new FileOutputStream(original)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
        }
        bitmap.recycle();
        androidx.exifinterface.media.ExifInterface exif = new androidx.exifinterface.media.ExifInterface(original);
        exif.setAttribute(androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION, "6");
        exif.setAttribute(androidx.exifinterface.media.ExifInterface.TAG_ARTIST, "private metadata");
        exif.saveAttributes();
        File prepared = IngredientPhoto.prepare(c, Uri.fromFile(original));
        try {
            Bitmap result = android.graphics.BitmapFactory.decodeFile(prepared.getAbsolutePath());
            assertEquals(600, result.getWidth());
            assertEquals(1200, result.getHeight());
            result.recycle();
            assertNull(new androidx.exifinterface.media.ExifInterface(prepared).getAttribute(
                androidx.exifinterface.media.ExifInterface.TAG_ARTIST));
            Uri uri =
                androidx.core.content.FileProvider.getUriForFile(c, c.getPackageName() + ".ingredientphotos", prepared);
            assertEquals("content", uri.getScheme());
            try (InputStream in = c.getContentResolver().openInputStream(uri)) {
                assertTrue(in.read() >= 0);
            }
        } finally {
            original.delete();
            prepared.delete();
        }
    }

    @Test
    public void allExifOrientationsPreserveTheExpectedCornerPositions() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        int r = android.graphics.Color.RED, g = android.graphics.Color.GREEN;
        int b = android.graphics.Color.BLUE, y = android.graphics.Color.YELLOW;
        int[][] expected = {{r, g, b, y}, {g, r, y, b}, {y, b, g, r}, {b, y, r, g},
            {r, b, g, y}, {b, r, y, g}, {y, g, b, r}, {g, y, r, b}};
        for (int orientation = 1; orientation <= 8; orientation++) {
            File source = File.createTempFile("exif-corners-", ".jpg", context.getCacheDir());
            File prepared = null;
            Bitmap original = Bitmap.createBitmap(160, 80, Bitmap.Config.ARGB_8888);
            android.graphics.Canvas canvas = new android.graphics.Canvas(original);
            android.graphics.Paint paint = new android.graphics.Paint();
            for (int quadrant = 0; quadrant < 4; quadrant++) {
                paint.setColor(expected[0][quadrant]);
                int x = quadrant % 2 * 80, z = quadrant / 2 * 40;
                canvas.drawRect(x, z, x + 80, z + 40, paint);
            }
            try {
                try (OutputStream out = new FileOutputStream(source)) {
                    original.compress(Bitmap.CompressFormat.JPEG, 100, out);
                }
                androidx.exifinterface.media.ExifInterface exif =
                    new androidx.exifinterface.media.ExifInterface(source);
                exif.setAttribute(androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION,
                    String.valueOf(orientation));
                exif.saveAttributes();
                prepared = IngredientPhoto.prepare(context, Uri.fromFile(source));
                Bitmap result = android.graphics.BitmapFactory.decodeFile(prepared.getAbsolutePath());
                try {
                    assertEquals(orientation >= 5 ? 80 : 160, result.getWidth());
                    assertEquals(orientation >= 5 ? 160 : 80, result.getHeight());
                    for (int quadrant = 0; quadrant < 4; quadrant++) {
                        int actual = result.getPixel(result.getWidth() * (quadrant % 2 * 2 + 1) / 4,
                            result.getHeight() * (quadrant / 2 * 2 + 1) / 4);
                        int wanted = expected[orientation - 1][quadrant];
                        for (int shift : new int[]{0, 8, 16})
                            assertTrue("EXIF " + orientation + ", corner " + quadrant,
                                Math.abs(((actual >> shift) & 255) - ((wanted >> shift) & 255)) < 40);
                    }
                } finally {
                    result.recycle();
                }
            } finally {
                original.recycle();
                source.delete();
                if (prepared != null)
                    prepared.delete();
            }
        }
    }

    @Test
    public void localizedPhotoReviewRendersInBothLanguages() throws Exception {
        Context c = ApplicationProvider.getApplicationContext();
        for (String language : List.of("en", "ar")) {
            c.getSharedPreferences("dishroulette_ui", 0).edit().putString(InterfaceSettings.LANGUAGE, language)
                .commit();
            try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
                InstrumentationRegistry.getInstrumentation().waitForIdleSync();
                scenario.onActivity(a -> {
                    snapshot(a, a.getWindow().getDecorView(), "home-" + language);
                    a.photoIngredients.showReview(language.equals("ar") ? "طماطم\nبيض\nأرز" : "tomato\neggs\nrice");
                });
                InstrumentationRegistry.getInstrumentation().waitForIdleSync();
                scenario.onActivity(a -> {
                    EditText field = dialog(a).findViewById(R.id.photoIngredientReview);
                    assertTrue(field.getHeight() > 0);
                    snapshot(a, dialog(a).getWindow().getDecorView(), "review-" + language);
                });
            }
        }
    }

    private void snapshot(Context context, android.view.View view, String name) {
        Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
        view.draw(new android.graphics.Canvas(bitmap));
        File directory = context.getExternalFilesDir("verification");
        try (OutputStream out = new FileOutputStream(new File(directory, name + ".png"))) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
        } catch (IOException e) {
            throw new AssertionError(e);
        } finally {
            bitmap.recycle();
        }
    }

    private AlertDialog dialog(MainActivity activity) {
        try {
            java.lang.reflect.Field field = PhotoIngredientsController.class.getDeclaredField("dialog");
            field.setAccessible(true);
            return (AlertDialog) field.get(activity.photoIngredients);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    private void awaitButton(ActivityScenario<MainActivity> scenario, int resource) throws Exception {
        long deadline = System.currentTimeMillis() + 10000;
        while (System.currentTimeMillis() < deadline) {
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            java.util.concurrent.atomic.AtomicBoolean found = new java.util.concurrent.atomic.AtomicBoolean();
            scenario.onActivity(a -> {
                AlertDialog d = dialog(a);
                found.set(d != null && d.isShowing() && d.getButton(AlertDialog.BUTTON_POSITIVE) != null
                    && a.getString(resource).contentEquals(d.getButton(AlertDialog.BUTTON_POSITIVE).getText()));
            });
            if (found.get())
                return;
            Thread.sleep(50);
        }
        throw new AssertionError("Timed out waiting for photo UI");
    }
}
