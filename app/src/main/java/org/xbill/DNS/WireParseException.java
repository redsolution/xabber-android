// Copyright (c) 1999-2004 Brian Wellington (bwelling@xbill.org)

package org.xbill.DNS;

import java.io.*;

/**
 * An exception thrown when a DNS message is invalid.
 *
 * @author Brian Wellington
 */

public class WireParseException extends IOException {

public
WireParseException() {
	super();
}

public
WireParseException(String s) {
	super(s);
}

public
WireParseException(String s, Throwable cause) {
	super(s);
	initCause(cause);
}

}
