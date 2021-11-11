package com.xabber.android.ui.widget

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import com.xabber.android.R
import com.xabber.android.data.log.LogManager

class CustomFlexboxLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
): FrameLayout(context, attrs) {

    private var viewPartMain: TextView? = null
    private var viewPartSlave: View? = null

    private var a: TypedArray = context.obtainStyledAttributes(attrs, R.styleable.CustomFlexboxLayout, 0, 0)

    private var viewPartSlaveWidth = 0
    private var viewPartSlaveHeight = 0

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        try {
            viewPartMain =
                findViewById(a.getResourceId(R.styleable.CustomFlexboxLayout_viewPartMain, -1))
            viewPartSlave =
                findViewById(a.getResourceId(R.styleable.CustomFlexboxLayout_viewPartSlave, -1))
        } catch (e: Exception) {
            LogManager.exception(javaClass.simpleName, e)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        var widthSize: Int = MeasureSpec.getSize(widthMeasureSpec)
        var heightSize: Int = MeasureSpec.getSize(heightMeasureSpec)

        if (viewPartMain == null || viewPartSlave == null || widthSize <= 0) {
            return
        }

        val availableWidth = widthSize - paddingLeft - paddingRight
        val viewPartMainLayoutParams = viewPartMain?.layoutParams as? LayoutParams ?: return

        val viewPartMainWidth =
            viewPartMain!!.measuredWidth + viewPartMainLayoutParams.leftMargin + viewPartMainLayoutParams.rightMargin

        val viewPartMainHeight =
            viewPartMain!!.measuredHeight + viewPartMainLayoutParams.topMargin + viewPartMainLayoutParams.bottomMargin

        val viewPartSlaveLayoutParams = viewPartSlave?.layoutParams as? LayoutParams ?: return

        viewPartSlaveWidth =
            viewPartSlave!!.measuredWidth + viewPartSlaveLayoutParams.leftMargin + viewPartSlaveLayoutParams.rightMargin

        viewPartSlaveHeight =
            viewPartSlave!!.measuredHeight + viewPartSlaveLayoutParams.topMargin + viewPartSlaveLayoutParams.bottomMargin

        val viewPartMainLineCount = viewPartMain!!.lineCount

        val viewPartMainLastLineWidth: Float =
            if (viewPartMainLineCount > 0) {
                val lineWidth = viewPartMain!!.layout.getLineWidth(viewPartMainLineCount - 1).toInt()
                val rightMargin = viewPartMainLayoutParams.rightMargin.toFloat()
                lineWidth + rightMargin
            } else {
                0f
            }

        widthSize = paddingLeft + paddingRight
        heightSize = paddingTop + paddingBottom

        when {
            viewPartMainLastLineWidth + viewPartSlaveWidth > availableWidth -> {
                widthSize += viewPartMainWidth
                heightSize += viewPartMainHeight + viewPartSlaveHeight
            }
            viewPartMainWidth >= viewPartMainLastLineWidth + viewPartSlaveWidth -> {
                widthSize += viewPartMainWidth
                heightSize += viewPartMainHeight
            }
            else -> {
                widthSize += (viewPartMainLastLineWidth + viewPartSlaveWidth).toInt()
                heightSize += viewPartMainHeight
            }
        }

        setMeasuredDimension(widthSize, heightSize)
        super.onMeasure(
            MeasureSpec.makeMeasureSpec(widthSize, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(heightSize, MeasureSpec.EXACTLY)
        )
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        viewPartMain?.let {
            it.layout(
                paddingLeft,
                paddingTop,
                it.width + paddingLeft,
                it.height + paddingTop
            )
        }
        viewPartSlave?.layout(
            right - left - viewPartSlaveWidth - paddingRight,
            bottom - top - paddingBottom - viewPartSlaveHeight,
            right - left - paddingRight,
            bottom - top - paddingBottom
        )
    }
}