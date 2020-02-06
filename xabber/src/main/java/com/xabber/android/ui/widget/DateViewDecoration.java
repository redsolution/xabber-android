package com.xabber.android.ui.widget;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.view.View;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.ui.adapter.chat.BasicMessageVH;
import com.xabber.android.utils.Utils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class DateViewDecoration extends RecyclerView.ItemDecoration {

    @IntDef({DateState.SCROLL_ACTIVE, DateState.SCROLL_IDLE,
            DateState.SCROLL_IDLE_NO_ANIMATION, DateState.INITIATED_ANIMATION,
            DateState.ANIMATING, DateState.FINISHED_ANIMATING})
    @Retention(RetentionPolicy.SOURCE)
    @interface DateState {
        int SCROLL_ACTIVE = 0;
        int SCROLL_IDLE = 1;
        int SCROLL_IDLE_NO_ANIMATION = 2;
        int INITIATED_ANIMATION = 3;
        int ANIMATING = 4;
        int FINISHED_ANIMATING = 5;
    }

    private Paint paintFont;
    private Drawable drawable;
    private Handler handler;
    private RecyclerView parent;

    private int dateViewXMargin;
    private int backgroundDrawableHeight = 65;
    private int backgroundDrawableXPadding = 22;
    private int backgroundDrawableYMargin = 10;
    private int dateLayoutHeight = 2 * backgroundDrawableYMargin + backgroundDrawableHeight;

    private int stickyDrawableTopBound = 2 * backgroundDrawableYMargin;
    private int stickyDrawableBottomBound = dateLayoutHeight;

    @DateState
    private int currentDateState;

    private boolean attached = false;

    private int alpha = 255;
    private long originTime;
    private long frameTime;

    public DateViewDecoration() {
        drawable = Application.getInstance().getResources()
                .getDrawable(SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.dark ?
                        R.drawable.rounded_background_grey_transparent_dark : R.drawable.rounded_background_grey_transparent);

        paintFont = new Paint();
        paintFont.setColor(Application.getInstance().getResources().getColor(R.color.white));
        paintFont.setTextSize(Utils.spToPxFloat(14f, Application.getInstance()) + 1f);
        paintFont.setTypeface(Typeface.DEFAULT_BOLD);
        paintFont.setAntiAlias(true);

        handler = new Handler();
    }

    private RecyclerView.OnScrollListener scrollListener = new RecyclerView.OnScrollListener() {
        @Override
        public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
            super.onScrollStateChanged(recyclerView, newState);
            switch (newState) {
                case RecyclerView.SCROLL_STATE_IDLE:
                    currentDateState = DateState.SCROLL_IDLE;

                    recyclerView.invalidate();
                    break;
                case RecyclerView.SCROLL_STATE_SETTLING:
                case RecyclerView.SCROLL_STATE_DRAGGING:
                    currentDateState = DateState.SCROLL_ACTIVE;

                    alpha = 255;
                    handler.removeCallbacks(runAlphaAnimation);
                    break;
            }
        }
    };

    private Runnable runAlphaAnimation = new Runnable() {
        @Override
        public void run() {
            currentDateState = DateState.ANIMATING;
            parent.invalidate();
        }
    };

    private void attachRecyclerViewData(RecyclerView parent) {
        parent.removeOnScrollListener(scrollListener);
        attached = true;
        parent.addOnScrollListener(scrollListener);
        this.parent = parent;
        if (parent.getScrollState() == RecyclerView.SCROLL_STATE_IDLE) {
            currentDateState = DateState.SCROLL_IDLE;
        }
    }

    @Override
    public void onDrawOver(@NonNull Canvas c, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        super.onDrawOver(c, parent, state);

        if (!attached)
            attachRecyclerViewData(parent);

        for (int i = 0; i < parent.getChildCount(); i++) {
            View child = parent.getChildAt(i);
            RecyclerView.ViewHolder holder = parent.getChildViewHolder(child);
            if (i != 0) {
                if (holder instanceof BasicMessageVH && ((BasicMessageVH)holder).needDate) {
                    // Draw a Date view for all visible messages that require one.
                    // Since the position is != 0, these dates will not behave in the same way
                    // as the sticky date, they will simply be directly tied to the message.
                    drawDateMessageHeader(c, parent, child, (BasicMessageVH) holder);
                }
            }
            if (i == 0) {
                if (holder instanceof BasicMessageVH) {
                    if (parent.getChildCount() > 1) {
                        View child1 = parent.getChildAt(1);
                        RecyclerView.ViewHolder holder1 = parent.getChildViewHolder(child1);
                        if (holder1 instanceof BasicMessageVH) {
                            // check if the date of appearing child(0) is the
                            // same as the date of the visible child(1)
                            if (((BasicMessageVH) holder).date.equals(((BasicMessageVH) holder1).date)) {
                                // if same, make sure we have enough space to draw the sticky header
                                if (checkIfStickyHeaderFitsAboveNextChild(child1))
                                    drawDateStickyHeader(c, parent, child, (BasicMessageVH)holder, true);
                                else {
                                    // if not, draw the header from the point of view of the next child and let it
                                    // calculate whether or not that child can show it as a sticky header or not.
                                    drawDateStickyHeader(c, parent, child1, (BasicMessageVH)holder1, false);
                                }
                                // this also means that there's no need in showing
                                // the date for the child(1), so we can skip it

                                i++;        // increment index to skip calling getChildAt(1)
                                continue;   // and skip the rest of the loop.
                            } else {
                                // If the dates are different, then that means that the child(1) is the
                                // first message with the different date,
                                // which means that we have to draw its' date
                                drawDateMessageHeader(c, parent, child1, (BasicMessageVH)holder1);
                                i++;
                                // we don't skip the loop here because in this
                                // else statement we still haven't drawn child(0)'s date
                            }
                        }
                    }
                    // And if we didn't leave the first iteration of the loop,
                    // just draw the sticky date normally and calculate
                    // whether we need to draw it as sticky or not.
                    drawDateStickyHeader(c, parent, child, (BasicMessageVH)holder, false);
                }
            }
        }
    }

    // Check if the sticky date of child(0) will be above the lower bound of child(1)
    // This is important if we have only 2 message views with a small height with the same date.
    private boolean checkIfStickyHeaderFitsAboveNextChild(View child1) {
        return child1.getBottom() > stickyDrawableBottomBound + backgroundDrawableYMargin;
    }

    // Draws a date that appears at the top of chat window, either as a sticky date
    // that stays in one place, or a date of the partially visible message
    private void drawDateStickyHeader(Canvas c, RecyclerView parent, View child, BasicMessageVH holder, boolean forceDrawAsSticky) {
        int width = measureText(paintFont, holder.date);

        dateViewXMargin = (parent.getMeasuredWidth() - width)/2;

        Rect drawableBounds = new Rect();

        drawableBounds.left = dateViewXMargin - backgroundDrawableXPadding;
        drawableBounds.right = dateViewXMargin + width + backgroundDrawableXPadding;

        // Check to see if the bottom of the first view is less than the full size of date layout.
        //
        // ** We add the margin to try and compensate for the message bubble's own
        // ** margins and make the date look like it is within the bounds of the message's background drawable,
        // ** since the visible border of the message background drawable and the View border are visibly different
        if (!forceDrawAsSticky && child.getBottom() < stickyDrawableBottomBound + backgroundDrawableYMargin) {
            //draw a moving date that didn't reach the sticky position yet.
            drawableBounds.bottom = child.getBottom() - backgroundDrawableYMargin;
            drawableBounds.top = child.getBottom() - dateLayoutHeight + backgroundDrawableYMargin;
        } else {
            //draw a normal sticky date with fixed position
            drawableBounds.bottom = stickyDrawableBottomBound;
            drawableBounds.top = stickyDrawableTopBound;
        }

        switch (currentDateState) {
            case DateState.SCROLL_IDLE:
                // check if the date covers message bounds
                // if so, start a runnable that sets currentDateState to
                // DateState.INITIATED_ANIMATION and post
                // a delayed runnable to the handler
                // if not, set state to SCROLL_IDLE_NO_ANIMATION;
                int childTopBound = child.getTop();

                if (drawableBounds.bottom - childTopBound > dateLayoutHeight / 2) {
                    handler.postDelayed(runAlphaAnimation, 500);
                    currentDateState = DateState.INITIATED_ANIMATION;
                } else {
                    currentDateState = DateState.SCROLL_IDLE_NO_ANIMATION;
                }
                break;
            case DateState.SCROLL_IDLE_NO_ANIMATION:
                // Don't start any alpha manipulations and proceed as usual.
                // This state exists just to skip SCROLL_IDLE recalculating the
                // same position too many times during possible redraws.
                // (Although there wouldn't be too many redraws of the item anyway)
                break;
            case DateState.INITIATED_ANIMATION:
                // do not do anything concerning animation,
                // wait until state changes to active animation
                break;
            case DateState.ANIMATING:
                // drawing the current frame of alpha transition
                if (alpha == 255) {
                    originTime = System.currentTimeMillis();
                    alpha -= 17;
                } else {
                    frameTime = System.currentTimeMillis() - originTime;
                    originTime = System.currentTimeMillis();
                    if (frameTime <= 0) {
                        frameTime = 17;
                    }
                }
                alpha -= frameTime;
                if (alpha <= 0) {
                    alpha = 0;
                    currentDateState = DateState.FINISHED_ANIMATING;
                } else {
                    handler.postDelayed(runAlphaAnimation, 17);
                }
                break;
            case DateState.FINISHED_ANIMATING:
                // set alpha = 0 in case of future redraws,
                // no more invalidation calls.
                alpha = 0;
                break;
            case DateState.SCROLL_ACTIVE:
                // reset alpha back to 255, handler is cleared in the scroll state listener.
                alpha = 255;
                break;
        }

        drawDate(c, holder.date, drawableBounds, alpha);
    }

    // Draws a date that appears on top of the first message of the day.
    // This date nearly always stays directly tied to the message position.
    private void drawDateMessageHeader(Canvas c, RecyclerView parent, View child, BasicMessageVH holder) {
        int width = measureText(paintFont, holder.date);

        dateViewXMargin = (parent.getMeasuredWidth() - width)/2;

        Rect drawableBounds = new Rect();

        drawableBounds.left = dateViewXMargin - backgroundDrawableXPadding;
        drawableBounds.right = dateViewXMargin + width + backgroundDrawableXPadding;

        drawableBounds.bottom = child.getTop() - backgroundDrawableYMargin;

        // Check if the background drawable's vertical position is closer to the top than
        // the position of the sticky drawable.
        // This happens because the sticky position has a doubled top margin and no bottom margin.
        if (drawableBounds.bottom < stickyDrawableBottomBound) {
            // If it's position is closer to the top, then make sure that it is drawn as a sticky date.
            drawableBounds.bottom = stickyDrawableBottomBound;
            drawableBounds.top = stickyDrawableTopBound;
        } else {
            drawableBounds.top = child.getTop() - backgroundDrawableHeight - backgroundDrawableYMargin;
        }

        drawDate(c, holder.date, drawableBounds, 255);
    }

    // Drawing the date itself with provided parameters
    private void drawDate(Canvas c, String date, Rect bounds, int alpha) {
        paintFont.setAlpha(alpha);
        drawable.setAlpha(alpha);

        drawable.setBounds(bounds);

        drawable.draw(c);
        c.drawText(date, dateViewXMargin, bounds.bottom - 18f, paintFont);

    }

    private int measureText(Paint paint, CharSequence text, int start, int end) {
        return (int) paint.measureText(text, start, end);
    }

    private int measureText(Paint paint, CharSequence text) {
        return measureText(paint, text, 0, text.length());
    }

    // Setting the additional top offset for the views that
    // require a Date decoration to be attached above the message
    @Override
    public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        RecyclerView.ViewHolder holder = parent.getChildViewHolder(view);
        if (holder instanceof BasicMessageVH && ((BasicMessageVH) holder).needDate) {
            outRect.set(0, dateLayoutHeight, 0, 0);
        } else {
            outRect.setEmpty();
        }
    }
}
