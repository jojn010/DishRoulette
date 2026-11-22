package com.example.dishroulette;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Base64;

import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Assume;
import org.junit.Test;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

/** Opt-in provider smoke check using the bundled pantry image and the saved connection. */
public class LivePhotoConnectionTest {
    @Test
    public void bundledPantryPhotoReturnsReviewableIngredients() throws Exception {
        Assume.assumeTrue("true".equals(InstrumentationRegistry.getArguments().getString("livePhoto")));
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        String language = InstrumentationRegistry.getArguments().getString("photoLanguage", "en");
        assertTrue("Unsupported test language", language.equals("en") || language.equals("ar"));
        SharedPreferences preferences = InterfaceSettings.prefs(context);
        boolean hadLanguage = preferences.contains(InterfaceSettings.LANGUAGE);
        String originalLanguage = preferences.getString(InterfaceSettings.LANGUAGE, "en");
        File photo = null;
        okhttp3.Call pending = null;
        try {
            preferences.edit().putString(InterfaceSettings.LANGUAGE, language).commit();
            Uri source = Uri.parse("android.resource://" + context.getPackageName() + "/" + R.drawable.pantry_photo);
            photo = IngredientPhoto.prepare(context, source);
            byte[] bytes = new byte[(int) photo.length()];
            try (DataInputStream input = new DataInputStream(new FileInputStream(photo))) {
                input.readFully(bytes);
            }
            CountDownLatch completed = new CountDownLatch(1);
            AtomicReference<List<String>> ingredients = new AtomicReference<>();
            AtomicReference<String> failure = new AtomicReference<>();
            pending = PhotoIngredientApi.analyze(context, Base64.encodeToString(bytes, Base64.NO_WRAP),
                new PhotoIngredientApi.Callback() {
                    public void success(List<String> result) {
                        ingredients.set(result);
                        completed.countDown();
                    }

                    public void failure(int message) {
                        failure.set(context.getString(message));
                        completed.countDown();
                    }
                });
            assertTrue("Photo analysis timed out", completed.await(75, TimeUnit.SECONDS));
            assertNull("Photo analysis failed", failure.get());
            assertNotNull(ingredients.get());
            assertFalse("The bundled pantry image should contain recognizable foods", ingredients.get().isEmpty());
            assertTrue(ingredients.get().size() <= 20);
            boolean foundTomato = false;
            for (String ingredient : ingredients.get()) {
                if (RecipeCatalog.parseIngredients(ingredient).contains("tomato"))
                    foundTomato = true;
            }
            assertTrue("The bundled photo contains clearly visible tomatoes", foundTomato);
        } finally {
            if (pending != null)
                pending.cancel();
            if (photo != null)
                photo.delete();
            SharedPreferences.Editor restore = preferences.edit();
            if (hadLanguage)
                restore.putString(InterfaceSettings.LANGUAGE, originalLanguage);
            else
                restore.remove(InterfaceSettings.LANGUAGE);
            restore.commit();
        }
    }
}
