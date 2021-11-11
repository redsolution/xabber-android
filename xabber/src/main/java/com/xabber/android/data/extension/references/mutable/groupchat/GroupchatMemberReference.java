package com.xabber.android.data.extension.references.mutable.groupchat;

import com.xabber.xmpp.groups.GroupMemberExtensionElement;
import com.xabber.android.data.extension.references.mutable.Mutable;

import org.jivesoftware.smack.util.XmlStringBuilder;

public class GroupchatMemberReference extends Mutable {

    private GroupMemberExtensionElement user;

    public GroupchatMemberReference(int begin, int end, GroupMemberExtensionElement user) {
        super(begin, end);
        this.user = user;
    }

    public GroupMemberExtensionElement getUser() {
        return user;
    }

    public void setUser(GroupMemberExtensionElement user) {
        this.user = user;
    }

    @Override
    public void appendToXML(XmlStringBuilder xml) {
        if (user != null) xml.append(user.toXML());
    }
}
