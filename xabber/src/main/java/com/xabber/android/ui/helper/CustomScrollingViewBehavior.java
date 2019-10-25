package com.xabber.android.ui.helper;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import androidx.coordinatorlayout.widget.CoordinatorLayout;

import com.google.android.material.appbar.AppBarLayout;

public class CustomScrollingViewBehavior extends AppBarLayout.ScrollingViewBehavior {
    public CustomScrollingViewBehavior() {
    }

    public CustomScrollingViewBehavior(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onRequestChildRectangleOnScreen(CoordinatorLayout parent, View child, Rect rectangle, boolean immediate) {
     return true;
    }
}
