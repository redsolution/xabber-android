package com.xabber.android.data.filedownload;

public enum FileCategory {
    image,
    audio,
    video,
    document,
    pdf,
    table,
    presentation,
    archive,
    file;

    public static FileCategory determineFileCategory(String fileExtension) {

        if (fileExtension.contains("image/")) {
            return FileCategory.image;

        } else if (fileExtension.contains("audio/")) {
            return FileCategory.audio;

        } else if (fileExtension.contains("video/")) {
            return FileCategory.video;

        } else if (fileExtension.contains("text/") || fileExtension.equals("application/json")
                || fileExtension.equals("application/xml")
                || fileExtension.equals("application/vnd.oasis.opendocument.text")
                || fileExtension.equals("application/vnd.oasis.opendocument.graphics")
                || fileExtension.equals("application/msword")) {
            return FileCategory.document;

        } else if (fileExtension.equals("application/pdf")) {
            return FileCategory.pdf;

        } else if (fileExtension.equals("application/vnd.oasis.opendocument.spreadsheet")
                || fileExtension.equals("application/vnd.ms-excel")) {
            return FileCategory.table;

        } else if (fileExtension.equals("application/vnd.ms-powerpoint")
                || fileExtension.equals("application/vnd.oasis.opendocument.presentation")) {
            return FileCategory.presentation;

        } else if (fileExtension.equals("application/zip") || fileExtension.equals("application/gzip")
                || fileExtension.equals("application/x-rar-compressed")
                || fileExtension.equals("application/x-tar")
                || fileExtension.equals("application/x-7z-compressed")) {
            return FileCategory.archive;

        } else {
            return FileCategory.file;
        }
    }
}
