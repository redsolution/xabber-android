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

import java.util.Collection;

import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smackx.packet.DataForm;

import com.xabber.android.data.Application;
import com.xabber.android.data.NetworkException;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.account.OnAccountRemovedListener;
import com.xabber.android.data.connection.ConnectionItem;
import com.xabber.android.data.connection.ConnectionManager;
import com.xabber.android.data.connection.OnPacketListener;
import com.xabber.android.data.connection.TLSMode;
import com.xabber.android.data.entity.NestedMap;
import com.xabber.android.data.extension.archive.MessageArchiveManager;
import com.xabber.xmpp.archive.OtrMode;
import com.xabber.xmpp.archive.SaveMode;
import com.xabber.xmpp.form.DataFormType;
import com.xabber.xmpp.ssn.DisclosureValue;
import com.xabber.xmpp.ssn.Feature;
import com.xabber.xmpp.ssn.LoggingValue;
import com.xabber.xmpp.ssn.SecurityValue;

/**
 * Stanza Session Negotiation.
 * 
 * http://xmpp.org/extensions/xep-0155.html
 * 
 * @author alexander.ivanov
 * 
 */
public class SSNManager implements OnPacketListener, OnAccountRemovedListener {

	/**
	 * Session state for the session id in account.
	 */
	private final NestedMap<SessionState> sessionStates;

	/**
	 * OTR encryption mode for the session id in account.
	 */
	private final NestedMap<OtrMode> sessionOtrs;

	private final static SSNManager instance;

	static {
		instance = new SSNManager(Application.getInstance());
		Application.getInstance().addManager(instance);
	}

	public static SSNManager getInstance() {
		return instance;
	}

	private SSNManager(Application application) {
		sessionStates = new NestedMap<SessionState>();
		sessionOtrs = new NestedMap<OtrMode>();
	}

	@Override
	public void onAccountRemoved(AccountItem accountItem) {
		sessionStates.clear(accountItem.getAccount());
		sessionOtrs.clear(accountItem.getAccount());
	}

	@Override
	public void onPacket(ConnectionItem connection, final String bareAddress,
			Packet packet) {
		String from = packet.getFrom();
		if (from == null)
			return;
		if (!(connection instanceof AccountItem)
				|| !(packet instanceof Message))
			return;
		String account = ((AccountItem) connection).getAccount();
		Message message = (Message) packet;
		String session = message.getThread();
		if (session == null)
			return;
		for (PacketExtension packetExtension : packet.getExtensions())
			if (packetExtension instanceof Feature) {
				Feature feature = (Feature) packetExtension;
				if (!feature.isValid())
					continue;
				DataFormType type = feature.getDataFormType();
				if (type == DataFormType.form)
					onFormReceived(account, from, bareAddress, session, feature);
				else if (type == DataFormType.submit)
					onSubmitReceived(account, from, bareAddress, session,
							feature);
				else if (type == DataFormType.result)
					onResultReceived(account, from, bareAddress, session,
							feature);
			}
	}

	private void onFormReceived(String account, String from,
			String bareAddress, String session, Feature feature) {
		OtrMode otrMode = getOtrMode(account, bareAddress, session);
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
		if (AccountManager.getInstance().getAccount(account)
				.getConnectionSettings().getTlsMode() == TLSMode.required)
			securityValue = SecurityValue.c2s;
		else
			securityValue = SecurityValue.none;
		if (securityValues == null)
			securityValue = null;
		else if (!securityValues.contains(securityValue))
			cancel = true;

		Collection<LoggingValue> loggingValues = feature.getLoggingOptions();
		LoggingValue loggingValue;
		if (loggingValues == null)
			loggingValue = null;
		else {
			loggingValue = otrMode.selectLoggingValue(loggingValues);
			if (loggingValue == null)
				cancel = true;
		}

		if (cancel) {
			DataForm dataForm = Feature.createDataForm(DataFormType.submit);
			if (feature.getAcceptValue() != null) {
				Feature.addAcceptField(dataForm, false);
				sessionStates.remove(account, session);
			} else {
				Feature.addRenegotiateField(dataForm, false);
			}
			sendFeature(account, from, session, new Feature(dataForm));
			return;
		}

		DataForm dataForm = Feature.createDataForm(DataFormType.submit);
		if (feature.getAcceptValue() != null)
			Feature.addAcceptField(dataForm, true);
		else
			Feature.addRenegotiateField(dataForm, true);
		if (disclosureValue != null)
			Feature.addDisclosureField(dataForm, null, disclosureValue);
		if (securityValue != null)
			Feature.addSecurityField(dataForm, null, securityValue);
		if (loggingValue != null) {
			try {
				if (loggingValue == LoggingValue.mustnot)
					MessageArchiveManager.getInstance().setSaveMode(account,
							from, session, SaveMode.fls);
				else
					MessageArchiveManager.getInstance().setSaveMode(account,
							from, session, SaveMode.body);
			} catch (NetworkException e) {
			}
			Feature.addLoggingField(dataForm, null, loggingValue);
		}
		sessionStates.put(account, session, SessionState.active);
		sendFeature(account, from, session, new Feature(dataForm));
	}

