package com.xabber.android.data.extension.mam;

import com.xabber.android.data.BaseChatEvent;
import com.xabber.android.data.entity.BaseEntity;

public class LastHistoryLoadStartedEvent extends BaseChatEvent {
    public LastHistoryLoadStartedEvent(BaseEntity entity) {
        super(entity);
    }

    public AccountJid getAccount() {
        return getEntity().getAccount();
    }

    public UserJid getUser() {
        return getEntity().getUser();
    }
}
