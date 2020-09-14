package com.xabber.android.data.extension.groupchat;

import com.xabber.android.data.BaseUIListener;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.ContactJid;

public interface OnGroupchatRequestListener extends BaseUIListener {

    void onGroupchatMembersReceived(AccountJid account, ContactJid groupchatJid);

    void onGroupchatInvitesReceived(AccountJid account, ContactJid groupchatJid);

    void onGroupchatBlocklistReceived(AccountJid account, ContactJid groupchatJid);

    void onMeReceived(AccountJid accountJid, ContactJid groupchatJid);
}
