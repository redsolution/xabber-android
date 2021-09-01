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
package com.xabber.android.data.extension.ssn;

import com.xabber.android.data.NetworkException;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.account.OnAccountRemovedListener;
import com.xabber.android.data.connection.ConnectionItem;
import com.xabber.android.data.connection.OnPacketListener;
import com.xabber.android.data.connection.StanzaSender;
import com.xabber.android.data.connection.TLSMode;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.NestedMap;
import com.xabber.android.data.log.LogManager;
import com.xabber.xmpp.ssn.DisclosureValue;
import com.xabber.xmpp.ssn.Feature;
import com.xabber.xmpp.ssn.LoggingValue;
import com.xabber.xmpp.ssn.SecurityValue;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smackx.xdata.packet.DataForm;
import org.jxmpp.jid.Jid;

import java.util.Collection;

/**
 * Stanza Session Negotiation.
 * <p/>
 * http://xmpp.org/extensions/xep-0155.html
 *
 * @author alexander.ivanov
 */
public class SSNManager implements OnPacketListener, OnAccountRemovedListener {

    /**
     * Session state for the session id in account.
     */
    private final NestedMap<SessionState> sessionStates;

    private static SSNManager instance;

    public static SSNManager getInstance() {
        if (instance == null) {
            instance = new SSNManager();
        }

        return instance;
    }

    private SSNManager() {
        sessionStates = new NestedMap<>();
    }

    @Override
    public void onAccountRemoved(AccountItem accountItem) {
        sessionStates.clear(accountItem.getAccount().toString());
    }

    @Override
    public void onStanza(ConnectionItem connection, Stanza stanza) {
        Jid from = stanza.getFrom();
        if (from == null) {
            return;
        }
        if (!(connection instanceof AccountItem) || !(stanza instanceof Message)) {
            return;
        }
        AccountJid account = ((AccountItem) connection).getAccount();
        Message message = (Message) stanza;
        String session = message.getThread();
        if (session == null) {
            return;
        }
        for (ExtensionElement packetExtension : stanza.getExtensions()) {
            if (packetExtension instanceof Feature) {
                Feature feature = (Feature) packetExtension;
                if (!feature.isValid()) {
                    continue;
                }
                DataForm.Type dataFormType = feature.getDataFormType();
                if (dataFormType == DataForm.Type.form) {
                    onFormReceived(account, from, session, feature);
                } else if (dataFormType == DataForm.Type.submit) {
                    onSubmitReceived(account, from, session, feature);
                } else if (dataFormType == DataForm.Type.result) {
                    onResultReceived(account, session, feature);
                }
            }
        }
    }

    private void onFormReceived(AccountJid account, Jid from, String session, Feature feature) {
        boolean cancel = false;

        Collection<DisclosureValue> disclosureValues = feature
                .getDisclosureOptions();
        DisclosureValue disclosureValue = DisclosureValue.never;
        if (disclosureValues == null)
            disclosureValue = null;
        else if (!disclosureValues.contains(disclosureValue))
            cancel = true;

        Collection<SecurityValue> securityValues = feature.getSecurityOptions();
        SecurityValue securityValue;
        AccountItem accountItem = AccountManager.getInstance().getAccount(account);
        if (accountItem != null
                && accountItem.getConnectionSettings().getTlsMode() == TLSMode.required) {
            securityValue = SecurityValue.c2s;
        } else {
            securityValue = SecurityValue.none;
        }
        if (securityValues == null)
            securityValue = null;
        else if (!securityValues.contains(securityValue))
            cancel = true;

        if (cancel) {
            DataForm dataForm = Feature.createDataForm(DataForm.Type.submit);
            if (feature.getAcceptValue() != null) {
                Feature.addAcceptField(dataForm, false);
                sessionStates.remove(account.toString(), session);
            } else {
                Feature.addRenegotiateField(dataForm, false);
            }
            sendFeature(account, from, session, new Feature(dataForm));
            return;
        }

        DataForm dataForm = Feature.createDataForm(DataForm.Type.submit);
        if (feature.getAcceptValue() != null)
            Feature.addAcceptField(dataForm, true);
        else
            Feature.addRenegotiateField(dataForm, true);
        if (disclosureValue != null)
            Feature.addDisclosureField(dataForm, null, disclosureValue);
        if (securityValue != null)
            Feature.addSecurityField(dataForm, null, securityValue);
        sessionStates.put(account.toString(), session, SessionState.active);
        sendFeature(account, from, session, new Feature(dataForm));
    }

    private void onSubmitReceived(AccountJid account, Jid from, String session, Feature feature) {
        if (feature.getTerminateValue() != null) {
            onTerminateReceived(account, from, session);
            return;
        }
        if (!isAccepted(account, session, feature)) {
            return;
        }
        LoggingValue loggingValue = feature.getLoggingValue();
        DataForm dataForm = Feature.createDataForm(DataForm.Type.result);
        if (loggingValue == null) {
            if (feature.getAcceptValue() != null) {
                Feature.addAcceptField(dataForm, true);
            } else {
                Feature.addRenegotiateField(dataForm, true);
            }
            sendFeature(account, from, session, new Feature(dataForm));
            sessionStates.put(account.toString(), session, SessionState.active);
        } else {
            if (feature.getAcceptValue() != null) {
                Feature.addAcceptField(dataForm, false);
                sessionStates.remove(account.toString(), session);
            } else {
                Feature.addRenegotiateField(dataForm, false);
            }
            sendFeature(account, from, session, new Feature(dataForm));
        }
    }

    private void onTerminateReceived(AccountJid account, Jid from, String session) {
        if (sessionStates.get(account.toString(), session) == null) {
            return;
        }
        sessionStates.remove(account.toString(), session);
        DataForm dataForm = Feature.createDataForm(DataForm.Type.result);
        Feature.addTerminateField(dataForm);
        sendFeature(account, from, session, new Feature(dataForm));
    }

    private boolean isAccepted(AccountJid account, String session, Feature feature) {
        Boolean accept = feature.getAcceptValue();
        if (accept != null && !accept) {
            sessionStates.remove(account.toString(), session);
            return false;
        }
        Boolean renegotiate = feature.getRenegotiateValue();
        if (renegotiate != null && !renegotiate) {
            if (sessionStates.get(account.toString(), session) == SessionState.renegotiation) {
                sessionStates.put(account.toString(), session, SessionState.active);
            }
            return false;
        }
        return true;
    }

    private void onResultReceived(AccountJid account, String session, Feature feature) {
        isAccepted(account, session, feature);
    }

    private void sendFeature(AccountJid account, Jid user, String session, Feature feature) {
        Message message = new Message(user, Message.Type.normal);
        message.setThread(session);
        message.addExtension(feature);
        try {
            StanzaSender.sendStanza(account, message);
        } catch (NetworkException e) {
            LogManager.exception(this, e);
        }
    }

}
