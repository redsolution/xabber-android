package com.xabber.android.data.extension.groupchat;

import androidx.annotation.NonNull;

import com.xabber.android.data.BaseUIListener;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.ContactJid;
import com.xabber.android.data.extension.groupchat.rights.GroupchatMemberRightsReplyIQ;

import javax.annotation.Nonnull;

public interface OnGroupchatRequestListener extends BaseUIListener {

    void onGroupchatMembersReceived(AccountJid account, ContactJid groupchatJid);

    void onGroupchatInvitesReceived(AccountJid account, ContactJid groupchatJid);

    void onGroupchatBlocklistReceived(AccountJid account, ContactJid groupchatJid);

    void onMeReceived(AccountJid accountJid, ContactJid groupchatJid);

    void onGroupchatMemberRightsFormReceived(@Nonnull AccountJid accountJid, @NonNull ContactJid groupchatJid, @NonNull GroupchatMemberRightsReplyIQ iq);

    void onGroupchatMemberUpdated(AccountJid accountJid, ContactJid groupchatJid, String groupchatMemberId);
}
