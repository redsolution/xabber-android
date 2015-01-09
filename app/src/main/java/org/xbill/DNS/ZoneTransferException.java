// Copyright (c) 2003-2004 Brian Wellington (bwelling@xbill.org)

package org.xbill.DNS;

/**
 * An exception thrown when a zone transfer fails.
 *
 * @author Brian Wellington
 */

public class ZoneTransferException extends Exception {

public
ZoneTransferException() {
	super();
}

public
ZoneTransferException(String s) {
	super(s);
}

}
