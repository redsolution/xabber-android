package com.xabber.android.ui.widget;

import android.content.Context;
import android.text.Layout;
import android.text.Spannable;
import android.util.AttributeSet;

import com.xabber.android.ui.text.CustomQuoteSpan;

public class CorrectlyMeasuringTextView extends CorrectlyTouchEventTextView {

    public CorrectlyMeasuringTextView(Context context) {
        super(context);
    }

    public CorrectlyMeasuringTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CorrectlyMeasuringTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void onMeasure(int wms, int hms) {
        super.onMeasure(wms, hms);
        try {
            Layout l = getLayout();
            CharSequence text = getText();
            if (l.getLineCount() <= 1) {
                return;
            }
            int quoteOffset = checkForQuoteSpans(text);
            int maxw = 0;
            for (int i = l.getLineCount() - 1; i >= 0; --i) {
                maxw = Math.max(maxw, Math.round(l.getPaint().measureText(text, l.getLineStart(i), l.getLineEnd(i))) + quoteOffset);
            }
            super.onMeasure(Math.min(maxw + getPaddingLeft() + getPaddingRight(), getMeasuredWidth()) | MeasureSpec.EXACTLY, getMeasuredHeight() | MeasureSpec.EXACTLY);
        } catch (Exception ignore) {

        }
    }

    private int checkForQuoteSpans(CharSequence text) {
        if (text instanceof Spannable) {
            CustomQuoteSpan[] spans = ((Spannable) text).getSpans(0, text.length(), CustomQuoteSpan.class);
            if (spans.length > 0) {
                return spans[0].getLeadingMargin(false);
            }
        }
        return 0;
    }

}
