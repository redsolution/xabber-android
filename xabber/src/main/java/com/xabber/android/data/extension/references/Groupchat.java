package com.xabber.android.data.extension.references;

import org.jivesoftware.smack.util.XmlStringBuilder;

public class Groupchat extends ReferenceElement {

    private final RefUser user;

    public Groupchat(int begin, int end, RefUser user) {
        super(begin, end);
        this.user = user;
    }

    @Override
    public Type getType() {
        return Type.groupchat;
    }

    @Override
    public void appendToXML(XmlStringBuilder xml) {
        if (user != null) xml.append(user.toXML());
    }

    public RefUser getUser() {
        return user;
    }
}
