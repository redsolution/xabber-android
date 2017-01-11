package com.xabber.android.data.extension.file;

import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.FileProvider;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.xabber.android.BuildConfig;
import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.database.realm.MessageItem;
import com.xabber.android.data.log.LogManager;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import io.realm.Realm;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class FileManager {

    public static final String LOG_TAG = FileManager.class.getSimpleName();

    private static final String[] VALID_IMAGE_EXTENSIONS = {"webp", "jpeg", "jpg", "png", "jpe", "gif"};

    private static final String CACHE_DIRECTORY = Environment.getExternalStorageDirectory().getAbsolutePath()
            + "/"  +  Application.getInstance().getString(R.string.application_title_short) + "/Cache/";


    private final static FileManager instance;

    static int maxImageSize;
    static int minImageSize;


    static {
        instance = new FileManager();
        Application.getInstance().addManager(instance);

        Resources resources = Application.getInstance().getResources();
        maxImageSize = resources.getDimensionPixelSize(R.dimen.max_chat_image_size);
        minImageSize = resources.getDimensionPixelSize(R.dimen.min_chat_image_size);
    }

    public static FileManager getInstance() {
        return instance;
    }

    public static void processFileMessage (final MessageItem messageItem, final boolean download) {
        if (!treatAsDownloadable(messageItem.getText())) {
            return;
        }
    }

    @NonNull
    private static String getCachePath(URL url) {
        return CACHE_DIRECTORY + url.getHost() + "/" + url.getPath();
    }

    private static void getFileUrlSize(final MessageItem messageItem) {
        LogManager.i(FileManager.class, "Requesting file size");

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(messageItem.getText())
                .head()
                .build();

        final String uniqueId = messageItem.getUniqueId();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {

            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    final String contentLength = response.header("Content-Length");
                    if (contentLength != null) {
                        Realm realm = Realm.getDefaultInstance();
                        realm.executeTransaction(new Realm.Transaction() {
                            @Override
                            public void execute(Realm realm) {
                                MessageItem first = realm.where(MessageItem.class)
                                        .equalTo(MessageItem.Fields.UNIQUE_ID, uniqueId)
                                        .findFirst();
                                if (first != null) {
                                    first.setFileSize(Long.parseLong(contentLength));
                                }
                            }
                        });
                        realm.close();
                    }
                }
            }
        });
    }

    public static boolean fileIsImage(String path) {
        return extensionIsImage(extractRelevantExtension(path));
    }


    public static boolean fileIsImage(File file) {
        return extensionIsImage(extractRelevantExtension(file.getPath()));
    }

    public static boolean extensionIsImage(String extension) {
        return Arrays.asList(VALID_IMAGE_EXTENSIONS).contains(extension);
    }


    public static String getFileName(String path) {
        return path.substring(path.lastIndexOf('/') + 1).toLowerCase();
    }
    public static void openFile(Context context, File file) {
        final Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(FileManager.getFileUri(file), getFileMimeType(file));
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        PackageManager manager = context.getPackageManager();
        List<ResolveInfo> infos = manager.queryIntentActivities(intent, 0);
        if (infos.size() > 0) {
            context.startActivity(intent);
        } else {
            Toast.makeText(context, R.string.no_application_to_open_file, Toast.LENGTH_SHORT).show();
        }
    }

    private static String getFileMimeType(File file) {
        return getExtensionMimeType(MimeTypeMap.getFileExtensionFromUrl(file.toURI().toString()));
    }

    private static String getExtensionMimeType(String extension) {
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.toLowerCase());
    }

    public static void loadImageFromFile(String path, ImageView imageView) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;

        // Returns null, sizes are in the options variable
        BitmapFactory.decodeFile(path, options);

        ViewGroup.LayoutParams layoutParams = imageView.getLayoutParams();
        scaleImage(layoutParams, options.outHeight, options.outWidth);

        imageView.setLayoutParams(layoutParams);
        Glide.with(imageView.getContext())
                .load(path)
                .into(imageView);
    }

    public static boolean treatAsDownloadable(String text) {
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

            String ref = url.getRef();
            boolean encrypted = ref != null && ref.matches("([A-Fa-f0-9]{2}){48}");

            if (encrypted) {
                if (getExtensionMimeType(extension) != null) {
                    return true;
                } else {
                    return false;
                }
            }

            return true;

        } catch (MalformedURLException e) {
            return false;
        }
    }

    public static String extractRelevantExtension(URL url) {
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


    private static void scaleImage(ViewGroup.LayoutParams layoutParams, int height, int width) {
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

    public static void saveFileToDownloads(File srcFile) throws IOException {
        LogManager.i(FileManager.class, "Saving file to downloads");
        final File dstFile = copyFile(srcFile, Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/" + srcFile.getName());

        String mimeTypeFromExtension = getFileMimeType(dstFile);
        if (mimeTypeFromExtension == null) {
            mimeTypeFromExtension = "application/octet-stream";
        }
        final DownloadManager downloadManager = (DownloadManager) Application.getInstance().getSystemService(Context.DOWNLOAD_SERVICE);
        downloadManager.addCompletedDownload(dstFile.getName(),
                String.format(Application.getInstance().getString(R.string.received_by),
                        Application.getInstance().getString(R.string.application_title_short)),
                true, mimeTypeFromExtension, dstFile.getPath(), dstFile.length(), true);
    }

    public static File copyFile(File srcFile, String dstPath) throws IOException {
        File dstFile = new File(dstPath);

        new File(dstFile.getParent()).mkdirs();

        InputStream in = new FileInputStream(srcFile);
        OutputStream out = new FileOutputStream(dstFile);

        // Transfer bytes from in to out
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        in.close();
        out.close();

        return dstFile;
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

}
