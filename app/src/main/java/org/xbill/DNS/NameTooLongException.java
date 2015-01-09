// Copyright (c) 2002-2004 Brian Wellington (bwelling@xbill.org)

package org.xbill.DNS;

/**
 * An exception thrown when a name is longer than the maximum length of a DNS
 * name.
 *
 * @author Brian Wellington
 */

public class NameTooLongException extends WireParseException {

public
NameTooLongException() {
	super();
}

public
NameTooLongException(String s) {
	super(s);
}

}
