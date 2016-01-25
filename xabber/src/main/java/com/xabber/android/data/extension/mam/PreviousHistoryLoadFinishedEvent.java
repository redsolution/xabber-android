package com.xabber.android.data.extension.mam;

import com.xabber.android.data.BaseChatEvent;
import com.xabber.android.data.entity.BaseEntity;

public class PreviousHistoryLoadFinishedEvent extends BaseChatEvent {
    public PreviousHistoryLoadFinishedEvent(BaseEntity entity) {
        super(entity);
    }

    public String getAccount() {
        return getEntity().getAccount();
    }

    public String getUser() {
        return getEntity().getUser();
    }
}
