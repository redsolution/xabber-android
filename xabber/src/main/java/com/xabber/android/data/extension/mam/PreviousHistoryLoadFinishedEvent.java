package com.xabber.android.data.extension.mam;

import com.xabber.android.data.BaseChatEvent;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.BaseEntity;
import com.xabber.android.data.entity.ContactJid;

public class PreviousHistoryLoadFinishedEvent extends BaseChatEvent {
    public PreviousHistoryLoadFinishedEvent(BaseEntity entity) {
        super(entity);
    }

    public AccountJid getAccount() {
        return getEntity().getAccount();
    }

    public ContactJid getUser() {
        return getEntity().getUser();
    }
}
