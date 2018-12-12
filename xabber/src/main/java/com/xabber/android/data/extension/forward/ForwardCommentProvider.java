package com.xabber.android.data.extension.forward;

import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.xmlpull.v1.XmlPullParser;

public class ForwardCommentProvider extends ExtensionElementProvider<ForwardComment> {

    @Override
    public ForwardComment parse(XmlPullParser parser, int initialDepth) throws Exception {
        String comment = null;

        outerloop: while (true) {
            int eventType = parser.getEventType();
            switch (eventType) {
                case XmlPullParser.START_TAG:
                    if (parser.getEventType() == XmlPullParser.START_TAG
                        && ForwardComment.ELEMENT.equals(parser.getName())
                        && ForwardComment.NAMESPACE.equals(parser.getNamespace())) {
                        comment = parser.nextText();
                    }
                    break;
                case XmlPullParser.END_TAG:
                    if (parser.getDepth() == initialDepth) {
                        break outerloop;
                    }
                    break;
                case XmlPullParser.END_DOCUMENT:
                    break outerloop;
            }
            parser.next();
        }
        if (comment != null) return new ForwardComment(comment);
        else return null;
    }
}
