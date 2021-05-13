package com.xabber.xmpp.avatar;

import com.xabber.android.data.log.LogManager;

import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.jivesoftware.smack.util.stringencoder.Base64;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

public class DataProvider extends ExtensionElementProvider<DataExtension> {

    @Override
    public DataExtension parse(XmlPullParser parser, int initialDepth)
            throws IOException {
        byte[] data = new byte[0];
        try {
            data = Base64.decode(parser.nextText());
        } catch (XmlPullParserException e) {
            LogManager.exception(getClass().getSimpleName(), e);
        }
        return new DataExtension(data);
    }

}



/*
package com.xabber.xmpp.avatar;

import com.xabber.xmpp.ProviderUtils;

import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.jivesoftware.smack.provider.IQProvider;
import org.xmlpull.v1.XmlPullParser;

public class DataProvider extends ExtensionElementProvider<Data> {
    @Override
    public Data parse(XmlPullParser parser, int initialDepth) throws Exception {
        Data data = new Data();
        if (parser.getName().equals("data")) {
            data.setPhotoHash(ProviderUtils.parseText(parser));
        }
*/
/*
        boolean done = false;
        while (!done) {
            int eventType = parser.getEventType();
            if (eventType == XmlPullParser.START_TAG) {
                if (parser.getName().equals("data")) {
                    data.setPhotoHash(ProviderUtils.parseText(parser));
                }
            } else if (eventType == XmlPullParser.END_TAG && parser.getName().equals("data")) {
                done = true;
            }
        }
        *//*

        return data;
    }
}
*/
