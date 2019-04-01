package com.xabber.android.data.extension.file;

import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.webkit.MimeTypeMap;

import com.xabber.android.data.Application;

import org.apache.commons.io.FilenameUtils;

import java.util.UUID;

public class UriUtils {

    public static boolean uriIsImage(Uri uri) {
        return FileManager.extensionIsImage(getExtensionFromUri(uri));
    }

    public static String getFullFileName(Uri uri) {
        String extension = getExtensionFromUri(uri);
        String name = getFileName(uri);
        if (name == null) name = UUID.randomUUID().toString();
        else name = name.replace(".", "");
        String fileName = name + "." + extension;
        return fileName;
    }

    public static String getMimeType(Uri uri) {
        String type = Application.getInstance().getContentResolver().getType(uri);
        if (type == null || type.isEmpty()) type = "*/*";
        return type;
    }

    private static String getExtensionFromUri(Uri uri) {
        String mimeType = Application.getInstance().getContentResolver().getType(uri);
        return MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);
    }

    private static String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = Application.getInstance().getContentResolver()
                    .query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            } finally {
                cursor.close();
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return FilenameUtils.getBaseName(result);
    }

}
