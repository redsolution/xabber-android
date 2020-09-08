package com.xabber.android.data.extension.groupchat.members;


import com.xabber.android.data.extension.groupchat.GroupchatMemberExtensionElement;

import java.util.Collection;

public class GroupchatMembersResultIQ extends GroupchatMembersQueryIQ {

    private Collection<GroupchatMemberExtensionElement> listOfMembers;

    public GroupchatMembersResultIQ() {
        super();
    }

    public GroupchatMembersResultIQ(Collection<GroupchatMemberExtensionElement> listOfMembers) {
        this.listOfMembers = listOfMembers;
    }

    public Collection<GroupchatMemberExtensionElement> getListOfMembers() {
        return listOfMembers;
    }

    public void setListOfMembers(Collection<GroupchatMemberExtensionElement> listOfMembers) {
        this.listOfMembers = listOfMembers;
    }
}
