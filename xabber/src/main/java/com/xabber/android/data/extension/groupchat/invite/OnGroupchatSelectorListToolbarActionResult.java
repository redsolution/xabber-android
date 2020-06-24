package com.xabber.android.data.extension.groupchat.invite;

import com.xabber.android.data.BaseUIListener;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.ContactJid;

import java.util.List;

public interface OnGroupchatSelectorListToolbarActionResult extends BaseUIListener {
    void onActionSuccess(AccountJid account, ContactJid groupchatJid, List<String> successfulJids);
    void onPartialSuccess(AccountJid account, ContactJid groupchatJid, List<String> successfulJids, List<String> failedJids);
    void onActionFailure(AccountJid account, ContactJid groupchatJid, List<String> failedJids);
}
