// Copyright (c) 2004 Brian Wellington (bwelling@xbill.org)

package org.xbill.DNS;

import java.io.*;

/**
 * Implements common functionality for the many record types whose format
 * is an unsigned 16 bit integer followed by a name.
 *
 * @author Brian Wellington
 */

abstract class U16NameBase extends Record {

private static final long serialVersionUID = -8315884183112502995L;

protected int u16Field;
protected Name nameField;

protected
U16NameBase() {}

protected
U16NameBase(Name name, int type, int dclass, long ttl) {
	super(name, type, dclass, ttl);
}

protected
U16NameBase(Name name, int type, int dclass, long ttl, int u16Field,
	    String u16Description, Name nameField, String nameDescription)
{
	super(name, type, dclass, ttl);
	this.u16Field = checkU16(u16Description, u16Field);
	this.nameField = checkName(nameDescription, nameField);
}

void
rrFromWire(DNSInput in) throws IOException {
	u16Field = in.readU16();
	nameField = new Name(in);
}

void
rdataFromString(Tokenizer st, Name origin) throws IOException {
	u16Field = st.getUInt16();
	nameField = st.getName(origin);
}

String
rrToString() {
	StringBuffer sb = new StringBuffer();
	sb.append(u16Field);
	sb.append(" ");
	sb.append(nameField);
	return sb.toString();
}

protected int
getU16Field() {
	return u16Field;
}

protected Name
getNameField() {
	return nameField;
}

void
rrToWire(DNSOutput out, Compression c, boolean canonical) {
	out.writeU16(u16Field);
	nameField.toWire(out, null, canonical);
}

}
