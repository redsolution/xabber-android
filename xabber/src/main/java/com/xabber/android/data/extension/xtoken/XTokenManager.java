package com.xabber.android.data.extension.xtoken;

import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.connection.ConnectionItem;
import com.xabber.android.data.connection.listeners.OnPacketListener;
import com.xabber.android.data.database.realm.XTokenRealm;
import com.xabber.xmpp.XToken;
import com.xabber.xmpp.XTokenIQ;

import org.jivesoftware.smack.packet.Stanza;

public class XTokenManager implements OnPacketListener {

    private static XTokenManager instance;

    public static XTokenManager getInstance() {
        if (instance == null)
            instance = new XTokenManager();
        return instance;
    }

    @Override
    public void onStanza(ConnectionItem connection, Stanza packet) {
        if (packet instanceof XTokenIQ) {
            AccountManager.getInstance()
                    .updateXToken(connection.getAccount(), iqToXToken((XTokenIQ) packet));
        }
    }

    public static XTokenRealm tokenToXTokenRealm(XToken token) {
        return new XTokenRealm(token.getUid(), token.getToken(), token.getExpire());
    }

    public static XToken xTokenRealmToXToken(XTokenRealm token) {
        return new XToken(token.getId(), token.getToken(), token.getExpire());
    }

    public static XToken iqToXToken(XTokenIQ iq) {
        return new XToken(iq.getUid(), iq.getToken(), iq.getExpire());
    }
}
