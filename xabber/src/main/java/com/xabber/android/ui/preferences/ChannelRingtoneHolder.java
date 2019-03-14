package com.xabber.android.ui.preferences;

import com.xabber.android.data.notification.NotificationChannelUtils;

public class ChannelRingtoneHolder extends BaseSoundPrefFragment.RingtoneHolder {
    NotificationChannelUtils.ChannelType type;

    public ChannelRingtoneHolder(String uri, NotificationChannelUtils.ChannelType type) {
        super(uri);
        this.type = type;
    }
}
