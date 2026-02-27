package com.xabber.android.ui.helper;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

import androidx.annotation.NonNull;

import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation;
import com.bumptech.glide.util.Preconditions;
import com.bumptech.glide.util.Util;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.security.MessageDigest;

public class RoundedBorders extends BitmapTransformation {
    private static final String ID = "com.redsolution.xabber.RoundedBorders";
    private static final byte[] ID_BYTES = ID.getBytes(Charset.forName("UTF-8"));

    private final int roundingRadius;
    private final int borderThickness;

    public RoundedBorders(int roundingRadius, int borderThickness){
        Preconditions.checkArgument(roundingRadius > 0, "roundingRadius must be greater than 0.");
        Preconditions.checkArgument(borderThickness >= 0, "borderThickness must be greater than or equal to 0");
        this.roundingRadius = roundingRadius;
        this.borderThickness = borderThickness;
    }

    @Override
    protected Bitmap transform(@NonNull BitmapPool pool, @NonNull Bitmap toTransform, int outWidth, int outHeight) {
        Preconditions.checkArgument(roundingRadius > 0, "roundingRadius must be greater than 0.");

        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.parseColor("#40979797"));
        paint.setStrokeWidth(borderThickness);
        RectF rect = new RectF(0, 0, toTransform.getWidth(), toTransform.getHeight());

        Canvas canvas = new Canvas(toTransform);
        //canvas.drawColor(Color.MAGENTA);
        canvas.drawRoundRect(rect, roundingRadius, roundingRadius, paint);
        return toTransform;

    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof RoundedBorders) {
            RoundedBorders other = (RoundedBorders) o;
            return !((roundingRadius != other.roundingRadius)||(borderThickness != other.borderThickness));
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Util.hashCode(ID.hashCode(),
                Util.hashCode(roundingRadius,
                        Util.hashCode(borderThickness)));
    }

    @Override
    public void updateDiskCacheKey(@NonNull MessageDigest messageDigest) {
        messageDigest.update(ID_BYTES);

        byte[] radiusData = ByteBuffer.allocate(4).putInt(roundingRadius).array();
        messageDigest.update(radiusData);
        byte[] borderData = ByteBuffer.allocate(4).putInt(borderThickness).array();
        messageDigest.update(borderData);
    }
}
