package com.xabber.android.data.extension.httpfileupload;


import android.graphics.BitmapFactory;
import android.webkit.MimeTypeMap;

import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.connection.CertificateManager;
import com.xabber.android.data.connection.ConnectionItem;
import com.xabber.android.data.database.messagerealm.Attachment;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.UserJid;
import com.xabber.android.data.extension.file.FileManager;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.data.message.MessageManager;
import com.xabber.xmpp.httpfileupload.Slot;

import org.jivesoftware.smack.ExceptionCallback;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.xdata.FormField;
import org.jivesoftware.smackx.xdata.packet.DataForm;
import org.jxmpp.jid.DomainBareJid;
import org.jxmpp.jid.Jid;

import java.io.File;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509TrustManager;

import de.duenndns.ssl.MemorizingTrustManager;
import io.realm.RealmList;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class HttpFileUploadManager {

    private static HttpFileUploadManager instance;

    private static final MediaType CONTENT_TYPE = MediaType.parse("application/octet-stream");
    private static final String LOG_TAG = HttpFileUploadManager.class.getSimpleName();

    private Map<AccountJid, Jid> uploadServers = new ConcurrentHashMap<>();

    public static HttpFileUploadManager getInstance() {
        if (instance == null) {
            instance = new HttpFileUploadManager();
        }

        return instance;
    }

    private HttpFileUploadManager() {
    }

    public boolean isFileUploadSupported(AccountJid account) {
        return uploadServers.containsKey(account);
    }

    public void uploadFile(final AccountJid account, final UserJid user, final List<String> filePaths) {
        final Jid uploadServerUrl = uploadServers.get(account);
        if (uploadServerUrl == null) {
            return;
        }

        // create fileMessage with files
        List<File> files = new ArrayList<>();
        for (String filePath : filePaths) {
            files.add(new File(filePath));
        }
        final String fileMessageId = MessageManager.getInstance().createFileMessage(account, user, files);

        List<String> fileUrls = new ArrayList<>();
        requestNextFileSlotOrComplete(filePaths, uploadServerUrl, account, user, fileUrls, fileMessageId);
    }

    private void completeUploading(AccountJid account, UserJid user, String fileMessageId, List<String> fileUrls) {
        MessageManager.getInstance().updateFileMessage(account, user, fileMessageId, fileUrls);
    }

    private void requestNextFileSlotOrComplete(final List<String> filePaths, final Jid uploadServerUrl,
                                               final AccountJid account, final UserJid user,
                                               final List<String> fileUrls, final String fileMessageId) {
        AccountItem accountItem = AccountManager.getInstance().getAccount(account);
        if (accountItem == null) {
            return;
        }

        if (filePaths.size() <= 0) {
            completeUploading(account, user, fileMessageId, fileUrls);
            return;
        }

        String filePath = filePaths.get(0);
        filePaths.remove(0);

        final File file = new File(filePath);

        final com.xabber.xmpp.httpfileupload.Request httpFileUpload = new com.xabber.xmpp.httpfileupload.Request();
        httpFileUpload.setFilename(file.getName());
        httpFileUpload.setSize(String.valueOf(file.length()));
        httpFileUpload.setTo(uploadServerUrl);

        try {
            accountItem.getConnection().sendIqWithResponseCallback(httpFileUpload, new StanzaListener() {
                @Override
                public void processStanza(Stanza packet) throws SmackException.NotConnectedException, InterruptedException {
                    if (!(packet instanceof Slot)) {
                        return;
                    }

                    uploadFileToSlot(account, user, (Slot) packet, file, filePaths, uploadServerUrl, fileUrls, fileMessageId);
                }

            }, new ExceptionCallback() {
                @Override
                public void processException(Exception exception) {
                    LogManager.i(this, "On HTTP file upload slot error");
                    LogManager.exception(this, exception);
                    Application.getInstance().onError(R.string.http_file_upload_slot_error);
                }
            });
        } catch (SmackException.NotConnectedException | InterruptedException e) {
            LogManager.exception(this, e);
        }
    }

    private void uploadFileToSlot(final AccountJid account, final UserJid user, final Slot slot,
                                  final File file, final List<String> filePaths, final Jid uploadServerUrl,
                                  final List<String> fileUrls, final String fileMessageId) {

        SSLSocketFactory sslSocketFactory = null;
        MemorizingTrustManager mtm = CertificateManager.getInstance().getNewFileUploadManager(account);

        final SSLContext sslContext;
        try {
            sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, new X509TrustManager[]{mtm}, new java.security.SecureRandom());
            sslSocketFactory = sslContext.getSocketFactory();
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            return;
        }

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

        LogManager.i(HttpFileUploadManager.this, "starting upload file to " + slot.getPutUrl() + " size " + file.length());
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                LogManager.i(HttpFileUploadManager.this, "onFailure " + e.getMessage());
                // TODO: 17.05.18 обработка ошибок
                //MessageManager.getInstance().updateMessageWithError(fileMessageId, e.toString());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                LogManager.i(HttpFileUploadManager.this, "onResponse " + response.isSuccessful() + " " + response.body().string());
                if (response.isSuccessful()) {
                    fileUrls.add(slot.getGetUrl());
                    requestNextFileSlotOrComplete(filePaths, uploadServerUrl, account, user, fileUrls, fileMessageId);
                } else {
                    // TODO: 17.05.18 обработка ошибок
                    //MessageManager.getInstance().updateMessageWithError(fileMessageId, response.message());
                }
            }
        });

    }

    private void discoverSupport(AccountJid account, XMPPConnection xmppConnection) throws SmackException.NotConnectedException,
            XMPPException.XMPPErrorException, SmackException.NoResponseException, InterruptedException {

        uploadServers.remove(account);

        ServiceDiscoveryManager discoManager = ServiceDiscoveryManager.getInstanceFor(xmppConnection);

        List<DomainBareJid> services;
        try {
            services = discoManager.findServices(com.xabber.xmpp.httpfileupload.Request.NAMESPACE, true, true);
        } catch (ClassCastException e) {
            services = Collections.emptyList();
            LogManager.exception(this, e);
        }

        if (!services.isEmpty()) {
            final DomainBareJid uploadServerUrl = services.get(0);
            LogManager.i(this, "Http file upload server: " + uploadServerUrl);
            uploadServers.put(account, uploadServerUrl);
        }
    }

    public void onAuthorized(final ConnectionItem connectionItem) {
        try {
            discoverSupport(connectionItem.getAccount(), connectionItem.getConnection());
        } catch (SmackException.NotConnectedException | XMPPException.XMPPErrorException
                | SmackException.NoResponseException | InterruptedException e) {
            LogManager.exception(this, e);
        }
    }

    public static ImageSize getImageSizes(String filePath) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(new File(filePath).getAbsolutePath(), options);
        int imageHeight = options.outHeight;
        int imageWidth = options.outWidth;
        return new ImageSize(imageHeight, imageWidth);
    }

    public static class ImageSize {
        private int height;
        private int width;

        public ImageSize(int height, int width) {
            this.height = height;
            this.width = width;
        }

        public int getHeight() {
            return height;
        }

        public int getWidth() {
            return width;
        }
    }


    public static String getMimeType(String path) {
        String extension = path.substring(path.lastIndexOf(".")).substring(1);
        String type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        if (type == null || type.isEmpty()) type = "*/*";
        return type;
    }

    public static String getFileName(String path) {
        File file = new File(path);
        return file.getName();
    }

    public static RealmList<Attachment> parseFileMessage(Stanza packet) {
        RealmList<Attachment> attachments = new RealmList<>();

        DataForm dataForm = DataForm.from(packet);
        if (dataForm != null) {

            List<FormField> fields = dataForm.getFields();
            for (FormField field : fields) {
                if (field instanceof ExtendedFormField) {
                    ExtendedFormField.Media media = ((ExtendedFormField)field).getMedia();
                    attachments.add(mediaToAttachment(media, field.getLabel()));
                }
            }
        }
        return attachments;
    }

    private static Attachment mediaToAttachment(ExtendedFormField.Media media, String title) {
        Attachment attachment = new Attachment();
        attachment.setTitle(title);

        try {
            if (media.getWidth() != null && !media.getWidth().isEmpty())
                attachment.setImageWidth(Integer.valueOf(media.getWidth()));

            if (media.getHeight() != null && !media.getHeight().isEmpty())
                attachment.setImageHeight(Integer.valueOf(media.getHeight()));

        } catch (NumberFormatException e) {
            LogManager.exception(LOG_TAG, e);
        }

        ExtendedFormField.Uri uri = media.getUri();
        if (uri != null) {
            attachment.setMimeType(uri.getType());
            attachment.setFileSize(uri.getSize());
            attachment.setDuration(uri.getDuration());
            attachment.setFileUrl(uri.getUri());
            attachment.setIsImage(FileManager.isImageUrl(uri.getUri()));
        }
        return attachment;
    }
}
