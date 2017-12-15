package com.xabber.android.data.connection;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Patterns;

import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.log.LogManager;

import org.apache.http.conn.ssl.AllowAllHostnameVerifier;
import org.jivesoftware.smack.proxy.ProxyInfo;
import org.jivesoftware.smack.sasl.core.SASLXOauth2Mechanism;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jivesoftware.smack.util.TLSUtils;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.X509TrustManager;

import de.duenndns.ssl.MemorizingTrustManager;

class ConnectionBuilder {
    private static final String LOG_TAG = ConnectionBuilder.class.getSimpleName();

    public static @NonNull XMPPTCPConnection build(AccountJid account, @NonNull final ConnectionSettings connectionSettings) {
        XMPPTCPConnectionConfiguration.Builder builder = XMPPTCPConnectionConfiguration.builder();

        builder.setXmppDomain(connectionSettings.getServerName());

        if (connectionSettings.isCustomHostAndPort()) {
            setCustomHost(connectionSettings, builder);

            builder.setPort(connectionSettings.getPort());
        }

        builder.setDebuggerEnabled(true);
        builder.setSecurityMode(connectionSettings.getTlsMode().getSecurityMode());
        builder.setCompressionEnabled(connectionSettings.useCompression());
        builder.setSendPresence(false);
        builder.setUsernameAndPassword(connectionSettings.getUserName(), connectionSettings.getPassword());
        builder.setResource(connectionSettings.getResource());

        builder.setProxyInfo(getProxyInfo(connectionSettings));

        try {
            LogManager.i(LOG_TAG, "SettingsManager.securityCheckCertificate: " + SettingsManager.securityCheckCertificate());

            if (SettingsManager.securityCheckCertificate()) {
                SSLContext sslContext = SSLContext.getInstance("TLS");
                MemorizingTrustManager mtm = CertificateManager.getInstance().getNewMemorizingTrustManager(account);
                sslContext.init(null, new X509TrustManager[]{mtm}, new java.security.SecureRandom());
                builder.setCustomSSLContext(sslContext);
                builder.setHostnameVerifier(
                        mtm.wrapHostnameVerifier(new CustomDomainVerifier()));
            } else {
                TLSUtils.acceptAllCertificates(builder);
                builder.setHostnameVerifier(new AllowAllHostnameVerifier());
            }
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            LogManager.exception(LOG_TAG, e);
        }

        // if account have token
        if (connectionSettings.getToken() != null && !connectionSettings.getToken().isEmpty()
                && connectionSettings.getPassword() != null
                && connectionSettings.getPassword().isEmpty()) {
            // then enable only SASLXOauth2Mechanism
            builder.addEnabledSaslMechanism(SASLXOauth2Mechanism.NAME);

            // and set token as password
            builder.setUsernameAndPassword(connectionSettings.getUserName(), connectionSettings.getToken());
        }

        LogManager.i(LOG_TAG, "new XMPPTCPConnection " + connectionSettings.getServerName());
        return new XMPPTCPConnection(builder.build());
    }

    private static void setCustomHost(@NonNull ConnectionSettings connectionSettings, XMPPTCPConnectionConfiguration.Builder builder) {
        String host = connectionSettings.getHost();
        InetAddress ipAddressOrNull = getIpAddressOrNull(host);

        LogManager.i(LOG_TAG, "setCustomHost. host: " + host + " ip address: " + ipAddressOrNull);

        if (ipAddressOrNull != null) {
            LogManager.i(LOG_TAG, "Using custom IP address " + ipAddressOrNull);
            builder.setHostAddress(ipAddressOrNull);
        } else {
            LogManager.i(LOG_TAG, "Using custom host " + host);
            builder.setHost(host);
        }
    }

    @Nullable
    private static InetAddress getIpAddressOrNull(String host) {
        InetAddress ipAddress = null;

        if (Patterns.IP_ADDRESS.matcher(host).matches()) {
            try {
                ipAddress = InetAddress.getByName(host);
            } catch (UnknownHostException e) {
                LogManager.exception(LOG_TAG, e);
            }
        }

        return ipAddress;
    }

    private static ProxyInfo getProxyInfo(ConnectionSettings connectionSettings) {

        ProxyInfo proxyInfo = null;

        ProxyType proxyType = connectionSettings.getProxyType();

        String proxyHost = connectionSettings.getProxyHost();
        int proxyPort = connectionSettings.getProxyPort();
        String proxyPassword = connectionSettings.getProxyPassword();
        String proxyUser = connectionSettings.getProxyUser();

        if (proxyType != null) {
            switch (proxyType) {
                case http:
                    proxyInfo = ProxyInfo.forHttpProxy(proxyHost, proxyPort, proxyUser, proxyPassword);
                    break;

                case socks4:
                    proxyInfo = ProxyInfo.forSocks4Proxy(proxyHost, proxyPort, proxyUser, proxyPassword);
                    break;

                case socks5:
                    proxyInfo = ProxyInfo.forSocks5Proxy(proxyHost, proxyPort, proxyUser, proxyPassword);
                    break;

                case orbot:
                    proxyHost = "localhost";
                    proxyPort = 9050;
                    proxyPassword = "";
                    proxyUser = "";
                    proxyInfo = ProxyInfo.forSocks5Proxy(proxyHost, proxyPort, proxyUser, proxyPassword);
                    break;

                case none:
                default:
                    proxyInfo = null;
            }
        }

        return proxyInfo;
    }
}
