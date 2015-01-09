// Copyright (c) 1999-2004 Brian Wellington (bwelling@xbill.org)

package org.xbill.DNS;

/**
 * Mail Group Record  - specifies a mailbox which is a member of a mail group.
 *
 * @author Brian Wellington
 */

public class MGRecord extends SingleNameBase {

private static final long serialVersionUID = -3980055550863644582L;

MGRecord() {}

Record
getObject() {
	return new MGRecord();
}

/** 
 * Creates a new MG Record with the given data
 * @param mailbox The mailbox that is a member of the group specified by the
 * domain.
 */
public
MGRecord(Name name, int dclass, long ttl, Name mailbox) {
	super(name, Type.MG, dclass, ttl, mailbox, "mailbox");
}

/** Gets the mailbox in the mail group specified by the domain */
public Name
getMailbox() {
	return getSingleName();
}

}
