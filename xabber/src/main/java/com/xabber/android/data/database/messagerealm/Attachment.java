package com.xabber.android.data.database.messagerealm;

import android.os.Looper;

import androidx.annotation.Nullable;

import com.xabber.android.data.log.LogManager;

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
        public static final String IS_VOICE = "isVoice";
        public static final String IMAGE_WIDTH = "imageWidth";
        public static final String IMAGE_HEIGHT = "imageHeight";
        public static final String DURATION = "duration";
        public static final String MIME_TYPE = "mimeType";
        public static final String REF_TYPE = "refType";
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

    /**
     * If message contains URL to a voice-recording file
     */
    private boolean isVoice;

    @Nullable
    private Integer imageWidth;

    @Nullable
    private Integer imageHeight;

    private Long fileSize;

    private String mimeType;

    @Nullable
    private String refType;

    /** Duration in seconds */
    private Long duration;

    public Attachment() {
        this.uniqueId = UUID.randomUUID().toString();
    }

    public String getUniqueId() {
        if (Looper.myLooper() != Looper.getMainLooper())
            LogManager.exception(Attachment.class.getSimpleName(), new IllegalStateException("Tried read from non UI"));
        return uniqueId;
    }

    public String getFilePath() {
        if (Looper.myLooper() != Looper.getMainLooper())
            LogManager.exception(Attachment.class.getSimpleName(), new IllegalStateException("Tried read from non UI"));
        return filePath;
    }

    public void setFilePath(String filePath) {
        if (Looper.myLooper() == Looper.getMainLooper())
            LogManager.exception(Attachment.class.getSimpleName(), new IllegalStateException("Tried to write on UI"));
        this.filePath = filePath;
    }

    public boolean isImage() {
        if (Looper.myLooper() != Looper.getMainLooper())
            LogManager.exception(Attachment.class.getSimpleName(), new IllegalStateException("Tried read from non UI"));
        return isImage;
    }

    public void setIsImage(boolean isImage) {
        if (Looper.myLooper() == Looper.getMainLooper())
            LogManager.exception(Attachment.class.getSimpleName(), new IllegalStateException("Tried to write on UI"));
        this.isImage = isImage;
    }

    public boolean isVoice() {
        if (Looper.myLooper() != Looper.getMainLooper())
            LogManager.exception(Attachment.class.getSimpleName(), new IllegalStateException("Tried read from non UI"));
        return isVoice;
    }

    public void setIsVoice(boolean isVoice) {
        if (Looper.myLooper() == Looper.getMainLooper())
            LogManager.exception(Attachment.class.getSimpleName(), new IllegalStateException("Tried to write on UI"));
        this.isVoice = isVoice;
    }

    @Nullable
    public Integer getImageWidth() {
        if (Looper.myLooper() != Looper.getMainLooper())
            LogManager.exception(Attachment.class.getSimpleName(), new IllegalStateException("Tried read from non UI"));
        return imageWidth;
    }

    public void setImageWidth(@Nullable Integer imageWidth) {
        if (Looper.myLooper() == Looper.getMainLooper())
            LogManager.exception(Attachment.class.getSimpleName(), new IllegalStateException("Tried to write on UI"));
        this.imageWidth = imageWidth;
    }

    @Nullable
    public Integer getImageHeight() {
        if (Looper.myLooper() != Looper.getMainLooper())
            LogManager.exception(Attachment.class.getSimpleName(), new IllegalStateException("Tried read from non UI"));
        return imageHeight;
    }

    public void setImageHeight(@Nullable Integer imageHeight) {
        if (Looper.myLooper() == Looper.getMainLooper())
            LogManager.exception(Attachment.class.getSimpleName(), new IllegalStateException("Tried to write on UI"));
        this.imageHeight = imageHeight;
    }

    public String getFileUrl() {
        if (Looper.myLooper() != Looper.getMainLooper())
            LogManager.exception(Attachment.class.getSimpleName(), new IllegalStateException("Tried read from non UI"));
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        if (Looper.myLooper() == Looper.getMainLooper())
            LogManager.exception(Attachment.class.getSimpleName(), new IllegalStateException("Tried to write on UI"));
        this.fileUrl = fileUrl;
    }

    public Long getFileSize() {
        if (Looper.myLooper() != Looper.getMainLooper())
            LogManager.exception(Attachment.class.getSimpleName(), new IllegalStateException("Tried read from non UI"));
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        if (Looper.myLooper() == Looper.getMainLooper())
            LogManager.exception(Attachment.class.getSimpleName(), new IllegalStateException("Tried to write on UI"));
        this.fileSize = fileSize;
    }

    public String getTitle() {
        if (Looper.myLooper() != Looper.getMainLooper())
            LogManager.exception(Attachment.class.getSimpleName(), new IllegalStateException("Tried read from non UI"));
        return title;
    }

    public void setTitle(String title) {
        if (Looper.myLooper() == Looper.getMainLooper())
            LogManager.exception(Attachment.class.getSimpleName(), new IllegalStateException("Tried to write on UI"));
        this.title = title;
    }

    public String getMimeType() {
        if (Looper.myLooper() != Looper.getMainLooper())
            LogManager.exception(Attachment.class.getSimpleName(), new IllegalStateException("Tried read from non UI"));
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        if (Looper.myLooper() == Looper.getMainLooper())
            LogManager.exception(Attachment.class.getSimpleName(), new IllegalStateException("Tried to write on UI"));
        this.mimeType = mimeType;
    }

    public Long getDuration() {
        if (Looper.myLooper() != Looper.getMainLooper())
            LogManager.exception(Attachment.class.getSimpleName(), new IllegalStateException("Tried read from non UI"));
        return duration;
    }

    public void setDuration(Long duration) {
        if (Looper.myLooper() == Looper.getMainLooper())
            LogManager.exception(Attachment.class.getSimpleName(), new IllegalStateException("Tried to write on UI"));
        this.duration = duration;
    }

    @Nullable
    public String getRefType() {
        if (Looper.myLooper() != Looper.getMainLooper())
            LogManager.exception(Attachment.class.getSimpleName(), new IllegalStateException("Tried read from non UI"));
        return refType;
    }

    public void setRefType(String refType) {
        if (Looper.myLooper() == Looper.getMainLooper())
            LogManager.exception(Attachment.class.getSimpleName(), new IllegalStateException("Tried to write on UI"));
        this.refType = refType;
    }
}
