package com.xabber.android.ui.widget;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.ui.adapter.chat.MessageVH;
import com.xabber.android.utils.Utils;

public class DateViewDecoration extends RecyclerView.ItemDecoration {

    private Paint paintFont;
    private Drawable drawable;

    private float dateViewXMargin;
    private float backgroundDrawableHeight = 65f;
    private float backgroundDrawableXPadding = 22f;
    private float backgroundDrawableYMargin = 10f;
    private float dateLayoutHeight = 2f * backgroundDrawableYMargin + backgroundDrawableHeight;

    private float stickyDrawableTopBound = 2f * backgroundDrawableYMargin;
    private float stickyDrawableBottomBound = dateLayoutHeight;

    public DateViewDecoration() {
        drawable = Application.getInstance().getResources()
                .getDrawable(SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.dark ?
                        R.drawable.rounded_background_grey_transparent_dark : R.drawable.rounded_background_grey_transparent);

        paintFont = new Paint();
        paintFont.setColor(Application.getInstance().getResources().getColor(R.color.white));
        paintFont.setTextSize(Utils.spToPxFloat(14f, Application.getInstance()) + 1f);
        paintFont.setTypeface(Typeface.DEFAULT_BOLD);
        paintFont.setAntiAlias(true);
    }

    @Override
    public void onDrawOver(@NonNull Canvas c, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        super.onDrawOver(c, parent, state);

        for (int i = 0; i < parent.getChildCount(); i++) {
            View child = parent.getChildAt(i);
            RecyclerView.ViewHolder holder = parent.getChildViewHolder(child);
            if (holder instanceof MessageVH) {
                if (((MessageVH)holder).needDate) {
                    if (i != 0) {
                        // Draw a Date view for all visible messages that require one.
                        // Since the position is != 0, these dates will not behave in the same way
                        // as the sticky date, they will simply be directly tied to the message.
                        drawDateHolderHeader(c, parent, child, (MessageVH)holder);
                    }
                }
            }
            if (i == 0) {
                if (holder instanceof MessageVH) {
                    if (parent.getChildCount() > 1) {
                        View child1 = parent.getChildAt(1);
                        RecyclerView.ViewHolder holder1 = parent.getChildViewHolder(child1);
                        if (holder1 instanceof MessageVH) {
                            // check if the date of appearing child(0) is the
                            // same as the date of the visible child(1)
                            if (((MessageVH) holder).date.equals(((MessageVH) holder1).date)) {
                                // if same, draw the date of the appearing child(0) as a sticky header date
                                drawDateStickyHeader(c, parent, child, (MessageVH)holder, true);
                                if (((MessageVH)holder1).needDate) {
                                    // and since we already got references for the next
                                    // holder, draw its' date if needed
                                    drawDateHolderHeader(c, parent, child1, (MessageVH)holder1);
                                }
                                i++;    // and increment index to skip calling getChildAt(1)
                                        // since we already did everything we needed to with it
                                continue;
                            }
                        }
                    }
                    // And if we didn't leave the first iteration of the loop,
                    // just draw the sticky date normally and calculate
                    // whether we need to draw it as sticky or not.
                    drawDateStickyHeader(c, parent, child, (MessageVH)holder, false);
                }
            }
        }
    }

    private void drawDateStickyHeader(Canvas c, RecyclerView parent, View child, MessageVH holder, boolean forceDrawAsSticky) {
        float width = measureText(paintFont, holder.date);

        dateViewXMargin = (parent.getMeasuredWidth() - width)/2;

        float drawableLeftBound;
        float drawableTopBound;
        float drawableRightBound;
        float drawableBottomBound;

        drawableLeftBound = dateViewXMargin - backgroundDrawableXPadding;
        drawableRightBound = dateViewXMargin + width + backgroundDrawableXPadding;

        // if bottom of the first view is less than the full size of date layout
        if (child.getBottom() - backgroundDrawableYMargin < stickyDrawableBottomBound && !forceDrawAsSticky) {
            //draw a moving sticky date that didn't reach the fixed position yet.
            drawableBottomBound = child.getBottom() - backgroundDrawableYMargin;
            drawableTopBound = child.getBottom() - dateLayoutHeight + backgroundDrawableYMargin;
        } else {
            //draw a normal sticky date with fixed position
            drawableBottomBound = stickyDrawableBottomBound;
            drawableTopBound = stickyDrawableTopBound;
        }

        drawable.setBounds((int)drawableLeftBound,
                (int)drawableTopBound,
                (int)drawableRightBound,
                (int)drawableBottomBound);

        drawable.draw(c);
        c.drawText(holder.date, dateViewXMargin, drawableBottomBound - 18f, paintFont);

        if (drawableBottomBound - child.getTop() > (backgroundDrawableHeight) / 2
                || drawableTopBound - child.getBottom() > (backgroundDrawableHeight) / 2) {

        }
    }

    private void drawDateHolderHeader(Canvas c, RecyclerView parent, View child, MessageVH holder) {
        float width = measureText(paintFont, holder.date);

        dateViewXMargin = (parent.getMeasuredWidth() - width)/2;

        float drawableLeftBound;
        float drawableTopBound;
        float drawableRightBound;
        float drawableBottomBound;

        drawableLeftBound = dateViewXMargin - backgroundDrawableXPadding;
        drawableRightBound = dateViewXMargin + width + backgroundDrawableXPadding;

        drawableTopBound = child.getTop() - backgroundDrawableHeight - backgroundDrawableYMargin;
        drawableBottomBound = child.getTop() - backgroundDrawableYMargin;

        if (drawableBottomBound < stickyDrawableBottomBound) {
            drawableBottomBound = stickyDrawableBottomBound;
            drawableTopBound = stickyDrawableTopBound;
        }

        drawable.setBounds((int)drawableLeftBound,
                (int)drawableTopBound,
                (int)drawableRightBound,
                (int)drawableBottomBound);

        drawable.draw(c);
        c.drawText(holder.date, dateViewXMargin, drawableBottomBound - 18f, paintFont);
    }

    private float measureText(Paint paint, CharSequence text, int start, int end) {
        return paint.measureText(text, start, end);
    }

    private float measureText(Paint paint, CharSequence text) {
        return measureText(paint, text, 0, text.length());
    }

    @Override
    public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        RecyclerView.ViewHolder holder = parent.getChildViewHolder(view);
        if (holder instanceof MessageVH && ((MessageVH) holder).needDate) {
            outRect.set(0, (int) dateLayoutHeight /*dateHeight*/, 0, 0);
        } else {
            outRect.setEmpty();
        }
    }
}
