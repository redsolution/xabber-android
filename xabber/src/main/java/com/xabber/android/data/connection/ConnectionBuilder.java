package com.xabber.android.data.connection;

import android.support.annotation.NonNull;

import com.xabber.android.data.Application;
import com.xabber.android.data.LogManager;
import com.xabber.android.data.SettingsManager;

import org.jivesoftware.smack.SASLAuthentication;
import org.jivesoftware.smack.sasl.provided.SASLPlainMechanism;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jivesoftware.smack.util.TLSUtils;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

import javax.net.ssl.SSLContext;
import javax.net.ssl.X509TrustManager;

import de.duenndns.ssl.MemorizingTrustManager;

public class ConnectionBuilder {
    private static final String LOG_TAG = ConnectionBuilder.class.getSimpleName();

    public static @NonNull XMPPTCPConnection build(@NonNull final ConnectionSettings connectionSettings) {
        XMPPTCPConnectionConfiguration.Builder builder = XMPPTCPConnectionConfiguration.builder();

        builder.setServiceName(connectionSettings.getServerName());

        if (connectionSettings.isCustomHostAndPort()) {
            builder.setHost(connectionSettings.getHost());
            builder.setPort(connectionSettings.getPort());
        }

        builder.setSecurityMode(connectionSettings.getTlsMode().getSecurityMode());
        builder.setCompressionEnabled(connectionSettings.useCompression());
        builder.setSendPresence(false);
        builder.setUsernameAndPassword(connectionSettings.getUserName(), connectionSettings.getPassword());
        builder.setResource(connectionSettings.getResource());

        try {
            if (SettingsManager.securityCheckCertificate()) {
                SSLContext sslContext = SSLContext.getInstance("TLS");
                MemorizingTrustManager mtm = new MemorizingTrustManager(Application.getInstance());
                sslContext.init(null, new X509TrustManager[]{mtm}, new java.security.SecureRandom());
                builder.setCustomSSLContext(sslContext);
                builder.setHostnameVerifier(
                        mtm.wrapHostnameVerifier(new org.apache.http.conn.ssl.StrictHostnameVerifier()));
            } else {
                TLSUtils.acceptAllCertificates(builder);
                TLSUtils.disableHostnameVerificationForTlsCertificicates(builder);
            }
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            LogManager.exception(LOG_TAG, e);
        }

        setUpSasl();

        return new XMPPTCPConnection(builder.build());
    }

    private static void setUpSasl() {
        if (SettingsManager.connectionUsePlainTextAuth()) {
            final Map<String, String> registeredSASLMechanisms = SASLAuthentication.getRegisterdSASLMechanisms();
            for (String mechanism : registeredSASLMechanisms.values()) {
                SASLAuthentication.blacklistSASLMechanism(mechanism);
            }

            SASLAuthentication.unBlacklistSASLMechanism(SASLPlainMechanism.NAME);

        } else {
            final Map<String, String> registeredSASLMechanisms = SASLAuthentication.getRegisterdSASLMechanisms();
            for (String mechanism : registeredSASLMechanisms.values()) {
                SASLAuthentication.unBlacklistSASLMechanism(mechanism);
            }
        }
    }
}
