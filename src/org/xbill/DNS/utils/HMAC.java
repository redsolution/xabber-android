// Copyright (c) 1999-2004 Brian Wellington (bwelling@xbill.org)

package org.xbill.DNS.utils;

import java.util.Arrays;
import java.security.*;

/**
 * An implementation of the HMAC message authentication code.
 *
 * @author Brian Wellington
 */

public class HMAC {

private MessageDigest digest;
private int blockLength;

private byte [] ipad, opad;

private static final byte IPAD = 0x36;
private static final byte OPAD = 0x5c;

private void
init(byte [] key) {
	int i;

	if (key.length > blockLength) {
		key = digest.digest(key);
		digest.reset();
	}
	ipad = new byte[blockLength];
	opad = new byte[blockLength];
	for (i = 0; i < key.length; i++) {
		ipad[i] = (byte) (key[i] ^ IPAD);
		opad[i] = (byte) (key[i] ^ OPAD);
	}
	for (; i < blockLength; i++) {
		ipad[i] = IPAD;
		opad[i] = OPAD;
	}
	digest.update(ipad);
}

/**
 * Creates a new HMAC instance
 * @param digest The message digest object.
 * @param blockLength The block length of the message digest.
 * @param key The secret key
 */
public
HMAC(MessageDigest digest, int blockLength, byte [] key) {
	digest.reset();
	this.digest = digest;
  	this.blockLength = blockLength;
	init(key);
}

/**
 * Creates a new HMAC instance
 * @param digestName The name of the message digest function.
 * @param blockLength The block length of the message digest.
 * @param key The secret key.
 */
public
HMAC(String digestName, int blockLength, byte [] key) {
	try {
		digest = MessageDigest.getInstance(digestName);
	} catch (NoSuchAlgorithmException e) {
		throw new IllegalArgumentException("unknown digest algorithm "
						   + digestName);
	}
	this.blockLength = blockLength;
	init(key);
}

/**
 * Creates a new HMAC instance
 * @param digest The message digest object.
 * @param key The secret key
 * @deprecated won't work with digests using a padding length other than 64;
 *             use {@code HMAC(MessageDigest digest, int blockLength,
 *             byte [] key)} instead.
 * @see        HMAC#HMAC(MessageDigest digest, int blockLength, byte [] key)
 */
public
HMAC(MessageDigest digest, byte [] key) {
	this(digest, 64, key);
}

/**
 * Creates a new HMAC instance
 * @param digestName The name of the message digest function.
 * @param key The secret key.
 * @deprecated won't work with digests using a padding length other than 64;
 *             use {@code HMAC(String digestName, int blockLength, byte [] key)}
 *             instead
 * @see        HMAC#HMAC(String digestName, int blockLength, byte [] key)
 */
public
HMAC(String digestName, byte [] key) {
	this(digestName, 64, key);
}

/**
 * Adds data to the current hash
 * @param b The data
 * @param offset The index at which to start adding to the hash
 * @param length The number of bytes to hash
 */
public void
update(byte [] b, int offset, int length) {
	digest.update(b, offset, length);
}

/**
 * Adds data to the current hash
 * @param b The data
 */
public void
update(byte [] b) {
	digest.update(b);
}

/**
 * Signs the data (computes the secure hash)
 * @return An array with the signature
 */
public byte []
sign() {
	byte [] output = digest.digest();
	digest.reset();
	digest.update(opad);
	return digest.digest(output);
}

/**
 * Verifies the data (computes the secure hash and compares it to the input)
 * @param signature The signature to compare against
 * @return true if the signature matches, false otherwise
 */
public boolean
verify(byte [] signature) {
	return verify(signature, false);
}

/**
 * Verifies the data (computes the secure hash and compares it to the input)
 * @param signature The signature to compare against
 * @param trucation_ok If true, the signature may be truncated; only the
 * number of bytes in the provided signature are compared.
 * @return true if the signature matches, false otherwise
 */
public boolean
verify(byte [] signature, boolean truncation_ok) {
	byte [] expected = sign();
	if (truncation_ok && signature.length < expected.length) {
		byte [] truncated = new byte[signature.length];
		System.arraycopy(expected, 0, truncated, 0, truncated.length);
		expected = truncated;
	}
	return Arrays.equals(signature, expected);
}

/**
 * Resets the HMAC object for further use
 */
public void
clear() {
	digest.reset();
	digest.update(ipad);
}

/**
 * Returns the length of the digest.
 */
public int
digestLength() {
	return digest.getDigestLength();
}

}
