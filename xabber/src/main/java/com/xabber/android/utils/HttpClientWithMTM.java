package com.xabber.android.utils;

import com.xabber.android.data.connection.CertificateManager;
import com.xabber.android.data.entity.AccountJid;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509TrustManager;

import de.duenndns.ssl.MemorizingTrustManager;
import okhttp3.OkHttpClient;

public class HttpClientWithMTM {

    public static OkHttpClient getClient(AccountJid accountJid) {

        // create ssl verification factory
        SSLSocketFactory sslSocketFactory = null;
        MemorizingTrustManager mtm = CertificateManager.getInstance().getNewFileUploadManager(accountJid);

        final SSLContext sslContext;
        try {
            sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, new X509TrustManager[]{mtm}, new java.security.SecureRandom());
            sslSocketFactory = sslContext.getSocketFactory();
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            return null;
        }

        // build http client
        final OkHttpClient client = new OkHttpClient().newBuilder()
                .sslSocketFactory(sslSocketFactory)
                .hostnameVerifier(mtm.wrapHostnameVerifier(new org.apache.http.conn.ssl.StrictHostnameVerifier()))
                .writeTimeout(5, TimeUnit.MINUTES)
                .connectTimeout(5, TimeUnit.MINUTES)
                .readTimeout(5, TimeUnit.MINUTES)
                .build();

        return client;
    }

}
