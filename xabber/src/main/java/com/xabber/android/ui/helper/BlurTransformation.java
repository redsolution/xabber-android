package com.xabber.android.ui.helper;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Build;

import androidx.annotation.NonNull;

import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation;

import java.security.MessageDigest;

public class BlurTransformation extends BitmapTransformation {
    private static final String ID =
            "com.xabber.android.ui.helper.BlurTransformation.";

    private final int radius;
    private final int sampling;
    private final int color;

    public BlurTransformation(int radius, int sampling, int color) {
        this.radius = radius;
        this.sampling = sampling;
        this.color = color;
    }

    @Override
    protected Bitmap transform(@NonNull BitmapPool pool, @NonNull Bitmap toTransform, int outWidth, int outHeight) {

        int width = toTransform.getWidth();
        int height = toTransform.getHeight();
        int scaledWidth = width / sampling;
        int scaledHeight = height / sampling;

        Bitmap bitmap = pool.get(scaledWidth, scaledHeight, Bitmap.Config.ARGB_8888);

        bitmap.setDensity(toTransform.getDensity());

        Canvas canvas = new Canvas(bitmap);
        canvas.scale(1 / (float) sampling, 1 / (float) sampling);
        Paint paint = new Paint();
        paint.setFlags(Paint.FILTER_BITMAP_FLAG);

        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(0);
        ColorMatrixColorFilter cmf = new ColorMatrixColorFilter(cm);
        paint.setColorFilter(cmf);

        Paint veil = new Paint();
        veil.setColor(color);
        veil.setAlpha(127);

        canvas.drawBitmap(toTransform, 0, 0, paint);
        canvas.drawRect(new Rect(0,0, toTransform.getWidth(), toTransform.getHeight()), veil);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            try {
                bitmap = RSBlur.blur(bitmap, radius);
            } catch (RuntimeException e) {
                bitmap = FastBlur.blur(bitmap, radius, true);
            }
        } else bitmap = FastBlur.blur(bitmap, radius, true);

        return bitmap;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof BlurTransformation &&
                ((BlurTransformation) o).radius == radius &&
                ((BlurTransformation) o).sampling == sampling &&
                ((BlurTransformation) o).color == color;
    }

    @Override
    public int hashCode() {
        return ID.hashCode() + radius * 1000 + sampling * 10 + color * 10;
    }

    @Override
    public void updateDiskCacheKey(@NonNull MessageDigest messageDigest) {
        messageDigest.update((ID + radius + sampling + color).getBytes(CHARSET));
    }
}
