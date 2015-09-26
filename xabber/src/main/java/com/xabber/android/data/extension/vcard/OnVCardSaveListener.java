package com.xabber.android.data.extension.vcard;


import com.xabber.android.data.BaseUIListener;

public interface OnVCardSaveListener extends BaseUIListener {
    void onVCardSaveSuccess(String account);
    void onVCardSaveFailed(String account);
}
