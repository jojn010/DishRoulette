package com.example.dishroulette;

import android.graphics.Bitmap;
import android.view.*;
import android.widget.TextView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;

import java.io.*;

import org.junit.Test;

import static org.junit.Assert.*;

public class NavigationAlignmentTest {
    private TextView text(View v, String value) {
        if (v instanceof TextView && ((TextView) v).getText().toString().equals(value))
            return (TextView) v;
        if (v instanceof ViewGroup)
            for (int i = 0; i < ((ViewGroup) v).getChildCount(); i++) {
                TextView t = text(((ViewGroup) v).getChildAt(i), value);
                if (t != null)
                    return t;
            }
        return null;
    }

    @Test
    public void iconLabelGroupsStayCenteredAfterLanguageSwitches() throws Exception {
        android.content.Context c = InstrumentationRegistry.getInstrumentation().getTargetContext();
        InterfaceSettings.prefs(c).edit().putString(InterfaceSettings.LANGUAGE, "ar").commit();
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            for (String language : new String[]{"ar", "en", "ar", "en"}) {
                scenario.onActivity(a -> a.changeLanguage(language));
                InstrumentationRegistry.getInstrumentation().waitForIdleSync();
                Thread.sleep(200);
                scenario.onActivity(a -> {
                    for (int id : new int[]{R.string.home, R.string.saved, R.string.settings}) {
                        TextView label = text(a.findViewById(R.id.simpleHome), a.getString(id));
                        assertNotNull(label);
                        NavigationButton button = (NavigationButton) label.getParent();
                        View icon = button.getChildAt(0);
                        int[] iconPosition = new int[2], labelPosition = new int[2];
                        icon.getLocationOnScreen(iconPosition);
                        label.getLocationOnScreen(labelPosition);
                        float iconCenter = iconPosition[0] + icon.getWidth() / 2f;
                        float labelCenter = labelPosition[0] + label.getCompoundPaddingLeft() +
                            (label.getLayout().getLineLeft(0) + label.getLayout().getLineRight(0)) / 2f;
                        if (button.getOrientation() == android.widget.LinearLayout.VERTICAL) {
                            assertEquals(label.getText().toString(), iconCenter, labelCenter, 1f);
                            assertTrue("Label must be below its icon",
                                labelPosition[1] >= iconPosition[1] + icon.getHeight());
                        } else {
                            assertEquals("Row icon and label must align vertically",
                                iconPosition[1] + icon.getHeight() / 2f, labelPosition[1] + label.getHeight() / 2f, 1f);
                            int[] position = new int[2];
                            button.getLocationOnScreen(position);
                            float left = Math.min(iconPosition[0], labelPosition[0]);
                            float right =
                                Math.max(iconPosition[0] + icon.getWidth(), labelPosition[0] + label.getWidth());
                            assertEquals("Compact icon-label group must be centered",
                                position[0] + button.getWidth() / 2f, (left + right) / 2f, 1f);
                            assertTrue(language.equals("ar") ?
                                iconPosition[0] >= labelPosition[0] + label.getWidth() :
                                labelPosition[0] >= iconPosition[0] + icon.getWidth());
                        }
                    }
                });
                Bitmap bitmap = InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
                File file = new File(c.getExternalFilesDir(null), "navigation-" + language + ".png");
                try (FileOutputStream out = new FileOutputStream(file)) {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                }
                bitmap.recycle();
            }
        }
    }
}
