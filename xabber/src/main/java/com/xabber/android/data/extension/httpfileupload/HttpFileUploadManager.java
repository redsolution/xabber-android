package com.xabber.android.data.extension.httpfileupload;


import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.LogManager;
import com.xabber.android.data.NetworkException;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.connection.ConnectionItem;
import com.xabber.android.data.connection.ConnectionManager;
import com.xabber.android.data.connection.StanzaSender;
import com.xabber.android.data.connection.listeners.OnAuthorizedListener;
import com.xabber.android.data.connection.listeners.OnResponseListener;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.UserJid;
import com.xabber.android.data.extension.file.FileManager;
import com.xabber.android.data.message.MessageManager;
import com.xabber.xmpp.httpfileupload.Slot;

import org.jivesoftware.smack.ExceptionCallback;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jxmpp.jid.DomainBareJid;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class HttpFileUploadManager implements OnAuthorizedListener {

    private final static HttpFileUploadManager instance;

    private static final MediaType CONTENT_TYPE = MediaType.parse("application/octet-stream");

    static {
        instance = new HttpFileUploadManager();
        Application.getInstance().addManager(instance);
    }

    public static HttpFileUploadManager getInstance() {
        return instance;
    }

    private Map<AccountJid, String> uploadServers = new ConcurrentHashMap<>();

    public boolean isFileUploadSupported(AccountJid account) {
        return uploadServers.containsKey(account);
    }

    public void uploadFile(final AccountJid account, final UserJid user, final String filePath) {
        final String uploadServerUrl = uploadServers.get(account);
        if (uploadServerUrl == null) {
            return;
        }

        final File file = new File(filePath);

        final com.xabber.xmpp.httpfileupload.Request httpFileUpload = new com.xabber.xmpp.httpfileupload.Request();
        httpFileUpload.setFilename(file.getName());
        httpFileUpload.setSize(String.valueOf(file.length()));
        httpFileUpload.setTo(uploadServerUrl);

        try {
            AccountManager.getInstance().getAccount(account).getConnection().sendIqWithResponseCallback(httpFileUpload, new StanzaListener() {
                @Override
                public void processPacket(Stanza packet) throws SmackException.NotConnectedException, InterruptedException {
                    if (!(packet instanceof Slot)) {
                        return;
                    }

                    uploadFileToSlot(account, (Slot) packet);
                }

                private void uploadFileToSlot(final AccountJid account, final Slot slot) {
                    OkHttpClient client = new OkHttpClient().newBuilder()
                            .writeTimeout(5, TimeUnit.MINUTES)
                            .connectTimeout(5, TimeUnit.MINUTES)
                            .readTimeout(5, TimeUnit.MINUTES)
                            .build();


                    Request request = new Request.Builder()
                            .url(slot.getPutUrl())
                            .put(RequestBody.create(CONTENT_TYPE, file))
                            .build();

                    final String fileMessageId;
                    fileMessageId = MessageManager.getInstance().createFileMessage(account, user, file);

                    LogManager.i(HttpFileUploadManager.this, "starting upload file to " + slot.getPutUrl() + " size " + file.length());
                    client.newCall(request).enqueue(new Callback() {
                        @Override
                        public void onFailure(Call call, IOException e) {
                            LogManager.i(HttpFileUploadManager.this, "onFailure " + e.getMessage());
                            MessageManager.getInstance().updateMessageWithError(fileMessageId);
                        }

                        @Override
                        public void onResponse(Call call, Response response) throws IOException {
                            LogManager.i(HttpFileUploadManager.this, "onResponse " + response.isSuccessful() + " " + response.body().string());
                            if (response.isSuccessful()) {
                                MessageManager.getInstance().updateFileMessage(account, user, fileMessageId, slot.getGetUrl());

                                if (FileManager.fileIsImage(file)) {
                                    saveImageToCache(slot.getGetUrl(), file);
                                }
                            } else {
                                MessageManager.getInstance().updateMessageWithError(fileMessageId);
                            }
                        }
                    });

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

    private void saveImageToCache(String getUrl, final File file) {
        final URL url;
        try {
            url = new URL(getUrl);
        } catch (MalformedURLException e) {
            LogManager.exception(this, e);
            return;
        }

        Application.getInstance().runInBackground(new Runnable() {
            @Override
            public void run() {
                try {

                    FileManager.saveFileToCache(file, url);
                } catch (IOException e) {
                    LogManager.exception(this, e);
                }
            }
        });
    }

    private void discoverSupport(AccountJid account, XMPPConnection xmppConnection) throws SmackException.NotConnectedException,
            XMPPException.XMPPErrorException, SmackException.NoResponseException, InterruptedException {

        uploadServers.remove(account);

        ServiceDiscoveryManager discoManager = ServiceDiscoveryManager.getInstanceFor(xmppConnection);

        List<DomainBareJid> services = discoManager.findServices(com.xabber.xmpp.httpfileupload.Request.NAMESPACE, true, true);

        if (!services.isEmpty()) {
            final DomainBareJid uploadServerUrl = services.get(0);
            LogManager.i(this, "Http file upload server: " + uploadServerUrl);
            uploadServers.put(account, uploadServerUrl.toString());
        }
    }

    @Override
    public void onAuthorized(final ConnectionItem connectionItem) {
        Application.getInstance().runInBackground(new Runnable() {
            @Override
            public void run() {
                try {
                    discoverSupport(connectionItem.getAccount(), connectionItem.getConnection());
                } catch (SmackException.NotConnectedException | XMPPException.XMPPErrorException
                        | SmackException.NoResponseException | InterruptedException e) {
                    LogManager.exception(this, e);
                }
            }
        });
    }
}
