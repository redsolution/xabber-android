package com.xabber.android.data.database.realm;

import android.support.annotation.Nullable;

import com.xabber.android.data.account.ArchiveMode;
import com.xabber.android.data.account.StatusMode;
import com.xabber.android.data.connection.ProxyType;
import com.xabber.android.data.connection.TLSMode;
import com.xabber.android.data.extension.mam.LoadHistorySettings;

import org.jivesoftware.smackx.mam.element.MamPrefsIQ;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Date;
import java.util.UUID;

import io.realm.RealmObject;
import io.realm.annotations.Index;
import io.realm.annotations.PrimaryKey;
import io.realm.annotations.Required;


public class AccountRealm extends RealmObject {

    public static class Fields {
        public static final String ID = "id";
        public static final String CLEAR_HISTORY_ON_EXIT = "clearHistoryOnExit";
        public static final String MAM_DEFAULT_BEHAVIOR = "mamDefaultBehavior";
        public static final String LOAD_HISTORY_SETTINGS = "loadHistorySettings";
        public static final String SUCCESSFUL_CONNECTION_HAPPENED = "successfulConnectionHappened";
    }

    @PrimaryKey
    @Required
    private String id;

    @Index
    private boolean enabled;

    private String serverName;
    private String userName;
    private String resource;

    private boolean custom;
    private String host;
    private int port;
    private boolean storePassword;
    private String password;
    private String token;

    private int colorIndex;
    private int timestamp;
    private int order;
    private boolean syncNotAllowed;
    private boolean xabberAutoLoginEnabled;

    private int priority;
    private String statusMode;
    private String statusText;

    private boolean saslEnabled;
    private String tlsMode;
    private boolean compression;

    private String proxyType;
    private String proxyHost;
    private int proxyPort;
    private String proxyUser;
    private String proxyPassword;

    private boolean syncable;

    private byte[] publicKeyBytes;
    private byte[] privateKeyBytes;

    private long lastSync;
    private String archiveMode;

    private boolean clearHistoryOnExit;
    private String mamDefaultBehavior;
    private String loadHistorySettings;

    /**
     * Flag indication that successful connection and authorization
     * happen at least ones with current connection settings
     */
    private boolean successfulConnectionHappened;

    public AccountRealm(String id) {
        this.id = id;
    }

    public AccountRealm() {
        this.id = UUID.randomUUID().toString();
    }

    public String getId() {
        return id;
    }

    public boolean isCustom() {
        return custom;
    }

