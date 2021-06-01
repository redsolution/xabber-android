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
import com.xabber.android.ui.adapter.chat.MessageVH;
import com.xabber.android.utils.Utils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;


/**
 * ChatList Item Decoration responsible for drawing "Date" and "Unread Message"
 * headers directly above messages and/or over the chat itself.
 */
public class MessageHeaderViewDecoration extends RecyclerView.ItemDecoration {

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

    private int headerViewXMargin;
    private static final int backgroundDrawableHeight = Utils.dipToPx(24f, Application.getInstance());
    private static final int backgroundDrawableXPadding = Utils.dipToPx(8f, Application.getInstance());
    private static final int backgroundDrawableYMargin = Utils.dipToPx(3.64f, Application.getInstance());
    private static final int dateLayoutHeight = 2 * backgroundDrawableYMargin + backgroundDrawableHeight;
    private static final int alphaThreshold = dateLayoutHeight * 6 / 10;
    private static final int dateTextBaseline = backgroundDrawableHeight * 3 / 11;
    private static final String unread = Application.getInstance().getResources().getString(R.string.unread_messages);

    private int stickyDrawableTopBound = 2 * backgroundDrawableYMargin;
    private int stickyDrawableBottomBound = dateLayoutHeight;

    @DateState
    private int currentDateState;

    private boolean attached = false;

    private int alpha = 255;
    private long originTime;
    private long frameTime;

