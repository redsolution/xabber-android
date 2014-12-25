// Copyright (c) 1999-2004 Brian Wellington (bwelling@xbill.org)

package org.xbill.DNS;

/**
 * Mailbox Record  - specifies a host containing a mailbox.
 *
 * @author Brian Wellington
 */

public class MBRecord extends SingleNameBase {

private static final long serialVersionUID = 532349543479150419L;

MBRecord() {}

Record
getObject() {
	return new MBRecord();
}

/** 
 * Creates a new MB Record with the given data
 * @param mailbox The host containing the mailbox for the domain.
 */
public
MBRecord(Name name, int dclass, long ttl, Name mailbox) {
	super(name, Type.MB, dclass, ttl, mailbox, "mailbox");
}

/** Gets the mailbox for the domain */
public Name
getMailbox() {
	return getSingleName();
}

public Name
getAdditionalName() {
	return getSingleName();
}

}
