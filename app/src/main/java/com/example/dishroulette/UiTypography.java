package com.example.dishroulette;

import android.content.Context;
import android.graphics.Typeface;
import android.view.View;
import android.widget.TextView;

final class UiTypography {
    private static Typeface arabic;

    private UiTypography() {
    }

    static Typeface arabic(Context c) {
        if (arabic == null)
            arabic = androidx.core.content.res.ResourcesCompat.getFont(c, R.font.cairo);
        return arabic;
    }

    static Typeface face(Context c, String latin, int style) {
        return InterfaceSettings.language(c).equals("ar") ? Typeface.create(arabic(c), style) : Typeface.create(latin,
            style);
    }

    static void apply(View v) {
        if (!InterfaceSettings.language(v.getContext()).equals("ar"))
            return;
        if (v instanceof TextView) {
            TextView t = (TextView) v;
            boolean bold = t.getTypeface() != null && t.getTypeface().isBold();
            boolean italic = t.getTypeface() != null && t.getTypeface().isItalic();
            int style =
                bold ? (italic ? Typeface.BOLD_ITALIC : Typeface.BOLD) : (italic ? Typeface.ITALIC : Typeface.NORMAL);
            t.setTypeface(Typeface.create(arabic(v.getContext()), style));
            t.setLetterSpacing(0);
        }
        if (v instanceof android.view.ViewGroup) {
            android.view.ViewGroup g = (android.view.ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++)
                apply(g.getChildAt(i));
        }
    }
}
