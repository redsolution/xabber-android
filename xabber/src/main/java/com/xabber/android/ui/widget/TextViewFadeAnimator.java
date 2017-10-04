package com.xabber.android.ui.widget;

import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.TextView;

/**
 * Created by valery.miller on 03.10.17.
 */

public class TextViewFadeAnimator {

    private static final int FADE_EFFECT_DURATION = 500;
    private static final int DELAY_DURATION = 0;
    private static final int DISPLAY_LENGTH = 2000;

    private TextView blobText;
    private String[] text = new String[] { "" };
    private int position = 0;
    private Animation fadeiInAnimationObject;
    private Animation textDisplayAnimationObject;
    private Animation delayBetweenAnimations;
    private Animation fadeOutAnimationObject;
    private int fadeEffectDuration;
    private int delayDuration;
    private int displayFor;

    public TextViewFadeAnimator(TextView textView, String[] textList) {
        this(textView, FADE_EFFECT_DURATION, DELAY_DURATION, DISPLAY_LENGTH, textList);
    }

    public TextViewFadeAnimator(TextView textView, int fadeEffectDuration, int delayDuration, int displayLength, String[] textList) {
        blobText = textView;
        text = textList;
        this.fadeEffectDuration = fadeEffectDuration;
        this.delayDuration = delayDuration;
        this.displayFor = displayLength;
        InnitializeAnimation();
    }

    public void startAnimation() {
        blobText.startAnimation(textDisplayAnimationObject);
    }

    public void stopAnimation() {
        blobText.clearAnimation();
    }

    private void InnitializeAnimation() {

        fadeiInAnimationObject = new AlphaAnimation(0f, 1f);
        fadeiInAnimationObject.setDuration(fadeEffectDuration);

        textDisplayAnimationObject = new AlphaAnimation(1f, 1f);
        textDisplayAnimationObject.setDuration(displayFor);

        delayBetweenAnimations = new AlphaAnimation(0f, 0f);
        delayBetweenAnimations.setDuration(delayDuration);

        fadeOutAnimationObject = new AlphaAnimation(1f, 0f);
        fadeOutAnimationObject.setDuration(fadeEffectDuration);

        fadeiInAnimationObject.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                position++;
                if (position >= text.length)
                    position = 0;
                blobText.setText(text[position]);
            }
            @Override
            public void onAnimationRepeat(Animation animation) {}
            @Override
            public void onAnimationEnd(Animation animation) {
                blobText.startAnimation(textDisplayAnimationObject);
            }
        });

        textDisplayAnimationObject.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}
            @Override
            public void onAnimationRepeat(Animation animation) {}
            @Override
            public void onAnimationEnd(Animation animation) {
                blobText.startAnimation(fadeOutAnimationObject);
            }
        });

        fadeOutAnimationObject.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}
            @Override
            public void onAnimationRepeat(Animation animation) {}
            @Override
            public void onAnimationEnd(Animation animation) {
                blobText.startAnimation(delayBetweenAnimations);
            }
        });

        delayBetweenAnimations.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}
            @Override
            public void onAnimationRepeat(Animation animation) {}
            @Override
            public void onAnimationEnd(Animation animation) {
                blobText.startAnimation(fadeiInAnimationObject);
            }
        });
    }
}