	private void onSubmitReceived(String account, String from,
			String bareAddress, String session, Feature feature) {
		if (feature.getTerminateValue() != null) {
			onTerminateReceived(account, from, session);
			return;
		}
		if (!isAccepted(account, from, bareAddress, session, feature))
			return;
		OtrMode otrMode = getOtrMode(account, bareAddress, session);
		LoggingValue loggingValue = feature.getLoggingValue();
		if (loggingValue == null || otrMode.acceptLoggingValue(loggingValue)) {
			DataForm dataForm = Feature.createDataForm(DataFormType.result);
			if (feature.getAcceptValue() != null)
				Feature.addAcceptField(dataForm, true);
			else
				Feature.addRenegotiateField(dataForm, true);
			sendFeature(account, from, session, new Feature(dataForm));
			sessionStates.put(account, session, SessionState.active);
		} else {
			DataForm dataForm = Feature.createDataForm(DataFormType.result);
			if (feature.getAcceptValue() != null) {
				Feature.addAcceptField(dataForm, false);
				sessionStates.remove(account, session);
			} else
				Feature.addRenegotiateField(dataForm, false);
			sendFeature(account, from, session, new Feature(dataForm));
		}
	}

	private void onTerminateReceived(String account, String from, String session) {
		if (sessionStates.get(account, session) == null)
			return;
		sessionStates.remove(account, session);
		DataForm dataForm = Feature.createDataForm(DataFormType.result);
		Feature.addTerminateField(dataForm);
		sendFeature(account, from, session, new Feature(dataForm));
	}

	private OtrMode getOtrMode(String account, String bareAddress,
			String session) {
		OtrMode otrMode = sessionOtrs.get(account, session);
		if (otrMode != null)
			return otrMode;
		otrMode = MessageArchiveManager.getInstance().getOtrMode(account,
				bareAddress);
		if (otrMode != null)
			return otrMode;
		return OtrMode.concede;
	}

	private boolean isAccepted(String account, String from, String bareAddress,
			String session, Feature feature) {
		Boolean accept = feature.getAcceptValue();
		if (accept != null && !accept) {
			sessionStates.remove(account, session);
			return false;
		}
		Boolean renegotiate = feature.getRenegotiateValue();
		if (renegotiate != null && !renegotiate) {
			if (sessionStates.get(account, session) == SessionState.renegotiation)
				sessionStates.put(account, session, SessionState.active);
			return false;
		}
		return true;
	}

	private void onResultReceived(String account, String from,
			String bareAddress, String session, Feature feature) {
		isAccepted(account, from, bareAddress, session, feature);
	}

	/**
	 * Sets OTR mode for the session and starts negotiation / renegotiation.
	 * 
	 * @param account
	 * @param user
	 * @param session
	 * @param otrMode
	 * @throws NetworkException
	 */
	public void setSessionOtrMode(String account, String user, String session,
			OtrMode otrMode) {
		if (sessionOtrs.get(account, session) == otrMode)
			return;
		sessionOtrs.put(account, session, otrMode);
		SessionState state = sessionStates.get(account, session);
		DataForm dataForm = Feature.createDataForm(DataFormType.form);
		Feature.addLoggingField(dataForm, otrMode.getLoggingValues(),
				otrMode.getLoggingValues()[0]);
		Feature.addDisclosureField(dataForm, DisclosureValue.values(),
				otrMode.getDisclosureValue());
		Feature.addSecurityField(dataForm, SecurityValue.values(),
				otrMode.getSecurityValue());
		if (state == null || state == SessionState.requesting) {
			sessionStates.put(account, session, SessionState.requesting);
			Feature.addAcceptField(dataForm, true);
		} else {
			sessionStates.put(account, session, SessionState.renegotiation);
			Feature.addRenegotiateField(dataForm, true);
		}
		sendFeature(account, user, session, new Feature(dataForm));
	}

	private void sendFeature(String account, String user, String session,
			Feature feature) {
		Message message = new Message(user, Message.Type.normal);
		message.setThread(session);
		message.addExtension(feature);
		try {
			ConnectionManager.getInstance().sendPacket(account, message);
		} catch (NetworkException e) {
		}
	}

}
