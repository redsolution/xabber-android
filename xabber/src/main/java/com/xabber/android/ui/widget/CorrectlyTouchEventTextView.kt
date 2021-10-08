package com.xabber.android.ui.widget

import android.content.Context
import android.content.DialogInterface
import android.os.Build
import android.text.Selection
import android.text.Spannable
import android.text.method.LinkMovementMethod
import android.text.method.Touch
import android.text.style.BackgroundColorSpan
import android.text.style.ClickableSpan
import android.text.style.URLSpan
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatTextView
import com.xabber.android.R
import com.xabber.android.ui.text.ClickSpan

open class CorrectlyTouchEventTextView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyle: Int = 0
): AppCompatTextView(context, attrs, defStyle) {

    var clickableSpanClicked = false

    override fun onTouchEvent(event: MotionEvent): Boolean {
        clickableSpanClicked = false
        super.onTouchEvent(event)
        return clickableSpanClicked
    }

    object LocalLinkMovementMethod : LinkMovementMethod() {

        private var isUrlHighlighted = false

        private fun onLinkSpannableTouched(
            textView: TextView,
            buffer: Spannable,
            event: MotionEvent,
            span: ClickableSpan,
            url: String
        ): Boolean {
            if (event.action == MotionEvent.ACTION_UP) {
                AlertDialog.Builder(textView.context).create().apply {
                    setTitle(R.string.open_this_link)
                    setMessage(url)

                    setButton(
                        DialogInterface.BUTTON_POSITIVE, textView.context.getString(R.string.open)
                    ) { _: DialogInterface?, _: Int -> span.onClick(textView) }

                    setButton(
                        DialogInterface.BUTTON_NEGATIVE, textView.context.getString(R.string.cancel)
                    ) { dialog: DialogInterface, _: Int -> dialog.dismiss() }

                }.show()

                removeUrlHighlightColor(textView)
            } else {
                Selection.setSelection(buffer, buffer.getSpanStart(span), buffer.getSpanEnd(span))
                highlightUrl(textView, span, buffer)
            }

            (textView as? CorrectlyTouchEventTextView)?.clickableSpanClicked = true

            return true
        }

        override fun onTouchEvent(
            widget: TextView, buffer: Spannable, event: MotionEvent
        ): Boolean {
            val action = event.action
            if (action != MotionEvent.ACTION_UP && action != MotionEvent.ACTION_DOWN) {
                Touch.onTouchEvent(widget, buffer, event)
            }
            val x = event.x.toInt() - widget.totalPaddingLeft + widget.scrollX
            val y = event.y.toInt() - widget.totalPaddingTop + widget.scrollY
            val layout = widget.layout
            val line = layout.getLineForVertical(y)
            val off = layout.getOffsetForHorizontal(line, x.toFloat())

            buffer.getSpans(off, off, URLSpan::class.java)
                .takeIf { it.isNotEmpty() }
                ?.let { return onLinkSpannableTouched(widget, buffer, event, it[0], it[0].url) }

            buffer.getSpans(off, off, ClickSpan::class.java)
                .takeIf { it.isNotEmpty() }
                ?.let { return onLinkSpannableTouched(widget, buffer, event, it[0], it[0].url) }

            Selection.removeSelection(buffer)
            Touch.onTouchEvent(widget, buffer, event)
            return false
        }

        private fun highlightUrl(
            textView: TextView, clickableSpan: ClickableSpan?, text: Spannable
        ) {
            if (isUrlHighlighted) {
                return
            }
            isUrlHighlighted = true
            val spanStart = text.getSpanStart(clickableSpan)
            val spanEnd = text.getSpanEnd(clickableSpan)
            val highlightSpan: BackgroundColorSpan =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    BackgroundColorSpan(textView.highlightColor)
                } else {
                    BackgroundColorSpan(textView.linkTextColors.defaultColor)
                }

            text.setSpan(highlightSpan, spanStart, spanEnd, Spannable.SPAN_INCLUSIVE_INCLUSIVE)
            textView.setTag(R.id.clickable_span_highlight_background, highlightSpan)
            Selection.setSelection(text, spanStart, spanEnd)
        }

        private fun removeUrlHighlightColor(textView: TextView) {
            if (!isUrlHighlighted) {
                return
            }
            isUrlHighlighted = false
            val text = textView.text as Spannable
            val highlightSpan =
                textView.getTag(R.id.clickable_span_highlight_background) as BackgroundColorSpan
            text.removeSpan(highlightSpan)
            Selection.removeSelection(text)
        }

    }
}