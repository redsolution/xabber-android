package com.xabber.android.data.extension.httpfileupload;


import android.content.Context;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.xabber.android.data.Application;
import com.xabber.android.data.LogManager;
import com.xabber.android.data.NetworkException;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.connection.ConnectionItem;
import com.xabber.android.data.connection.ConnectionManager;
import com.xabber.android.data.connection.OnAuthorizedListener;
import com.xabber.android.data.connection.OnResponseListener;
import com.xabber.android.data.message.MessageItem;
import com.xabber.android.data.message.MessageManager;
import com.xabber.xmpp.httpfileupload.Request;
import com.xabber.xmpp.httpfileupload.Slot;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.entity.ContentType;
import cz.msebera.android.httpclient.entity.FileEntity;

public class HttpFileUploadManager implements OnAuthorizedListener {

    private final static HttpFileUploadManager instance;

    private static final String CONTENT_TYPE = RequestParams.APPLICATION_OCTET_STREAM;

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

    public void uploadFile(final Context context, final String account, final String user, final String filePath) {
        final String uploadServerUrl = uploadServers.get(account);
        if (uploadServerUrl == null) {
            return;
        }

        final File file = new File(filePath);

        final Request httpFileUpload = new Request();
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
                    AsyncHttpClient client = new AsyncHttpClient();
                    client.setConnectTimeout(60 * 1000);
                    client.setLoggingEnabled(SettingsManager.debugLog());
                    client.setResponseTimeout(60 * 1000);

                    FileEntity fileEntity = new FileEntity(file, ContentType.DEFAULT_BINARY);


                    LogManager.i(this, "fileEntity.getContentLength() " + fileEntity.getContentLength());

                    client.put(context, slot.getPutUrl(), fileEntity, CONTENT_TYPE, new AsyncHttpResponseHandler() {

                        MessageItem fileMessage;

                        @Override
                        public void onStart() {
                            super.onStart();
                            LogManager.i(this, "onStart");

                            fileMessage = MessageManager.getInstance().createFileMessage(account, user, file);
                        }

                        @Override
                        public void onSuccess(int i, Header[] headers, byte[] bytes) {
                            LogManager.i(this, "onSuccess " + i);
                            MessageManager.getInstance().replaceMessage(account, user, fileMessage, slot.getGetUrl());
                        }

                        @Override
                        public void onFailure(int i, Header[] headers, byte[] bytes, Throwable throwable) {
                            LogManager.i(this, "onFailure " + i);

                            MessageManager.getInstance().updateMessageWithError(account, user, fileMessage, file.getName());

                        }

                        @Override
                        public void onRetry(int retryNo) {
                            super.onRetry(retryNo);
                            LogManager.i(this, "onRetry " + retryNo);
                        }

                        @Override
                        public void onCancel() {
                            super.onCancel();

                            LogManager.i(this, "onCancel");

                        }

                        @Override
                        public void onFinish() {
                            super.onFinish();
                            LogManager.i(this, "onFinish");
                        }


                    });
                }

                @Override
                public void onError(String account, String packetId, IQ iq) {

                }

                @Override
                public void onTimeout(String account, String packetId) {

                }

                @Override
                public void onDisconnect(String account, String packetId) {

                }
            });
        } catch (NetworkException e) {
            e.printStackTrace();
        }
    }




    private void discoverSupport(XMPPConnection xmppConnection) throws SmackException.NotConnectedException,
            XMPPException.XMPPErrorException, SmackException.NoResponseException {

        final String account = xmppConnection.getUser();

        uploadServers.remove(account);

        ServiceDiscoveryManager discoManager = ServiceDiscoveryManager.getInstanceFor(xmppConnection);

        List<String> services = discoManager.findServices(Request.NAMESPACE, true, true);

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
                    e.printStackTrace();
                }

            }
        }.start();
    }
}
