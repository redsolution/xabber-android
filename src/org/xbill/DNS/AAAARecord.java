// Copyright (c) 1999-2004 Brian Wellington (bwelling@xbill.org)

package org.xbill.DNS;

import java.io.*;
import java.net.*;

/**
 * IPv6 Address Record - maps a domain name to an IPv6 address
 *
 * @author Brian Wellington
 */

public class AAAARecord extends Record {

private static final long serialVersionUID = -4588601512069748050L;

private InetAddress address;

AAAARecord() {}

Record
getObject() {
	return new AAAARecord();
}

/**
 * Creates an AAAA Record from the given data
 * @param address The address suffix
 */
public
AAAARecord(Name name, int dclass, long ttl, InetAddress address) {
	super(name, Type.AAAA, dclass, ttl);
	if (Address.familyOf(address) != Address.IPv6)
		throw new IllegalArgumentException("invalid IPv6 address");
	this.address = address;
}

void
rrFromWire(DNSInput in) throws IOException {
	address = InetAddress.getByAddress(in.readByteArray(16));
}

void
rdataFromString(Tokenizer st, Name origin) throws IOException {
	address = st.getAddress(Address.IPv6);
}

/** Converts rdata to a String */
String
rrToString() {
	return address.getHostAddress();
}

/** Returns the address */
public InetAddress
getAddress() {
	return address;
}

void
rrToWire(DNSOutput out, Compression c, boolean canonical) {
	out.writeByteArray(address.getAddress());
}

}