    public void setCustom(boolean custom) {
        this.custom = custom;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getResource() {
        return resource;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    public boolean isStorePassword() {
        return storePassword;
    }

    public void setStorePassword(boolean storePassword) {
        this.storePassword = storePassword;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public int getColorIndex() {
        return colorIndex;
    }

    public void setColorIndex(int colorIndex) {
        this.colorIndex = colorIndex;
    }

    public int getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(int timestamp) {
        this.timestamp = timestamp;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public boolean isSyncNotAllowed() {
        return syncNotAllowed;
    }

    public void setSyncNotAllowed(boolean syncNotAllowed) {
        this.syncNotAllowed = syncNotAllowed;
    }

    public boolean isXabberAutoLoginEnabled() {
        return xabberAutoLoginEnabled;
    }

    public void setXabberAutoLoginEnabled(boolean xabberAutoLoginEnabled) {
        this.xabberAutoLoginEnabled = xabberAutoLoginEnabled;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public StatusMode getStatusMode() {
        return StatusMode.valueOf(this.statusMode);
    }

    public void setStatusMode(StatusMode statusMode) {
        this.statusMode = statusMode.name();
    }

    public String getStatusText() {
        return statusText;
    }

    public void setStatusText(String statusText) {
        this.statusText = statusText;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isSaslEnabled() {
        return saslEnabled;
    }

    public void setSaslEnabled(boolean saslEnabled) {
        this.saslEnabled = saslEnabled;
    }

    public TLSMode getTlsMode() {
        return TLSMode.valueOf(this.tlsMode);
    }

    public void setTlsMode(TLSMode tlsMode) {
        this.tlsMode = tlsMode.name();
    }

    public boolean isCompression() {
        return compression;
    }

    public void setCompression(boolean compression) {
        this.compression = compression;
    }

    public ProxyType getProxyType() {
        return ProxyType.valueOf(this.proxyType);
    }

    public void setProxyType(ProxyType proxyType) {
        this.proxyType = proxyType.name();
    }

    public String getProxyHost() {
        return proxyHost;
    }

    public void setProxyHost(String proxyHost) {
        this.proxyHost = proxyHost;
    }

    public int getProxyPort() {
        return proxyPort;
    }

    public void setProxyPort(int proxyPort) {
        this.proxyPort = proxyPort;
    }

    public String getProxyUser() {
        return proxyUser;
    }

    public void setProxyUser(String proxyUser) {
        this.proxyUser = proxyUser;
    }

    public String getProxyPassword() {
        return proxyPassword;
    }

    public void setProxyPassword(String proxyPassword) {
        this.proxyPassword = proxyPassword;
    }

    public boolean isSyncable() {
        return syncable;
    }

    public void setSyncable(boolean syncable) {
        this.syncable = syncable;
    }

    public KeyPair getKeyPair() {
        if (this.privateKeyBytes == null || this.publicKeyBytes == null) {
            return null;
        }
        X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(publicKeyBytes);
        PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
        PublicKey publicKey;
        PrivateKey privateKey;
        KeyFactory keyFactory;
        try {
            keyFactory = KeyFactory.getInstance("DSA");
            publicKey = keyFactory.generatePublic(publicKeySpec);
            privateKey = keyFactory.generatePrivate(privateKeySpec);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException(e);
        }
        return new KeyPair(publicKey, privateKey);
    }

    public void setKeyPair(KeyPair keyPair) {
        if (keyPair == null) {
            publicKeyBytes = null;
            privateKeyBytes = null;
        } else {
            PublicKey publicKey = keyPair.getPublic();
            X509EncodedKeySpec x509EncodedKeySpec = new X509EncodedKeySpec(publicKey.getEncoded());
            PrivateKey privateKey = keyPair.getPrivate();
            PKCS8EncodedKeySpec pkcs8EncodedKeySpec = new PKCS8EncodedKeySpec(privateKey.getEncoded());
            this.publicKeyBytes = x509EncodedKeySpec.getEncoded();
            this.privateKeyBytes = pkcs8EncodedKeySpec.getEncoded();
        }
    }

    public Date getLastSync() {
        return new Date(this.lastSync);
    }

    public void setLastSync(Date lastSync) {
        if (lastSync == null) {
            this.lastSync = 0;
        } else {
            this.lastSync = lastSync.getTime();
        }
    }

    public ArchiveMode getArchiveMode() {
        return ArchiveMode.valueOf(this.archiveMode);
    }

    public void setArchiveMode(ArchiveMode archiveMode) {
        this.archiveMode = archiveMode.name();
    }

    public boolean isClearHistoryOnExit() {
        return clearHistoryOnExit;
    }

    public void setClearHistoryOnExit(boolean clearHistoryOnExit) {
        this.clearHistoryOnExit = clearHistoryOnExit;
    }

    @Nullable
    public MamPrefsIQ.DefaultBehavior getMamDefaultBehavior() {
        if (mamDefaultBehavior == null) {
            return null;
        }

        return MamPrefsIQ.DefaultBehavior.valueOf(mamDefaultBehavior);
    }

    public void setMamDefaultBehavior(MamPrefsIQ.DefaultBehavior mamDefaultBehavior) {
        this.mamDefaultBehavior = mamDefaultBehavior.toString();
    }

    @Nullable
    public LoadHistorySettings getLoadHistorySettings() {
        if (loadHistorySettings == null) {
            return null;
        }

        return LoadHistorySettings.valueOf(loadHistorySettings);
    }

    public void setLoadHistorySettings(LoadHistorySettings loadHistorySettings) {
        this.loadHistorySettings = loadHistorySettings.toString();
    }

    public boolean isSuccessfulConnectionHappened() {
        return successfulConnectionHappened;
    }

    public void setSuccessfulConnectionHappened(boolean successfulConnectionHappened) {
        this.successfulConnectionHappened = successfulConnectionHappened;
    }
}
