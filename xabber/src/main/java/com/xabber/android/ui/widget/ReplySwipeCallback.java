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
import com.xabber.android.ui.adapter.chat.ActionMessageVH;
import com.xabber.android.utils.Utils;

import java.util.ArrayList;

import static androidx.recyclerview.widget.ItemTouchHelper.ACTION_STATE_SWIPE;

public class ReplySwipeCallback extends ItemTouchHelper.Callback implements View.OnTouchListener {

    private SwipeAction swipeListener;
    private ReplyArrowState currentReplyArrowState = ReplyArrowState.GONE;

    private static final Drawable replyIcon = Application.getInstance().getResources().getDrawable(R.drawable.ic_reply);
    private static final int fullSize = Utils.dipToPx(24f, Application.getInstance());
    private static final int paddingRight = Utils.dipToPx(12f, Application.getInstance());
    private static final float MAX_SWIPE_DISTANCE_RATIO = 0.18f;
    private static final float ACTIVE_SWIPE_DISTANCE_RATIO = 0.13f;

    private RecyclerView.ViewHolder currentItemViewHolder = null;
    private boolean touchListenerIsSet = false;
    private boolean touchListenerIsEnabled = false;
    private boolean swipeEnabled = true;
    private boolean swipeBack;
    private boolean isAnimating = false;
    private ArrayList<Float> scaleAnimationSteps = new ArrayList<>(8);
    private int currentAnimationStep = 0;

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
        addAnimationSteps();
    }

    @Override
    public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        if (viewHolder instanceof ActionMessageVH) return 0;
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

    private void drawReplyArrow(Canvas c, RecyclerView.ViewHolder viewHolder) {
        if (currentReplyArrowState == ReplyArrowState.GONE) return;

        View itemView = viewHolder.itemView;

        calculateBounds(itemView);

        replyIcon.setBounds(left, top, right, bottom);
        replyIcon.draw(c);
    }

    private void calculateBounds(View itemView) {
        int height = itemView.getBottom() - itemView.getTop();
        int centerY = itemView.getTop() + height/2;
        int centerX = itemView.getRight() + (int)(dXModified * 0.5);
        isAnimating = false;

        int currentSize = getCurrentIconSize();

        left = centerX - currentSize/2;
        right = centerX + currentSize/2;

        top = centerY - currentSize/2;
        bottom = centerY + currentSize/2;

        int rightMax = itemView.getRight() - paddingRight;
        int leftMax = right - fullSize;
        int topMax = centerY - fullSize / 2;
        int bottomMax = centerY + fullSize / 2;

        if (isAnimating)
            recyclerView.postInvalidateDelayed(15, leftMax, topMax, rightMax, bottomMax);
    }

    private int getCurrentIconSize() {
        int iconSize = 0;
        switch (currentReplyArrowState) {
            case ANIMATING_IN:
                if (currentAnimationStep < 7) {
                    iconSize = (int) (fullSize * scaleAnimationSteps.get(currentAnimationStep));
                    isAnimating = true;
                    currentAnimationStep++;
                } else {
                    currentAnimationStep = 7;
                    iconSize = fullSize;
                    isAnimating = false;
                    currentReplyArrowState = ReplyArrowState.VISIBLE;
                }
                break;
            case ANIMATING_OUT:
                if (currentAnimationStep > 0) {
                    if (currentAnimationStep == 6) currentAnimationStep--;//skip enlarged frame of the animation.
                    iconSize = (int) (fullSize * scaleAnimationSteps.get(currentAnimationStep));
                    isAnimating = true;
                    currentAnimationStep--;
                } else {
                    currentAnimationStep = 0;
                    iconSize = 0;
                    isAnimating = false;
                    currentReplyArrowState = ReplyArrowState.GONE;
                    currentItemViewHolder = null;
                }
                break;
            case VISIBLE:
                iconSize = fullSize;
                isAnimating = false;
                break;
        }
        return iconSize;
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
                            currentAnimationStep = 0;
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

    private void addAnimationSteps() {
        scaleAnimationSteps.clear();
        scaleAnimationSteps.add(0f);//0
        scaleAnimationSteps.add(0.15f);//15ms
        scaleAnimationSteps.add(0.32f);//30ms
        scaleAnimationSteps.add(0.51f);//45ms
        scaleAnimationSteps.add(0.72f);//60ms
        scaleAnimationSteps.add(0.95f);//75ms
        scaleAnimationSteps.add(1.15f);//90ms
        scaleAnimationSteps.add(1f);//105ms/end
    }

    enum ReplyArrowState {
        GONE,
        ANIMATING_IN,
        VISIBLE,
        ANIMATING_OUT
    }
}