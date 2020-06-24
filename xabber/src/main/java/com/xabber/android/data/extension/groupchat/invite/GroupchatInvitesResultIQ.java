package com.xabber.android.data.extension.groupchat.invite;

import com.xabber.android.data.extension.groupchat.GroupchatExtensionElement;
import com.xabber.android.data.extension.groupchat.GroupchatUserExtension;

import org.jivesoftware.smack.packet.IQ;

import java.util.ArrayList;

public class GroupchatInvitesResultIQ extends IQ {

    public static final String NAMESPACE = GroupchatExtensionElement.NAMESPACE
            + GroupchatInvitesQueryIQ.HASH_INVITE;
    public static final String ELEMENT = QUERY_ELEMENT;
    public static final String USER_ELEMENT = GroupchatUserExtension.ELEMENT;
    public static final String ATTRIBUTE_JID = "jid";

    private ArrayList<String> listOfInvitedJids;

    public GroupchatInvitesResultIQ() {
        super(ELEMENT, NAMESPACE);
    }

    public void setListOfInvitedJids(ArrayList<String> listOfInvitedJids) {
        this.listOfInvitedJids = listOfInvitedJids;
    }

    public ArrayList<String> getListOfInvitedJids() {
        return listOfInvitedJids;
    }

    @Override
    protected IQChildElementXmlStringBuilder getIQChildElementBuilder(IQChildElementXmlStringBuilder xml) {
        if (listOfInvitedJids != null && !listOfInvitedJids.isEmpty()) {
            xml.rightAngleBracket();
            for (String jid : listOfInvitedJids) {
                xml.halfOpenElement(USER_ELEMENT);
                xml.attribute(ATTRIBUTE_JID, jid);
                xml.closeEmptyElement();
            }
        } else {
            xml.setEmptyElement();
        }
        return null;
    }
}
