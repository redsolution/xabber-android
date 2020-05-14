package com.xabber.android.ui.text;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.Layout;
import android.text.style.LeadingMarginSpan;
import android.util.DisplayMetrics;
import android.util.TypedValue;

public class CustomQuoteSpan implements LeadingMarginSpan {

    private final int color;
    private final int width;
    private final int paddingLeft;
    private final int paddingRight;

    private static final float WIDTH_DP = 2f;
    private static final float PADDING_LEFT_SP = 0f;
    private static final float PADDING_RIGHT_SP = 6f;

    public CustomQuoteSpan(int color, DisplayMetrics metrics) {
        this.color = color;
        this.width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, WIDTH_DP, metrics);
        this.paddingLeft = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, PADDING_LEFT_SP, metrics);
        this.paddingRight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, PADDING_RIGHT_SP, metrics);
    }

    @Override
    public int getLeadingMargin(boolean first) {
        return paddingLeft + width + paddingRight;
    }

    @Override
    public void drawLeadingMargin(Canvas c, Paint p, int x, int dir, int top, int baseline, int bottom, CharSequence text, int start, int end, boolean first, Layout layout) {
        Paint.Style originalStyle = p.getStyle();
        int originalColor = p.getColor();

        p.setStyle(Paint.Style.FILL);
        p.setColor(color);
        c.drawRect(x + dir * paddingLeft, top, x + dir * (paddingLeft + width), bottom, p);

        p.setStyle(originalStyle);
        p.setColor(originalColor);
    }
}