package com.xabber.android.data.push;

import com.xabber.android.R;

public enum PushState {

    notSupport,

    enabling,

    enabled;

    public int getStringId() {
        switch (this) {

            case notSupport:
                return R.string.account_push_state_not_support;
            case enabling:
                return R.string.account_push_state_enabling;
            case enabled:
                return R.string.account_push_state_enabled;
            default:
                throw new IllegalStateException();
        }
    }

}
