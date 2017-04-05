package com.xabber.android.data.extension.blocking;

import com.xabber.android.data.BaseUIListener;
import com.xabber.android.data.entity.AccountJid;

public interface OnBlockedListChangedListener extends BaseUIListener{
    void onBlockedListChanged(AccountJid account);
}
