package com.xabber.android.data.roster;

import com.xabber.android.data.BaseUIListener;

import java.util.Collection;

/**
 * Listener for chat state (XEP-0085) change.
 * Created by valery.miller on 12.02.18.
 */

public interface OnChatStateListener extends BaseUIListener {

    void onChatStateChanged(Collection<RosterContact> entities);

}
