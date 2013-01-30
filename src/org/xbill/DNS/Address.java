// Copyright (c) 1999-2004 Brian Wellington (bwelling@xbill.org)

package org.xbill.DNS;

import java.net.*;
import java.net.Inet6Address;

/**
 * Routines dealing with IP addresses.  Includes functions similar to
 * those in the java.net.InetAddress class.
 *
 * @author Brian Wellington
 */

public final class Address {

public static final int IPv4 = 1;
public static final int IPv6 = 2;

private
Address() {}

private static byte []
parseV4(String s) {
	int numDigits;
	int currentOctet;
	byte [] values = new byte[4];
	int currentValue;
	int length = s.length();

	currentOctet = 0;
	currentValue = 0;
	numDigits = 0;
	for (int i = 0; i < length; i++) {
		char c = s.charAt(i);
		if (c >= '0' && c <= '9') {
			/* Can't have more than 3 digits per octet. */
			if (numDigits == 3)
				return null;
			/* Octets shouldn't start with 0, unless they are 0. */
			if (numDigits > 0 && currentValue == 0)
				return null;
			numDigits++;
			currentValue *= 10;
			currentValue += (c - '0');
			/* 255 is the maximum value for an octet. */
			if (currentValue > 255)
				return null;
		} else if (c == '.') {
			/* Can't have more than 3 dots. */
			if (currentOctet == 3)
				return null;
			/* Two consecutive dots are bad. */
			if (numDigits == 0)
				return null;
			values[currentOctet++] = (byte) currentValue;
			currentValue = 0;
			numDigits = 0;
		} else
			return null;
	}
	/* Must have 4 octets. */
	if (currentOctet != 3)
		return null;
	/* The fourth octet can't be empty. */
	if (numDigits == 0)
		return null;
	values[currentOctet] = (byte) currentValue;
	return values;
}

private static byte []
parseV6(String s) {
	int range = -1;
	byte [] data = new byte[16];

	String [] tokens = s.split(":", -1);

	int first = 0;
	int last = tokens.length - 1;

	if (tokens[0].length() == 0) {
		// If the first two tokens are empty, it means the string
		// started with ::, which is fine.  If only the first is
		// empty, the string started with :, which is bad.
		if (last - first > 0 && tokens[1].length() == 0)
			first++;
		else
			return null;
	}

	if (tokens[last].length() == 0) {
		// If the last two tokens are empty, it means the string
		// ended with ::, which is fine.  If only the last is
		// empty, the string ended with :, which is bad.
		if (last - first > 0 && tokens[last - 1].length() == 0)
			last--;
		else
			return null;
	}

	if (last - first + 1 > 8)
		return null;

	int i, j;
	for (i = first, j = 0; i <= last; i++) {
		if (tokens[i].length() == 0) {
			if (range >= 0)
				return null;
			range = j;
			continue;
		}

		if (tokens[i].indexOf('.') >= 0) {
			// An IPv4 address must be the last component
			if (i < last)
				return null;
			// There can't have been more than 6 components.
			if (i > 6)
				return null;
			byte [] v4addr = Address.toByteArray(tokens[i], IPv4);
			if (v4addr == null)
				return null;
			for (int k = 0; k < 4; k++)
				data[j++] = v4addr[k];
			break;
		}

		try {
			for (int k = 0; k < tokens[i].length(); k++) {
				char c = tokens[i].charAt(k);
				if (Character.digit(c, 16) < 0)
					return null;
			}
			int x = Integer.parseInt(tokens[i], 16);
			if (x > 0xFFFF || x < 0)
				return null;
			data[j++] = (byte)(x >>> 8);
			data[j++] = (byte)(x & 0xFF);
		}
		catch (NumberFormatException e) {
			return null;
		}
	}

	if (j < 16 && range < 0)
		return null;

	if (range >= 0) {
		int empty = 16 - j;
		System.arraycopy(data, range, data, range + empty, j - range);
		for (i = range; i < range + empty; i++)
			data[i] = 0;
	}

	return data;
}

/**
 * Convert a string containing an IP address to an array of 4 or 16 integers.
 * @param s The address, in text format.
 * @param family The address family.
 * @return The address
 */
public static int []
toArray(String s, int family) {
	byte [] byteArray = toByteArray(s, family);
	if (byteArray == null)
		return null;
	int [] intArray = new int[byteArray.length];
	for (int i = 0; i < byteArray.length; i++)
		intArray[i] = byteArray[i] & 0xFF;
	return intArray;
}

/**
 * Convert a string containing an IPv4 address to an array of 4 integers.
 * @param s The address, in text format.
 * @return The address
 */
public static int []
toArray(String s) {
	return toArray(s, IPv4);
}

/**
 * Convert a string containing an IP address to an array of 4 or 16 bytes.
 * @param s The address, in text format.
 * @param family The address family.
 * @return The address
 */
public static byte []
toByteArray(String s, int family) {
	if (family == IPv4)
		return parseV4(s);
	else if (family == IPv6)
		return parseV6(s);
	else
		throw new IllegalArgumentException("unknown address family");
}

/**
 * Determines if a string contains a valid IP address.
 * @param s The string
 * @return Whether the string contains a valid IP address
 */
public static boolean
isDottedQuad(String s) {
	byte [] address = Address.toByteArray(s, IPv4);
	return (address != null);
}

/**
 * Converts a byte array containing an IPv4 address into a dotted quad string.
 * @param addr The array
 * @return The string representation
 */
public static String
toDottedQuad(byte [] addr) {
	return ((addr[0] & 0xFF) + "." + (addr[1] & 0xFF) + "." +
		(addr[2] & 0xFF) + "." + (addr[3] & 0xFF));
}

/**
 * Converts an int array containing an IPv4 address into a dotted quad string.
 * @param addr The array
 * @return The string representation
 */
public static String
toDottedQuad(int [] addr) {
	return (addr[0] + "." + addr[1] + "." + addr[2] + "." + addr[3]);
}

private static Record []
lookupHostName(String name) throws UnknownHostException {
	try {
		Record [] records = new Lookup(name).run();
		if (records == null)
			throw new UnknownHostException("unknown host");
		return records;
	}
	catch (TextParseException e) {
		throw new UnknownHostException("invalid name");
	}
}

private static InetAddress
addrFromRecord(String name, Record r) throws UnknownHostException {
	ARecord a = (ARecord) r;
	return InetAddress.getByAddress(name, a.getAddress().getAddress());
}

/**
 * Determines the IP address of a host
 * @param name The hostname to look up
 * @return The first matching IP address
 * @exception UnknownHostException The hostname does not have any addresses
 */
public static InetAddress
getByName(String name) throws UnknownHostException {
	try {
		return getByAddress(name);
	} catch (UnknownHostException e) {
		Record [] records = lookupHostName(name);
		return addrFromRecord(name, records[0]);
	}
}

/**
 * Determines all IP address of a host
 * @param name The hostname to look up
 * @return All matching IP addresses
 * @exception UnknownHostException The hostname does not have any addresses
 */
public static InetAddress []
getAllByName(String name) throws UnknownHostException {
	try {
		InetAddress addr = getByAddress(name);
		return new InetAddress[] {addr};
	} catch (UnknownHostException e) {
		Record [] records = lookupHostName(name);
		InetAddress [] addrs = new InetAddress[records.length];
		for (int i = 0; i < records.length; i++)
			addrs[i] = addrFromRecord(name, records[i]);
		return addrs;
	}
}

/**
 * Converts an address from its string representation to an IP address.
 * The address can be either IPv4 or IPv6.
 * @param addr The address, in string form
 * @return The IP addresses
 * @exception UnknownHostException The address is not a valid IP address.
 */
public static InetAddress
getByAddress(String addr) throws UnknownHostException {
	byte [] bytes;
	bytes = toByteArray(addr, IPv4);
	if (bytes != null)
		return InetAddress.getByAddress(bytes);
	bytes = toByteArray(addr, IPv6);
	if (bytes != null)
		return InetAddress.getByAddress(bytes);
	throw new UnknownHostException("Invalid address: " + addr);
}

/**
 * Converts an address from its string representation to an IP address in
 * a particular family.
 * @param addr The address, in string form
 * @param family The address family, either IPv4 or IPv6.
 * @return The IP addresses
 * @exception UnknownHostException The address is not a valid IP address in
 * the specified address family.
 */
public static InetAddress
getByAddress(String addr, int family) throws UnknownHostException {
	if (family != IPv4 && family != IPv6)
		throw new IllegalArgumentException("unknown address family");
	byte [] bytes;
	bytes = toByteArray(addr, family);
	if (bytes != null)
		return InetAddress.getByAddress(bytes);
	throw new UnknownHostException("Invalid address: " + addr);
}

/**
 * Determines the hostname for an address
 * @param addr The address to look up
 * @return The associated host name
 * @exception UnknownHostException There is no hostname for the address
 */
public static String
getHostName(InetAddress addr) throws UnknownHostException {
	Name name = ReverseMap.fromAddress(addr);
	Record [] records = new Lookup(name, Type.PTR).run();
	if (records == null)
		throw new UnknownHostException("unknown address");
	PTRRecord ptr = (PTRRecord) records[0];
	return ptr.getTarget().toString();
}

/**
 * Returns the family of an InetAddress.
 * @param address The supplied address.
 * @return The family, either IPv4 or IPv6.
 */
public static int
familyOf(InetAddress address) {
	if (address instanceof Inet4Address)
		return IPv4;
	if (address instanceof Inet6Address)
		return IPv6;
	throw new IllegalArgumentException("unknown address family");
}

/**
 * Returns the length of an address in a particular family.
 * @param family The address family, either IPv4 or IPv6.
 * @return The length of addresses in that family.
 */
public static int
addressLength(int family) {
	if (family == IPv4)
		return 4;
	if (family == IPv6)
		return 16;
	throw new IllegalArgumentException("unknown address family");
}

/**
 * Truncates an address to the specified number of bits.  For example,
 * truncating the address 10.1.2.3 to 8 bits would yield 10.0.0.0.
 * @param address The source address
 * @param maskLength The number of bits to truncate the address to.
 */
public static InetAddress
truncate(InetAddress address, int maskLength)
{
	int family = familyOf(address);
	int maxMaskLength = addressLength(family) * 8;
	if (maskLength < 0 || maskLength > maxMaskLength)
		throw new IllegalArgumentException("invalid mask length");
	if (maskLength == maxMaskLength)
		return address;
	byte [] bytes = address.getAddress();
	for (int i = maskLength / 8 + 1; i < bytes.length; i++)
		bytes[i] = 0;
	int maskBits = maskLength % 8;
	int bitmask = 0;
	for (int i = 0; i < maskBits; i++)
		bitmask |= (1 << (7 - i));
	bytes[maskLength / 8] &= bitmask;
	try {
		return InetAddress.getByAddress(bytes);
	} catch (UnknownHostException e) {
		throw new IllegalArgumentException("invalid address");
	}
}

}
