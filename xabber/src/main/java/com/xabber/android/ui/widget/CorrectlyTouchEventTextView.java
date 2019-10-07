package com.xabber.android.ui.widget;

import android.content.Context;
import androidx.appcompat.widget.AppCompatTextView;
import android.text.Layout;
import android.text.Selection;
import android.text.Spannable;
import android.text.method.LinkMovementMethod;
import android.text.method.Touch;
import android.text.style.BackgroundColorSpan;
import android.text.style.ClickableSpan;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.TextView;

import com.xabber.android.R;

public class CorrectlyTouchEventTextView extends AppCompatTextView {
    boolean clickableSpanClicked;

    public CorrectlyTouchEventTextView(Context context) {
        super(context);
    }

    public CorrectlyTouchEventTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CorrectlyTouchEventTextView(
            Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        clickableSpanClicked = false;
        super.onTouchEvent(event);
        return clickableSpanClicked;
    }

    public static class LocalLinkMovementMethod extends LinkMovementMethod {
        static LocalLinkMovementMethod instance;

        private boolean isUrlHighlighted;

        public static LocalLinkMovementMethod getInstance() {
            if (instance == null)
                instance = new LocalLinkMovementMethod();
            return instance;
        }

        @Override
        public boolean onTouchEvent(TextView widget, Spannable buffer, MotionEvent event) {
            int action = event.getAction();

            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_DOWN) {
                int x = (int) event.getX();
                int y = (int) event.getY();

                x -= widget.getTotalPaddingLeft();
                y -= widget.getTotalPaddingTop();

                x += widget.getScrollX();
                y += widget.getScrollY();

                Layout layout = widget.getLayout();
                int line = layout.getLineForVertical(y);
                int off = layout.getOffsetForHorizontal(line, x);

                ClickableSpan[] link = buffer.getSpans(off, off, ClickableSpan.class);

                if (link.length != 0) {
                    if (action == MotionEvent.ACTION_UP) {
                        link[0].onClick(widget);
                        removeUrlHighlightColor(widget);
                    } else if (action == MotionEvent.ACTION_DOWN) {
                        Selection.setSelection(buffer,
                                buffer.getSpanStart(link[0]),
                                buffer.getSpanEnd(link[0]));
                        highlightUrl(widget, link[0], buffer);
                    }

                    if (widget instanceof CorrectlyTouchEventTextView){
                        ((CorrectlyTouchEventTextView) widget).clickableSpanClicked = true;
                    }

                    return true;
                } else {
                    Selection.removeSelection(buffer);
                    Touch.onTouchEvent(widget, buffer, event);
                    return false;
                }
            }
            return Touch.onTouchEvent(widget, buffer, event);
        }

        protected void highlightUrl(TextView textView, ClickableSpan clickableSpan, Spannable text) {
            if (isUrlHighlighted) return;
            isUrlHighlighted = true;

            int spanStart = text.getSpanStart(clickableSpan);
            int spanEnd = text.getSpanEnd(clickableSpan);
            BackgroundColorSpan highlightSpan;

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
                highlightSpan = new BackgroundColorSpan(textView.getHighlightColor());
            } else highlightSpan = new BackgroundColorSpan(textView.getLinkTextColors().getDefaultColor());
            text.setSpan(highlightSpan, spanStart, spanEnd, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            textView.setTag(R.id.clickable_span_highlight_background, highlightSpan);
            Selection.setSelection(text, spanStart, spanEnd);
        }

        protected void removeUrlHighlightColor(TextView textView) {
            if (!isUrlHighlighted) return;
            isUrlHighlighted = false;

            Spannable text = (Spannable) textView.getText();
            BackgroundColorSpan highlightSpan = (BackgroundColorSpan) textView.getTag(R.id.clickable_span_highlight_background);
            text.removeSpan(highlightSpan);
            Selection.removeSelection(text);
        }
    }
}
