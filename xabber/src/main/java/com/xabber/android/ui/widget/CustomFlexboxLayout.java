package com.xabber.android.ui.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.xabber.android.R;
import com.xabber.android.data.log.LogManager;

public class CustomFlexboxLayout extends RelativeLayout {

    private TextView viewPartMain;
    private View viewPartSlave;

    private TypedArray a;

    private int viewPartSlaveWidth;
    private int viewPartSlaveHeight;

    public CustomFlexboxLayout(Context context) {
        super(context);
    }

    public CustomFlexboxLayout(Context context, AttributeSet attrs) {
        super(context, attrs);

        a = context.obtainStyledAttributes(attrs, R.styleable.CustomFlexboxLayout, 0, 0);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        try {
            viewPartMain = this.findViewById(a.getResourceId(R.styleable.CustomFlexboxLayout_viewPartMain, -1));
            viewPartSlave = this.findViewById(a.getResourceId(R.styleable.CustomFlexboxLayout_viewPartSlave, -1));
        } catch (Exception e) {
            LogManager.exception(getClass().getSimpleName(), e);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        if (viewPartMain == null || viewPartSlave == null || widthSize <= 0) {
            return;
        }

        int availableWidth = widthSize - getPaddingLeft() - getPaddingRight();

        LayoutParams viewPartMainLayoutParams = (LayoutParams) viewPartMain.getLayoutParams();

        int viewPartMainWidth = viewPartMain.getMeasuredWidth()
                + viewPartMainLayoutParams.leftMargin
                + viewPartMainLayoutParams.rightMargin;

        int viewPartMainHeight = viewPartMain.getMeasuredHeight()
                + viewPartMainLayoutParams.topMargin
                + viewPartMainLayoutParams.bottomMargin;

        LayoutParams viewPartSlaveLayoutParams = (LayoutParams) viewPartSlave.getLayoutParams();

        viewPartSlaveWidth = viewPartSlave.getMeasuredWidth()
                + viewPartSlaveLayoutParams.leftMargin
                + viewPartSlaveLayoutParams.rightMargin;

        viewPartSlaveHeight = viewPartSlave.getMeasuredHeight()
                + viewPartSlaveLayoutParams.topMargin
                + viewPartSlaveLayoutParams.bottomMargin;

        int viewPartMainLineCount = viewPartMain.getLineCount();
        float viewPartMainLastLineWidth;
        if (viewPartMainLineCount > 0) {
            int lineWidth = (int) viewPartMain.getLayout().getLineWidth(viewPartMainLineCount - 1);
            float rightMargin = viewPartMainLayoutParams.rightMargin;
            viewPartMainLastLineWidth = lineWidth + rightMargin;
        } else {
            viewPartMainLastLineWidth = 0;
        }

        widthSize = getPaddingLeft() + getPaddingRight();
        heightSize = getPaddingTop() + getPaddingBottom();

        if (viewPartMainLastLineWidth + viewPartSlaveWidth > availableWidth) {
            widthSize += viewPartMainWidth;
            heightSize += viewPartMainHeight + viewPartSlaveHeight;
        } else if (viewPartMainWidth >= viewPartMainLastLineWidth + viewPartSlaveWidth) {
            widthSize += viewPartMainWidth;
            heightSize += viewPartMainHeight;
        } else {
            widthSize += viewPartMainLastLineWidth + viewPartSlaveWidth;
            heightSize += viewPartMainHeight;
        }

        this.setMeasuredDimension(widthSize, heightSize);
        super.onMeasure(
                MeasureSpec.makeMeasureSpec(widthSize, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(heightSize, MeasureSpec.EXACTLY)
        );
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        if (viewPartMain == null || viewPartSlave == null) {
            return;
        }

        viewPartMain.layout(
                getPaddingLeft(),
                getPaddingTop(),
                viewPartMain.getWidth() + getPaddingLeft(),
                viewPartMain.getHeight() + getPaddingTop()
        );

        viewPartSlave.layout(
                right - left - viewPartSlaveWidth - getPaddingRight(),
                bottom - top - getPaddingBottom() - viewPartSlaveHeight,
                right - left - getPaddingRight(),
                bottom - top - getPaddingBottom()
        );
    }

}
