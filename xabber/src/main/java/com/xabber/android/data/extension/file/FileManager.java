package com.xabber.android.data.extension.file;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.media.ExifInterface;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.xabber.android.BuildConfig;
import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.database.messagerealm.MessageItem;
import com.xabber.android.data.extension.httpfileupload.HttpFileUploadManager;
import com.xabber.android.data.log.LogManager;

import org.apache.commons.io.FilenameUtils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.UUID;

public class FileManager {

    public static final String LOG_TAG = FileManager.class.getSimpleName();

    private static final String[] VALID_IMAGE_EXTENSIONS = {"webp", "jpeg", "jpg", "png", "jpe", "gif"};

    private final static FileManager instance;

    private static int maxImageSize;
    private static int minImageSize;


    static {
        instance = new FileManager();

        Resources resources = Application.getInstance().getResources();
        maxImageSize = resources.getDimensionPixelSize(R.dimen.max_chat_image_size);
        minImageSize = resources.getDimensionPixelSize(R.dimen.min_chat_image_size);
    }

    public static FileManager getInstance() {
        return instance;
    }

    public static void processFileMessage (final MessageItem messageItem) {
        boolean isImage = isImageUrl(messageItem.getText());
        messageItem.setIsImage(isImage);
    }

    public static boolean fileIsImage(File file) {
        return extensionIsImage(extractRelevantExtension(file.getPath()));
    }

    private static boolean extensionIsImage(String extension) {
        return Arrays.asList(VALID_IMAGE_EXTENSIONS).contains(extension);
    }

    public static boolean loadImageFromFile(Context context, String path, ImageView imageView) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;

        // Returns null, sizes are in the options variable
        BitmapFactory.decodeFile(path, options);

        ViewGroup.LayoutParams layoutParams = imageView.getLayoutParams();
        scaleImage(layoutParams, options.outHeight, options.outWidth);

        if (options.outHeight == 0 || options.outWidth == 0) {
            return false;
        }

        imageView.setLayoutParams(layoutParams);
        Glide.with(context)
                .load(path)
                .into(imageView);

