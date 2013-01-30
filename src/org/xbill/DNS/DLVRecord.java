// Copyright (c) 2002-2004 Brian Wellington (bwelling@xbill.org)

package org.xbill.DNS;

import java.io.*;
import org.xbill.DNS.utils.*;

/**
 * DLV - contains a Delegation Lookaside Validation record, which acts
 * as the equivalent of a DS record in a lookaside zone.
 * @see DNSSEC
 * @see DSRecord
 *
 * @author David Blacka
 * @author Brian Wellington
 */

public class DLVRecord extends Record {

public static final int SHA1_DIGEST_ID = DSRecord.Digest.SHA1;
public static final int SHA256_DIGEST_ID = DSRecord.Digest.SHA1;

private static final long serialVersionUID = 1960742375677534148L;

private int footprint;
private int alg;
private int digestid;
private byte [] digest;

DLVRecord() {}

Record
getObject() {
	return new DLVRecord();
}

/**
 * Creates a DLV Record from the given data
 * @param footprint The original KEY record's footprint (keyid).
 * @param alg The original key algorithm.
 * @param digestid The digest id code.
 * @param digest A hash of the original key.
 */
public
DLVRecord(Name name, int dclass, long ttl, int footprint, int alg,
	  int digestid, byte [] digest)
{
	super(name, Type.DLV, dclass, ttl);
	this.footprint = checkU16("footprint", footprint);
	this.alg = checkU8("alg", alg);
	this.digestid = checkU8("digestid", digestid);
	this.digest = digest;
}

void
rrFromWire(DNSInput in) throws IOException {
	footprint = in.readU16();
	alg = in.readU8();
	digestid = in.readU8();
	digest = in.readByteArray();
}

void
rdataFromString(Tokenizer st, Name origin) throws IOException {
	footprint = st.getUInt16();
	alg = st.getUInt8();
	digestid = st.getUInt8();
	digest = st.getHex();
}

/**
 * Converts rdata to a String
 */
String
rrToString() {
	StringBuffer sb = new StringBuffer();
	sb.append(footprint);
	sb.append(" ");
	sb.append(alg);
	sb.append(" ");
	sb.append(digestid);
	if (digest != null) {
		sb.append(" ");
		sb.append(base16.toString(digest));
	}

	return sb.toString();
}

/**
 * Returns the key's algorithm.
 */
public int
getAlgorithm() {
	return alg;
}

/**
 *  Returns the key's Digest ID.
 */
public int
getDigestID()
{
	return digestid;
}
  
/**
 * Returns the binary hash of the key.
 */
public byte []
getDigest() {
	return digest;
}

/**
 * Returns the key's footprint.
 */
public int
getFootprint() {
	return footprint;
}

void
rrToWire(DNSOutput out, Compression c, boolean canonical) {
	out.writeU16(footprint);
	out.writeU8(alg);
	out.writeU8(digestid);
	if (digest != null)
		out.writeByteArray(digest);
}

}
