package com.xabber.android.ui.adapter.accountoptions;

import android.support.annotation.DrawableRes;
import android.support.annotation.StringRes;

import com.xabber.android.BuildConfig;
import com.xabber.android.R;

public enum  AccountOption {
    CONNECTION_SETTINGS(R.drawable.ic_settings_grey600_24dp, R.string.account_connection_settings),
    SYNCHRONIZATION(R.drawable.ic_cloud_sync, R.string.account_sync),
    PUSH_NOTIFICATIONS(R.drawable.ic_sync_done, R.string.account_push),
    COLOR(R.drawable.ic_color_lens_grey600_24dp, R.string.account_color),
    BLOCK_LIST(R.drawable.ic_block_grey600_24dp, R.string.blocked_contacts),
    SERVER_INFO(R.drawable.ic_info_grey600_24dp, R.string.account_server_info),
    CHAT_HISTORY(R.drawable.ic_archive_grey600_24dp, R.string.account_chat_history),
    BOOKMARKS(R.drawable.ic_bookmark, R.string.account_bookmarks);

    @DrawableRes
    private final int iconId;

    @StringRes
    private final int titleId;

    private String description;

    AccountOption(int iconId, int titleId) {
        this.iconId = iconId;
        this.titleId = titleId;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    int getIconId() {
        return iconId;
    }

    int getTitleId() {
        return titleId;
    }

    public String getDescription() {
        return description;
    }

    public static AccountOption[] getValues() {
        if (BuildConfig.FLAVOR.equals("dev")) {
            return AccountOption.values();
        } else {
            int i = 0;
            AccountOption[] values = new AccountOption[AccountOption.values().length - 1];
            for (AccountOption option : AccountOption.values()) {
                if (option != AccountOption.PUSH_NOTIFICATIONS) {
                    values[i] = option;
                    i++;
                }
            }
            return values;
        }
    }
}
