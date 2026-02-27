package com.xabber.android.ui.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.xabber.android.R;

import java.util.ArrayList;

@SuppressLint("AppCompatCustomView")
public class PlayerVisualizerView extends View {
    /**
     * bytes array converted from file.
     */
    private byte[] bytes;
    private ArrayList<Integer> wave = new ArrayList<>();

    private int amplitude;

    /**
     * Percentage of audio sample scale
     * Should updated dynamically while audioPlayer is played
     */
    private float denseness;

    private Paint playedStatePainting = new Paint();
    private Paint notPlayedStatePainting = new Paint();

    private int width;
    private int height;

    private boolean manualInputMode;

    public PlayerVisualizerView(Context context) {
        super(context);
        init();
    }

    public PlayerVisualizerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        bytes = null;

        notPlayedStatePainting.setStrokeWidth(1f);
        notPlayedStatePainting.setAntiAlias(true);
        notPlayedStatePainting.setColor(ContextCompat.getColor(getContext(), R.color.grey_500));
        notPlayedStatePainting.setAlpha(127);
        playedStatePainting.setStrokeWidth(1f);
        playedStatePainting.setAntiAlias(true);
        playedStatePainting.setColor(ContextCompat.getColor(getContext(), R.color.grey_800));
        playedStatePainting.setAlpha(127);
    }

    public void setPlayedColor(int color) {
        playedStatePainting.reset();
        playedStatePainting.setStrokeWidth(1f);
        playedStatePainting.setAntiAlias(true);
        playedStatePainting.setColor(color);
    }

    public void setPlayedColorAlpha(int alpha) {
        playedStatePainting.setAlpha(alpha);
    }

    public void setNotPlayedColorAlpha(int alpha) {
        notPlayedStatePainting.setAlpha(alpha);
    }

    public void setNotPlayedColor(int color) {
        notPlayedStatePainting.reset();
        notPlayedStatePainting.setStrokeWidth(1f);
        notPlayedStatePainting.setAntiAlias(true);
        notPlayedStatePainting.setColor(color);
    }

    public void setNotPlayedColorRes(int id) {
        notPlayedStatePainting.reset();
        notPlayedStatePainting.setStrokeWidth(1f);
        notPlayedStatePainting.setAntiAlias(true);
        notPlayedStatePainting.setColor(ContextCompat.getColor(getContext(), id));
        notPlayedStatePainting.setAlpha(127);
    }

    /**
     * update and redraw Visualizer view
     */
    /*public void updateVisualizer(byte[] bytes) {
        this.bytes = bytes;
        //decodeOpus(bytes);
        invalidate();
    }*/

    public void updateVisualizer(ArrayList<Integer> wave) {
        this.wave = wave;
        calculateAmplitude();
        invalidate();
    }

    public void refreshVisualizer() {
        ArrayList<Integer> empty = new ArrayList<Integer>();
        empty.add(0);
        this.wave = empty;
        invalidate();
    }

    public void setAmplitude(int amp) {
        this.amplitude = amp;
    }

    /**
     * Update player percent. 0 - file not played, 1 - full played
     *
     * @param percent
     */
    public void updatePlayerPercent(float percent, boolean manual) {
        if (!manualInputMode || manual) {
            denseness = (int) Math.ceil(width * percent);
            if (denseness < 0) {
                denseness = 0;
            } else if (denseness > width) {
                denseness = width;
            }
            invalidate();
        }
    }

    public void setManualInputMode(boolean manual) {
        manualInputMode = manual;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        width = getWidth();
        height = getHeight();
    }

    protected ArrayList<Integer> getFakeAmplitudeList() {
        ArrayList<Integer> waveform = new ArrayList<>();

        float totalBarsCount = width / dp(3);
        if (totalBarsCount <= 0.1f) {
            return null;
        }
        boolean preemptiveSave = false;
        int samplesCount = bytes.length;
        int samplesPerBar = samplesCount/(int)totalBarsCount;
        int currentAmplitude = 0;
        for (int a = 0; a < samplesCount; a++) {
            currentAmplitude = currentAmplitude + bytes[a];
            if (a % samplesPerBar == 0) {
                waveform.add(currentAmplitude);
                if (currentAmplitude > amplitude)
                    amplitude = currentAmplitude;

                currentAmplitude = 0;

                if (a + samplesPerBar > samplesCount)
                    preemptiveSave = true;
            }
        }
        if (preemptiveSave) {
            waveform.add(currentAmplitude);
            if (currentAmplitude > amplitude) {
                amplitude = currentAmplitude;
            }
        }
        return waveform;
    }

    private void calculateAmplitude () {
        if (wave == null || wave.isEmpty())
            return;
        int amplitude = 0;
        for (int i = 0; i < wave.size(); i++) {
            if (wave.get(i) > amplitude)
                amplitude = wave.get(i);
        }
        this.amplitude = amplitude;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (width == 0) {
            return;
        }
        int totalBarsCount = width / dp(3);
        if (totalBarsCount == 0) {
            return;
        }


        if (wave != null && !wave.isEmpty()) {
            for (int a = 0; a < totalBarsCount; a++) {

                int barHeight;
                int sampleIndex;
                int sampleSize;

                sampleIndex = (a * wave.size())/ totalBarsCount;
                sampleSize = wave.get(sampleIndex);
                if (amplitude != 0 && sampleSize / amplitude <= 1) {
                    barHeight = height * sampleSize / amplitude;
                } else barHeight = 0;

                if (barHeight < dp(1))
                    barHeight = dp(1);

                int x = a * dp(3);
                float left = x;
                float right = x + dp(2);
                float top = height - barHeight;
                float bottom = height;
                if (x < denseness) {
                    canvas.drawRect(left, top, right, bottom, playedStatePainting);
                } else {
                    canvas.drawRect(left, top, right, bottom, notPlayedStatePainting);
                }
            }
        } else {
            for (int a = 0; a < totalBarsCount; a++) {

                int barHeight = dp(1);

                float left = a * dp(3);
                float right = left + dp(2);
                float top = height - barHeight;
                float bottom = height;
                if (left < denseness) {
                    canvas.drawRect(left, top, right, bottom, playedStatePainting);
                } else {
                    canvas.drawRect(left, top, right, bottom, notPlayedStatePainting);
                }
            }
        }
    }


    @Override
    public boolean performClick() {
        return super.performClick();
    }

    public static class onProgressTouch implements View.OnTouchListener {

        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            float touchPoint;
            int width;
            switch (motionEvent.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    touchPoint = motionEvent.getX();
                    width = view.getWidth();
                    ((PlayerVisualizerView)view).setManualInputMode(true);
                    ((PlayerVisualizerView)view).updatePlayerPercent(touchPoint/width, true);
                    view.getParent().requestDisallowInterceptTouchEvent(true);
                    break;
                case MotionEvent.ACTION_MOVE:
                    touchPoint = motionEvent.getX();
                    width = view.getWidth();
                    ((PlayerVisualizerView)view).updatePlayerPercent(touchPoint/width, true);
                    break;
                case MotionEvent.ACTION_CANCEL:
                case MotionEvent.ACTION_UP:
                    ((PlayerVisualizerView)view).setManualInputMode(false);
                    view.getParent().requestDisallowInterceptTouchEvent(false);
                    break;
            }
            return true;
        }

    }

    public int dp(float value) {
        if (value == 0) {
            return 0;
        }
        return (int) Math.ceil(getContext().getResources().getDisplayMetrics().density * value);
    }
}
