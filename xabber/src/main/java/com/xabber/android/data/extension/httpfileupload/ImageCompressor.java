package com.xabber.android.data.extension.httpfileupload;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.File;
import java.io.FileOutputStream;

public class ImageCompressor {

    private static final int IMAGE_QUALITY = 90;
    private static final int MAX_SIZE_PIXELS = 1280;

    public static File compressImage(final File file, String outputDirectory) {
        String path = file.getPath();
        String format = path.substring(path.lastIndexOf(".")).substring(1);
        Bitmap source = BitmapFactory.decodeFile(file.getPath());
        Bitmap.CompressFormat compressFormat;

        // if png pr webp have allowed resolution then not compress it
        if ("png".equals(format) || "webp".equals(format)) return file;

        // select format
        switch (format) {
            case "png":
                compressFormat = Bitmap.CompressFormat.PNG;
                break;
            case "webp":
                compressFormat = Bitmap.CompressFormat.WEBP;
                break;
            case "gif":
                return file;
            default:
                compressFormat = Bitmap.CompressFormat.JPEG;
        }

        // resize image
        Bitmap resizedBmp;
        if (source.getHeight() > MAX_SIZE_PIXELS || source.getWidth() > MAX_SIZE_PIXELS) {
            resizedBmp = resizeBitmap(source, MAX_SIZE_PIXELS);
        } else  {
            resizedBmp = source;
        }

        // create directory if not exist
        File directory = new File(outputDirectory);
        directory.mkdirs();

        // compress image
        File result = new File(outputDirectory, file.getName());
        FileOutputStream fOut;
        try {
            fOut = new FileOutputStream(result);
            resizedBmp.compress(compressFormat, IMAGE_QUALITY, fOut);
            fOut.flush();
            fOut.close();
            source.recycle();
            resizedBmp.recycle();
            return result;
        } catch (Exception e) {
            return null;
        }
    }

    private static Bitmap resizeBitmap(Bitmap source, int maxSizePixels) {
        int targetWidth, targetHeight;
        double aspectRatio;

        if (source.getWidth() > source.getHeight()) {
            targetWidth = maxSizePixels;
            aspectRatio = (double) source.getHeight() / (double) source.getWidth();
            targetHeight = (int) (targetWidth * aspectRatio);
        } else {
            targetHeight = maxSizePixels;
            aspectRatio = (double) source.getWidth() / (double) source.getHeight();
            targetWidth = (int) (targetHeight * aspectRatio);
        }

        return Bitmap.createScaledBitmap(source, targetWidth, targetHeight, false);
    }

}
