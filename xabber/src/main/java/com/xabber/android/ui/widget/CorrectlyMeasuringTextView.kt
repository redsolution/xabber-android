package com.xabber.android.ui.widget

import android.content.Context
import android.text.Spannable
import android.util.AttributeSet
import com.xabber.android.ui.text.CustomQuoteSpan
import kotlin.math.roundToInt

class CorrectlyMeasuringTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : CorrectlyTouchEventTextView(context, attrs, defStyleAttr) {

    public override fun onMeasure(wms: Int, hms: Int) {
        try {
            val l = layout
            val text = text

            if (l.lineCount <= 1) {
                super.onMeasure(wms, hms)
                return
            }

            val quoteOffset = text.quoteSpanOffsetOrNull ?: 0

            val maxLineWidth =  (0 until l.lineCount).map {
                l.paint.measureText(
                    text, l.getLineStart(it), l.getLineEnd(it)
                ).roundToInt() + quoteOffset
            }.maxOfOrNull { it } ?: 0

            val width = (maxLineWidth + paddingLeft + paddingRight).coerceAtMost(measuredWidth)

            super.onMeasure(width or MeasureSpec.EXACTLY, measuredHeight or MeasureSpec.EXACTLY)
        } catch (ignore: Exception) {
            super.onMeasure(wms, hms)
        }
    }

    private val CharSequence.quoteSpanOffsetOrNull: Int?
    get() = (this as? Spannable)?.getSpans(0, text.length, CustomQuoteSpan::class.java)
            ?.takeIf { it.isNotEmpty() }
            ?.get(0)
            ?.getLeadingMargin(false)

}