package org.jivesoftware.smackx.packet;

import org.jivesoftware.smack.packet.IQ;

/**
 * Represents ping packet.
 * 
 * <a href="http://xmpp.org/extensions/xep-0199.html">XEP-0199</a>.
 * 
 * @author alexander.ivanov
 * 
 */
public class Ping extends IQ {
	public Ping() {
	}

	public Ping(String to) {
		setTo(to);
	}

	@Override
	public String getChildElementXML() {
		return "<ping xmlns=\"urn:xmpp:ping\"/>";
	}
}
