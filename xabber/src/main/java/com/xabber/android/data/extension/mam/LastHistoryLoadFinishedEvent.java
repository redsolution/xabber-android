package com.xabber.android.data.extension.mam;

import com.xabber.android.data.BaseChatEvent;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.BaseEntity;
import com.xabber.android.data.entity.UserJid;

public class LastHistoryLoadFinishedEvent extends BaseChatEvent {
    public LastHistoryLoadFinishedEvent(BaseEntity entity) {
        super(entity);
    }

    public AccountJid getAccount() {
        return getEntity().getAccount();
    }

    public UserJid getUser() {
        return getEntity().getUser();
    }
}
