package com.xabber.android.data.extension.httpfileupload;


import android.content.Context;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.xabber.android.data.Application;
import com.xabber.android.data.LogManager;
import com.xabber.android.data.NetworkException;
import com.xabber.android.data.connection.ConnectionItem;
import com.xabber.android.data.connection.ConnectionManager;
import com.xabber.android.data.connection.OnAuthorizedListener;
import com.xabber.android.data.connection.OnResponseListener;
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

    public void uploadFile(final Context context, final String account, String filePath, final HttpUploadListener listener) {
        final String uploadServerUrl = uploadServers.get(account);
        if (uploadServerUrl == null) {
            return;
        }

        final File file = new File(filePath);

        final Request httpFileUpload = new Request();
        httpFileUpload.setFilename(file.getName());
        httpFileUpload.setSize(String.valueOf(file.length()));
        httpFileUpload.setTo(uploadServerUrl);

        new Thread("Thread to upload file " + filePath + " for account " + account) {
            @Override
            public void run() {
                try {
                    ConnectionManager.getInstance().sendRequest(account, httpFileUpload, new OnResponseListener() {

                        @Override
                        public void onReceived(String account, String packetId, IQ iq) {
                            if (!(iq instanceof Slot)) {
                                return;
                            }

                            final Slot slot = (Slot) iq;

                            AsyncHttpClient client = new AsyncHttpClient();
                            client.setConnectTimeout(60 * 1000);
                            client.setLoggingEnabled(true);

                            FileEntity fileEntity = new FileEntity(file, ContentType.DEFAULT_BINARY);
                            client.put(context, slot.getPutUrl(), fileEntity, CONTENT_TYPE, new AsyncHttpResponseHandler() {
                                @Override
                                public void onProgress(long bytesWritten, long totalSize) {
                                    super.onProgress(bytesWritten, totalSize);
                                    LogManager.i(this, "onProgress: " + bytesWritten);
                                }

                                @Override
                                public void onSuccess(int i, Header[] headers, byte[] bytes) {
                                    LogManager.i(this, "onSuccess " + i);
                                    Application.getInstance().runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            listener.onSuccessfullUpload(slot.getGetUrl());
                                        }
                                    });

                                }

                                @Override
                                public void onFailure(int i, Header[] headers, byte[] bytes, Throwable throwable) {
                                    LogManager.i(this, "onFailure " + i);

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
        }.run();
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
