package com.xabber.android.ui.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.xabber.android.R;

public class ChatListItemLayout extends ViewGroup {

    // Chat name and companions
    private TextView title;
    private TextView time;

    // Message text and companions
    private TextView message;
    private TextView counter;
    private ImageView messageMarker;

    // Avatar
    private ImageView avatar;
    private ImageView status;

    // Misc
    private View colorLine;
    private View colorLineBack;

    public ChatListItemLayout(Context context) {
        this(context, null);
    }

    public ChatListItemLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ChatListItemLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        cacheLayoutViews();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthUsedTopLine;
        int widthUsedBottomLine;
        final int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        final int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        if (colorLineBack.getVisibility() != GONE && colorLine.getVisibility() != GONE) {
            measureChildWithMargins(colorLineBack, widthMeasureSpec, 0, heightMeasureSpec, 0);
            measureChildWithMargins(colorLine, widthMeasureSpec, 0, heightMeasureSpec, 0);
        }

        if (avatar.getVisibility() != GONE) {
            measureChildWithMargins(avatar, widthMeasureSpec, 0, heightMeasureSpec, 0);
            widthUsedTopLine = widthUsedBottomLine = getMeasuredWidthWithMargins(avatar);

            if (status.getVisibility() != GONE) {
                measureChildWithMargins(status, widthMeasureSpec, 0, heightMeasureSpec, 0);
            }

        } else {
            if (status.getVisibility() != GONE) {
                measureChildWithMargins(status, widthMeasureSpec, 0, heightMeasureSpec, 0);
            }
            widthUsedTopLine = widthUsedBottomLine = getMeasuredWidthWithMargins(status) + (int)(getResources().getDisplayMetrics().density * 12);
        }

        // general layout look:

        // | +--+  LouieCarrot@xabber.org             11:05 |
        // | +--o  Hello, how are you?                  (1) |

        // Top Line measure     [ LouieCarrot@xabber.org             11:05 ]
        measureChildWithMargins(time, widthMeasureSpec, widthUsedTopLine, heightMeasureSpec, 0);
        widthUsedTopLine += time.getMeasuredWidth();
        measureChildWithMargins(title, widthMeasureSpec, widthUsedTopLine, heightMeasureSpec, 0);

        // Bottom Line measure  [ Hello, how are you?                  (1) ]
        if (messageMarker.getVisibility() == VISIBLE) {
            // only measure messageMarker
            measureChildWithMargins(messageMarker, widthMeasureSpec, widthUsedBottomLine, heightMeasureSpec, 0);
            widthUsedBottomLine += getMeasuredWidthWithMargins(messageMarker);
        } else if (counter.getVisibility() == VISIBLE) {
            // only measure unreadCounter
            measureChildWithMargins(counter, widthMeasureSpec, widthUsedBottomLine, heightMeasureSpec, 0);
            widthUsedBottomLine += getMeasuredWidthWithMargins(counter);
        }

        measureChildWithMargins(message, widthMeasureSpec, widthUsedBottomLine, heightMeasureSpec, 0);

        setMeasuredDimension(widthSize, heightSize);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        final int strictLeftPos = getPaddingLeft();
        final int strictRightPos = r - l - getPaddingRight();
        final int strictTopPos = getPaddingTop();
        final int strictBotPos = b - t - getPaddingBottom();
        final int height = strictBotPos - strictTopPos;
        float density = getResources().getDisplayMetrics().density;
        int variableLeftPos;

        if (colorLineBack.getVisibility() != GONE && colorLine.getVisibility() != GONE) {
            layoutView(colorLineBack, 0, 0, colorLineBack.getMeasuredWidth(), colorLineBack.getMeasuredHeight());
            layoutView(colorLine, 0, 1, colorLine.getMeasuredWidth(), colorLine.getMeasuredHeight() - 1);
        }

        if (avatar.getVisibility() != GONE) {
            variableLeftPos = layoutAvatarElements(strictLeftPos, height, density);
        } else {
            variableLeftPos = layoutViewCenterVertical(status,
                    strictLeftPos + (int)(getResources().getDisplayMetrics().density * 2),
                    status.getMeasuredWidth(),
                    status.getMeasuredHeight(),
                    height
            );
            variableLeftPos += (int)(getResources().getDisplayMetrics().density * 10);
        }

        // time
        layoutView(time,
                strictRightPos - time.getMeasuredWidth(),
                (int)(density * 12),
                strictRightPos,
                (int)(density * 12) + time.getMeasuredHeight()
        );

