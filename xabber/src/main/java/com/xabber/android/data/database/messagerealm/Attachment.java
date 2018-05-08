package com.xabber.android.data.database.messagerealm;

import android.support.annotation.Nullable;

import java.util.UUID;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;
import io.realm.annotations.Required;

public class Attachment extends RealmObject {

    public static class Fields {
        public static final String UNIQUE_ID = "uniqueId";
        public static final String TITLE = "title";
        public static final String FILE_PATH = "filePath";
        public static final String FILE_URL = "fileUrl";
        public static final String FILE_SIZE = "fileSize";
        public static final String IS_IMAGE = "isImage";
        public static final String IMAGE_WIDTH = "imageWidth";
        public static final String IMAGE_HEIGHT = "imageHeight";
        public static final String DURATION = "duration";
        public static final String MIME_TYPE = "mimeType";
    }

    @PrimaryKey
    @Required
    private String uniqueId;

    private String title;

    private String fileUrl;

    /**
     * If message "contains" file with local file path
     */
    private String filePath;

    /**
     * If message contains URL to image (and may be drawn as image)
     */
    private boolean isImage;

    @Nullable
    private Integer imageWidth;

    @Nullable
    private Integer imageHeight;

    private Long fileSize;

    private String mimeType;

    /** Duration in seconds */
    private Long duration;

    public Attachment() {
        this.uniqueId = UUID.randomUUID().toString();
    }

    public String getUniqueId() {
        return uniqueId;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public boolean isImage() {
        return isImage;
    }

    public void setIsImage(boolean isImage) {
        this.isImage = isImage;
    }

    @Nullable
    public Integer getImageWidth() {
        return imageWidth;
    }

    public void setImageWidth(@Nullable Integer imageWidth) {
        this.imageWidth = imageWidth;
    }

    @Nullable
    public Integer getImageHeight() {
        return imageHeight;
    }

    public void setImageHeight(@Nullable Integer imageHeight) {
        this.imageHeight = imageHeight;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public Long getDuration() {
        return duration;
    }

    public void setDuration(Long duration) {
        this.duration = duration;
    }
}
