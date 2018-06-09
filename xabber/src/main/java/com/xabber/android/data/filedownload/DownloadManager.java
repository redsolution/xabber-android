package com.xabber.android.data.filedownload;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Parcelable;
import android.os.ResultReceiver;
import android.os.StatFs;
import android.support.annotation.Nullable;
import android.util.Log;

import com.xabber.android.data.database.messagerealm.Attachment;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.service.DownloadService;

import rx.subjects.PublishSubject;

public class DownloadManager {

    private static final String LOG_TAG = "DownloadManager";
    private static DownloadManager instance;

    private PublishSubject<ProgressData> progressSubscribe = PublishSubject.create();
    private boolean isDownloading;
    private String attachmentId;

    public static DownloadManager getInstance() {
        if (instance == null) instance = new DownloadManager();
        return instance;
    }

    public PublishSubject<ProgressData> subscribeForProgress() {
        return progressSubscribe;
    }

    public void downloadFile(Attachment attachment, AccountJid accountJid, Context context) {

        if (isDownloading) {
            progressSubscribe.onNext(new ProgressData(0, "Downloading already started", false, attachmentId));
            return;
        }

        isDownloading = true;

        // check space
        if (attachment.getFileSize() >= getAvailableSpace()) {
            Log.d(LOG_TAG, "Not enough space for downloading");
            progressSubscribe.onNext(new ProgressData(0, "Not enough space for downloading", false, attachmentId));
            isDownloading = false;
            return;
        }

        attachmentId = attachment.getUniqueId();
        Intent intent = new Intent(context, DownloadService.class);
        intent.putExtra(DownloadService.KEY_RECEIVER, new DownloadReceiver(new Handler()));
        intent.putExtra(DownloadService.KEY_ATTACHMENT_ID, attachment.getUniqueId());
        intent.putExtra(DownloadService.KEY_ACCOUNT_JID, (Parcelable) accountJid);
        intent.putExtra(DownloadService.KEY_FILE_NAME, attachment.getTitle());
        intent.putExtra(DownloadService.KEY_URL, attachment.getFileUrl());
        intent.putExtra(DownloadService.KEY_FILE_SIZE, attachment.getFileSize());
        context.startService(intent);
        return;
    }

    public void cancelDownload(Context context) {
        Intent intent = new Intent(context, DownloadService.class);
        context.stopService(intent);
    }

    private long getAvailableSpace() {
        StatFs stat = new StatFs(Environment.getExternalStorageDirectory().getPath());
        return (long) stat.getAvailableBlocks() * (long) stat.getBlockSize();
    }

    private class DownloadReceiver extends ResultReceiver {

        public DownloadReceiver(Handler handler) {
            super(handler);
        }

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            super.onReceiveResult(resultCode, resultData);
            switch (resultCode) {
                case DownloadService.UPDATE_PROGRESS_CODE:
                    int currentProgress = resultData.getInt(DownloadService.KEY_PROGRESS);
                    progressSubscribe.onNext(new ProgressData(currentProgress, null, false, attachmentId));
                    break;
                case DownloadService.ERROR_CODE:
                    String error = resultData.getString(DownloadService.KEY_ERROR);
                    progressSubscribe.onNext(new ProgressData(0, error, false, attachmentId));
                    isDownloading = false;
                    break;
                case DownloadService.COMPLETE_CODE:
                    progressSubscribe.onNext(new ProgressData(100, null, true, attachmentId));
                    isDownloading = false;
                    break;
            }
        }
    }

    public class ProgressData {
        final int progress;
        final String error;
        final boolean completed;
        final String attachmentId;

        public ProgressData(int progress, String error, boolean completed, String attachmentId) {
            this.progress = progress;
            this.error = error;
            this.completed = completed;
            this.attachmentId = attachmentId;
        }

        public int getProgress() {
            return progress;
        }

        @Nullable public String getError() {
            return error;
        }

        public boolean isCompleted() {
            return completed;
        }

        public String getAttachmentId() {
            return attachmentId;
        }
    }

}
