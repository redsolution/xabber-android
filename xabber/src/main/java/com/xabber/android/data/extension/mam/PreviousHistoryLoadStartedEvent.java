package com.xabber.android.data.extension.mam;

import com.xabber.android.data.BaseChatEvent;
import com.xabber.android.data.entity.BaseEntity;

public class PreviousHistoryLoadStartedEvent extends BaseChatEvent {
    public PreviousHistoryLoadStartedEvent(BaseEntity entity) {
        super(entity);
    }

    public String getAccount() {
        return getEntity().getAccount();
    }

    public String getUser() {
        return getEntity().getUser();
    }
}
