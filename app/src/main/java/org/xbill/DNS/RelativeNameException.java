// Copyright (c) 2003-2004 Brian Wellington (bwelling@xbill.org)

package org.xbill.DNS;

/**
 * An exception thrown when a relative name is passed as an argument to
 * a method requiring an absolute name.
 *
 * @author Brian Wellington
 */

public class RelativeNameException extends IllegalArgumentException {

    public RelativeNameException(Name name) {
        super("'" + name + "' is not an absolute name");
    }

    public RelativeNameException(String s) {
        super(s);
    }

}
