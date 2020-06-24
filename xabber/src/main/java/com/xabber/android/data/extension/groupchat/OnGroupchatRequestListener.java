package com.xabber.android.data.extension.groupchat;

import com.xabber.android.data.BaseUIListener;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.ContactJid;
import com.xabber.android.data.extension.groupchat.block.GroupchatBlocklistItemElement;
import com.xabber.android.data.message.chat.groupchat.GroupchatMember;

import java.util.ArrayList;

public interface OnGroupchatRequestListener extends BaseUIListener {

    void onGroupchatMembersReceived(AccountJid account, ContactJid groupchatJid, ArrayList<GroupchatMember> listOfMembers);

    void onGroupchatInvitesReceived(AccountJid account, ContactJid groupchatJid, ArrayList<String> listOfInvites);

    void onGroupchatBlocklistReceived(AccountJid account, ContactJid groupchatJid, ArrayList<GroupchatBlocklistItemElement> listOfInvites);
}