    public MessageHeaderViewDecoration() {
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

    // Adding a scroll listener, saving the RecyclerView reference
    // and setting current scroll state if possible.
    // Mainly needed for the transparency animation.
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
                if (holder instanceof MessageVH && ((MessageVH)holder).isUnread) {
                    drawUnreadMessageHeader(c, parent, child, (MessageVH) holder);
                }
            }
            if (i == 0) {
                i = measureFirstChildren(c, parent, child, (BasicMessageVH)holder, 0);
            }
        }
    }

    // A recursive check that measures whether we can draw the date as a sticky properly or not.
    // Since this check starts in a for loop, we return the iteration at which we stopped here to the main loop
    // To skip the iterations we already checked here.
    private int measureFirstChildren(Canvas c, RecyclerView parent, View originalChild, BasicMessageVH holder, int currentLoopIteration) {
        // Check if we need to draw an "Unread messages" header above originalChild message
        if (needToDrawUnreadHeader(holder)) {
            drawUnreadMessageHeader(c, parent, originalChild, (MessageVH) holder);
        }
        if (parent.getChildCount() > currentLoopIteration + 1) {
            View nextChild = parent.getChildAt(currentLoopIteration + 1);
            RecyclerView.ViewHolder nextHolder = parent.getChildViewHolder(nextChild);

            if (nextHolder instanceof BasicMessageVH) {
                // Check if the date of the originalChild is
                // the same as the date of the nextChild
                if (holder.date.equals(((BasicMessageVH) nextHolder).date)) {
                    // if same, make sure we have enough space to draw the sticky header
                    if (checkIfStickyHeaderFitsAboveNextChild(nextChild)) {
                        drawDateStickyHeader(c, parent, originalChild, holder, true);

                        // We only try to examine nextChild for unread message header if we will not
                        // recursively call measureFirstChild again, to avoid a double check
                        // (as nextChild in the first loop, and as originalChild in the next one).
                        if (needToDrawUnreadHeader((BasicMessageVH) nextHolder)) {
                            drawUnreadMessageHeader(c, parent, nextChild, (MessageVH) nextHolder);
                        }

                        // after drawing it, leave the recursive call with the current loop + 1,
                        // since we checked both the originalChild (currentLoopIteration)
                        // and nextChild (currentLoopIteration + 1)
                        return currentLoopIteration + 1;
                    } else {
                        // Since sticky doesn't fit, we can't do much at this loop.
                        // Just return the call to the method, while iterating the current loop by 1
                        return measureFirstChildren(c, parent, nextChild, (BasicMessageVH) nextHolder, currentLoopIteration + 1);
                    }
                } else {
                    // If the dates are different, then that means that the next child
                    // is the first message with a different date, i.e. it needs a date header.
                    drawDateMessageHeader(c, parent, nextChild, (BasicMessageVH)nextHolder);
                    // We did what we needed with nextChild, so we bump the loop iteration by 1,
                    // but since we didn't do anything with the originalChild, we can't return yet.

                    // Same as above nextHolder Unread check. Just avoiding repeated same checks.
                    if (needToDrawUnreadHeader((BasicMessageVH) nextHolder)) {
                        drawUnreadMessageHeader(c, parent, nextChild, (MessageVH) nextHolder);
                    }

                    currentLoopIteration++;
                }
            }
        }
        // Here we draw the sticky date that isn't forced to be drawn at the same position.
        // Either when we ran out of items to check, or the nextChild is of a different date than originalChild.
        drawDateStickyHeader(c, parent, originalChild, holder, false);
        return currentLoopIteration;
    }

    // Check if the sticky date of child(0) will be above the lower bound of child(1)
    // This is important if we have 2 message views with a small height with the same date.
    private boolean checkIfStickyHeaderFitsAboveNextChild(View nextChild) {
        return nextChild.getBottom() > stickyDrawableBottomBound + backgroundDrawableYMargin;
    }

    // Check if we should make the date view disappear or not
    private boolean shouldAnimateAlpha(Rect date, int messageTopBound) {
        // The bigger alphaThreshold constant is, the bigger the area that date
        // has to occupy within the bounds of message to start disappearing
        return date.bottom - messageTopBound > alphaThreshold;
    }

    private boolean needToDrawUnreadHeader(BasicMessageVH holder) {
        return holder instanceof MessageVH && ((MessageVH)holder).isUnread;
    }

    // Draws a date that appears at the top of chat window, either as a sticky date
    // that stays in one place, or a date of the partially visible message
    private void drawDateStickyHeader(Canvas c, RecyclerView parent, View child, BasicMessageVH holder, boolean forceDrawAsSticky) {
        int width = measureText(paintFont, holder.date);

        headerViewXMargin = (parent.getMeasuredWidth() - width)/2;

        Rect drawableBounds = new Rect();

        drawableBounds.left = headerViewXMargin - backgroundDrawableXPadding;
        drawableBounds.right = headerViewXMargin + width + backgroundDrawableXPadding;

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

                if (shouldAnimateAlpha(drawableBounds, childTopBound)) {
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

        drawString(c, holder.date, drawableBounds, alpha);
    }

    // Draws a date that appears on top of the first message of the day.
    // This date nearly always stays directly tied to the message position.
    private void drawDateMessageHeader(Canvas c, RecyclerView parent, View child, BasicMessageVH holder) {
        int width = measureText(paintFont, holder.date);
        // additional vertical offset for the Date header.
        int additionalOffset = 0;

        headerViewXMargin = (parent.getMeasuredWidth() - width)/2;

        Rect drawableBounds = new Rect();

        if(needToDrawUnreadHeader(holder)) {
            additionalOffset = dateLayoutHeight;
        }

        drawableBounds.left = headerViewXMargin - backgroundDrawableXPadding;
        drawableBounds.right = headerViewXMargin + width + backgroundDrawableXPadding;

        drawableBounds.bottom = child.getTop() - backgroundDrawableYMargin - additionalOffset;

        // Check if the background drawable's vertical position is closer to the top than
        // the position of the sticky drawable.
        // This happens because the sticky position has a doubled top margin and no bottom margin.
        if (drawableBounds.bottom < stickyDrawableBottomBound) {
            // If it's position is closer to the top, then make sure that it is drawn as a sticky date.
            drawableBounds.bottom = stickyDrawableBottomBound;
            drawableBounds.top = stickyDrawableTopBound;
        } else {
            drawableBounds.top = child.getTop() - backgroundDrawableHeight - backgroundDrawableYMargin - additionalOffset;
        }

        drawString(c, holder.date, drawableBounds, 255);
    }

    private void drawUnreadMessageHeader(Canvas c, RecyclerView parent, View child, MessageVH holder) {
        int width = measureText(paintFont, unread);
        int alpha;

        headerViewXMargin = (parent.getMeasuredWidth() - width)/2;

        Rect drawableBounds = new Rect();

        drawableBounds.left = headerViewXMargin - backgroundDrawableXPadding;
        drawableBounds.right = headerViewXMargin + width + backgroundDrawableXPadding;

        drawableBounds.top = child.getTop() - backgroundDrawableHeight - backgroundDrawableYMargin;
        drawableBounds.bottom = child.getTop() - backgroundDrawableYMargin;

        // if top of unread is too close to the top, we
        // change alpha depending on how close it is to the top
        if (drawableBounds.top > stickyDrawableBottomBound + (backgroundDrawableHeight / 2)) {
            alpha = 255;
        } else {
            alpha = (drawableBounds.top - stickyDrawableBottomBound) * 255 / (backgroundDrawableHeight / 2);
            if (alpha <= 0) {
                alpha = 0;
            } else if (alpha > 255){
                alpha = 255;
            }
        }

        drawString(c, unread, drawableBounds, alpha);
    }

    // Drawing the date itself with provided parameters
    private void drawString(Canvas c, String string, Rect bounds, int alpha) {
        paintFont.setAlpha(alpha);
        drawable.setAlpha(alpha);

        drawable.setBounds(bounds);

        drawable.draw(c);
        c.drawText(string, headerViewXMargin, bounds.bottom - dateTextBaseline, paintFont);
    }

    private int measureText(Paint paint, CharSequence text, int start, int end) {
        return (int) paint.measureText(text, start, end);
    }

    private int measureText(Paint paint, CharSequence text) {
        return measureText(paint, text, 0, text.length());
    }

    // Setting the additional top offset for the views that
    // require some header decoration to be attached above the message
    @Override
    public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        RecyclerView.ViewHolder holder = parent.getChildViewHolder(view);
        int topOffset = 0;
        if (holder instanceof BasicMessageVH && ((BasicMessageVH) holder).needDate) {
            topOffset += dateLayoutHeight;
        }
        if (holder instanceof MessageVH && ((MessageVH)holder).isUnread) {
            topOffset += dateLayoutHeight;
        }
        outRect.set(0, topOffset, 0, 0);
    }
}
