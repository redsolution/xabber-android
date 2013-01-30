// Copyright (c) 1999-2004 Brian Wellington (bwelling@xbill.org)

package org.xbill.DNS.utils;

import java.io.*;

/**
 * Routines for converting between Strings of base32-encoded data and arrays
 * of binary data.  This currently supports the base32 and base32hex alphabets
 * specified in RFC 4648, sections 6 and 7.
 * 
 * @author Brian Wellington
 */

public class base32 {

public static class Alphabet {
	private Alphabet() {}

	public static final String BASE32 =
		"ABCDEFGHIJKLMNOPQRSTUVWXYZ234567=";
	public static final String BASE32HEX =
		"0123456789ABCDEFGHIJKLMNOPQRSTUV=";
};

private String alphabet;
private boolean padding, lowercase;

/**
 * Creates an object that can be used to do base32 conversions.
 * @param alphabet Which alphabet should be used
 * @param padding Whether padding should be used
 * @param lowercase Whether lowercase characters should be used.
 * default parameters (The standard base32 alphabet, no padding, uppercase)
 */
public
base32(String alphabet, boolean padding, boolean lowercase) {
	this.alphabet = alphabet;
	this.padding = padding;
	this.lowercase = lowercase;
}

static private int
blockLenToPadding(int blocklen) {
	switch (blocklen) {
	case 1:
		return 6;
	case 2:
		return 4;
	case 3:
		return 3;
	case 4:
		return 1;
	case 5:
		return 0;
	default:
		return -1;
	}
}

static private int
paddingToBlockLen(int padlen) {
	switch (padlen) {
	case 6:
		return 1;
	case 4:
		return 2;
	case 3:
		return 3;
	case 1:
		return 4;
	case 0:
		return 5;
	default :
		return -1;
	}
}

/**
 * Convert binary data to a base32-encoded String
 * 
 * @param b An array containing binary data
 * @return A String containing the encoded data
 */
public String
toString(byte [] b) {
	ByteArrayOutputStream os = new ByteArrayOutputStream();

	for (int i = 0; i < (b.length + 4) / 5; i++) {
		short s[] = new short[5];
		int t[] = new int[8];

		int blocklen = 5;
		for (int j = 0; j < 5; j++) {
			if ((i * 5 + j) < b.length)
				s[j] = (short) (b[i * 5 + j] & 0xFF);
			else {
				s[j] = 0;
				blocklen--;
			}
		}
		int padlen = blockLenToPadding(blocklen);

		// convert the 5 byte block into 8 characters (values 0-31).

		// upper 5 bits from first byte
		t[0] = (byte) ((s[0] >> 3) & 0x1F);
		// lower 3 bits from 1st byte, upper 2 bits from 2nd.
		t[1] = (byte) (((s[0] & 0x07) << 2) | ((s[1] >> 6) & 0x03));
		// bits 5-1 from 2nd.
		t[2] = (byte) ((s[1] >> 1) & 0x1F);
		// lower 1 bit from 2nd, upper 4 from 3rd
		t[3] = (byte) (((s[1] & 0x01) << 4) | ((s[2] >> 4) & 0x0F));
		// lower 4 from 3rd, upper 1 from 4th.
		t[4] = (byte) (((s[2] & 0x0F) << 1) | ((s[3] >> 7) & 0x01));
		// bits 6-2 from 4th
		t[5] = (byte) ((s[3] >> 2) & 0x1F);
		// lower 2 from 4th, upper 3 from 5th;
		t[6] = (byte) (((s[3] & 0x03) << 3) | ((s[4] >> 5) & 0x07));
		// lower 5 from 5th;
		t[7] = (byte) (s[4] & 0x1F);

		// write out the actual characters.
		for (int j = 0; j < t.length - padlen; j++) {
			char c = alphabet.charAt(t[j]);
			if (lowercase)
				c = Character.toLowerCase(c);
			os.write(c);
		}

		// write out the padding (if any)
		if (padding) {
			for (int j = t.length - padlen; j < t.length; j++)
				os.write('=');
		}
	}

    return new String(os.toByteArray());
}

/**
 * Convert a base32-encoded String to binary data
 * 
 * @param str A String containing the encoded data
 * @return An array containing the binary data, or null if the string is invalid
 */
public byte[]
fromString(String str) {
	ByteArrayOutputStream bs = new ByteArrayOutputStream();
	byte [] raw = str.getBytes();
	for (int i = 0; i < raw.length; i++)
	{
		char c = (char) raw[i];
		if (!Character.isWhitespace(c)) {
			c = Character.toUpperCase(c);
			bs.write((byte) c);
		}
	}

	if (padding) {
		if (bs.size() % 8 != 0)
			return null;
	} else {
		while (bs.size() % 8 != 0)
			bs.write('=');
	}

	byte [] in = bs.toByteArray();

	bs.reset();
	DataOutputStream ds = new DataOutputStream(bs);

	for (int i = 0; i < in.length / 8; i++) {
		short[] s = new short[8];
		int[] t = new int[5];

		int padlen = 8;
		for (int j = 0; j < 8; j++) {
			char c = (char) in[i * 8 + j];
			if (c == '=')
				break;
			s[j] = (short) alphabet.indexOf(in[i * 8 + j]);
			if (s[j] < 0)
				return null;
			padlen--;
		}
		int blocklen = paddingToBlockLen(padlen);
		if (blocklen < 0)
			return null;

		// all 5 bits of 1st, high 3 (of 5) of 2nd
		t[0] = (s[0] << 3) | s[1] >> 2;
		// lower 2 of 2nd, all 5 of 3rd, high 1 of 4th
		t[1] = ((s[1] & 0x03) << 6) | (s[2] << 1) | (s[3] >> 4);
		// lower 4 of 4th, high 4 of 5th
		t[2] = ((s[3] & 0x0F) << 4) | ((s[4] >> 1) & 0x0F);
		// lower 1 of 5th, all 5 of 6th, high 2 of 7th
		t[3] = (s[4] << 7) | (s[5] << 2) | (s[6] >> 3);
		// lower 3 of 7th, all of 8th
		t[4] = ((s[6] & 0x07) << 5) | s[7];

		try {
			for (int j = 0; j < blocklen; j++)
				ds.writeByte((byte) (t[j] & 0xFF));
		}
		catch (IOException e) {
		}
	}

    return bs.toByteArray();
}

}
