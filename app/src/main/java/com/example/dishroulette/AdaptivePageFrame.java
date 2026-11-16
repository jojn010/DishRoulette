package com.example.dishroulette;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;

/**
 * Center readable content inside the measured window, including split screen and system insets.
 */
public final class AdaptivePageFrame extends FrameLayout {
    private final int maximumWidth;

    public AdaptivePageFrame(Context context) {
        this(context, 720);
    }

    public AdaptivePageFrame(Context context, AttributeSet attrs) {
        super(context, attrs);
        maximumWidth = dp(720);
    }

    AdaptivePageFrame(Context context, int maxDp) {
        super(context);
        maximumWidth = dp(maxDp);
    }

    private int dp(int n) {
        return Math.round(n * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        int available = MeasureSpec.getMode(widthSpec) == MeasureSpec.UNSPECIFIED ? maximumWidth :
            Math.max(0, MeasureSpec.getSize(widthSpec) - getPaddingLeft() - getPaddingRight());
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) child.getLayoutParams();
            lp.width = Math.min(maximumWidth, Math.max(0, available - lp.leftMargin - lp.rightMargin));
            lp.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        }
        super.onMeasure(widthSpec, heightSpec);
    }
}
