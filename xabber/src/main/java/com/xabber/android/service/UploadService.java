package com.xabber.android.service;

import android.app.IntentService;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.ResultReceiver;
import android.provider.OpenableColumns;
import android.support.annotation.Nullable;
import android.webkit.MimeTypeMap;

import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.UserJid;
import com.xabber.android.data.extension.file.FileManager;
import com.xabber.android.data.extension.file.FileUtils;
import com.xabber.android.data.extension.httpfileupload.ImageCompressor;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.data.message.MessageManager;
import com.xabber.android.utils.HttpClientWithMTM;
import com.xabber.xmpp.httpfileupload.Slot;

import org.apache.commons.io.FilenameUtils;
import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.StanzaCollector;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Stanza;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class UploadService extends IntentService {

    private final static String SERVICE_NAME = "Upload Service";
    private static final MediaType CONTENT_TYPE = MediaType.parse("application/octet-stream");
    private static final String XABBER_COMPRESSED_DIR = "Xabber/temp";
    private static final String XABBER_DIR = "Xabber";

    public final static String KEY_RECEIVER = "receiver";
    public final static String KEY_ACCOUNT_JID = "account_jid";
    public final static String KEY_USER_JID = "user_jid";
    public final static String KEY_FILE_PATHS = "file_paths";
    public final static String KEY_FILE_URIS = "file_uris";
    public final static String KEY_UPLOAD_SERVER_URL = "upload_server_url";
    public final static String KEY_FILE_COUNT = "file_count";
    public final static String KEY_PROGRESS = "progress";
    public final static String KEY_ERROR = "error";
    public final static String KEY_MESSAGE_ID = "message_id";

    public static final int UPDATE_PROGRESS_CODE = 2232;
    public static final int ERROR_CODE = 2233;
    public static final int COMPLETE_CODE = 2234;

    private ResultReceiver receiver;
    private boolean needStop = false;

    public UploadService() {
        super(SERVICE_NAME);
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        if (intent == null) return;
        this.receiver = intent.getParcelableExtra(KEY_RECEIVER);
        AccountJid account = intent.getParcelableExtra(KEY_ACCOUNT_JID);
        UserJid user = intent.getParcelableExtra(KEY_USER_JID);
        List<String> filePaths = intent.getStringArrayListExtra(KEY_FILE_PATHS);
        List<Uri> fileUris = intent.getParcelableArrayListExtra(KEY_FILE_URIS);
        CharSequence uploadServerUrl = intent.getCharSequenceExtra(KEY_UPLOAD_SERVER_URL);
        String existMessageId = intent.getStringExtra(KEY_MESSAGE_ID);

        if (filePaths != null) startWork(account, user, filePaths, uploadServerUrl, existMessageId);
        else if (fileUris != null) startWorkWithUris(account, user, fileUris, uploadServerUrl);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        needStop = true;
    }

    private void startWorkWithUris(AccountJid account, UserJid user, List<Uri> fileUris,
                           CharSequence uploadServerUrl) {

        // determine which files are local or remote
        List<File> files = new ArrayList<>();
        List<Uri> remoteFiles = new ArrayList<>();
        for (Uri uri : fileUris) {
            String path = FileUtils.getPath(this, uri);
            if (path != null) files.add(new File(path));
            else remoteFiles.add(uri);
        }

        // create message with progress
        String messageId = MessageManager.getInstance().createFileMessage(account, user, files);

        // create dir
        File directory = new File(getDownloadDirPath());
        if (!directory.exists())
            if (!directory.mkdir()) {
                publishError(messageId, "Directory not created");
                return;
            }

        // get files from uri's
        List<File> copiedFiles = new ArrayList<>();
        for (Uri uri : remoteFiles) {
            if (needStop) {
                stopWork(messageId);
                return;
            }

            // copy file to local storage if need
            try {
                copiedFiles.add(new File(copyFileToLocalStorage(uri)));
            } catch (IOException e) {
                publishError(messageId, "Cannot get file: " + e.toString());
            }
            publishProgress(messageId, copiedFiles.size(), remoteFiles.size());
        }

        // add attachments to message
        MessageManager.getInstance().updateMessageWithNewAttachments(messageId, copiedFiles);

        // startWork for upload files
        List<String> filePaths = new ArrayList<>();
        for (File file : files)
            filePaths.add(file.getPath());
        for (File file : copiedFiles)
            filePaths.add(file.getPath());
        startWork(account, user, filePaths, uploadServerUrl, messageId);
    }

    private void startWork(AccountJid account, UserJid user, List<String> filePaths,
                           CharSequence uploadServerUrl, String existMessageId) {

        // get account item
        AccountItem accountItem = AccountManager.getInstance().getAccount(account);
        if (accountItem == null) {
            publishError(null, "Account not found");
            return;
        }

        // get upload jid
        Jid uploadJid;
        try {
            uploadJid = JidCreate.bareFrom(uploadServerUrl);
        } catch (XmppStringprepException e) {
            publishError(null, "Wrong upload jid");
            return;
        }

        final String fileMessageId;
        if (existMessageId == null) { // create fileMessage with files
            List<File> files = new ArrayList<>();
            for (String filePath : filePaths) {
                files.add(new File(filePath));
            }
            fileMessageId = MessageManager.getInstance().createFileMessage(account, user, files);
        } else fileMessageId = existMessageId; // use existing fileMessage

        HashMap<String, String> uploadedFilesUrls = new HashMap<>();
        List<String> notUploadedFilesPaths = new ArrayList<>();
        List<File> notUploadedFiles = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        for (String filePath : filePaths) {
            if (needStop) {
                stopWork(fileMessageId);
                return;
            }

            try {
                File uncompressedFile = new File(filePath);
                final File file;

                // compress file if image
                if (FileManager.fileIsImage(uncompressedFile)) {
                    file = ImageCompressor.compressImage(uncompressedFile, getCompressedDirPath());
                    if (file == null)
                        throw new Exception("Compress image failed");
                } else file = uncompressedFile;

                // request slot
                Stanza slot = requestSlot(accountItem, file, uploadJid);
                if (!(slot instanceof Slot))
                    throw new Exception("Could not request upload slot");

                // upload file
                Response response = uploadFileToSlot(account, (Slot) slot, file);
                if (response.isSuccessful())
                    uploadedFilesUrls.put(filePath, ((Slot) slot).getGetUrl());
                else throw new Exception("Upload failed: " + response.message());

            } catch (Exception e) {
                notUploadedFilesPaths.add(filePath);
                notUploadedFiles.add(new File(filePath));
                errors.add(e.toString());
            }

            publishProgress(fileMessageId, uploadedFilesUrls.size(), filePaths.size());
        }

        removeTempDirectory();

        // check that files are uploaded
        if (uploadedFilesUrls.size() == 0) {
            setErrorForMessage(fileMessageId, generateErrorDescriptionForFiles(notUploadedFilesPaths, errors));
            publishError(fileMessageId, "Could not upload any files");
            return;
        }

        // save results to Realm and send message
        MessageManager.getInstance().updateFileMessage(account, user, fileMessageId,
                uploadedFilesUrls, notUploadedFilesPaths);
        publishCompleted(fileMessageId);

        // if some files have errors move its to separate message
        if (notUploadedFilesPaths.size() > 0) {
            String messageId = MessageManager.getInstance().createFileMessage(account, user, notUploadedFiles);
            setErrorForMessage(messageId, generateErrorDescriptionForFiles(notUploadedFilesPaths, errors));
        }
    }

    private void removeTempDirectory() {
        File tempDirectory = new File(getCompressedDirPath());
        FileManager.deleteDirectoryRecursion(tempDirectory);
    }

    private void stopWork(String messageId) {
        removeTempDirectory();
        publishError(messageId, "Uploading aborted");
        MessageManager.getInstance().removeMessage(messageId);
    }

    private void setErrorForMessage(String fileMessageId, String error) {
        MessageManager.getInstance().updateMessageWithError(fileMessageId, error);
    }

    private void publishProgress(String fileMessageId, int uploadedFiles, int fileCount) {
        Bundle resultData = new Bundle();
        resultData.putInt(KEY_PROGRESS, uploadedFiles);
        resultData.putInt(KEY_FILE_COUNT, fileCount);
        resultData.putString(KEY_MESSAGE_ID, fileMessageId);
        receiver.send(UPDATE_PROGRESS_CODE, resultData);
    }

    private void publishCompleted(String fileMessageId) {
        Bundle resultData = new Bundle();
        resultData.putString(KEY_MESSAGE_ID, fileMessageId);
        receiver.send(COMPLETE_CODE, resultData);
    }

    private void publishError(String fileMessageId, String error) {
        Bundle resultData = new Bundle();
        resultData.putString(KEY_ERROR, error);
        resultData.putString(KEY_MESSAGE_ID, fileMessageId);
        receiver.send(ERROR_CODE, resultData);
        LogManager.e(this, error);
    }

    private Stanza requestSlot(AccountItem accountItem, File file, Jid uploadServerUrl)
            throws InterruptedException, XMPPException.XMPPErrorException,
            SmackException.NotConnectedException, SmackException.NoResponseException {

        com.xabber.xmpp.httpfileupload.Request request = new com.xabber.xmpp.httpfileupload.Request();
        request.setFilename(file.getName());
        request.setSize(String.valueOf(file.length()));
        request.setTo(uploadServerUrl);

        return sendIqRequestAndWaitForResponse(accountItem.getConnection(), request);
    }

    private <I extends IQ> I sendIqRequestAndWaitForResponse(AbstractXMPPConnection connection, IQ request)
            throws SmackException.NotConnectedException, InterruptedException, XMPPException.XMPPErrorException,
            SmackException.NoResponseException {
        StanzaCollector collector = connection.createStanzaCollectorAndSend(request);
        IQ resultResponse = collector.nextResultOrThrow();
        @SuppressWarnings("unchecked")
        I concreteResultResponse = (I) resultResponse;
        return concreteResultResponse;
    }

    private Response uploadFileToSlot(final AccountJid account, final Slot slot, final File file)
            throws IOException, NoSuchAlgorithmException, KeyManagementException {

        OkHttpClient client = HttpClientWithMTM.getClient(account);

        Request request = new Request.Builder()
                .url(slot.getPutUrl())
                .put(RequestBody.create(CONTENT_TYPE, file))
                .build();

        if (client != null) return client.newCall(request).execute();
        else throw new IOException("Upload failed: failed to create httpclient");
    }

    private static String getCompressedDirPath() {
        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath()
                + File.separator + XABBER_COMPRESSED_DIR;
    }

    private static String getDownloadDirPath() {
        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath()
                + File.separator + XABBER_DIR;
    }

    private String generateErrorDescriptionForFiles(List<String> files, List<String> errors) {
        StringBuilder stringBuilder = new StringBuilder();
        int i = 0;
        for (String file : files) {
            stringBuilder.append(file.substring(file.lastIndexOf("/")).substring(1));
            stringBuilder.append(":\n");
            stringBuilder.append(errors.size() > i ? errors.get(i) : "no description");
            stringBuilder.append("\n\n");
            i++;
        }
        return stringBuilder.toString();
    }

    private String copyFileToLocalStorage(Uri uri) throws IOException {
        String extension = getExtensionFromUri(uri);
        String name = getFileName(uri);
        if (name == null) name = UUID.randomUUID().toString();
        else name = name.replace(".", "");
        String fileName = name + "." + extension;
        File file = new File(getDownloadDirPath(),  fileName);

        OutputStream os = null;
        InputStream is = null;

        if (file.exists()) {
            file = new File(getDownloadDirPath(),
                    FileManager.generateUniqueNameForFile(getDownloadDirPath(), fileName));
        }

        if (file.createNewFile()) {
            os = new FileOutputStream(file);
            is = getContentResolver().openInputStream(uri);

            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
            os.flush();
            os.close();
            is.close();
        }
        return file.getPath();
    }

    private String getExtensionFromUri(Uri uri) {
        String mimeType = getContentResolver().getType(uri);
        return MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);
    }

    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
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
