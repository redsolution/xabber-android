package com.xabber.android.data.extension.vcard;


import com.xabber.android.data.BaseUIListener;
import com.xabber.android.data.entity.AccountJid;

public interface OnVCardSaveListener extends BaseUIListener {
    void onVCardSaveSuccess(AccountJid account);
    void onVCardSaveFailed(AccountJid account);
}
