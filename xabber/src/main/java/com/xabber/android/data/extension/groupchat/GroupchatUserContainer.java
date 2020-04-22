package com.xabber.android.data.extension.groupchat;

import com.xabber.android.data.extension.references.mutable.groupchat.GroupchatUserReference;

import org.jivesoftware.smack.util.XmlStringBuilder;

public class GroupchatUserContainer extends Groupchat {

    private GroupchatUserReference user;

    public GroupchatUserContainer() {}

    public GroupchatUserContainer(GroupchatUserReference user) {
        this.user = user;
    }

    public GroupchatUserExtension getUser() {
        return user != null ? user.getUser() : null;
    }

    public GroupchatUserReference getUserReference() {
        return user;
    }

    @Override
    public void appendToXML(XmlStringBuilder xml) {
        if (user != null) xml.append(user.toXML());
    }
}
