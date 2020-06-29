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

    private TextView text1;
    private TextView text2;
    private TextView text3;
    private TextView text4;
    private ImageButton button1;
    private ImageButton button2;
    private ImageButton button3;
    private ImageButton button4;
    private View dividerTop;
    private View dividerBottom;

    private TextPaint measurePaint;

    private List<TextView> textViews;
    private List<ImageButton> buttonViews;
    private List<String> buttonStrings;
    private List<String> buttonGroupchatStrings;

    private boolean isForGroupchat = false;

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
        buttonGroupchatStrings = Arrays.asList(getResources().getString(R.string.contact_bar_chat),
                getResources().getString(R.string.groupchat_bar_invite),
                getResources().getString(R.string.contact_bar_notifications),
                getResources().getString(R.string.groupchat_bar_leave));
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        cacheViews();
    }

    public void setForGroupchat(boolean forGroupchat) {
        this.isForGroupchat = forGroupchat;
        for (int i = 0; i < textViews.size(); i++) {
            textViews.get(i).setText(getButtonStrings().get(i));
        }
        if (isForGroupchat) {
            button2.setImageDrawable(getContext().getResources().getDrawable(R.drawable.ic_group_add_black_24dp));
            button4.setImageDrawable(getContext().getResources().getDrawable(R.drawable.ic_group_leave_24dp));
        } else {
            button2.setImageDrawable(getContext().getResources().getDrawable(R.drawable.ic_calls_list_new));
            button4.setImageDrawable(getContext().getResources().getDrawable(R.drawable.ic_block_grey600_24dp));
        }
        redrawText();
    }

    @Override
    public boolean shouldDelayChildPressedState() {
        return false;
    }

    private void cacheViews() {
        button1 = (ImageButton) findViewById(R.id.first_button);
        button2 = (ImageButton) findViewById(R.id.second_button);
        button3 = (ImageButton) findViewById(R.id.third_button);
        button4 = (ImageButton) findViewById(R.id.fourth_button);
        text1 = (TextView) findViewById(R.id.first_button_text);
        text2 = (TextView) findViewById(R.id.second_button_text);
        text3 = (TextView) findViewById(R.id.third_button_text);
        text4 = (TextView) findViewById(R.id.fourth_button_text);
        dividerTop = findViewById(R.id.divider_top);
        dividerBottom = findViewById(R.id.divider_bottom);
        textViews = Arrays.asList(text1, text2, text3, text4);
        buttonViews = Arrays.asList(button1, button2, button3, button4);
    }

    private List<String> getButtonStrings() {
        if (isForGroupchat) return buttonGroupchatStrings;
        else return buttonStrings;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        final int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = getPaddingTop() + getPaddingBottom();
        int iconMeasureSpecWidth = MeasureSpec.makeMeasureSpec(width / 4, MeasureSpec.EXACTLY);
        int iconMeasureSpecHeight;

        if (text1.getVisibility() != GONE) {
            int textIndex = 0;
            int maxTextIndex = -1;
            int textSize = Integer.MIN_VALUE;
            float autoSize;

            for (String name : getButtonStrings()) {
                if (name.length() > textSize) {
                    textSize = name.length();
                    maxTextIndex = textIndex;
                }
                textIndex++;
            }

            measurePaint = text1.getPaint();
            float textWidth = measurePaint.measureText(getButtonStrings().get(maxTextIndex));
            // measureChild(textViews.get(maxTextIndex), widthMeasureSpec, heightMeasureSpec);

            if (textWidth > (width / 4f)) {
                float ratio = (width / 4.1f) / textWidth;

                if (ratio < 0.5f) ratio = 0.5f;
                if (ratio > 1f) ratio = 1f;

                autoSize = text1.getTextSize() * ratio;
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

            iconMeasureSpecHeight = MeasureSpec.makeMeasureSpec((int) (getResources().getDisplayMetrics().density * 40) + text1.getMeasuredHeight(), MeasureSpec.EXACTLY);
            for (ImageButton button : buttonViews) {
                setPaddingBottom(button, (int) (getResources().getDisplayMetrics().density * 18));
                increaseVerticalLayoutSize(button, (int) (getResources().getDisplayMetrics().density * 18));
            }
        } else {
            iconMeasureSpecHeight = MeasureSpec.makeMeasureSpec((int) (getResources().getDisplayMetrics().density * 40), MeasureSpec.EXACTLY);
        }

        measureChild(button1, iconMeasureSpecWidth, iconMeasureSpecHeight);
        measureChild(button2, iconMeasureSpecWidth, iconMeasureSpecHeight);
        measureChild(button3, iconMeasureSpecWidth, iconMeasureSpecHeight);
        measureChild(button4, iconMeasureSpecWidth, iconMeasureSpecHeight);

        measureChild(dividerBottom, widthMeasureSpec, heightMeasureSpec);
        measureChild(dividerTop, widthMeasureSpec, heightMeasureSpec);
        height += button1.getMeasuredHeight();
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int leftBorder = getPaddingLeft();
        int topWithPadding = getPaddingTop();
        int bottomTextBorder = topWithPadding + button1.getMeasuredHeight();
        boolean layoutText = text1.getVisibility() != GONE;
        final int quarter = (r - l - getPaddingRight()) / 4;

        button1.layout(leftBorder, topWithPadding, leftBorder + button1.getMeasuredWidth(), topWithPadding + button1.getMeasuredHeight());
        if (layoutText) layoutTextView(text1, leftBorder, bottomTextBorder, text1.getMeasuredWidth(), text1.getMeasuredHeight(), quarter);
        leftBorder += button1.getMeasuredWidth();

        button2.layout(leftBorder, topWithPadding, leftBorder + button2.getMeasuredWidth(), topWithPadding + button2.getMeasuredHeight());
        if (layoutText) layoutTextView(text2, leftBorder, bottomTextBorder, text2.getMeasuredWidth(), text2.getMeasuredHeight(), quarter);
        leftBorder += button2.getMeasuredWidth();

        button3.layout(leftBorder, topWithPadding, leftBorder + button3.getMeasuredWidth(), topWithPadding + button3.getMeasuredHeight());
        if (layoutText) layoutTextView(text3, leftBorder, bottomTextBorder, text3.getMeasuredWidth(), text3.getMeasuredHeight(), quarter);
        leftBorder += button3.getMeasuredWidth();

        button4.layout(leftBorder, topWithPadding, leftBorder + button4.getMeasuredWidth(), topWithPadding + button4.getMeasuredHeight());
        if (layoutText) layoutTextView(text4, leftBorder, bottomTextBorder, text4.getMeasuredWidth(), text4.getMeasuredHeight(), quarter);

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
        text1.invalidate();
        text1.requestLayout();
        text2.invalidate();
        text2.requestLayout();
        text3.invalidate();
        text3.requestLayout();
        text4.invalidate();
        text4.requestLayout();
    }
}
