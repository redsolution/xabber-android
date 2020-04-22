package com.xabber.android.data.extension.references.mutable.groupchat;

import com.xabber.android.data.extension.groupchat.GroupchatUserExtension;
import com.xabber.android.data.extension.references.mutable.Mutable;

import org.jivesoftware.smack.util.XmlStringBuilder;

public class GroupchatUserReference extends Mutable {

    private GroupchatUserExtension user;

    public GroupchatUserReference(int begin, int end, GroupchatUserExtension user) {
        super(begin, end);
        this.user = user;
    }

    public void setUser(GroupchatUserExtension user) {
        this.user = user;
    }

    public GroupchatUserExtension getUser() {
        return user;
    }

    @Override
    public void appendToXML(XmlStringBuilder xml) {
        if (user != null) xml.append(user.toXML());
    }
}
