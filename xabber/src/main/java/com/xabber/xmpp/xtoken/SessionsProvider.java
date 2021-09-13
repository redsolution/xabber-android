package com.xabber.xmpp.xtoken;

import org.jivesoftware.smack.provider.IQProvider;
import org.xmlpull.v1.XmlPullParser;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class SessionsProvider extends IQProvider<ResultSessionsIQ> {

    @Override
    public ResultSessionsIQ parse(XmlPullParser parser, int initialDepth) throws Exception {
        List<ResultSessionsIQ.Session> sessions = new ArrayList<>();

        outerloop: while (true) {
            int eventType = parser.getEventType();
            switch (eventType) {
                case XmlPullParser.START_TAG:
                    if (ResultSessionsIQ.ELEMENT.equals(parser.getName())
                            && ResultSessionsIQ.NAMESPACE.equals(parser.getNamespace())) {
                        parser.next();
                    } else if (ResultSessionsIQ.Session.ELEMENT.equals(parser.getName())) {
                        ResultSessionsIQ.Session session = parseSession(parser);
                        if (session != null) sessions.add(session);
                        parser.next();
                    }
                    break;
                case XmlPullParser.END_TAG:
                    if (ResultSessionsIQ.ELEMENT.equals(parser.getName())) {
                        break outerloop;
                    } else parser.next();
                    break;
                default:
                    parser.next();
            }
        }

        if (!sessions.isEmpty()) return new ResultSessionsIQ(sessions);
        else return null;
    }

    private ResultSessionsIQ.Session parseSession(XmlPullParser parser) throws Exception {
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
                    if (ResultSessionsIQ.Session.ELEMENT_CLIENT.equals(parser.getName())) {
                        client = parser.nextText();
                    } else if (ResultSessionsIQ.Session.ELEMENT_DEVICE.equals(parser.getName())) {
                        device = parser.nextText();
                    } else if (ResultSessionsIQ.Session.ELEMENT_TOKEN_UID.equals(parser.getName())) {
                        uid = parser.nextText();
                    } else if (ResultSessionsIQ.Session.ELEMENT_IP.equals(parser.getName())) {
                        ip = parser.nextText();
                    } else if (ResultSessionsIQ.Session.ELEMENT_EXPIRE.equals(parser.getName())) {
                        expire = Long.valueOf(parser.nextText());
                    } else if (ResultSessionsIQ.Session.ELEMENT_LAST_AUTH.equals(parser.getName())) {
                        lastAuth = Long.valueOf(parser.nextText());
                    } else parser.next();
                    break;
                case XmlPullParser.END_TAG:
                    if (ResultSessionsIQ.Session.ELEMENT.equals(parser.getName())) {
                        break outerloop;
                    } else parser.next();
                    break;
                default:
                    parser.next();
            }
        }

        if (client != null && device != null && uid != null && ip != null)
            return new ResultSessionsIQ.Session(client, device, uid, ip, TimeUnit.SECONDS.toMillis(expire), TimeUnit.SECONDS.toMillis(lastAuth));
        else return null;
    }

}
