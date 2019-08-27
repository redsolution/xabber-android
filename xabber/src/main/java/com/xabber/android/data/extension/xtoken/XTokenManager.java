package com.xabber.android.data.extension.xtoken;

import android.os.Build;

import androidx.core.util.Pair;

import com.xabber.android.data.Application;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.connection.ConnectionItem;
import com.xabber.android.data.connection.listeners.OnPacketListener;
import com.xabber.android.data.database.realm.XTokenRealm;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.utils.StringUtils;
import com.xabber.xmpp.smack.XMPPTCPConnection;
import com.xabber.xmpp.smack.XTokenRequestIQ;

import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Stanza;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

public class XTokenManager implements OnPacketListener {

    private static final String LOG_TAG = XTokenManager.class.getSimpleName();
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

    public void sendXTokenRequest(XMPPTCPConnection connection) {
        LogManager.d(LOG_TAG, "Request x-token before bind");
        String device = Build.MANUFACTURER + " " + Build.MODEL + ", Android " + Build.VERSION.RELEASE;
        String client = Application.getInstance().getVersionName();
        XTokenRequestIQ requestIQ = new XTokenRequestIQ(client, device);
        requestIQ.setType(IQ.Type.set);
        requestIQ.setTo(connection.getHost());
        try {
            connection.sendStanza(requestIQ);
        } catch (Exception e) {
            LogManager.d(LOG_TAG, "Error on request x-token: " + e.toString());
        }
    }

    public void sendRevokeXTokenRequest(XMPPTCPConnection connection, String tokenID) {
        List<String> ids = new ArrayList<>();
        ids.add(tokenID);
        sendRevokeXTokenRequest(connection, ids);
    }

    public void sendRevokeXTokenRequest(XMPPTCPConnection connection, List<String> tokenIDs) {
        LogManager.d(LOG_TAG, "Request revoke x-token");
        XTokenRevokeIQ revokeIQ = new XTokenRevokeIQ(tokenIDs);
        revokeIQ.setType(IQ.Type.set);
        revokeIQ.setTo(connection.getXMPPServiceDomain());
        try {
            connection.sendStanza(revokeIQ);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void requestSessions(final String currentTokenUID, final XMPPTCPConnection connection,
                                final SessionsListener listener) {
        Application.getInstance().runInBackgroundUserRequest(new Runnable() {
            @Override
            public void run() {
                sendSessionsRequestIQ(currentTokenUID, connection, new WeakReference<>(listener));
            }
        });
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

    public interface SessionsListener {
        void onResult(SessionVO currentSession, List<SessionVO> sessions);
        void onError();
    }

    private static Pair<SessionVO, List<SessionVO>> parseSessions(String currentSessionUID, SessionsIQ iq) {
        SessionVO currentSession = null;
        List<SessionVO> result = new ArrayList<>();
        List<Session> sessions = iq.getSessions();
        Collections.sort(sessions, Collections.reverseOrder(new Comparator<Session>() {
            @Override
            public int compare(Session session, Session t1) {
                return (int)(session.getLastAuth() - t1.getLastAuth());
            }
        }));
        for (Session session : sessions) {
            if (session.getUid().equals(currentSessionUID)) {
                currentSession = new SessionVO(
                        session.getClient(),
                        session.getDevice(),
                        session.getUid(),
                        session.getIp(),
                        "online"
                );
            } else {
                result.add(new SessionVO(
                        session.getClient(),
                        session.getDevice(),
                        session.getUid(),
                        session.getIp(),
                        StringUtils.getSmartTimeTextForRoster(Application.getInstance(),
                                new Date(session.getLastAuth()))
                ));
            }
        }
        return new Pair<>(currentSession, result);
    }

    private void sendSessionsRequestIQ(final String currentTokenUID,
                                       final XMPPTCPConnection connection,
                                       final WeakReference<SessionsListener> wrListener) {
        LogManager.d(LOG_TAG, "Request x-token list");
        SessionsRequestIQ requestIQ = new SessionsRequestIQ();
        requestIQ.setType(IQ.Type.get);
        requestIQ.setTo(connection.getXMPPServiceDomain());
        try {
            connection.sendStanzaWithResponseCallback(requestIQ,
                    new SessionsResultFilter(requestIQ, connection),
                    new SessionRequestListener(currentTokenUID, wrListener));
        } catch (Exception e) {
            SessionsListener listener = wrListener.get();
            if (listener != null) listener.onError();
            e.printStackTrace();
        }
    }

    private static class SessionRequestListener implements StanzaListener {
        private final String currentTokenUID;
        private final WeakReference<SessionsListener> wrListener;

        public SessionRequestListener(String currentTokenUID, WeakReference<SessionsListener> wrListener) {
            this.currentTokenUID = currentTokenUID;
            this.wrListener = wrListener;
        }

        @Override
        public void processStanza(Stanza packet) {
            if (packet instanceof SessionsIQ) {
                final Pair<SessionVO, List<SessionVO>> result =
                        parseSessions(currentTokenUID, (SessionsIQ) packet);

                Application.getInstance().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        SessionsListener listener = wrListener.get();
                        if (listener != null)
                            listener.onResult(result.first, result.second);
                    }
                });
            }
        }
    }
}
