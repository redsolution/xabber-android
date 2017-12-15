package com.xabber.android.ui.adapter.contactlist.viewobjects;

import android.support.annotation.Nullable;

import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.account.StatusMode;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.ui.adapter.contactlist.AccountConfiguration;
import com.xabber.android.ui.color.ColorManager;

/**
 * Created by valery.miller on 16.10.17.
 */

public class ButtonVO extends BaseRosterItemVO {

    public final static String ACTION_SHOW_ALL_CHATS = "Show all chats";
    public final static String ACTION_ADD_CONTACT = "Add contact";

    private String title;
    private String action;
    private AccountJid account;

    public ButtonVO(int accountColorIndicator, boolean showOfflineShadow, String title, String action, @Nullable AccountJid account) {
        super(accountColorIndicator, showOfflineShadow);
        this.title = title;
        this.action = action;
        this.account = account;
    }

    public static ButtonVO convert(@Nullable AccountConfiguration configuration, String title, String action) {
        boolean showOfflineShadow = false;
        int accountColorIndicator = ColorManager.getInstance().getAccountPainter().getDefaultMainColor();
        AccountJid account = null;

        if (configuration != null) {
             account = configuration.getAccount();

            accountColorIndicator = ColorManager.getInstance().getAccountPainter().getAccountMainColor(account);

            AccountItem accountItem = AccountManager.getInstance().getAccount(account);

            if (accountItem != null) {
                StatusMode statusMode = accountItem.getDisplayStatusMode();
                if (statusMode == StatusMode.unavailable || statusMode == StatusMode.connection) {
                    showOfflineShadow = true;
                } else {
                    showOfflineShadow = false;
                }
            }
        }

        return new ButtonVO(accountColorIndicator, showOfflineShadow, title, action, account);
    }

    public String getTitle() {
        return title;
    }

    public String getAction() {
        return action;
    }

    public @Nullable AccountJid getAccount() {
        return account;
    }
}
