package org.jivesoftware.smackx.mam.provider;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.provider.IQProvider;
import org.jivesoftware.smack.util.ParserUtils;
import org.jivesoftware.smackx.mam.packet.MamFinIQ;
import org.jivesoftware.smackx.rsm.packet.RSMSet;
import org.jivesoftware.smackx.rsm.provider.RSMSetProvider;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

public class MamFinIQProvider extends IQProvider<MamFinIQ> {
    @Override
    public MamFinIQ parse(XmlPullParser parser, int initialDepth) throws XmlPullParserException, IOException, SmackException {
        boolean done = false;

        MamFinIQ mamFinIQ = new MamFinIQ();

        mamFinIQ.setComplete(ParserUtils.getBooleanAttribute(parser, "complete"));

        RSMSet rsmSet = null;
        while (!done) {
            int eventType = parser.next();
            if (eventType == XmlPullParser.START_TAG) {
                if (parser.getName().equals("set")) {
                    rsmSet = new RSMSetProvider().parse(parser);
                }
            } else {
                if (eventType == XmlPullParser.END_TAG && parser.getDepth() == initialDepth) {
                    done = true;
                }
            }
        }

        mamFinIQ.setRsmSet(rsmSet);

        return mamFinIQ;
    }
}
