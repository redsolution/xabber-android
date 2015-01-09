// Copyright (c) 1999-2004 Brian Wellington (bwelling@xbill.org)

package org.xbill.DNS;

import java.io.*;
import java.security.PublicKey;

import org.xbill.DNS.utils.*;

/**
 * The base class for KEY/DNSKEY records, which have identical formats 
 *
 * @author Brian Wellington
 */

abstract class KEYBase extends Record {

private static final long serialVersionUID = 3469321722693285454L;

protected int flags, proto, alg;
protected byte [] key;
protected int footprint = -1;
protected PublicKey publicKey = null;

protected
KEYBase() {}

public
KEYBase(Name name, int type, int dclass, long ttl, int flags, int proto,
	int alg, byte [] key)
{
	super(name, type, dclass, ttl);
	this.flags = checkU16("flags", flags);
	this.proto = checkU8("proto", proto);
	this.alg = checkU8("alg", alg);
	this.key = key;
}

void
rrFromWire(DNSInput in) throws IOException {
	flags = in.readU16();
	proto = in.readU8();
	alg = in.readU8();
	if (in.remaining() > 0)
		key = in.readByteArray();
}

/** Converts the DNSKEY/KEY Record to a String */
String
rrToString() {
	StringBuffer sb = new StringBuffer();
	sb.append(flags);
	sb.append(" ");
	sb.append(proto);
	sb.append(" ");
	sb.append(alg);
	if (key != null) {
		if (Options.check("multiline")) {
			sb.append(" (\n");
			sb.append(base64.formatString(key, 64, "\t", true));
			sb.append(" ; key_tag = ");
			sb.append(getFootprint());
		} else {
			sb.append(" ");
			sb.append(base64.toString(key));
		}
	}
	return sb.toString();
}

/**
 * Returns the flags describing the key's properties
 */
public int
getFlags() {
	return flags;
}

/**
 * Returns the protocol that the key was created for
 */
public int
getProtocol() {
	return proto;
}

/**
 * Returns the key's algorithm
 */
public int
getAlgorithm() {
	return alg;
}

/**
 * Returns the binary data representing the key
 */
public byte []
getKey() {
	return key;
}

/**
 * Returns the key's footprint (after computing it)
 */
public int
getFootprint() {
	if (footprint >= 0)
		return footprint;

	int foot = 0;

	DNSOutput out = new DNSOutput();
	rrToWire(out, null, false);
	byte [] rdata = out.toByteArray();

	if (alg == DNSSEC.Algorithm.RSAMD5) {
		int d1 = rdata[rdata.length - 3] & 0xFF;
		int d2 = rdata[rdata.length - 2] & 0xFF;
		foot = (d1 << 8) + d2;
	}
	else {
		int i; 
		for (i = 0; i < rdata.length - 1; i += 2) {
			int d1 = rdata[i] & 0xFF;
			int d2 = rdata[i + 1] & 0xFF;
			foot += ((d1 << 8) + d2);
		}
		if (i < rdata.length) {
			int d1 = rdata[i] & 0xFF;
			foot += (d1 << 8);
		}
		foot += ((foot >> 16) & 0xFFFF);
	}
	footprint = (foot & 0xFFFF);
	return footprint;
}

/**
 * Returns a PublicKey corresponding to the data in this key.
 * @throws DNSSEC.DNSSECException The key could not be converted.
 */
public PublicKey
getPublicKey() throws DNSSEC.DNSSECException {
	if (publicKey != null)
		return publicKey;

	publicKey = DNSSEC.toPublicKey(this);
	return publicKey;
}

void
rrToWire(DNSOutput out, Compression c, boolean canonical) {
	out.writeU16(flags);
	out.writeU8(proto);
	out.writeU8(alg);
	if (key != null)
		out.writeByteArray(key);
}

}
