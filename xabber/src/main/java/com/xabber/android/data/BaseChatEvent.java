package com.xabber.android.data;

import com.xabber.android.data.entity.BaseEntity;

public class BaseChatEvent {

    private BaseEntity entity;

    public BaseChatEvent(BaseEntity entity) {
        this.entity = entity;
    }

    public BaseEntity getEntity() {
        return entity;
    }
}
