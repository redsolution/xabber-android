package com.xabber.android.service;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.ResultReceiver;
import android.support.annotation.Nullable;
import android.util.Log;

import com.xabber.android.data.database.MessageDatabaseManager;
import com.xabber.android.data.database.messagerealm.Attachment;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.extension.file.FileManager;
import com.xabber.android.utils.HttpClientWithMTM;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import io.realm.Realm;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class DownloadService extends IntentService {

    private static final String LOG_TAG = "DownloadService";
    private final static String SERVICE_NAME = "Download Service";
    public static final int UPDATE_PROGRESS_CODE = 3132;
    public static final int ERROR_CODE = 3133;
    public static final int COMPLETE_CODE = 3134;
    private static final String XABBER_DIR = "Xabber";

    public final static String KEY_ATTACHMENT_ID = "attachment_id";
    public final static String KEY_RECEIVER = "receiver";
    public final static String KEY_PROGRESS = "progress";
    public final static String KEY_ACCOUNT_JID = "account_jid";
    public final static String KEY_FILE_NAME = "file_name";
    public final static String KEY_FILE_SIZE = "file_size";
    public final static String KEY_URL = "url";
    public final static String KEY_ERROR = "error";

    private ResultReceiver receiver;
    private String attachmentId;
    private boolean needStop = false;

    public DownloadService() {
        super(SERVICE_NAME);
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        if (intent == null) return;
        this.receiver = intent.getParcelableExtra(KEY_RECEIVER);
        this.attachmentId = intent.getStringExtra(KEY_ATTACHMENT_ID);
        String fileName = intent.getStringExtra(KEY_FILE_NAME);
        long fileSize = intent.getLongExtra(KEY_FILE_SIZE, 0);
        String url = intent.getStringExtra(KEY_URL);
        AccountJid accountJid = intent.getParcelableExtra(KEY_ACCOUNT_JID);

        // build http client
        OkHttpClient client = HttpClientWithMTM.getClient(accountJid);

        // start download
        if (client != null) requestFileDownload(fileName, fileSize, url, client);
        else publishError("Downloading not started");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        needStop = true;
    }

    private void requestFileDownload(final String fileName, final long fileSize, String url, OkHttpClient client) {
        Request request = new Request.Builder().url(url).build();
        try {
            Response response = client.newCall(request).execute();
            if (!response.isSuccessful()) {
                Log.d(LOG_TAG, "download onFailure " + response.toString());
                publishError(response.toString());
                return;
            }

            // create dir
            File directory = new File(getDownloadDirPath());
            if (!directory.exists())
                if (!directory.mkdir()) {
                    publishError("Directory not created");
                    return;
                }

            // create file
            String filePath = directory.getPath() + File.separator + fileName;
            File file = new File(filePath);

            if (file.exists()) {
                file = new File(directory.getPath() + File.separator +
                        FileManager.generateUniqueNameForFile(directory.getPath()
                                + File.separator, fileName));
            }

            if (file.createNewFile()) {

                // download
                FileOutputStream fos = new FileOutputStream(file);
                byte [] buffer = new byte [8192];
                int r;

                int downloadedBytes = 0;
                while ((r = response.body().byteStream().read(buffer)) > 0) {
                    if (!needStop) {
                        fos.write(buffer, 0, r);
                        downloadedBytes += r;
                        publishProgress(downloadedBytes, fileSize);
                    } else {
                        fos.close();
                        file.delete();
                        publishError("Download aborted");
                        return;
                    }
                }
                fos.flush();
                fos.close();

                // save path to realm
                saveAttachmentPathToRealm(file.getPath());
            } else publishError("File not created");

        } catch (IOException e) {
            Log.d(LOG_TAG, "download onFailure " + e.getMessage());
            publishError(e.getMessage());
        }
    }

    private void saveAttachmentPathToRealm(final String path) {
        MessageDatabaseManager.getInstance().getNewBackgroundRealm().executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                Attachment attachment = realm.where(Attachment.class)
                        .equalTo(Attachment.Fields.UNIQUE_ID, attachmentId).findFirst();
                attachment.setFilePath(path);
                publishCompleted();
            }
        });
    }

    private void publishProgress(long downloadedBytes, long fileSize) {
        int progress = (int) Math.round((double) downloadedBytes / (double) fileSize * 100.d);
        Bundle resultData = new Bundle();
        resultData.putInt(KEY_PROGRESS, progress);
        receiver.send(UPDATE_PROGRESS_CODE, resultData);
    }

    private void publishCompleted() {
        Bundle resultData = new Bundle();
        receiver.send(COMPLETE_CODE, resultData);
    }

    private void publishError(String error) {
        Bundle resultData = new Bundle();
        resultData.putString(KEY_ERROR, error);
        receiver.send(ERROR_CODE, resultData);
    }

    private static String getDownloadDirPath() {
        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath()
                + File.separator + XABBER_DIR;
    }
}
