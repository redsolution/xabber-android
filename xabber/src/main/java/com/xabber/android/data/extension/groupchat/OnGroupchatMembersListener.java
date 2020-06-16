package com.xabber.android.data.extension.groupchat;

import com.xabber.android.data.BaseUIListener;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.ContactJid;
import com.xabber.android.data.message.chat.groupchat.GroupchatMember;

import java.util.ArrayList;

public interface OnGroupchatMembersListener extends BaseUIListener {

    void onGroupchatMembersReceived(AccountJid account, ContactJid groupchatJid, ArrayList<GroupchatMember> listOfMembers);

}
