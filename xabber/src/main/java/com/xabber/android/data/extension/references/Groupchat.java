package com.xabber.android.data.extension.references;

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

    public RefUser getUser() {
        return user;
    }
}
