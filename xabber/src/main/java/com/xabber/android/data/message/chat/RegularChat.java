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

import com.xabber.android.data.NetworkException;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.ContactJid;
import com.xabber.android.data.extension.otr.OTRManager;
import com.xabber.android.data.extension.otr.SecurityLevel;
import com.xabber.android.data.log.LogManager;

import net.java.otr4j.OtrException;

import org.jivesoftware.smack.packet.Message.Type;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.jid.parts.Resourcepart;

/**
 * Represents normal chat.
 *
 * @author alexander.ivanov
 */
public class RegularChat extends AbstractChat {

    /**
     * Resource used for contact.
     */
    private Resourcepart resource;
    private Resourcepart OTRresource;
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

    public Resourcepart getOTRresource() {
        return OTRresource;
    }

    public void setOTRresource(Resourcepart OTRresource) {
        this.OTRresource = OTRresource;
    }

    public Resourcepart getResource() {
        return resource;
    }
    public void setResource(Resourcepart resource) { this.resource = resource; }

    @NonNull
    @Override
    public Jid getTo() {
        if (OTRresource != null) {
            return JidCreate.fullFrom(contactJid.getJid().asEntityBareJidIfPossible(), OTRresource);
        } else {
            if (resource == null) {
                return contactJid.getJid();
            } else return JidCreate.fullFrom(contactJid.getJid().asEntityBareJidIfPossible(), resource);
        }
    }

    @Override
    public Type getType() {
        return Type.chat;
    }

    @Override
    public boolean canSendMessage() {
        if (super.canSendMessage()) {
            if (SettingsManager.securityOtrMode() != SettingsManager.SecurityOtrMode.required) return true;

            SecurityLevel securityLevel = OTRManager.getInstance().getSecurityLevel(account, contactJid);

            if (securityLevel != SecurityLevel.plain) return true;
            try {
                OTRManager.getInstance().startSession(account, contactJid);
            } catch (NetworkException ignored) { }
        }
        return false;
    }

    @Override
    protected String prepareText(String text) {
        text = super.prepareText(text);
        try {
            return OTRManager.getInstance().transformSending(account, contactJid, text);
        } catch (OtrException e) {
            LogManager.exception(this, e);
            return null;
        }
    }

    @Override
    protected void onComplete() {
        super.onComplete();
        sendMessages();
    }

}
