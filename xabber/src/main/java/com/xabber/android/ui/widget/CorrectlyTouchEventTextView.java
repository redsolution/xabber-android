package com.xabber.android.ui.widget;

import android.content.Context;
import android.content.DialogInterface;
import android.text.Layout;
import android.text.Selection;
import android.text.Spannable;
import android.text.method.LinkMovementMethod;
import android.text.method.Touch;
import android.text.style.BackgroundColorSpan;
import android.text.style.ClickableSpan;
import android.text.style.URLSpan;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatTextView;

import com.xabber.android.R;
import com.xabber.android.ui.text.ClickSpan;

public class CorrectlyTouchEventTextView extends AppCompatTextView {
    boolean clickableSpanClicked;

    public CorrectlyTouchEventTextView(Context context) {
        super(context);
    }

    public CorrectlyTouchEventTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CorrectlyTouchEventTextView(Context context, AttributeSet attrs, int defStyle) {
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
            if (instance == null) instance = new LocalLinkMovementMethod();
            return instance;
        }

        private boolean onLinkSpannableTouched(
                TextView textView,
                Spannable buffer,
                MotionEvent event,
                ClickableSpan span,
                String url
        ) {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                AlertDialog alertDialog = new AlertDialog.Builder(textView.getContext()).create();
                alertDialog.setTitle(R.string.open_this_link);
                alertDialog.setMessage(url);

                alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, textView.getContext().getString(R.string.open),
                        (dialog, which) -> span.onClick(textView));

                alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, textView.getContext().getString(R.string.cancel),
                        (dialog, which) -> dialog.dismiss());

                alertDialog.show();
                removeUrlHighlightColor(textView);
            } else {
                Selection.setSelection(buffer, buffer.getSpanStart(span), buffer.getSpanEnd(span));
                highlightUrl(textView, span, buffer);
            }

            if (textView instanceof CorrectlyTouchEventTextView){
                ((CorrectlyTouchEventTextView) textView).clickableSpanClicked = true;
            }

            return true;
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

                final URLSpan[] link = buffer.getSpans(off, off, URLSpan.class);

                final ClickSpan[] referencedLinks = buffer.getSpans(off, off, ClickSpan.class);

                if (link.length != 0) {
                    return onLinkSpannableTouched(widget, buffer, event, link[0], link[0].getURL());
                } else if (referencedLinks.length != 0) {
                    return onLinkSpannableTouched(
                            widget, buffer, event, referencedLinks[0], referencedLinks[0].getUrl()
                    );
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
