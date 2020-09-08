package com.xabber.android.data.extension.groupchat;

import com.xabber.android.data.extension.references.mutable.groupchat.GroupchatMemberReference;

import org.jivesoftware.smack.util.XmlStringBuilder;

public class GroupchatMemberContainer extends GroupchatExtensionElement {

    private GroupchatMemberReference user;

    public GroupchatMemberContainer() {
    }

    public GroupchatMemberContainer(GroupchatMemberReference user) {
        this.user = user;
    }

    public GroupchatMemberExtensionElement getUser() {
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
