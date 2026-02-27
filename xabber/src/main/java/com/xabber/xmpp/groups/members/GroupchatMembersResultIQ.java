package com.xabber.xmpp.groups.members;


import com.xabber.xmpp.groups.GroupMemberExtensionElement;

import java.util.Collection;

public class GroupchatMembersResultIQ extends GroupchatMembersQueryIQ {

    private Collection<GroupMemberExtensionElement> listOfMembers;

    public GroupchatMembersResultIQ() {
        super();
    }

    public GroupchatMembersResultIQ(Collection<GroupMemberExtensionElement> listOfMembers) {
        this.listOfMembers = listOfMembers;
    }

    public Collection<GroupMemberExtensionElement> getListOfMembers() {
        return listOfMembers;
    }

    public void setListOfMembers(Collection<GroupMemberExtensionElement> listOfMembers) {
        this.listOfMembers = listOfMembers;
    }
}
