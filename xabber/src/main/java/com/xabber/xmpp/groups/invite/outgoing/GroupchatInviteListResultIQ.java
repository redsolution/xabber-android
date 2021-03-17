package com.xabber.xmpp.groups.invite.outgoing;

import com.xabber.xmpp.groups.GroupchatExtensionElement;
import com.xabber.xmpp.groups.GroupMemberExtensionElement;

import org.jivesoftware.smack.packet.IQ;

import java.util.ArrayList;

public class GroupchatInviteListResultIQ extends IQ {

    public static final String NAMESPACE = GroupchatExtensionElement.NAMESPACE
            + GroupchatInviteListQueryIQ.HASH_INVITE;
    public static final String ELEMENT = QUERY_ELEMENT;
    public static final String USER_ELEMENT = GroupMemberExtensionElement.ELEMENT;
    public static final String ATTRIBUTE_JID = "jid";

    private ArrayList<String> listOfInvitedJids;

    public GroupchatInviteListResultIQ() {
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
