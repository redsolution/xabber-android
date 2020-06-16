package com.xabber.android.data.extension.groupchat;


import java.util.Collection;

public class GroupchatMembersResultIQ extends GroupchatMembersQueryIQ {

    private Collection<GroupchatUserExtension> listOfMembers;

    public GroupchatMembersResultIQ() {
        super();
    }

    public GroupchatMembersResultIQ(Collection<GroupchatUserExtension> listOfMembers) {
        this.listOfMembers = listOfMembers;
    }

    public Collection<GroupchatUserExtension> getListOfMembers() {
        return listOfMembers;
    }

    public void setListOfMembers(Collection<GroupchatUserExtension> listOfMembers) {
        this.listOfMembers = listOfMembers;
    }
}
