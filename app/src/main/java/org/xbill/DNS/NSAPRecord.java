// Copyright (c) 2004 Brian Wellington (bwelling@xbill.org)

package org.xbill.DNS;

import java.io.*;
import org.xbill.DNS.utils.*;

/**
 * NSAP Address Record.
 *
 * @author Brian Wellington
 */

public class NSAPRecord extends Record {

private static final long serialVersionUID = -1037209403185658593L;

private byte [] address;

NSAPRecord() {}

Record
getObject() {
	return new NSAPRecord();
}

private static final byte []
checkAndConvertAddress(String address) {
	if (!address.substring(0, 2).equalsIgnoreCase("0x")) {
		return null;
	}
	ByteArrayOutputStream bytes = new ByteArrayOutputStream();
	boolean partial = false;
	int current = 0;
	for (int i = 2; i < address.length(); i++) {
		char c = address.charAt(i);
		if (c == '.') {
			continue;
		}
		int value = Character.digit(c, 16);
		if (value == -1) {
			return null;
		}
		if (partial) {
			current += value;
			bytes.write(current);
			partial = false;
		} else {
			current = value << 4;
			partial = true;
		}

	}
	if (partial) {
		return null;
	}
	return bytes.toByteArray();
}

/**
 * Creates an NSAP Record from the given data
 * @param address The NSAP address.
 * @throws IllegalArgumentException The address is not a valid NSAP address.
 */
public
NSAPRecord(Name name, int dclass, long ttl, String address) {
	super(name, Type.NSAP, dclass, ttl);
	this.address = checkAndConvertAddress(address);
	if (this.address == null) {
		throw new IllegalArgumentException("invalid NSAP address " +
						   address);
	}
}

void
rrFromWire(DNSInput in) throws IOException {
	address = in.readByteArray();
}

void
rdataFromString(Tokenizer st, Name origin) throws IOException {
	String addr = st.getString();
	this.address = checkAndConvertAddress(addr);
	if (this.address == null)
		throw st.exception("invalid NSAP address " + addr);
}

/**
 * Returns the NSAP address.
 */
public String
getAddress() {
	return byteArrayToString(address, false);
}

void
rrToWire(DNSOutput out, Compression c, boolean canonical) {
	out.writeByteArray(address);
}

String
rrToString() {
	return "0x" + base16.toString(address);
}

}
