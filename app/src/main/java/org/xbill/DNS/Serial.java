// Copyright (c) 2003-2004 Brian Wellington (bwelling@xbill.org)

package org.xbill.DNS;

/**
 * Helper functions for doing serial arithmetic.  These should be used when
 * setting/checking SOA serial numbers.  SOA serial number arithmetic is
 * defined in RFC 1982.
 *
 * @author Brian Wellington
 */

public final class Serial {

private static final long MAX32 = 0xFFFFFFFFL;

private
Serial() {
}

/**
 * Compares two numbers using serial arithmetic.  The numbers are assumed
 * to be 32 bit unsigned integers stored in longs.
 * @param serial1 The first integer
 * @param serial2 The second integer
 * @return 0 if the 2 numbers are equal, a positive number if serial1 is greater
 * than serial2, and a negative number if serial2 is greater than serial1.
 * @throws IllegalArgumentException serial1 or serial2 is out of range
 */
public static int
compare(long serial1, long serial2) {
	if (serial1 < 0 || serial1 > MAX32)
		throw new IllegalArgumentException(serial1 + " out of range");
	if (serial2 < 0 || serial2 > MAX32)
		throw new IllegalArgumentException(serial2 + " out of range");
	long diff = serial1 - serial2;
	if (diff >= MAX32)
		diff -= (MAX32 + 1);
	else if (diff < -MAX32)
		diff += (MAX32 + 1);
	return (int)diff;
}

/**
 * Increments a serial number.  The number is assumed to be a 32 bit unsigned
 * integer stored in a long.  This basically adds 1 and resets the value to
 * 0 if it is 2^32.
 * @param serial The serial number
 * @return The incremented serial number
 * @throws IllegalArgumentException serial is out of range
 */
public static long
increment(long serial) {
	if (serial < 0 || serial > MAX32)
		throw new IllegalArgumentException(serial + " out of range");
	if (serial == MAX32)
		return 0;
	return serial + 1;
}

}
