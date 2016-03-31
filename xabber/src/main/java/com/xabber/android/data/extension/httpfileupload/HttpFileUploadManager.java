package com.xabber.android.data.extension.httpfileupload;


import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.LogManager;
import com.xabber.android.data.NetworkException;
import com.xabber.android.data.connection.ConnectionItem;
import com.xabber.android.data.connection.ConnectionManager;
import com.xabber.android.data.connection.listeners.OnAuthorizedListener;
import com.xabber.android.data.connection.listeners.OnResponseListener;
import com.xabber.android.data.extension.file.FileManager;
import com.xabber.android.data.message.MessageManager;
import com.xabber.xmpp.httpfileupload.Slot;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;

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

    private Map<String, String> uploadServers = new ConcurrentHashMap<>();

    public boolean isFileUploadSupported(String account) {
        return uploadServers.containsKey(account);
    }

    public void uploadFile(final String account, final String user, final String filePath) {
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
            ConnectionManager.getInstance().sendRequest(account, httpFileUpload, new OnResponseListener() {
                @Override
                public void onReceived(final String account, String packetId, IQ iq) {
                    if (!httpFileUpload.getStanzaId().equals(packetId) || !(iq instanceof Slot)) {
                        return;
                    }

                    uploadFileToSlot(account, (Slot) iq);
                }


                private void uploadFileToSlot(final String account, final Slot slot) {
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

                @Override
                public void onError(String account, String packetId, IQ iq) {
                    LogManager.i(this, "On HTTP file upload slot error");
                    Application.getInstance().onError(R.string.http_file_upload_slot_error);
                }

                @Override
                public void onTimeout(String account, String packetId) {

                }

                @Override
                public void onDisconnect(String account, String packetId) {

                }
            });
        } catch (NetworkException e) {
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

    private void discoverSupport(XMPPConnection xmppConnection) throws SmackException.NotConnectedException,
            XMPPException.XMPPErrorException, SmackException.NoResponseException {

        final String account = xmppConnection.getUser();

        uploadServers.remove(account);

        ServiceDiscoveryManager discoManager = ServiceDiscoveryManager.getInstanceFor(xmppConnection);

        List<String> services = discoManager.findServices(com.xabber.xmpp.httpfileupload.Request.NAMESPACE, true, true);

        if (!services.isEmpty()) {
            final String uploadServerUrl = services.get(0);
            if (!uploadServerUrl.isEmpty()) {
                LogManager.i(this, "Http file upload server: " + uploadServerUrl);
                uploadServers.put(account, uploadServerUrl);
            }
        }
    }

    @Override
    public void onAuthorized(final ConnectionItem connection) {
        new Thread("Thread to check for " + connection.getRealJid()) {
            @Override
            public void run() {
                try {
                    discoverSupport(connection.getConnectionThread().getXMPPConnection());
                } catch (SmackException.NotConnectedException | XMPPException.XMPPErrorException | SmackException.NoResponseException e) {
                    LogManager.exception(this, e);
                }
            }
        }.start();
    }
}
