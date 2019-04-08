package com.xabber.android.data.push;

import com.xabber.android.R;

public enum PushState {

    disabled,

    notSupport,

    connecting,

    enabled;

    public int getStringId() {
        switch (this) {
            case disabled:
                return R.string.account_push_state_disabled;
            case notSupport:
                return R.string.account_push_state_not_support;
            case connecting:
                return R.string.account_push_state_connecting;
            case enabled:
                return R.string.account_push_state_enabled;
            default:
                throw new IllegalStateException();
        }
    }

}
