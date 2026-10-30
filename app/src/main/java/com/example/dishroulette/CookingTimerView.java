package com.example.dishroulette;

import android.content.Context;
import android.animation.ValueAnimator;
import android.provider.Settings;
import android.view.animation.LinearInterpolator;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.View;
import java.util.Locale;

/** A remaining-time dial that scales down on narrow screens. */
final class CookingTimerView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private long remaining, duration = 1;
    private float displayedFraction;
    private ValueAnimator sweep;
    private String time = "", status = "";

    CookingTimerView(Context context) {
        super(context);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
    }

    void update(long remainingMs, long durationMs, boolean running) {
        stopSweep();
        remaining = Math.max(0, remainingMs);
        duration = Math.max(1, durationMs);
        long seconds = (remaining + 999) / 1000;
        Locale locale = getResources().getConfiguration().getLocales().get(0);
        time = String.format(locale, "%02d:%02d", seconds / 60, seconds % 60);
        status = getContext().getString(remaining == 0 ? R.string.timer_finished : R.string.cooking_timer);
        setContentDescription(status + ", " + time);
        displayedFraction = remainingFraction();
        boolean reduceMotion = InterfaceSettings.prefs(getContext()).getBoolean("reduce_motion", false);
        float animationScale = Settings.Global.getFloat(getContext().getContentResolver(),
                Settings.Global.ANIMATOR_DURATION_SCALE, 1f);
        if (running && remaining > 0 && !reduceMotion && animationScale > 0 && isAttachedToWindow() && isShown() && getWindowVisibility() == VISIBLE) {
            // Interpolate only the dial; the persisted deadline remains the source of truth.
            float next = Math.max(0f, (float)(remaining - 1000) / duration);
            sweep = ValueAnimator.ofFloat(displayedFraction, next);
            sweep.setDuration(1000);
            sweep.setInterpolator(new LinearInterpolator());
            sweep.addUpdateListener(value -> {
                displayedFraction = (float)value.getAnimatedValue();
                invalidate();
            });
            sweep.start();
        }
        invalidate();
    }

    private void stopSweep() {
        if (sweep != null) { sweep.cancel(); sweep = null; }
    }

    @Override protected void onDetachedFromWindow() {
        stopSweep();
        super.onDetachedFromWindow();
    }

    @Override protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        if (visibility != VISIBLE) stopSweep();
    }

    float remainingFraction() { return Math.min(1f, (float) remaining / duration); }

    @Override protected void onMeasure(int widthSpec, int heightSpec) {
        int desired = Math.round(260 * getResources().getDisplayMetrics().density);
        int width = resolveSize(desired, widthSpec);
        int height = resolveSize(desired, heightSpec);
        int size = Math.min(width, height);
        setMeasuredDimension(size, size);
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float size = Math.min(getWidth(), getHeight());
        float cx = getWidth() / 2f, cy = getHeight() / 2f, radius = size * .42f;
        int accent = UiColors.color(getContext(), R.color.accent);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(UiColors.color(getContext(), R.color.surface));
        canvas.drawCircle(cx, cy, radius, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(size * .035f);
        paint.setColor(UiColors.color(getContext(), R.color.line));
        canvas.drawCircle(cx, cy, radius, paint);
        paint.setColor(accent);
        paint.setStrokeCap(Paint.Cap.ROUND);
        RectF ring = new RectF(cx-radius, cy-radius, cx+radius, cy+radius);
        canvas.drawArc(ring, -90, 360 * displayedFraction, false, paint);
        paint.setStrokeCap(Paint.Cap.BUTT);
        paint.setStrokeWidth(size * .006f);
        for (int i = 0; i < 60; i++) {
            double angle = Math.toRadians(i * 6 - 90);
            float outer = radius - size * .04f;
            float inner = outer - size * (i % 5 == 0 ? .035f : .015f);
            paint.setColor(UiColors.color(getContext(), i % 5 == 0 ? R.color.muted : R.color.line));
            canvas.drawLine(cx+(float)Math.cos(angle)*inner, cy+(float)Math.sin(angle)*inner,
                    cx+(float)Math.cos(angle)*outer, cy+(float)Math.sin(angle)*outer, paint);
        }
        if (remaining > 0) {
            double angle = Math.toRadians(360 * displayedFraction - 90);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(accent);
            canvas.drawCircle(cx+(float)Math.cos(angle)*radius, cy+(float)Math.sin(angle)*radius, size*.028f, paint);
        }
        paint.setStyle(Paint.Style.FILL);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        paint.setColor(UiColors.color(getContext(), R.color.ink));
        paint.setTextSize(size * .19f);
        if (paint.measureText(time) > size * .66f) paint.setTextSize(paint.getTextSize() * size * .66f / paint.measureText(time));
        canvas.drawText(time, cx, cy-(paint.ascent()+paint.descent())/2, paint);
        paint.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        paint.setTextSize(size * .055f);
        if (paint.measureText(status) > size * .64f) paint.setTextSize(paint.getTextSize() * size * .64f / paint.measureText(status));
        paint.setColor(UiColors.color(getContext(), R.color.muted));
        canvas.drawText(status, cx, cy+size*.17f, paint);
    }
}
