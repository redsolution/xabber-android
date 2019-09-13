package com.xabber.xmpp.avatar;

import com.xabber.xmpp.ProviderUtils;

import org.jivesoftware.smack.provider.IQProvider;
import org.xmlpull.v1.XmlPullParser;

public class AvatarProvider extends IQProvider<Avatar> {
    @Override
    public Avatar parse(XmlPullParser parser, int initialDepth) throws Exception {
        Avatar avatar = new Avatar();

        boolean done = false;
        while (!done) {
            int eventType = parser.next();
            if (eventType == XmlPullParser.START_TAG) {
                if (parser.getName().equals("item")) {
                    avatar.setSha1hash(ProviderUtils.parseText(parser));
                }
                if (parser.getName().equals("data")) {
                    avatar.setPhotoHash(ProviderUtils.parseText(parser));
                }
            } else if (eventType == XmlPullParser.END_TAG && parser.getName().equals("pubsub")) {
                done = true;
            }
        }
        return null;
    }
}
