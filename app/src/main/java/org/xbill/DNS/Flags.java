// Copyright (c) 1999-2004 Brian Wellington (bwelling@xbill.org)

package org.xbill.DNS;

/**
 * Constants and functions relating to flags in the DNS header.
 *
 * @author Brian Wellington
 */

public final class Flags {

private static Mnemonic flags = new Mnemonic("DNS Header Flag",
					     Mnemonic.CASE_LOWER);

/** query/response */
public static final byte QR		= 0;

/** authoritative answer */
public static final byte AA		= 5;

/** truncated */
public static final byte TC		= 6;

/** recursion desired */
public static final byte RD		= 7;

/** recursion available */
public static final byte RA		= 8;

/** authenticated data */
public static final byte AD		= 10;

/** (security) checking disabled */
public static final byte CD		= 11;

/** dnssec ok (extended) */
public static final int DO		= ExtendedFlags.DO;

static {
	flags.setMaximum(0xF);
	flags.setPrefix("FLAG");
	flags.setNumericAllowed(true);

	flags.add(QR, "qr");
	flags.add(AA, "aa");
	flags.add(TC, "tc");
	flags.add(RD, "rd");
	flags.add(RA, "ra");
	flags.add(AD, "ad");
	flags.add(CD, "cd");
}

private
Flags() {}

/** Converts a numeric Flag into a String */
public static String
string(int i) {
	return flags.getText(i);
}

/** Converts a String representation of an Flag into its numeric value */
public static int
value(String s) {
	return flags.getValue(s);
}

/**
 * Indicates if a bit in the flags field is a flag or not.  If it's part of
 * the rcode or opcode, it's not.
 */
public static boolean
isFlag(int index) {
	flags.check(index);
	if ((index >= 1 && index <= 4) || (index >= 12))
		return false;
	return true;
}

}
