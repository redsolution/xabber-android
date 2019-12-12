package com.xabber.android.ui.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.xabber.android.R;
import com.xabber.android.data.Application;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

@SuppressLint("AppCompatCustomView")
public class PlayerVisualizerView extends ImageView {
    /**
     * bytes array converted from file.
     */
    private byte[] bytes;

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
        playedStatePainting.setStrokeWidth(1f);
        playedStatePainting.setAntiAlias(true);
        playedStatePainting.setColor(ContextCompat.getColor(getContext(), R.color.grey_800));
    }

    public void setPlayedColor(int color) {
        playedStatePainting.reset();
        playedStatePainting.setStrokeWidth(1f);
        playedStatePainting.setAntiAlias(true);
        playedStatePainting.setColor(color);
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
    }

    /**
     * update and redraw Visualizer view
     */
    public void updateVisualizer(byte[] bytes) {
        this.bytes = bytes;
        invalidate();
    }

    public void updateVisualizerFromFile(File file) {
        int size = (int) file.length();
        final byte[] bytes = new byte[size];
        try {
            BufferedInputStream buf = new BufferedInputStream(new FileInputStream(file));
            buf.read(bytes, 0, bytes.length);
            buf.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        updateVisualizer(bytes);
    }

    public void updateVisualizerFromFileAsync(final File file) {
        Application.getInstance().runInBackground(new Runnable() {
            @Override
            public void run() {
                int size = (int) file.length();
                final byte[] bytes = new byte[size];
                try {
                    BufferedInputStream buf = new BufferedInputStream(new FileInputStream(file));
                    buf.read(bytes, 0, bytes.length);
                    buf.close();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Application.getInstance().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        updateVisualizer(bytes);
                    }
                });
            }
        });
    }

    /**
     * Update player percent. 0 - file not played, 1 - full played
     *
     * @param percent
     */
    public void updatePlayerPercent(float percent) {
        denseness = (int) Math.ceil(width * percent);
        if (denseness < 0) {
            denseness = 0;
        } else if (denseness > width) {
            denseness = width;
        }
        invalidate();
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

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (bytes == null || width == 0) {
            return;
        }
        float totalBarsCount = width / dp(3);
        if (totalBarsCount <= 0.1f) {
            return;
        }
        byte value;
        int samplesCount = bytes.length;
        int samplesPerBar = samplesCount/(int)totalBarsCount;
        ArrayList<Integer> waveform = getFakeAmplitudeList();

        for (int a = 0; a < totalBarsCount; a++) {

            //value = (byte) ((bytes[a]) & ((2 << 4) - 1));
            int barHeight;
            if (amplitude != 0)
                barHeight = height * waveform.get(a)/amplitude;
            else barHeight = 0;
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
    }

    /*@Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (bytes == null || width == 0) {
            return;
        }
        float totalBarsCount = width / dp(3);
        if (totalBarsCount <= 0.1f) {
            return;
        }
        byte value;
        int samplesCount = (bytes.length * 8 / 5);
        float samplesPerBar = samplesCount / totalBarsCount;
        float barCounter = 0;
        int nextBarNum = 0;

        int y = (height - dp(VISUALIZER_HEIGHT)) / 2;
        int barNum = 0;
        int lastBarNum;
        int drawBarCount;

        for (int a = 0; a < samplesCount; a++) {
            if (a != nextBarNum) {
                continue;
            }
            drawBarCount = 0;
            lastBarNum = nextBarNum;
            while (lastBarNum == nextBarNum) {
                barCounter += samplesPerBar;
                nextBarNum = (int) barCounter;
                drawBarCount++;
            }

            int bitPointer = a * 5;
            int byteNum = bitPointer / Byte.SIZE;
            int byteBitOffset = bitPointer - byteNum * Byte.SIZE;
            int currentByteCount = Byte.SIZE - byteBitOffset;
            int nextByteRest = 5 - currentByteCount;
            value = (byte) ((bytes[byteNum] >> byteBitOffset) & ((2 << (Math.min(5, currentByteCount) - 1)) - 1));
            if (nextByteRest > 0) {
                value <<= nextByteRest;
                value |= bytes[byteNum + 1] & ((2 << (nextByteRest - 1)) - 1);
            }

            for (int b = 0; b < drawBarCount; b++) {
                int x = barNum * dp(3);
                float left = x;
                float top = y + dp(VISUALIZER_HEIGHT - Math.max(1, VISUALIZER_HEIGHT * value / 31.0f));
                float right = x + dp(2);
                float bottom = y + dp(VISUALIZER_HEIGHT);
                if (x < denseness && x + dp(2) < denseness) {
                    canvas.drawRect(left, top, right, bottom, playedStatePainting);
                } else {
                    canvas.drawRect(left, top, right, bottom, notPlayedStatePainting);
                    if (x < denseness) {
                        canvas.drawRect(left, top, right, bottom, playedStatePainting);
                    }
                }
                barNum++;
            }
        }
    }
*/

    public int dp(float value) {
        if (value == 0) {
            return 0;
        }
        return (int) Math.ceil(getContext().getResources().getDisplayMetrics().density * value);
    }
}
