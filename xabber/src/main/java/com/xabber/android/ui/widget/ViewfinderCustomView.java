package com.xabber.android.ui.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;


import com.google.zxing.ResultPoint;
import com.journeyapps.barcodescanner.ViewfinderView;
import java.util.List;


public class ViewfinderCustomView extends ViewfinderView {


    //protected RectF rectF;
    protected Paint paintCorner;
    private float[] cornerPoints;

    public ViewfinderCustomView(Context context, AttributeSet attrs) {
        super(context, attrs);
        paintCorner = new Paint();
        cornerPoints = new float[10];
        //rectF = new RectF();
    }

    @Override
    public void onDraw(Canvas canvas) {
        refreshSizes();
        if (framingRect == null || previewFramingRect == null) {
            return;
        }

        final Rect frame = framingRect;
        final Rect previewFrame = previewFramingRect;

        final int width = canvas.getWidth();
        final int height = canvas.getHeight();

        // Draw the exterior (i.e. outside the framing rect) darkened
        paint.setColor(resultBitmap != null ? resultColor : maskColor);
        //canvas.drawRect(0,0,width,height,paint);



        canvas.drawRect(0, 0, width, frame.top, paint);
        canvas.drawRect(0, frame.top, frame.left, frame.bottom + 1, paint);
        canvas.drawRect(frame.right + 1, frame.top, width, frame.bottom + 1, paint);
        canvas.drawRect(0, frame.bottom + 1, width, height, paint);

        float arrowHand = 50;
        float widthDrawOffset = 3.5f;
        paintCorner.setColor(Color.WHITE);
        paintCorner.setStrokeWidth(widthDrawOffset*2f);

        //upper left corner
        canvas.drawLine(frame.left,frame.top,frame.left+arrowHand, frame.top, paintCorner);
        canvas.drawLine(frame.left,frame.top - widthDrawOffset,frame.left, frame.top+arrowHand, paintCorner);
        //upper right corner
        canvas.drawLine(frame.right,frame.top,frame.right-arrowHand, frame.top, paintCorner);
        canvas.drawLine(frame.right,frame.top-widthDrawOffset,frame.right, frame.top+arrowHand, paintCorner);
        //lower left corner
        canvas.drawLine(frame.left,frame.bottom,frame.left+arrowHand, frame.bottom, paintCorner);
        canvas.drawLine(frame.left,frame.bottom+widthDrawOffset,frame.left, frame.bottom-arrowHand, paintCorner);
        //lower right corner
        canvas.drawLine(frame.right,frame.bottom,frame.right-arrowHand, frame.bottom, paintCorner);
        canvas.drawLine(frame.right,frame.bottom+widthDrawOffset,frame.right, frame.bottom-arrowHand, paintCorner);





        if (android.os.Build.VERSION.SDK_INT >= 11) {
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }
        Paint mPaint = new Paint();

        //int size = width;
        //if (width>height)
        //    size = height;
        //int cornersRadius = 50;
        //size = (int)(0.8*size);
                /*

                    Parameters
                        left  The X coordinate of the left side of the rectangle
                        top  The Y coordinate of the top of the rectangle
                        right  The X coordinate of the right side of the rectangle
                        bottom  The Y coordinate of the bottom of the rectangle
                */
        //rectF.set(width/2 - size/2,height/2-size/2,width/2+size/2,height/2+size/2);
        //mPaint.setColor(0xFFFFFF);
        //mPaint.setAlpha(0);
        //mPaint.setAntiAlias(true);
        //mPaint.setColor(Color.TRANSPARENT);
        //mPaint.setXfermode(new PorterDuffXfermode(
        //        PorterDuff.Mode.CLEAR));
        //canvas.drawRoundRect(rectF, cornersRadius, cornersRadius, mPaint);
        //canvas.drawCircle(buttonX, buttonY, 100, mPaint);



        if (resultBitmap != null) {
            // Draw the opaque result bitmap over the scanning rectangle
            paint.setAlpha(CURRENT_POINT_OPACITY);
            canvas.drawBitmap(resultBitmap, null, frame, paint);
        } else {

            // Draw a red "laser scanner" line through the middle to show decoding is active

            /*
            paint.setColor(laserColor);
            paint.setAlpha(SCANNER_ALPHA[scannerAlpha]);
            scannerAlpha = (scannerAlpha + 1) % SCANNER_ALPHA.length;
            final int middle = frame.height() / 2 + frame.top;
            canvas.drawRect(frame.left + 2, middle - 1, frame.right - 1, middle + 2, paint);
            */

            final float scaleX = frame.width() / (float) previewFrame.width();
            final float scaleY = frame.height() / (float) previewFrame.height();

            final int frameLeft = frame.left;
            final int frameTop = frame.top;

            // draw the last possible result points
            if (!lastPossibleResultPoints.isEmpty()) {
                paint.setAlpha(CURRENT_POINT_OPACITY / 2);
                paint.setColor(resultPointColor);
                float radius = POINT_SIZE / 2.0f;
                for (final ResultPoint point : lastPossibleResultPoints) {
                    canvas.drawCircle(
                            frameLeft + (int) (point.getX() * scaleX),
                            frameTop + (int) (point.getY() * scaleY),
                            radius, paint
                    );
                }
                lastPossibleResultPoints.clear();
            }

            // draw current possible result points
            if (!possibleResultPoints.isEmpty()) {
                paint.setAlpha(CURRENT_POINT_OPACITY);
                paint.setColor(resultPointColor);
                for (final ResultPoint point : possibleResultPoints) {
                    canvas.drawCircle(
                            frameLeft + (int) (point.getX() * scaleX),
                            frameTop + (int) (point.getY() * scaleY),
                            POINT_SIZE, paint
                    );
                }

                // swap and clear buffers
                final List<ResultPoint> temp = possibleResultPoints;
                possibleResultPoints = lastPossibleResultPoints;
                lastPossibleResultPoints = temp;
                possibleResultPoints.clear();
            }

            // Request another update at the animation interval, but only repaint the laser line,
            // not the entire viewfinder mask.
            //postInvalidateDelayed(ANIMATION_DELAY,
            //        frame.left - POINT_SIZE,
            //        frame.top - POINT_SIZE,
            //        frame.right + POINT_SIZE,
            //        frame.bottom + POINT_SIZE);
        }
    }


}
