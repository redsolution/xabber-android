package com.xabber.xmpp.groups;

import com.xabber.android.data.extension.references.mutable.groupchat.GroupchatMemberReference;

import org.jivesoftware.smack.util.XmlStringBuilder;

public class GroupMemberContainerExtensionElement extends GroupExtensionElement {

    private final GroupchatMemberReference user;

    public GroupMemberContainerExtensionElement(GroupchatMemberReference user) {
        this.user = user;
    }

    public GroupMemberExtensionElement getUser() {
        return user != null ? user.getUser() : null;
    }

    public GroupchatMemberReference getUserReference() {
        return user;
    }

    @Override
    public void appendToXML(XmlStringBuilder xml) {
        if (user != null) xml.append(user.toXML());
    }
}
