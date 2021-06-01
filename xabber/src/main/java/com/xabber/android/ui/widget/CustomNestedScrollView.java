package com.xabber.android.ui.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;


//Custom Scroll View that stops Child Element's padding from blocking
//Touch Events targeted to Views under the Scroll View
public class CustomNestedScrollView extends NestedScrollView {
    private boolean topPadding = false;

    public CustomNestedScrollView (@NonNull Context context) {
        this(context,null);
    }

    public CustomNestedScrollView (@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context,attrs,0);
    }

    public CustomNestedScrollView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }


    @Override
    public boolean onTouchEvent (MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            topPadding = (getChildAt(0).getPaddingTop() - getScrollY() > ev.getY());
        }
        if (topPadding) {
            if (ev.getAction() == MotionEvent.ACTION_UP)
                topPadding = false;
            return false;
        }
        return super.onTouchEvent(ev);
    }
}
