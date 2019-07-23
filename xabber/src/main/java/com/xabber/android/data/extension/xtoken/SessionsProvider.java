package com.xabber.android.data.extension.xtoken;

import org.jivesoftware.smack.provider.IQProvider;
import org.xmlpull.v1.XmlPullParser;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class SessionsProvider extends IQProvider<SessionsIQ> {

    @Override
    public SessionsIQ parse(XmlPullParser parser, int initialDepth) throws Exception {
        List<Session> sessions = new ArrayList<>();

        outerloop: while (true) {
            int eventType = parser.getEventType();
            switch (eventType) {
                case XmlPullParser.START_TAG:
                    if (SessionsIQ.ELEMENT.equals(parser.getName())
                            && SessionsIQ.NAMESPACE.equals(parser.getNamespace())) {
                        parser.next();
                    } else if (Session.ELEMENT.equals(parser.getName())) {
                        Session session = parseSession(parser);
                        if (session != null) sessions.add(session);
                        parser.next();
                    }
                    break;
                case XmlPullParser.END_TAG:
                    if (SessionsIQ.ELEMENT.equals(parser.getName())) {
                        break outerloop;
                    } else parser.next();
                    break;
                default:
                    parser.next();
            }
        }

        if (!sessions.isEmpty()) return new SessionsIQ(sessions);
        else return null;
    }

    private Session parseSession(XmlPullParser parser) throws Exception {
        String client = null;
        String device = null;
        String uid = null;
        String ip = null;
        long expire = 0;
        long lastAuth = 0;

        outerloop: while (true) {
            int eventType = parser.getEventType();
            switch (eventType) {
                case XmlPullParser.START_TAG:
                    if (Session.ELEMENT_CLIENT.equals(parser.getName())) {
                        client = parser.nextText();
                    } else if (Session.ELEMENT_DEVICE.equals(parser.getName())) {
                        device = parser.nextText();
                    } else if (Session.ELEMENT_TOKEN_UID.equals(parser.getName())) {
                        uid = parser.nextText();
                    } else if (Session.ELEMENT_IP.equals(parser.getName())) {
                        ip = parser.nextText();
                    } else if (Session.ELEMENT_EXPIRE.equals(parser.getName())) {
                        expire = Long.valueOf(parser.nextText());
                    } else if (Session.ELEMENT_LAST_AUTH.equals(parser.getName())) {
                        lastAuth = Long.valueOf(parser.nextText());
                    } else parser.next();
                    break;
                case XmlPullParser.END_TAG:
                    if (Session.ELEMENT.equals(parser.getName())) {
                        break outerloop;
                    } else parser.next();
                    break;
                default:
                    parser.next();
            }
        }

        if (client != null && device != null && uid != null && ip != null)
            return new Session(client, device, uid, ip, TimeUnit.SECONDS.toMillis(expire), TimeUnit.SECONDS.toMillis(lastAuth));
        else return null;
    }
}
