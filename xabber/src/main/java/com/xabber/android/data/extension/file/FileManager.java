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
import android.webkit.MimeTypeMap;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.LogManager;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.message.MessageItem;
import com.xabber.android.data.message.MessageManager;

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
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.HttpHeaders;

public class FileManager {

    public static final String[] VALID_IMAGE_EXTENSIONS = {"webp", "jpeg", "jpg", "png", "jpe", "gif"};
    public static final String[] VALID_CRYPTO_EXTENSIONS = {"pgp", "gpg", "otr"};

    private static final String CACHE_DIRECTORY = Environment.getExternalStorageDirectory().getAbsolutePath()
            + "/"  +  Application.getInstance().getString(R.string.application_title_short) + "/Cache/";


    private Set<String> startedDownloads;

    private final static FileManager instance;

    static {
        instance = new FileManager();
        Application.getInstance().addManager(instance);
    }

    public static FileManager getInstance() {
        return instance;
    }

    public FileManager() {
        this.startedDownloads = new ConcurrentSkipListSet<>();
    }

    public static void processFileMessage (final MessageItem messageItem, boolean download) {
        if (!treatAsDownloadable(messageItem.getText())) {
            return;
        }

        LogManager.i(FileManager.class, "Processing file message " + messageItem.getText());

        final URL url;
        try {
            url = new URL(messageItem.getText());
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return;
        }

        File file = messageItem.getFile();

        if (file == null) {
            file = new File(getCachePath(url));
            messageItem.setFile(file);
        }

        if (!file.exists()) {
            if (download && SettingsManager.connectionLoadImages() && FileManager.fileIsImage(messageItem.getFile())) {
                FileManager.getInstance().downloadFile(messageItem, null);
            } else {
                getFileUrlSize(messageItem);
            }
        }
    }

    @NonNull
    private static String getCachePath(URL url) {
        return CACHE_DIRECTORY + url.getHost() + "/" + url.getPath();
    }

    private static void getFileUrlSize(final MessageItem messageItem) {
        LogManager.i(FileManager.class, "Requesting file size");

        AsyncHttpClient client = new AsyncHttpClient();
        client.head(messageItem.getText(), new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                for (Header header : headers) {
                    if (header.getName().equals(HttpHeaders.CONTENT_LENGTH)) {
                        messageItem.setFileSize(Long.parseLong(header.getValue()));
                        MessageManager.getInstance().onChatChanged(messageItem.getChat().getAccount(), messageItem.getChat().getUser(), false);
                        break;
                    }
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {

            }
        });
    }


    public interface ProgressListener {
        void onProgress(long bytesWritten, long totalSize);
        void onFinish(long totalSize);
    }


    public void downloadFile(final MessageItem messageItem, final ProgressListener progressListener) {
        final String downloadUrl = messageItem.getText();
        if (startedDownloads.contains(downloadUrl)) {
            LogManager.i(FileManager.class, "Downloading of file " + downloadUrl + " already started");
            return;
        }
        LogManager.i(FileManager.class, "Downloading file " + downloadUrl);
        startedDownloads.add(downloadUrl);

        final AsyncHttpClient client = new AsyncHttpClient();
        client.setLoggingEnabled(SettingsManager.debugLog());
        client.setResponseTimeout(60 * 1000);

        client.get(downloadUrl, new AsyncHttpResponseHandler() {
            @Override
            public void onStart() {
                super.onStart();
                LogManager.i(FileManager.class, "on download start");
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, final byte[] responseBody) {
                LogManager.i(FileManager.class, "on download onSuccess: " + statusCode);
                saveFile(responseBody, messageItem.getFile(), progressListener);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                LogManager.i(FileManager.class, "on download onFailure: " + statusCode);

            }

            @Override
            public void onProgress(long bytesWritten, long totalSize) {
                if (progressListener != null) {
                    progressListener.onProgress(bytesWritten, totalSize);
                }
            }

            @Override
            public void onFinish() {
                super.onFinish();
                startedDownloads.remove(downloadUrl);
            }
        });
    }

    private static void saveFile(final byte[] responseBody, final File file, final ProgressListener progressListener) {
        LogManager.i(FileManager.class, "Saving file " + file.getPath());

        Application.getInstance().runInBackground(new Runnable() {
            @Override
            public void run() {
                new File(file.getParent()).mkdirs();
                try {
                    BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file));
                    bos.write(responseBody);
                    bos.flush();
                    bos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    file.delete();
                }

                Application.getInstance().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (progressListener != null) {
                            progressListener.onFinish(responseBody.length);
                        }
                    }
                });

            }
        });
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
        intent.setDataAndType(Uri.fromFile(file), getFileMimeType(file));

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

    public static void loadImageFromFile(File file, ImageView imageView) {
        LogManager.i(FileManager.class, "Loading image from file " + file.getPath());

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;

        // Returns null, sizes are in the options variable
        BitmapFactory.decodeFile(file.getAbsolutePath(), options);
        final int height = options.outHeight;
        final int width = options.outWidth;

        ImageScaler imageScaler = new ImageScaler(imageView.getContext(), height, width).invoke();
        imageView.setLayoutParams(new LinearLayout.LayoutParams(imageScaler.getScaledWidth(), imageScaler.getScaledHeight()));
        Glide.with(imageView.getContext()).load(file).crossFade().override(imageScaler.getScaledWidth(), imageScaler.getScaledHeight()).into(imageView);
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

            if (Arrays.asList(WebExtensions.WEB_EXTENSIONS).contains(extension)) {
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
            String extension = filename.substring(dotPosition + 1).toLowerCase();
            // we want the real file extension, not the crypto one
            if (Arrays.asList(VALID_CRYPTO_EXTENSIONS).contains(extension)) {
                return extractRelevantExtension(path.substring(0,dotPosition));
            } else {
                return extension;
            }
        }
        return null;
    }

    private static class ImageScaler {
        private Context context;
        private int height;
        private int width;
        private int scaledWidth;
        private int scaledHeight;

        public ImageScaler(Context context, int height, int width) {
            this.context = context;
            this.height = height;
            this.width = width;
        }

        public int getScaledWidth() {
            return scaledWidth;
        }

        public int getScaledHeight() {
            return scaledHeight;
        }

        public ImageScaler invoke() {
            Resources resources = context.getResources();
            final int maxImageSize = resources.getDimensionPixelSize(R.dimen.max_chat_image_size);
            final int minImageSize = resources.getDimensionPixelSize(R.dimen.min_chat_image_size);

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
            return this;
        }
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

    public static void saveFileToCache(File srcFile, URL url) throws IOException {
        LogManager.i(FileManager.class, "Saving file to cache");
        copyFile(srcFile, getCachePath(url));
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
            e.printStackTrace();
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
            e.printStackTrace();
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
            e.printStackTrace();
            return null;
        } finally {
            if (bos != null) {
                try {
                    bos.flush();
                    bos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return Uri.fromFile(rotateImageFile);
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
