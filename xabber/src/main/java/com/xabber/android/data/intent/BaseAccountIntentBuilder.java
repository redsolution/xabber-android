/**
 * Copyright (c) 2013, Redsolution LTD. All rights reserved.
 *
 * This file is part of Xabber project; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License, Version 3.
 *
 * Xabber is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License,
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */
package com.xabber.android.data.intent;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.Nullable;

import com.xabber.android.data.log.LogManager;
import com.xabber.android.data.entity.AccountJid;

import org.jxmpp.stringprep.XmppStringprepException;

class BaseAccountIntentBuilder<T extends BaseAccountIntentBuilder<?>> extends
        SegmentIntentBuilder<T> {

    private static final String LOG_TAG = BaseAccountIntentBuilder.class.getSimpleName();
    private AccountJid account;

    public BaseAccountIntentBuilder(Context context, Class<?> cls) {
        super(context, cls);
    }

    @SuppressWarnings("unchecked")
    public T setAccount(AccountJid account) {
        this.account = account;
        return (T) this;
    }

    @Override
    void preBuild() {
        super.preBuild();
        if (account == null) {
            return;
        }
        if (getSegmentCount() != 0) {
            throw new IllegalStateException("Wrong segment count: " + getSegmentCount());
        }
        addSegment(account.toString());
    }

    @Nullable
    public static AccountJid getAccount(Intent intent) {
        try {
            String segment = getSegment(intent, 0);
            if (segment != null) {
                return AccountJid.from(segment);
            }
        } catch (XmppStringprepException e) {
            LogManager.exception(LOG_TAG, e);
        }
        return null;
    }

}