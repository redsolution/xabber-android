package com.xabber.android.ui.adapter.contactlist;

import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.BaseEntity;
import com.xabber.android.data.entity.UserJid;

class AccountBottomSeparator extends BaseEntity {
    AccountBottomSeparator(AccountJid account, UserJid user) {
        super(account, user);
    }
}
