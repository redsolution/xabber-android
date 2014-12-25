// Copyright (c) 1999-2004 Brian Wellington (bwelling@xbill.org)

package org.xbill.DNS;

/**
 * Mail Forwarder Record  - specifies a mail agent which forwards mail
 * for a domain (obsolete)
 *
 * @author Brian Wellington
 */

public class MFRecord extends SingleNameBase {

private static final long serialVersionUID = -6670449036843028169L;

MFRecord() {}

Record
getObject() {
	return new MFRecord();
}

/** 
 * Creates a new MF Record with the given data
 * @param mailAgent The mail agent that forwards mail for the domain.
 */
public
MFRecord(Name name, int dclass, long ttl, Name mailAgent) {
	super(name, Type.MF, dclass, ttl, mailAgent, "mail agent");
}

/** Gets the mail agent for the domain */
public Name
getMailAgent() {
	return getSingleName();
}

public Name
getAdditionalName() {
	return getSingleName();
}

}