        return true;
    }

    public static boolean isImageUrl(String text) {
        if (text == null) {
            return false;
        }

        if (text.trim().contains(" ")) {
            return false;
        }
        try {
            URL url = new URL(text);
            if (!url.getProtocol().equalsIgnoreCase("http") && !url.getProtocol().equalsIgnoreCase("https")) {
                return false;
            }
            String extension = extractRelevantExtension(url);
            if (extension == null) {
                return false;
            }

            return extensionIsImage(extension);
        } catch (MalformedURLException e) {
            return false;
        }
    }

    private static String extractRelevantExtension(URL url) {
        String path = url.getPath();
        return extractRelevantExtension(path);
    }

    private static String extractRelevantExtension(String path) {
        if (path == null || path.isEmpty()) {
            return null;
        }

        String filename = path.substring(path.lastIndexOf('/') + 1).toLowerCase();
        int dotPosition = filename.lastIndexOf(".");

        if (dotPosition != -1) {
            return filename.substring(dotPosition + 1).toLowerCase();
        }
        return null;
    }


    public static void scaleImage(ViewGroup.LayoutParams layoutParams, int height, int width) {
        int scaledWidth;
        int scaledHeight;

        if (width <= height) {
            if (height > maxImageSize) {
                scaledWidth = (int) (width / ((double) height / maxImageSize));
                scaledHeight = maxImageSize;
            } else if (width < minImageSize) {
                scaledWidth = minImageSize;
                scaledHeight = (int) (height / ((double) width / minImageSize));
                if (scaledHeight > maxImageSize) {
                    scaledHeight = maxImageSize;
                }
            } else {
                scaledWidth = width;
                scaledHeight = height;
            }
        } else {
            if (width > maxImageSize) {
                scaledWidth = maxImageSize;
                scaledHeight = (int) (height / ((double) width / maxImageSize));
            } else if (height < minImageSize) {
                scaledWidth = (int) (width / ((double) height / minImageSize));
                if (scaledWidth > maxImageSize) {
                    scaledWidth = maxImageSize;
                }
                scaledHeight = minImageSize;
            } else {
                scaledWidth = width;
                scaledHeight = height;
            }
        }

        layoutParams.width = scaledWidth;
        layoutParams.height = scaledHeight;

    }

    public static boolean isImageSizeGreater(Uri srcUri, int maxSize) {
        final String srcPath = FileUtils.getPath(Application.getInstance(), srcUri);
        if (srcPath == null) {
            return false;
        }

        FileInputStream fis;
        try {
            fis = new FileInputStream(new File(srcPath));
        } catch (FileNotFoundException e) {
            return false;
        }
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(fis, null, options);
        try {
            fis.close();
        } catch (IOException e) {
            LogManager.exception(LOG_TAG, e);
        }

        return options.outHeight > maxSize || options.outWidth > maxSize;
    }

    public static boolean isImageNeedRotation(Uri srcUri) {
        final String srcPath = FileUtils.getPath(Application.getInstance(), srcUri);
        if (srcPath == null) {
            return false;
        }

        ExifInterface exif;
        try {
            exif = new ExifInterface(srcPath);
        } catch (IOException e) {
            LogManager.exception(LOG_TAG, e);
            return false;
        }

        int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

        switch (orientation) {
            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
            case ExifInterface.ORIENTATION_ROTATE_180:
            case ExifInterface.ORIENTATION_FLIP_VERTICAL:
            case ExifInterface.ORIENTATION_TRANSPOSE:
            case ExifInterface.ORIENTATION_ROTATE_90:
            case ExifInterface.ORIENTATION_TRANSVERSE:
            case ExifInterface.ORIENTATION_ROTATE_270:
                return true;

            case ExifInterface.ORIENTATION_NORMAL:
            case ExifInterface.ORIENTATION_UNDEFINED:
            default:
                return false;
        }
    }

    @Nullable
    public static Uri saveImage(byte[] data, String fileName) {
        final File rotateImageFile;
        BufferedOutputStream bos = null;
        try {
            rotateImageFile = createTempImageFile(fileName);
            bos = new BufferedOutputStream(new FileOutputStream(rotateImageFile));
            bos.write(data);


        } catch (IOException e) {
            LogManager.exception(LOG_TAG, e);
            return null;
        } finally {
            if (bos != null) {
                try {
                    bos.flush();
                    bos.close();
                } catch (IOException e) {
                    LogManager.exception(LOG_TAG, e);
                }
            }
        }
        return FileManager.getFileUri(rotateImageFile);
    }

    public static Uri getFileUri(File file) {
        return FileProvider.getUriForFile(Application.getInstance(), BuildConfig.APPLICATION_ID + ".provider", file);
    }

    public static File createTempImageFile(String name) throws IOException {
        // Create an image file name
        return File.createTempFile(
                name,  /* prefix */
                ".jpg",         /* suffix */
                Application.getInstance().getExternalFilesDir(null)      /* directory */
        );
    }

    public static Intent getIntentForShareFile(File file) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_STREAM, getFileUri(file));
        intent.setType(HttpFileUploadManager.getMimeType(file.getPath()));
        intent.putExtra(Intent.EXTRA_TEXT, file.getName());
        return intent;
    }

    /** For java 6 */
    public static void deleteDirectoryRecursion(File file) {
        if (file.isDirectory()) {
            File[] entries = file.listFiles();
            if (entries != null) {
                for (File entry : entries) {
                    deleteDirectoryRecursion(entry);
                }
            }
        }
        if (!file.delete()) {
            Log.d(LOG_TAG, "Failed to delete " + file);
        }
    }

    public static String generateUniqueNameForFile(String path, String sourceName) {
        String extension = FilenameUtils.getExtension(sourceName);
        String baseName =  FilenameUtils.getBaseName(sourceName);
        int i = 0;
        String newName;
        File file;
        do {
            // limitation to prevent infinite loop
            if (i > 200) return UUID.randomUUID().toString() + "." + extension;
            i++;
            newName = baseName + "(" + i + ")." + extension;
            file = new File(path + newName);
        } while (file.exists());
        return newName;
    }

}
