package com.xabber.android.ui.widget;

import android.annotation.SuppressLint;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.utils.Utils;

import static androidx.recyclerview.widget.ItemTouchHelper.ACTION_STATE_SWIPE;

public class ReplySwipeCallback extends ItemTouchHelper.Callback implements View.OnTouchListener {

    private SwipeAction swipeListener;
    private ReplyArrowState currentReplyArrowState = ReplyArrowState.GONE;

    private static final Drawable replyIcon = Application.getInstance().getResources().getDrawable(R.drawable.ic_reply);
    private static final int fullSize = Utils.dipToPx(24f, Application.getInstance());
    private static int currentSize;
    private static final int paddingRight = Utils.dipToPx(12f, Application.getInstance());
    private static final float MAX_SWIPE_DISTANCE_RATIO = 0.18f;
    private static final float ACTIVE_SWIPE_DISTANCE_RATIO = 0.14f;

    private RecyclerView.ViewHolder currentItemViewHolder = null;
    private boolean touchListenerIsSet = false;
    private boolean touchListenerIsEnabled = false;
    private boolean swipeEnabled = true;
    private boolean swipeBack;
    private boolean isAnimating = false;

    private int left;
    private int top;
    private int right;
    private int bottom;

    private RecyclerView recyclerView;
    private float dXReleasedAt;
    private float dXModified;
    private float dXReal;
    private float dY;
    private int actionState;
    private boolean isCurrentlyActive;

    public interface SwipeAction {
        void onFullSwipe(int position);
    }

    public ReplySwipeCallback(SwipeAction listener) {
        this.swipeListener = listener;
    }

    @Override
    public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        return makeMovementFlags(0, swipeEnabled ? ItemTouchHelper.LEFT : 0);
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
        return false;
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {

    }

    @Override
    public float getSwipeVelocityThreshold(float defaultValue) {
        return defaultValue/2;
    }

    @Override
    public int convertToAbsoluteDirection(int flags, int layoutDirection) {
        if (swipeBack) {
            swipeBack = false;
            return 0;
        }
        return super.convertToAbsoluteDirection(flags, layoutDirection);
    }

    @Override
    public void onChildDraw(Canvas c,
                            RecyclerView recyclerView,
                            RecyclerView.ViewHolder viewHolder,
                            float dX, float dY,
                            int actionState, boolean isCurrentlyActive) {
        setTouchListener(recyclerView);

        if (actionState == ACTION_STATE_SWIPE && isCurrentlyActive) {
            touchListenerIsEnabled = true;
        }

        updateViewHolderState(actionState, isCurrentlyActive);
        updateTouchData(dX, dY);
        currentItemViewHolder = viewHolder;
        super.onChildDraw(c, recyclerView, viewHolder, dXModified, dY, actionState, isCurrentlyActive);
    }

    public void setSwipeEnabled(boolean enabled) {
        swipeEnabled = enabled;
    }

    //TODO add proper (dis)appearance animations
    private void drawReplyArrow(Canvas c, RecyclerView.ViewHolder viewHolder) {
        if (currentReplyArrowState == ReplyArrowState.GONE) return;

        View itemView = viewHolder.itemView;

        calculateBounds(itemView);

        replyIcon.setBounds(left, top, right, bottom);
        replyIcon.draw(c);
    }

