package com.xabber.android.data.extension.httpfileupload;

import com.xabber.android.data.log.LogManager;

import org.jxmpp.jid.BareJid;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;

import java.util.UUID;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;
import io.realm.annotations.Required;

public class UploadServer extends RealmObject {

    public static class Fields {
        public static final String ID = "id";
        public static final String ACCOUNT = "account";
        public static final String SERVER = "server";
    }

    @PrimaryKey
    @Required
    private String id;
    private String account;
    private String server;

    public UploadServer() {
        this.id = UUID.randomUUID().toString();
    }

    public UploadServer(BareJid account, Jid server) {
        this.id = UUID.randomUUID().toString();
        this.account = account.toString();
        this.server = server.toString();
    }

    public BareJid getAccount() {
        if (account == null) return null;
        try {
            return JidCreate.bareFrom(account);
        } catch (XmppStringprepException e) {
            LogManager.exception(this, e);
            throw new IllegalStateException();
        }
    }

    public Jid getServer() {
        if (server == null) return null;
        try {
            return JidCreate.from(server);
        } catch (XmppStringprepException e) {
            LogManager.exception(this, e);
            throw new IllegalStateException();
        }
    }

    public String getId() {
        return id;
    }

    public void setServer(Jid server) {
        this.server = server.toString();
    }

    public void setAccount(String account) {
        this.account = account;
    }
}