        // message marker or unread count
        if (messageMarker.getVisibility() == VISIBLE) {
            layoutView(messageMarker,
                    strictRightPos - messageMarker.getMeasuredWidth(),
                    strictBotPos - (int)(density * 12) - messageMarker.getMeasuredHeight(),
                    strictRightPos,
                    strictBotPos - (int)(density * 12)
            );
        } else if (counter.getVisibility() == VISIBLE) {
            layoutView(counter,
                    strictRightPos - counter.getMeasuredWidth(),
                    strictBotPos - (int)(density * 12) - counter.getMeasuredHeight(),
                    strictRightPos,
                    strictBotPos - (int)(density * 12)
            );
        }

        // chat name
        layoutView(title,
                variableLeftPos,
                (int)(density * 12),
                variableLeftPos + title.getMeasuredWidth(),
                (int)(density * 12) + title.getMeasuredHeight()
        );

        // chat message
        layoutView(message,
                variableLeftPos,
                strictBotPos - (int)(density * 12) - message.getMeasuredHeight(),
                variableLeftPos + message.getMeasuredWidth(),
                strictBotPos - (int)(density * 12)
        );
    }

    private void layoutView(View view, int left, int top, int right, int bottom) {
        view.layout(left, top, right, bottom);
    }

    private int layoutAvatarElements(int left, int containerHeight, float density) {
        MarginLayoutParams margins = (MarginLayoutParams) avatar.getLayoutParams();
        final int leftWithMargins = left + margins.leftMargin;
        final int avatarHeight = avatar.getMeasuredHeight();
        final int avatarWidth = avatar.getMeasuredWidth();
        // avatar is centered vertically
        final int topWithMargins = (containerHeight - (avatarHeight + margins.topMargin + margins.bottomMargin)) / 2;

        avatar.layout(leftWithMargins, topWithMargins, leftWithMargins + avatarWidth, topWithMargins + avatarHeight);

        if (status.getVisibility() != GONE) {
            layoutView(status,
                    leftWithMargins + avatarWidth - status.getMeasuredWidth(),
                    topWithMargins + avatarHeight - status.getMeasuredHeight(),
                    leftWithMargins + avatarWidth,
                    topWithMargins + avatarHeight
            );
        }
        return leftWithMargins + avatarWidth + margins.rightMargin;
    }

    private int layoutViewCenterVertical(View view, int left, int width, int height, int containerHeight) {
        MarginLayoutParams margins = (MarginLayoutParams) view.getLayoutParams();
        final int leftWithMargins = left + margins.leftMargin;
        final int topWithMargins = (containerHeight - (height + margins.topMargin + margins.bottomMargin)) / 2;

        view.layout(leftWithMargins, topWithMargins, leftWithMargins + width, topWithMargins + height);
        return leftWithMargins + width + margins.rightMargin;
    }

    private void cacheLayoutViews() {
        avatar = (ImageView) findViewById(R.id.ivAvatar);
        status = (ImageView) findViewById(R.id.ivStatus);
        title = (TextView) findViewById(R.id.tvContactName);
        message = (TextView) findViewById(R.id.tvMessageText);
        time = (TextView) findViewById(R.id.tvTime);
        counter = (TextView) findViewById(R.id.tvUnreadCount);
        messageMarker = (ImageView) findViewById(R.id.ivMessageStatus);
        colorLine = findViewById(R.id.accountColorIndicator);
        colorLineBack = findViewById(R.id.accountColorIndicatorBack);
    }

    private int getMeasuredWidthWithMargins(View view) {
        final MarginLayoutParams lp = (MarginLayoutParams) view.getLayoutParams();
        return view.getMeasuredWidth() + lp.leftMargin + lp.rightMargin;
    }

    private int getMeasuredHeightWithMargins(View view) {
        final MarginLayoutParams lp = (MarginLayoutParams) view.getLayoutParams();
        return view.getMeasuredHeight() + lp.topMargin + lp.bottomMargin;
    }

    /**
     * Validates if a set of layout parameters is valid for a child this ViewGroup.
     */
    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof MarginLayoutParams;
    }

    /**
     * @return A set of default layout parameters when given a child with no layout parameters.
     */
    @Override
    protected LayoutParams generateDefaultLayoutParams() {
        return new MarginLayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
    }

    /**
     * @return A set of layout parameters created from attributes passed in XML.
     */
    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new MarginLayoutParams(getContext(), attrs);
    }

    /**
     * Called when {@link #checkLayoutParams(LayoutParams)} fails.
     *
     * @return A set of valid layout parameters for this ViewGroup that copies appropriate/valid
     * attributes from the supplied, not-so-good-parameters.
     */
    @Override
    protected LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return generateDefaultLayoutParams();
    }
}
