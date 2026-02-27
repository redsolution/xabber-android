package com.xabber.android.data.filedownload;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Parcelable;
import android.os.ResultReceiver;
import android.os.StatFs;
import android.util.Log;

import androidx.annotation.Nullable;

import com.xabber.android.data.Application;
import com.xabber.android.data.database.realmobjects.ReferenceRealmObject;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.service.DownloadService;

import java.util.HashMap;
import java.util.LinkedList;

import rx.subjects.PublishSubject;

public class DownloadManager {

    private static final String LOG_TAG = "DownloadManager";
    private static DownloadManager instance;

    private final PublishSubject<ProgressData> progressSubscribe = PublishSubject.create();
    private boolean isDownloading;
    private String attachmentId;
    private final LinkedList<ReferenceRealmObject> downloadQueue = new LinkedList<>();
    private final HashMap<String, AccountJid> accountAttachments = new HashMap<>();

    public static DownloadManager getInstance() {
        if (instance == null) instance = new DownloadManager();
        return instance;
    }

    public PublishSubject<ProgressData> subscribeForProgress() {
        return progressSubscribe;
    }

    public void downloadFile(ReferenceRealmObject referenceRealmObject, AccountJid accountJid, Context context) {

        if (isDownloading) {
            if (downloadQueue.size() >= 10) {
                progressSubscribe.onNext(new ProgressData(0, "Downloading already started", false, attachmentId));
            } else {
                boolean duplicate = false;
                for (int i = 0; i < downloadQueue.size(); i++) {
                    if (downloadQueue.get(i).getUniqueId().equals(referenceRealmObject.getUniqueId())) {
                        duplicate = true; //already have this file in the queue
                    }
                }
                if (attachmentId != null && attachmentId.equals(referenceRealmObject.getUniqueId()))
                    duplicate = true; //already downloading this file

                if (!duplicate) {
                    downloadQueue.offer(referenceRealmObject);
                    accountAttachments.put(referenceRealmObject.getUniqueId(), accountJid);
                    LogManager.d(LOG_TAG + "/PUT_IN_QUEUE", "attachment id = " + referenceRealmObject.getUniqueId() + " account = " + accountJid);
                }
            }
            return;
        }

        isDownloading = true;

        // check space
        if (referenceRealmObject.getFileSize() >= getAvailableSpace()) {
            Log.d(LOG_TAG, "Not enough space for downloading");
            progressSubscribe.onNext(new ProgressData(0, "Not enough space for downloading", false, attachmentId));
            isDownloading = false;
            return;
        }

        attachmentId = referenceRealmObject.getUniqueId();
        accountAttachments.put(attachmentId, accountJid);
        LogManager.d(LOG_TAG + "/ACTIVE_DOWNLOAD", "attachment id = " + referenceRealmObject.getUniqueId() + " account = " + accountJid);
        Intent intent = new Intent(context, DownloadService.class);
        intent.putExtra(DownloadService.KEY_RECEIVER, new DownloadReceiver(new Handler()));
        intent.putExtra(DownloadService.KEY_ATTACHMENT_ID, referenceRealmObject.getUniqueId());
        intent.putExtra(DownloadService.KEY_ACCOUNT_JID, (Parcelable) accountJid);
        intent.putExtra(DownloadService.KEY_FILE_NAME, referenceRealmObject.getTitle());
        intent.putExtra(DownloadService.KEY_URL, referenceRealmObject.getFileUrl());
        intent.putExtra(DownloadService.KEY_FILE_SIZE, referenceRealmObject.getFileSize());
        context.startService(intent);
    }

    private void nextDownload() {
        accountAttachments.remove(attachmentId);
        if (downloadQueue.size() > 0) {
            ReferenceRealmObject first = downloadQueue.poll();
            if (first != null)
                downloadFile(first, accountAttachments.get(first.getUniqueId()), Application.getInstance().getApplicationContext());
        }
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

        DownloadReceiver(Handler handler) {
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
                    nextDownload();
                    break;
                case DownloadService.COMPLETE_CODE:
                    progressSubscribe.onNext(new ProgressData(100, null, true, attachmentId));
                    isDownloading = false;
                    nextDownload();
                    break;
            }
        }
    }

    public static class ProgressData {
        final int progress;
        final String error;
        final boolean completed;
        final String attachmentId;

        ProgressData(int progress, String error, boolean completed, String attachmentId) {
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
