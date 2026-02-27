package com.xabber.android.data.extension.file;

import android.content.Intent;
import android.graphics.BitmapFactory;
import android.media.ExifInterface;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;

import com.xabber.android.BuildConfig;
import com.xabber.android.data.Application;
import com.xabber.android.data.extension.httpfileupload.HttpFileUploadManager;
import com.xabber.android.data.log.LogManager;

import org.apache.commons.io.FilenameUtils;

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
import java.util.UUID;

public class FileManager {

    public static final String LOG_TAG = FileManager.class.getSimpleName();

    private static final String[] VALID_IMAGE_EXTENSIONS = {"webp", "jpeg", "jpg", "png", "jpe", "gif"};

    private final static FileManager instance;
    private static final String XABBER_DIR = "Xabber";
    private static final String XABBER_AUDIO_DIR = "Xabber Audio";

    static {
        instance = new FileManager();
    }

    public static FileManager getInstance() {
        return instance;
    }

    public static boolean fileIsImage(File file) {
        return extensionIsImage(extractRelevantExtension(file.getPath()));
    }

    public static boolean extensionIsImage(String extension) {
        return Arrays.asList(VALID_IMAGE_EXTENSIONS).contains(extension);
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
            if (!url.getProtocol().equalsIgnoreCase("http")
                    && !url.getProtocol().equalsIgnoreCase("https")) {
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

    public static String extractFileName(String uri) {
        if (uri == null || uri.isEmpty()) {
            return null;
        }

        return uri.substring(uri.lastIndexOf('/') + 1).toLowerCase();
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

    public static boolean isImageSizeGreater(Uri srcUri, int maxSize) {
        final String srcPath = FileUtils.getPath(Application.getInstance(), srcUri);
        if (srcPath == null) {
            return false;
        }

        FileInputStream fis;
        try {
            fis = new FileInputStream(srcPath);
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

    public static boolean isImageNeededDimensionsFlip(Uri srcUri) {
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
            case ExifInterface.ORIENTATION_TRANSPOSE:
            case ExifInterface.ORIENTATION_ROTATE_90:
            case ExifInterface.ORIENTATION_TRANSVERSE:
            case ExifInterface.ORIENTATION_ROTATE_270:
                return true;

            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
            case ExifInterface.ORIENTATION_ROTATE_180:
            case ExifInterface.ORIENTATION_FLIP_VERTICAL:
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

    @Nullable
    public static Uri savePNGImage(byte[] data, String fileName) {
        final File rotateImageFile;
        BufferedOutputStream bos = null;
        try {
            rotateImageFile = createTempPNGImageFile(fileName);
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

    public static File createTempOpusFile(String name) throws IOException {
        return File.createTempFile(name, ".opus", Application.getInstance().getCacheDir());
    }

    public File createAudioFile(String name) {
        // create dir
        File directory = new File(getDownloadDirPath());
        if (!directory.exists()) {
            if (!directory.mkdir()) {
                LogManager.e(LOG_TAG, "Can't create a folder " + getDownloadDirPath());
                return null;
            }
        }
        directory = new File(getSpecificDownloadDirPath());
        if (!directory.exists()) {
            if (!directory.mkdir()) {
                LogManager.e(LOG_TAG, "Can't create a folder " + getSpecificDownloadDirPath());
                return null;
            }
        }

        // create file

        String filePath = directory.getPath() + File.separator
                + FilenameUtils.getBaseName(name) + "_"
                + System.currentTimeMillis() / 1000 + "."
                + FilenameUtils.getExtension(name);
        File file = new File(filePath);

        if (file.exists()) {
            file = new File(directory.getPath() + File.separator +
                    FileManager.generateUniqueNameForFile(directory.getPath()
                            + File.separator, name));
            return file;
        }
        return file;
    }


    /**
     * Makes a copy of the source file and then deletes it.
     *
     * @param source file that will be copied and subsequently deleted
     * @param dest   file the data will be copied to
     * @return success of copying
     */
    public static boolean copy(File source, File dest) {
        boolean success = true;
        try {
            InputStream in = new FileInputStream(source);
            try {
                OutputStream out = new FileOutputStream(dest);
                try {
                    byte[] buf = new byte[1024];
                    int len;
                    while ((len = in.read(buf)) > 0) {
                        out.write(buf, 0, len);
                    }
                } finally {
                    out.close();
                }
            } finally {
                in.close();
            }
        } catch (IOException e) {
            success = false;
        }
        if (success)
            deleteTempFile(source);
        return success;
    }

    public static void deleteTempFile(File fileOrig) {
        if (fileOrig.exists()) {
            fileOrig.delete();
        }
    }

    public static void deleteTempFile(String filePath) {
        File file = new File(filePath);
        if (file.exists()) {
            file.delete();
        }
    }

    private String getDownloadDirPath() {
        return Application.getInstance().getExternalFilesDir(null).getPath()
                + File.separator + XABBER_DIR;
    }

    private String getSpecificDownloadDirPath() {
        return getDownloadDirPath() + File.separator + XABBER_AUDIO_DIR;
    }

    public static File createTempPNGImageFile(String name) throws IOException {
        // Create an image file name
        return File.createTempFile(
                name,  /* prefix */
                ".png",         /* suffix */
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

    /**
     * For java 6
     */
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
        String baseName = FilenameUtils.getBaseName(sourceName);
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

    public boolean deleteFile(File file) {
        boolean deletedAll = true;
        if (file != null) {
            if (file.isDirectory()) {
                String[] children = file.list();
                if (children != null) {
                    for (String child : children) {
                        deletedAll = deleteFile(new File(file, child)) && deletedAll;
                    }
                }
            } else {
                deletedAll = file.delete();
            }
        }

        return deletedAll;
    }

}
