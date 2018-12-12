package com.xabber.android.data.extension.forward;

import android.util.Log;

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
                        try {
                            comment = parser.nextText();
                        } catch (Exception e) { Log.d("CommentProvider", "error in parsing"); }
                    } else parser.next();
                    break;
                case XmlPullParser.END_TAG:
                    if (parser.getDepth() == initialDepth) {
                        break outerloop;
                    }
                    break;
                default:
                    parser.next();
            }
        }
        if (comment != null) return new ForwardComment(comment);
        else return null;
    }
}
