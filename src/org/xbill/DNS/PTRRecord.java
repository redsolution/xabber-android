// Copyright (c) 1999-2004 Brian Wellington (bwelling@xbill.org)

package org.xbill.DNS;

/**
 * Pointer Record  - maps a domain name representing an Internet Address to
 * a hostname.
 *
 * @author Brian Wellington
 */

public class PTRRecord extends SingleCompressedNameBase {

private static final long serialVersionUID = -8321636610425434192L;

PTRRecord() {}

Record
getObject() {
	return new PTRRecord();
}

/** 
 * Creates a new PTR Record with the given data
 * @param target The name of the machine with this address
 */
public
PTRRecord(Name name, int dclass, long ttl, Name target) {
	super(name, Type.PTR, dclass, ttl, target, "target");
}

/** Gets the target of the PTR Record */
public Name
getTarget() {
	return getSingleName();
}

}
