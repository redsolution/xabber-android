package com.xabber.android.data.extension.groupchat.rights;

import com.xabber.android.data.extension.groupchat.members.GroupchatMembersQueryIQ;
import com.xabber.android.data.extension.httpfileupload.CustomDataProvider;

import org.jivesoftware.smack.provider.IQProvider;
import org.jivesoftware.smackx.xdata.packet.DataForm;
import org.xmlpull.v1.XmlPullParser;

public class GroupchatMemberRightsReplyIqProvider extends IQProvider<GroupchatMemberRightsReplyIQ> {

    @Override
    public GroupchatMemberRightsReplyIQ parse(XmlPullParser parser, int initialDepth) throws Exception {
        GroupchatMemberRightsReplyIQ resultIQ = new GroupchatMemberRightsReplyIQ();
        DataForm dataForm = new DataForm(DataForm.Type.form);

        outerloop: while(true){
            int event = parser.getEventType();
            switch (event){

                case XmlPullParser.START_TAG:

                    switch (parser.getNamespace()) {
                        case DataForm.NAMESPACE:
                            dataForm = CustomDataProvider.INSTANCE.parse(parser, initialDepth);
                            break outerloop;
                        default:
                            parser.next();
                    }

                case XmlPullParser.END_TAG:
                    if (GroupchatMembersQueryIQ.ELEMENT.equals(parser.getName())) {
                        break outerloop;
                    } else parser.next();
                    break;

                default:
                    parser.next();
            }
        }
        resultIQ.setDataFrom(dataForm);
        return resultIQ;
    }

}