    private void calculateBounds(View itemView) {
        int height = itemView.getBottom() - itemView.getTop();
        int center = itemView.getTop() + height/2;
        isAnimating = false;

        if (currentReplyArrowState == ReplyArrowState.ANIMATING_IN) {
            if (currentSize < fullSize && currentSize + fullSize * 0.1f <= fullSize) {
                currentSize += fullSize * 0.1f;
                isAnimating = true;
            } else {
                currentSize = fullSize;
                isAnimating = false;
                currentReplyArrowState = ReplyArrowState.VISIBLE;
            }
        } else if (currentReplyArrowState == ReplyArrowState.ANIMATING_OUT) {
            if (currentSize > 0 && currentSize - fullSize * 0.1f >= 0) {
                currentSize -= fullSize * 0.1f;
                isAnimating = true;
            } else {
                currentSize = 0;
                isAnimating = false;
                currentReplyArrowState = ReplyArrowState.GONE;
                currentItemViewHolder = null;
            }
        } else if (currentReplyArrowState == ReplyArrowState.VISIBLE) {
            currentSize = fullSize;
            isAnimating = false;
        }

        right = itemView.getRight() - paddingRight + (int)dXModified/7;
        left = right - currentSize;
        top = center - currentSize/2;
        bottom = center + currentSize/2;

        int rightMax = itemView.getRight() - paddingRight;
        int leftMax = right - fullSize;
        int topMax = center - fullSize / 2;
        int bottomMax = center + fullSize / 2;

        if (isAnimating)
            recyclerView.postInvalidateDelayed(15, leftMax, topMax, rightMax, bottomMax);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setTouchListener(RecyclerView recyclerView) {
        if (!touchListenerIsSet) {
            recyclerView.setOnTouchListener(this);
            this.recyclerView = recyclerView;
            touchListenerIsSet = true;
        }
    }

    private void updateViewHolderState(int actionState, boolean isCurrentlyActive) {
        this.actionState = actionState;
        this.isCurrentlyActive = isCurrentlyActive;
    }

    private void updateTouchData(float dX, float dY) {
        this.dXReal = dX;
        this.dXModified = updateModifiedTouchData(dX);
        this.dY = dY;
    }

    private float updateModifiedTouchData(float dXReal) {
        float dXModified;
        float dXThreshold = -Math.min(recyclerView.getWidth(), recyclerView.getHeight()) * MAX_SWIPE_DISTANCE_RATIO;

        if (isCurrentlyActive) {
            //View is being actively moved by the user.

            if (dXReal < dXThreshold) {
                dXModified = dXThreshold;
            } else {
                dXModified = dXReal;
            }
        } else {
            //View is in the restoration phase

            if (dXReleasedAt < dXThreshold) {
                //the real delta of finger movement at the time of "release" is bigger than the threshold(max swipe distance of the view).
                //e.g. finger moved 500px, while the view stopped at the threshold of 300px
                //
                //by doing this we can "appropriate" original view's animation's positional data
                //instead of overriding it until it becomes lower than the threshold
                dXModified = (dXReal / dXReleasedAt) * dXThreshold;
            } else {
                //the real delta of finger movement at the time of "release" is smaller than the threshold, so dXModified and dXReal should be equal.
                dXModified = dXReal;
            }
        }
        return dXModified;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        swipeBack = event.getAction() == MotionEvent.ACTION_CANCEL || event.getAction() == MotionEvent.ACTION_UP;

        if (actionState == ACTION_STATE_SWIPE && touchListenerIsEnabled) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_CANCEL:
                case MotionEvent.ACTION_UP:
                    dXReleasedAt = dXReal;
                    touchListenerIsEnabled = false;
                    setItemsClickable(recyclerView, true);

                    if (swipeListener != null) {
                        if (currentReplyArrowState == ReplyArrowState.VISIBLE || currentReplyArrowState == ReplyArrowState.ANIMATING_IN && currentItemViewHolder != null) {
                            swipeListener.onFullSwipe(currentItemViewHolder.getAdapterPosition());
                        }
                    }
                    if (currentReplyArrowState == ReplyArrowState.VISIBLE || currentReplyArrowState == ReplyArrowState.ANIMATING_IN)
                        currentReplyArrowState = ReplyArrowState.ANIMATING_OUT;
                    else
                        currentReplyArrowState = ReplyArrowState.GONE;
                    break;
                default:
                    //We set max swipe distance as 0.2f, but since we don't want the user to drag
                    //each message all the way to the end for a reply, the "active" reply state starts at 0.15f,
                    //and is accompanied by the haptic feedback and the appearance of the reply icon.
                    //animations soon?
                    dXReleasedAt = 0f;
                    if (dXModified < -Math.min(recyclerView.getWidth(), recyclerView.getHeight()) * ACTIVE_SWIPE_DISTANCE_RATIO) {
                        if (currentReplyArrowState == ReplyArrowState.GONE) {
                            currentReplyArrowState = ReplyArrowState.ANIMATING_IN;
                            Utils.performHapticFeedback(recyclerView, HapticFeedbackConstants.KEYBOARD_TAP);
                            setItemsClickable(recyclerView, false);
                            //if (drawThread == null) {
                            //    Thread drawThread = new Thread(new DrawThread(), "drawThread");
                            //    drawThread.start();
                            //}
                        }
                    } else {
                        if (currentReplyArrowState == ReplyArrowState.VISIBLE || currentReplyArrowState == ReplyArrowState.ANIMATING_IN) {
                            currentReplyArrowState = ReplyArrowState.ANIMATING_OUT;
                        }
                    }
            }
        }
        return false;
    }

    private void setItemsClickable(RecyclerView recyclerView,
                                   boolean isClickable) {
        for (int i = 0; i < recyclerView.getChildCount(); ++i) {
            recyclerView.getChildAt(i).setClickable(isClickable);
        }
    }

    public void onDraw(Canvas c) {
        if (currentItemViewHolder != null && (currentReplyArrowState != ReplyArrowState.GONE)) {
            drawReplyArrow(c, currentItemViewHolder);
        }
    }

    enum ReplyArrowState {
        GONE,
        ANIMATING_IN,
        VISIBLE,
        ANIMATING_OUT
    }
}