package com.xabber.android.data.extension.blocking;

import com.xabber.android.data.BaseUIListener;

public interface OnBlockedListChangedListener extends BaseUIListener{
    void onBlockedListChanged(String account);
}
