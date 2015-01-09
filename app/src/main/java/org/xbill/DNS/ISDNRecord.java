// Copyright (c) 2004 Brian Wellington (bwelling@xbill.org)

package org.xbill.DNS;

import java.io.*;

/**
 * ISDN - identifies the ISDN number and subaddress associated with a name.
 *
 * @author Brian Wellington
 */

public class ISDNRecord extends Record {

private static final long serialVersionUID = -8730801385178968798L;

private byte [] address;
private byte [] subAddress;

ISDNRecord() {}

Record
getObject() {
	return new ISDNRecord();
}

/**
 * Creates an ISDN Record from the given data
 * @param address The ISDN number associated with the domain.
 * @param subAddress The subaddress, if any.
 * @throws IllegalArgumentException One of the strings is invalid.
 */
public
ISDNRecord(Name name, int dclass, long ttl, String address, String subAddress) {
	super(name, Type.ISDN, dclass, ttl);
	try {
		this.address = byteArrayFromString(address);
		if (subAddress != null)
			this.subAddress = byteArrayFromString(subAddress);
	}
	catch (TextParseException e) {
		throw new IllegalArgumentException(e.getMessage());
	}
}

void
rrFromWire(DNSInput in) throws IOException {
	address = in.readCountedString();
	if (in.remaining() > 0)
		subAddress = in.readCountedString();
}

void
rdataFromString(Tokenizer st, Name origin) throws IOException {
	try {
		address = byteArrayFromString(st.getString());
		Tokenizer.Token t = st.get();
		if (t.isString()) {
			subAddress = byteArrayFromString(t.value);
		} else {
			st.unget();
		}
	}
	catch (TextParseException e) {
		throw st.exception(e.getMessage());
	}
}

/**
 * Returns the ISDN number associated with the domain.
 */
public String
getAddress() {
	return byteArrayToString(address, false);
}

/**
 * Returns the ISDN subaddress, or null if there is none.
 */
public String
getSubAddress() {
	if (subAddress == null)
		return null;
	return byteArrayToString(subAddress, false);
}

void
rrToWire(DNSOutput out, Compression c, boolean canonical) {
	out.writeCountedString(address);
	if (subAddress != null)
		out.writeCountedString(subAddress);
}

String
rrToString() {
	StringBuffer sb = new StringBuffer();
	sb.append(byteArrayToString(address, true));
	if (subAddress != null) {
		sb.append(" ");
		sb.append(byteArrayToString(subAddress, true));
	}
	return sb.toString();
}

}
