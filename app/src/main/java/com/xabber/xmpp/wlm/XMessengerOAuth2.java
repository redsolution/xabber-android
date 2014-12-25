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
package com.xabber.xmpp.wlm;

import java.io.IOException;

import org.jivesoftware.smack.SASLAuthentication;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.sasl.SASLMechanism;

/**
 * X-MESSENGER-OAUTH2 mechanism for SASL authentication with Windows Live
 * Messenger service.
 */
public class XMessengerOAuth2 extends SASLMechanism {

	public XMessengerOAuth2(SASLAuthentication saslAuthentication) {
		super(saslAuthentication);
	}

	@Override
	protected String getName() {
		return "X-MESSENGER-OAUTH2";
	}

	@Override
	protected void authenticate() throws IOException, XMPPException {
		getSASLAuthentication().send(new AuthMechanism(getName(), password));
	}

}
