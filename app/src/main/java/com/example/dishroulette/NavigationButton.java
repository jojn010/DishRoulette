package com.example.dishroulette;

import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Independent centered icon and label, unaffected by compound-drawable offsets after RTL changes.
 */
final class NavigationButton extends LinearLayout {
    private final ImageView icon;
    private final TextView label;

    NavigationButton(MainActivity activity, String title, int drawable, boolean selected, Runnable action) {
        super(activity);
        setGravity(Gravity.CENTER);
        setPadding(dp(4), dp(6), dp(4), dp(6));
        setClickable(true);
        setFocusable(true);
        setSelected(selected);
        setContentDescription(title);
        icon = new ImageView(activity);
        icon.setImageResource(drawable);
        icon.setImageTintList(ColorStateList.valueOf(UiColors.color(activity, R.color.accent)));
        icon.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        addView(icon);
        label = new TextView(activity);
        label.setText(title);
        label.setTextSize(12);
        label.setGravity(Gravity.CENTER);
        label.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        label.setTextColor(UiColors.color(activity, R.color.accent));
        label.setTypeface(UiTypography.face(activity, "sans-serif", selected ? Typeface.BOLD : Typeface.NORMAL));
        UiTypography.apply(label);
        label.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        addView(label);
        GradientDrawable background = new GradientDrawable();
        background.setCornerRadius(dp(22));
        background.setColor(selected ? UiColors.color(activity, R.color.surface) : android.graphics.Color.TRANSPARENT);
        GradientDrawable mask = new GradientDrawable();
        mask.setCornerRadius(dp(22));
        mask.setColor(android.graphics.Color.WHITE);
        setBackground(new RippleDrawable(ColorStateList.valueOf(
            androidx.core.graphics.ColorUtils.setAlphaComponent(UiColors.color(activity, R.color.accent), 32)),
            background, mask));
        setOnClickListener(v -> {
            activity.playUiClick();
            action.run();
        });
        setStacked(false);
    }

    private int dp(int n) {
        return Math.round(n * getResources().getDisplayMetrics().density);
    }

    void setStacked(boolean stacked) {
        setOrientation(stacked ? HORIZONTAL : VERTICAL);
        LayoutParams iconLp = new LayoutParams(dp(19), dp(19));
        iconLp.gravity = Gravity.CENTER;
        if (stacked)
            iconLp.setMarginEnd(dp(8));
        else
            iconLp.bottomMargin = dp(2);
        icon.setLayoutParams(iconLp);
        LayoutParams labelLp = new LayoutParams(stacked ? LayoutParams.WRAP_CONTENT : LayoutParams.MATCH_PARENT,
            LayoutParams.WRAP_CONTENT);
        labelLp.gravity = Gravity.CENTER;
        label.setLayoutParams(labelLp);
        setMinimumHeight(Math.round(dp(stacked ? 52 : 64) * Math.max(1f, getResources().getConfiguration().fontScale)));
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        info.setClassName(android.widget.Button.class.getName());
    }
}
