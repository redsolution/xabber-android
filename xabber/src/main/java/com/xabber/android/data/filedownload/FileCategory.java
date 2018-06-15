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

    public static FileCategory determineFileCategory(String mimeType) {
        if (mimeType == null) return FileCategory.file;

        if (mimeType.contains("image/")) {
            return FileCategory.image;

        } else if (mimeType.contains("audio/")) {
            return FileCategory.audio;

        } else if (mimeType.contains("video/")) {
            return FileCategory.video;

        } else if (mimeType.contains("text/") || mimeType.equals("application/json")
                || mimeType.equals("application/xml")
                || mimeType.equals("application/vnd.oasis.opendocument.text")
                || mimeType.equals("application/vnd.oasis.opendocument.graphics")
                || mimeType.equals("application/msword")) {
            return FileCategory.document;

        } else if (mimeType.equals("application/pdf")) {
            return FileCategory.pdf;

        } else if (mimeType.equals("application/vnd.oasis.opendocument.spreadsheet")
                || mimeType.equals("application/vnd.ms-excel")) {
            return FileCategory.table;

        } else if (mimeType.equals("application/vnd.ms-powerpoint")
                || mimeType.equals("application/vnd.oasis.opendocument.presentation")) {
            return FileCategory.presentation;

        } else if (mimeType.equals("application/zip") || mimeType.equals("application/gzip")
                || mimeType.equals("application/x-rar-compressed")
                || mimeType.equals("application/x-tar")
                || mimeType.equals("application/x-7z-compressed")) {
            return FileCategory.archive;

        } else {
            return FileCategory.file;
        }
    }

    public static String getCategoryName(FileCategory category, boolean withHtml) {
        switch (category) {
            case image:
                return withHtml ? "<font color='#1565c0'>Image:</font> " : "Image: ";
            case audio:
                return withHtml ? "<font color='#1565c0'>Audio:</font> " : "Audio: ";
            case video:
                return withHtml ? "<font color='#1565c0'>Video:</font> " : "Video: ";
            case document:
                return withHtml ? "<font color='#1565c0'>Document:</font> " : "Document: ";
            case pdf:
                return withHtml ? "<font color='#1565c0'>PDF:</font> " : "PDF: ";
            case table:
                return withHtml ? "<font color='#1565c0'>Table:</font> " : "Table: ";
            case presentation:
                return withHtml ? "<font color='#1565c0'>Presentation:</font> " : "Presentation: ";
            case archive:
                return withHtml ? "<font color='#1565c0'>Archive:</font> " : "Archive: ";
            default:
                return withHtml ? "<font color='#1565c0'>File:</font> " : "File: ";
        }
    }
}
