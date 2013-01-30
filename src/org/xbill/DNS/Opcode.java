// Copyright (c) 1999-2004 Brian Wellington (bwelling@xbill.org)

package org.xbill.DNS;

/**
 * Constants and functions relating to DNS opcodes
 *
 * @author Brian Wellington
 */

public final class Opcode {

/** A standard query */
public static final int QUERY		= 0;

/** An inverse query (deprecated) */
public static final int IQUERY		= 1;

/** A server status request (not used) */
public static final int STATUS		= 2;

/**
 * A message from a primary to a secondary server to initiate a zone transfer
 */
public static final int NOTIFY		= 4;

/** A dynamic update message */
public static final int UPDATE		= 5;

private static Mnemonic opcodes = new Mnemonic("DNS Opcode",
					       Mnemonic.CASE_UPPER);

static {
	opcodes.setMaximum(0xF);
	opcodes.setPrefix("RESERVED");
	opcodes.setNumericAllowed(true);

	opcodes.add(QUERY, "QUERY");
	opcodes.add(IQUERY, "IQUERY");
	opcodes.add(STATUS, "STATUS");
	opcodes.add(NOTIFY, "NOTIFY");
	opcodes.add(UPDATE, "UPDATE");
}

private
Opcode() {}

/** Converts a numeric Opcode into a String */
public static String
string(int i) {
	return opcodes.getText(i);
}

/** Converts a String representation of an Opcode into its numeric value */
public static int
value(String s) {
	return opcodes.getValue(s);
}

}
