// Copyright (c) 2004 Brian Wellington (bwelling@xbill.org)

package org.xbill.DNS;

/**
 * Route Through Record - lists a route preference and intermediate host.
 *
 * @author Brian Wellington
 */

public class RTRecord extends U16NameBase {

private static final long serialVersionUID = -3206215651648278098L;

RTRecord() {}

Record
getObject() {
	return new RTRecord();
}

/**
 * Creates an RT Record from the given data
 * @param preference The preference of the route.  Smaller numbers indicate
 * more preferred routes.
 * @param intermediateHost The domain name of the host to use as a router.
 */
public
RTRecord(Name name, int dclass, long ttl, int preference,
	 Name intermediateHost)
{
	super(name, Type.RT, dclass, ttl, preference, "preference",
	      intermediateHost, "intermediateHost");
}

/** Gets the preference of the route. */
public int
getPreference() {
	return getU16Field();
}

/** Gets the host to use as a router. */
public Name
getIntermediateHost() {
	return getNameField();
}

}
