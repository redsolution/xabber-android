package com.xabber.android.service;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.ResultReceiver;
import android.support.annotation.Nullable;

import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.connection.CertificateManager;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.UserJid;
import com.xabber.android.data.extension.file.FileManager;
import com.xabber.android.data.extension.httpfileupload.ImageCompressor;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.data.message.MessageManager;
import com.xabber.xmpp.httpfileupload.Slot;

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
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509TrustManager;

import de.duenndns.ssl.MemorizingTrustManager;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class UploadService extends IntentService {

    private final static String SERVICE_NAME = "Upload Service";
    private static final MediaType CONTENT_TYPE = MediaType.parse("application/octet-stream");
    private static final String XABBER_COMPRESSED_DIR = "Xabber/temp";

    public final static String KEY_RECEIVER = "receiver";
    public final static String KEY_ACCOUNT_JID = "account_jid";
    public final static String KEY_USER_JID = "user_jid";
    public final static String KEY_FILE_PATHS = "file_paths";
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
        CharSequence uploadServerUrl = intent.getCharSequenceExtra(KEY_UPLOAD_SERVER_URL);

        startWork(account, user, filePaths, uploadServerUrl);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        needStop = true;
    }

    private void startWork(AccountJid account, UserJid user, List<String> filePaths, CharSequence uploadServerUrl) {

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

        // create fileMessage with files
        List<File> files = new ArrayList<>();
        for (String filePath : filePaths) {
            files.add(new File(filePath));
        }
        final String fileMessageId = MessageManager.getInstance().createFileMessage(account, user, files);

        List<String> uploadedFilesUrls = new ArrayList<>();
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
                    uploadedFilesUrls.add(((Slot) slot).getGetUrl());
                else throw new Exception("Upload failed: " + response.message());

            } catch (Exception e) {
                publishError(fileMessageId, e.toString());
            }

            publishProgress(fileMessageId, uploadedFilesUrls.size(), filePaths.size());
        }

        removeTempDirectory();

        // check that files are uploaded
        if (uploadedFilesUrls.size() == 0) {
            String error = "Could not upload any files";
            setErrorForMessage(fileMessageId, error);
            publishError(fileMessageId, error);
            return;
        }

        // save results to Realm and send message
        MessageManager.getInstance().updateFileMessage(account, user, fileMessageId, uploadedFilesUrls);
        publishCompleted(fileMessageId);
    }

    private void removeTempDirectory() {
        try {
            File tempDirectory = new File(getCompressedDirPath());
            org.apache.commons.io.FileUtils.deleteDirectory(tempDirectory);
        } catch (IOException e) {
            e.printStackTrace();
        }
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

        MemorizingTrustManager mtm = CertificateManager.getInstance().getNewFileUploadManager(account);
        SSLContext sslContext = SSLContext.getInstance("SSL");
        sslContext.init(null, new X509TrustManager[]{mtm}, new java.security.SecureRandom());
        SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

        OkHttpClient client = new OkHttpClient().newBuilder()
                .sslSocketFactory(sslSocketFactory)
                .hostnameVerifier(mtm.wrapHostnameVerifier(new org.apache.http.conn.ssl.StrictHostnameVerifier()))
                .writeTimeout(5, TimeUnit.MINUTES)
                .connectTimeout(5, TimeUnit.MINUTES)
                .readTimeout(5, TimeUnit.MINUTES)
                .build();

        Request request = new Request.Builder()
                .url(slot.getPutUrl())
                .put(RequestBody.create(CONTENT_TYPE, file))
                .build();

        return client.newCall(request).execute();
    }

    private static String getCompressedDirPath() {
        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath()
                + File.separator + XABBER_COMPRESSED_DIR;
    }
}
