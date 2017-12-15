package com.xabber.android.ui.adapter.contactlist.viewobjects;

import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.account.StatusMode;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.ui.color.ColorManager;

/**
 * Created by valery.miller on 11.10.17.
 */

public class BottomAccountSeparatorVO extends BaseRosterItemVO {

    public BottomAccountSeparatorVO(int accountColorIndicator, boolean showOfflineShadow) {
        super(accountColorIndicator, showOfflineShadow);
    }

    public static BottomAccountSeparatorVO convert(AccountJid account) {
        boolean showOfflineShadow;
        int accountColorIndicator = ColorManager.getInstance().getAccountPainter().getAccountMainColor(account);

        AccountItem accountItem = AccountManager.getInstance().getAccount(account);
        StatusMode statusMode = null;
        if (accountItem != null) {
            statusMode = accountItem.getDisplayStatusMode();
        }

        if (statusMode == StatusMode.unavailable || statusMode == StatusMode.connection) {
            showOfflineShadow = true;
        } else {
            showOfflineShadow = false;
        }

        return new BottomAccountSeparatorVO(accountColorIndicator, showOfflineShadow);
    }
}
