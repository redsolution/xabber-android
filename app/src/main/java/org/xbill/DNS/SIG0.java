// Copyright (c) 2001-2004 Brian Wellington (bwelling@xbill.org)

package org.xbill.DNS;

import java.security.PrivateKey;
import java.util.Date;

/**
 * Creates SIG(0) transaction signatures.
 *
 * @author Pasi Eronen
 * @author Brian Wellington
 */

public class SIG0 {

/**
 * The default validity period for outgoing SIG(0) signed messages.
 * Can be overriden by the sig0validity option.
 */
private static final short VALIDITY = 300;
    
private
SIG0() { }

/**
 * Sign a message with SIG(0). The DNS key and private key must refer to the
 * same underlying cryptographic key.
 * @param message The message to be signed
 * @param key The DNSKEY record to use as part of signing
 * @param privkey The PrivateKey to use when signing
 * @param previous If this message is a response, the SIG(0) from the query
 */
public static void
signMessage(Message message, KEYRecord key, PrivateKey privkey,
	    SIGRecord previous) throws DNSSEC.DNSSECException
{
	
	int validity = Options.intValue("sig0validity");
	if (validity < 0)
		validity = VALIDITY;

	long now = System.currentTimeMillis();
	Date timeSigned = new Date(now);
	Date timeExpires = new Date(now + validity * 1000);

	SIGRecord sig =  DNSSEC.signMessage(message, previous, key, privkey,
					    timeSigned, timeExpires);
	
	message.addRecord(sig, Section.ADDITIONAL);
}

/**
 * Verify a message using SIG(0).
 * @param message The message to be signed
 * @param b An array containing the message in unparsed form.  This is
 * necessary since SIG(0) signs the message in wire format, and we can't
 * recreate the exact wire format (with the same name compression).
 * @param key The KEY record to verify the signature with.
 * @param previous If this message is a response, the SIG(0) from the query
 */
public static void
verifyMessage(Message message, byte [] b, KEYRecord key, SIGRecord previous)
	throws DNSSEC.DNSSECException
{
	SIGRecord sig = null;
	Record [] additional = message.getSectionArray(Section.ADDITIONAL);
	for (int i = 0; i < additional.length; i++) {
		if (additional[i].getType() != Type.SIG)
			continue;
		if (((SIGRecord) additional[i]).getTypeCovered() != 0)
			continue;
		sig = (SIGRecord) additional[i];
		break;
	}
	DNSSEC.verifyMessage(message, b, sig, previous, key);
}

}
