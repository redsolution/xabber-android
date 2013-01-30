// Copyright (c) 2004 Brian Wellington (bwelling@xbill.org)

package org.xbill.DNS;

/**
 * Implements common functionality for the many record types whose format
 * is a single compressed name.
 *
 * @author Brian Wellington
 */

abstract class SingleCompressedNameBase extends SingleNameBase {

private static final long serialVersionUID = -236435396815460677L;

protected
SingleCompressedNameBase() {}

protected
SingleCompressedNameBase(Name name, int type, int dclass, long ttl,
			 Name singleName, String description)
{
        super(name, type, dclass, ttl, singleName, description);
}

void
rrToWire(DNSOutput out, Compression c, boolean canonical) {
	singleName.toWire(out, c, canonical);
}

}
