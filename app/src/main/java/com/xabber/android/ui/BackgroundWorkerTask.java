package com.xabber.android.ui;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.support.v4.view.ViewPager;
import android.util.Log;

import java.lang.ref.WeakReference;

/**
 * Created by alexander on 24/09/15.
 */
public class BackgroundWorkerTask extends AsyncTask<Bitmap, Void, BitmapDrawable> {

    private final WeakReference<ViewPager> pagerViewReference;
    private int width;
    private int height;

    public BackgroundWorkerTask(ViewPager viewPager, int dWidth, int dHeight) {
        pagerViewReference = new WeakReference<ViewPager>(viewPager);
        width = dWidth;
        height = dHeight;
    }

    @Override
    protected BitmapDrawable doInBackground(Bitmap... params) {
        int bmHeight = params[0].getHeight();
        int bmWidth = params[0].getWidth();
        float aspect = (float) bmWidth / (float) bmHeight;
        if (height > width) {
            if (bmHeight > bmWidth) {
                Bitmap scaled = Bitmap.createScaledBitmap(params[0],
                        height, Math.round(height / aspect), false);
                Bitmap bg_crop = Bitmap.createBitmap(scaled, (scaled.getWidth() / 2) - (width / 2),
                        (scaled.getHeight() / 2) - (height / 2), width, height);
                return new BitmapDrawable(bg_crop);
            } else if (bmWidth > bmHeight) {
                Bitmap scaled = Bitmap.createScaledBitmap(params[0],
                        Math.round(height * aspect), height, false);
                Bitmap bg_crop = Bitmap.createBitmap(scaled, (scaled.getWidth() / 2) - (width / 2),
                        (scaled.getHeight() / 2) - (height / 2), width, height);
                return new BitmapDrawable(bg_crop);
            } else {
                Bitmap scaled = Bitmap.createScaledBitmap(params[0],
                        height, height, false);
                Bitmap bg_crop = Bitmap.createBitmap(scaled, (params[0].getWidth() / 2) - (width / 2),
                        (params[0].getHeight() / 2) - (height / 2), width, height);
                return new BitmapDrawable(bg_crop);
            }
        } else {
            if (bmHeight > bmWidth) {
                Bitmap scaled = Bitmap.createScaledBitmap(params[0],
                        width, Math.round(width / aspect), false);
                Log.w("Height/width", String.valueOf(scaled.getHeight()) + " " + String.valueOf(scaled.getWidth()));
                Bitmap bg_crop = Bitmap.createBitmap(scaled, (scaled.getWidth() / 2) - (width / 2),
                        (scaled.getHeight() / 2) - (height / 2), width, height);
                return new BitmapDrawable(bg_crop);
            } else if (bmWidth > bmHeight) {
                Bitmap scaled = Bitmap.createScaledBitmap(params[0],
                        Math.round(width * aspect), width, false);
                Bitmap bg_crop = Bitmap.createBitmap(scaled, (scaled.getWidth() / 2) - (width / 2),
                        (scaled.getHeight() / 2) - (height / 2), width, height);
                return new BitmapDrawable(bg_crop);
            } else {
                Bitmap scaled = Bitmap.createScaledBitmap(params[0],
                        width, width, false);
                Bitmap bg_crop = Bitmap.createBitmap(scaled, (params[0].getWidth() / 2) - (width / 2),
                        (params[0].getHeight() / 2) - (height / 2), width, height);
                return new BitmapDrawable(bg_crop);
            }
        }
    }

    @Override
    protected void onPostExecute(BitmapDrawable bitmapDraw) {
        if (pagerViewReference != null && bitmapDraw != null) {
            final ViewPager viewPager = pagerViewReference.get();
            if (viewPager != null) {
                viewPager.setBackground(bitmapDraw);
            }
        }
    }
}
