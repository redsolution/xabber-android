package com.xabber.android.ui.widget;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class IntroViewDecoration extends RecyclerView.ItemDecoration {

    private View introView;
    private int distanceFromMessage = 60;
    private int offsetFromTop = 10;


    public IntroViewDecoration(View introView) {
        this.introView = introView;
    }

    @Override
    public void onDraw(@NonNull Canvas c, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        super.onDraw(c, parent, state);

        int dx = parent.getMeasuredWidth() / 10;

        introView.layout(parent.getLeft() + dx, 0, parent.getRight() - dx, introView.getMeasuredHeight());
        View firstItem = parent.getChildAt(0);
        if (parent.getChildAdapterPosition(firstItem) == 0) {
            c.save();
            int height = introView.getMeasuredHeight();

            int centerY = parent.getMeasuredHeight() / 2;
            int introRadius = height / 2;
            int dy;
            if (firstItem.getTop() > centerY + introRadius) {
                dy = centerY - introRadius;
            } else {
                dy = firstItem.getTop() - height - distanceFromMessage + offsetFromTop;
            }

            c.translate(dx, dy);
            introView.draw(c);
            c.restore();
        }
    }

    @Override
    public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        if (parent.getChildAdapterPosition(view) == 0) {
            introView.measure(View.MeasureSpec.makeMeasureSpec(parent.getMeasuredWidth() * 8 / 10, View.MeasureSpec.AT_MOST),
                    View.MeasureSpec.makeMeasureSpec(parent.getMeasuredHeight(), View.MeasureSpec.AT_MOST));
            outRect.set(0, introView.getMeasuredHeight() + distanceFromMessage, 0, 0);
        } else {
            outRect.setEmpty();
        }
    }
}
