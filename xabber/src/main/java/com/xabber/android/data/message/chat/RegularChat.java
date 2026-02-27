/*
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
package com.xabber.android.data.message.chat;

import android.content.Intent;

import androidx.annotation.NonNull;

import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.ContactJid;

import org.jivesoftware.smack.packet.Message.Type;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;

/**
 * Represents normal chat.
 *
 * @author alexander.ivanov
 */
public class RegularChat extends AbstractChat {

    private Intent intent;

    public RegularChat(AccountJid account, ContactJid user) {
        super(account, user);
        resource = null;
    }

    public Intent getIntent() {
        return intent;
    }

    public void setIntent(Intent intent) {
        this.intent = intent;
    }

    @NonNull
    @Override
    public Jid getTo() {
        if (resource == null) {
            return contactJid.getJid();
        } else {
            return JidCreate.fullFrom(contactJid.getJid().asEntityBareJidIfPossible(), resource);
        }
    }

    @Override
    public Type getType() {
        return Type.chat;
    }

    @Override
    protected void onComplete() {
        super.onComplete();
        sendMessages();
    }

}
