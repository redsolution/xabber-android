package com.xabber.android.ui.adapter.contactlist.viewobjects;

/**
 * Created by valery.miller on 11.10.17.
 */

public class BaseRosterItemVO {

    private int accountColorIndicator;
    private boolean showOfflineShadow;

    public BaseRosterItemVO(int accountColorIndicator, boolean showOfflineShadow) {
        this.accountColorIndicator = accountColorIndicator;
        this.showOfflineShadow = showOfflineShadow;
    }

    public int getAccountColorIndicator() {
        return accountColorIndicator;
    }

    public void setAccountColorIndicator(int accountColorIndicator) {
        this.accountColorIndicator = accountColorIndicator;
    }

    public boolean isShowOfflineShadow() {
        return showOfflineShadow;
    }

    public void setShowOfflineShadow(boolean showOfflineShadow) {
        this.showOfflineShadow = showOfflineShadow;
    }
}
