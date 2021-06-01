package com.xabber.android.ui.widget;

import android.content.Context;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.xabber.android.R;

import java.util.Arrays;
import java.util.List;

public class ContactBarAutoSizingLayout extends ViewGroup {

    private TextView chat;
    private TextView call;
    private TextView notify;
    private TextView block;
    private ImageButton buttonChat;
    private ImageButton buttonCall;
    private ImageButton buttonNotify;
    private ImageButton buttonBlock;
    private View dividerTop;
    private View dividerBottom;

    private TextPaint measurePaint;

    private List<TextView> textViews;
    private List<ImageButton> buttonViews;
    private List<String> buttonStrings;

    public ContactBarAutoSizingLayout(Context context) {
        this(context, null);
    }

    public ContactBarAutoSizingLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ContactBarAutoSizingLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        buttonStrings = Arrays.asList(getResources().getString(R.string.contact_bar_chat),
                getResources().getString(R.string.contact_bar_call),
                getResources().getString(R.string.contact_bar_notifications),
                getResources().getString(R.string.contact_bar_block),
                getResources().getString(R.string.contact_bar_unblock));
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        cacheViews();
    }

    @Override
    public boolean shouldDelayChildPressedState() {
        return false;
    }

    private void cacheViews() {
        buttonChat = (ImageButton) findViewById(R.id.chat_button);
        buttonCall = (ImageButton) findViewById(R.id.call_button);
        buttonNotify = (ImageButton) findViewById(R.id.notify_button);
        buttonBlock = (ImageButton) findViewById(R.id.block_button);
        chat = (TextView) findViewById(R.id.chat_button_text);
        call = (TextView) findViewById(R.id.call_button_text);
        notify = (TextView) findViewById(R.id.notification_text);
        block = (TextView) findViewById(R.id.block_text);
        dividerTop = findViewById(R.id.divider_top);
        dividerBottom = findViewById(R.id.divider_bottom);
        textViews = Arrays.asList(chat, call, notify, block);
        buttonViews = Arrays.asList(buttonChat, buttonCall, buttonNotify, buttonBlock);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        final int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = getPaddingTop() + getPaddingBottom();
        int iconMeasureSpecWidth = MeasureSpec.makeMeasureSpec(width / 4, MeasureSpec.EXACTLY);
        int iconMeasureSpecHeight;

        if (chat.getVisibility() != GONE) {
            int textIndex = 0;
            int maxTextIndex = -1;
            int textSize = Integer.MIN_VALUE;
            float autoSize;

            for (String name : buttonStrings) {
                if (name.length() > textSize) {
                    textSize = name.length();
                    maxTextIndex = textIndex;
                }
                textIndex++;
            }

            measurePaint = chat.getPaint();
            float textWidth = measurePaint.measureText(buttonStrings.get(maxTextIndex));
            // measureChild(textViews.get(maxTextIndex), widthMeasureSpec, heightMeasureSpec);

            if (textWidth > (width / 4f)) {
                float ratio = (width / 4.1f) / textWidth;

                if (ratio < 0.5f) ratio = 0.5f;
                if (ratio > 1f) ratio = 1f;

                autoSize = chat.getTextSize() * ratio;
                for (int i = 0; i < 4; i ++) {
                    // textViews.get(i).setTextSize(autoSize);
                    textViews.get(i).getPaint().setTextSize(autoSize);
                    measureChild(textViews.get(i), iconMeasureSpecWidth, heightMeasureSpec);
                }
            } else {
                for (int i = 0; i < 4; i ++) {
                    measureChild(textViews.get(i), iconMeasureSpecWidth, heightMeasureSpec);
                }
            }
            // height += chat.getMeasuredHeight();

            iconMeasureSpecHeight = MeasureSpec.makeMeasureSpec((int) (getResources().getDisplayMetrics().density * 40) + chat.getMeasuredHeight(), MeasureSpec.EXACTLY);
            for (ImageButton button : buttonViews) {
                setPaddingBottom(button, (int) (getResources().getDisplayMetrics().density * 18));
                increaseVerticalLayoutSize(button, (int) (getResources().getDisplayMetrics().density * 18));
            }
        } else {
            iconMeasureSpecHeight = MeasureSpec.makeMeasureSpec((int) (getResources().getDisplayMetrics().density * 40), MeasureSpec.EXACTLY);
        }

        measureChild(buttonChat, iconMeasureSpecWidth, iconMeasureSpecHeight);
        measureChild(buttonCall, iconMeasureSpecWidth, iconMeasureSpecHeight);
        measureChild(buttonNotify, iconMeasureSpecWidth, iconMeasureSpecHeight);
        measureChild(buttonBlock, iconMeasureSpecWidth, iconMeasureSpecHeight);

        measureChild(dividerBottom, widthMeasureSpec, heightMeasureSpec);
        measureChild(dividerTop, widthMeasureSpec, heightMeasureSpec);
        height += buttonChat.getMeasuredHeight();
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int leftBorder = getPaddingLeft();
        int topWithPadding = getPaddingTop();
        int bottomTextBorder = topWithPadding + buttonChat.getMeasuredHeight();
        boolean layoutText = chat.getVisibility() != GONE;
        final int quarter = (r - l - getPaddingRight()) / 4;

        buttonChat.layout(leftBorder, topWithPadding, leftBorder + buttonChat.getMeasuredWidth(), topWithPadding + buttonChat.getMeasuredHeight());
        if (layoutText) layoutTextView(chat, leftBorder, bottomTextBorder, chat.getMeasuredWidth(), chat.getMeasuredHeight(), quarter);
        leftBorder += buttonChat.getMeasuredWidth();

        buttonCall.layout(leftBorder, topWithPadding, leftBorder + buttonCall.getMeasuredWidth(), topWithPadding + buttonCall.getMeasuredHeight());
        if (layoutText) layoutTextView(call, leftBorder, bottomTextBorder, call.getMeasuredWidth(), call.getMeasuredHeight(), quarter);
        leftBorder += buttonCall.getMeasuredWidth();

        buttonNotify.layout(leftBorder, topWithPadding, leftBorder + buttonNotify.getMeasuredWidth(), topWithPadding + buttonNotify.getMeasuredHeight());
        if (layoutText) layoutTextView(notify, leftBorder, bottomTextBorder, notify.getMeasuredWidth(), notify.getMeasuredHeight(), quarter);
        leftBorder += buttonNotify.getMeasuredWidth();

        buttonBlock.layout(leftBorder, topWithPadding, leftBorder + buttonBlock.getMeasuredWidth(), topWithPadding + buttonBlock.getMeasuredHeight());
        if (layoutText) layoutTextView(block, leftBorder, bottomTextBorder, block.getMeasuredWidth(), block.getMeasuredHeight(), quarter);

        dividerBottom.layout(0, b - t - dividerBottom.getMeasuredHeight(), r, b - t);
        dividerTop.layout(0, 0, r, dividerTop.getMeasuredHeight());
    }

    private void layoutTextView(TextView textView, int left, int bottom, int width, int height, int containerQuarterWidth) {
        textView.layout(
                left + (containerQuarterWidth - width) / 2,
                bottom - height,
                left + (containerQuarterWidth - width) / 2 + width,
                bottom
        );
    }

    private void setPaddingBottom(ImageButton button, int bottom) {
        button.setPadding(0, bottom * 2 / 18, 0, bottom);
    }

    private void increaseVerticalLayoutSize(View view, int increaseBy) {
        LayoutParams lp = view.getLayoutParams();
        lp.height = (int) (getResources().getDisplayMetrics().density * 40) + increaseBy;
        view.setLayoutParams(lp);
    }

    public void redrawText() {
        chat.invalidate();
        chat.requestLayout();
        call.invalidate();
        call.requestLayout();
        notify.invalidate();
        notify.requestLayout();
        block.invalidate();
        block.requestLayout();
    }
}
