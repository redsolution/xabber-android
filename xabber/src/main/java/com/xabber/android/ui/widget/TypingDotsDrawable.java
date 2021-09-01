package com.xabber.android.ui.widget;

import static com.xabber.android.ui.helper.AndroidUtilsKt.dipToPx;
import static com.xabber.android.ui.helper.AndroidUtilsKt.dipToPxFloat;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.xabber.android.R;
import com.xabber.android.data.Application;

public class TypingDotsDrawable extends Drawable {

    private Paint dotPaint = new Paint();

    private float[] scale = new float[3];
    private float[] startTimes = new float[] {0, 150, 300};
    private float elapsedTime = 0;
    private long lastUpdateTime;
    private DecelerateInterpolator decelerateInterpolator = new DecelerateInterpolator();
    private boolean started;

    public TypingDotsDrawable() {
        dotPaint.setColor(Application.getInstance().getResources().getColor(R.color.grey_500));
        dotPaint.setAntiAlias(true);
        dotPaint.setAlpha(255);
    }

    public void setDotColor(int color) {
        dotPaint.setColor(color);
        invalidateSelf();
    }

    private void update() {
        long newTime = System.currentTimeMillis();
        long dt = newTime - lastUpdateTime;
        lastUpdateTime = newTime;
        if (dt > 50) {
            dt = 50;
        }
        elapsedTime += dt;
        if (elapsedTime >= 960) {
            elapsedTime = dt;
        }
        for (int a = 0; a < 3; a++) {
            float timeSinceStart = elapsedTime - startTimes[a];
            scale[a] = calcScale(timeSinceStart);
        }
        invalidateSelf();
    }

    private float calcScale(float dotTime) {
        if (dotTime > 0) {
            if (dotTime <= 320) {
                float diff = decelerateInterpolator.getInterpolation(dotTime / 320.0f);
                return 1.33f + diff;
            } else if (dotTime <= 640) {
                float diff = decelerateInterpolator.getInterpolation((dotTime - 320.0f) / 320.0f);
                return 1.33f + (1 - diff);
            }
        }
        return 1.33f;
    }

    public void start() {
        lastUpdateTime = System.currentTimeMillis();
        started = true;
        invalidateSelf();
    }

    public boolean isStarted() {
        return started;
    }

    public void stop() {
        for (int a = 0; a < 3; a++) {
            scale[a] = 1.33f;
        }
        elapsedTime = 0;
        startTimes[0] = 0;
        startTimes[1] = 150;
        startTimes[2] = 300;
        started = false;
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        canvas.drawCircle(dipToPxFloat(3, Application.getInstance()),
                dipToPxFloat(10, Application.getInstance()),
                dipToPxFloat(scale[0], Application.getInstance()),
                dotPaint);

        canvas.drawCircle(dipToPxFloat(9, Application.getInstance()),
                dipToPxFloat(10, Application.getInstance()),
                dipToPxFloat(scale[1], Application.getInstance()),
                dotPaint);

        canvas.drawCircle(dipToPxFloat(15, Application.getInstance()),
                dipToPxFloat(10, Application.getInstance()),
                dipToPxFloat(scale[2], Application.getInstance()),
                dotPaint);

        if (started) {
            scheduleSelf(this::update, 20);
        }
    }

    @Override
    public void setAlpha(int alpha) {
        dotPaint.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {

    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSPARENT;
    }

    @Override
    public int getIntrinsicWidth() {
        return dipToPx(18, Application.getInstance());
    }

    @Override
    public int getIntrinsicHeight() {
        return dipToPx(18, Application.getInstance());
    }
}
