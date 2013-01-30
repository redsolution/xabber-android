// Copyright (c) 1999-2010 Brian Wellington (bwelling@xbill.org)

package org.xbill.DNS;

import java.io.*;
import java.math.*;
import java.security.*;
import java.security.interfaces.*;
import java.security.spec.*;
import java.util.*;

/**
 * Constants and methods relating to DNSSEC.
 *
 * DNSSEC provides authentication for DNS information. 
 * @see RRSIGRecord
 * @see DNSKEYRecord
 * @see RRset
 *
 * @author Brian Wellington
 */

public class DNSSEC {

public static class Algorithm {
	private Algorithm() {}

	/** RSA/MD5 public key (deprecated) */
	public static final int RSAMD5 = 1;

	/** Diffie Hellman key */
	public static final int DH = 2;

	/** DSA public key */
	public static final int DSA = 3;

	/** Elliptic Curve key */
	public static final int ECC = 4;

	/** RSA/SHA1 public key */
	public static final int RSASHA1 = 5;
	
	/** DSA/SHA1, NSEC3-aware public key */
	public static final int DSA_NSEC3_SHA1 = 6;
	
	/** RSA/SHA1, NSEC3-aware public key */
	public static final int RSA_NSEC3_SHA1 = 7;
	
	/** RSA/SHA256 public key */
	public static final int RSASHA256 = 8;
	
	/** RSA/SHA512 public key */
	public static final int RSASHA512 = 10;
	
	/** Indirect keys; the actual key is elsewhere. */
	public static final int INDIRECT = 252;

	/** Private algorithm, specified by domain name */
	public static final int PRIVATEDNS = 253;

	/** Private algorithm, specified by OID */
	public static final int PRIVATEOID = 254;

	private static Mnemonic algs = new Mnemonic("DNSSEC algorithm",
						    Mnemonic.CASE_UPPER);

	static {
		algs.setMaximum(0xFF);
		algs.setNumericAllowed(true);

		algs.add(RSAMD5, "RSAMD5");
		algs.add(DH, "DH");
		algs.add(DSA, "DSA");
		algs.add(ECC, "ECC");
		algs.add(RSASHA1, "RSASHA1");
		algs.add(DSA_NSEC3_SHA1, "DSA-NSEC3-SHA1");
		algs.add(RSA_NSEC3_SHA1, "RSA-NSEC3-SHA1");
		algs.add(RSASHA256, "RSASHA256");
		algs.add(RSASHA512, "RSASHA512");
		algs.add(INDIRECT, "INDIRECT");
		algs.add(PRIVATEDNS, "PRIVATEDNS");
		algs.add(PRIVATEOID, "PRIVATEOID");
	}

	/**
	 * Converts an algorithm into its textual representation
	 */
	public static String
	string(int alg) {
		return algs.getText(alg);
	}

	/**
	 * Converts a textual representation of an algorithm into its numeric
	 * code.  Integers in the range 0..255 are also accepted.
	 * @param s The textual representation of the algorithm
	 * @return The algorithm code, or -1 on error.
	 */
	public static int
	value(String s) {
		return algs.getValue(s);
	}
}

private
DNSSEC() { }

private static void
digestSIG(DNSOutput out, SIGBase sig) {
	out.writeU16(sig.getTypeCovered());
	out.writeU8(sig.getAlgorithm());
	out.writeU8(sig.getLabels());
	out.writeU32(sig.getOrigTTL());
	out.writeU32(sig.getExpire().getTime() / 1000);
	out.writeU32(sig.getTimeSigned().getTime() / 1000);
	out.writeU16(sig.getFootprint());
	sig.getSigner().toWireCanonical(out);
}

/**
 * Creates a byte array containing the concatenation of the fields of the
 * SIG record and the RRsets to be signed/verified.  This does not perform
 * a cryptographic digest.
 * @param rrsig The RRSIG record used to sign/verify the rrset.
 * @param rrset The data to be signed/verified.
 * @return The data to be cryptographically signed or verified.
 */
public static byte []
digestRRset(RRSIGRecord rrsig, RRset rrset) {
	DNSOutput out = new DNSOutput();
	digestSIG(out, rrsig);

	int size = rrset.size();
	Record [] records = new Record[size];

	Iterator it = rrset.rrs();
	Name name = rrset.getName();
	Name wild = null;
	int sigLabels = rrsig.getLabels() + 1; // Add the root label back.
	if (name.labels() > sigLabels)
		wild = name.wild(name.labels() - sigLabels);
	while (it.hasNext())
		records[--size] = (Record) it.next();
	Arrays.sort(records);

	DNSOutput header = new DNSOutput();
	if (wild != null)
		wild.toWireCanonical(header);
	else
		name.toWireCanonical(header);
	header.writeU16(rrset.getType());
	header.writeU16(rrset.getDClass());
	header.writeU32(rrsig.getOrigTTL());
	for (int i = 0; i < records.length; i++) {
		out.writeByteArray(header.toByteArray());
		int lengthPosition = out.current();
		out.writeU16(0);
		out.writeByteArray(records[i].rdataToWireCanonical());
		int rrlength = out.current() - lengthPosition - 2;
		out.save();
		out.jump(lengthPosition);
		out.writeU16(rrlength);
		out.restore();
	}
	return out.toByteArray();
}

/**
 * Creates a byte array containing the concatenation of the fields of the
 * SIG(0) record and the message to be signed.  This does not perform
 * a cryptographic digest.
 * @param sig The SIG record used to sign the rrset.
 * @param msg The message to be signed.
 * @param previous If this is a response, the signature from the query.
 * @return The data to be cryptographically signed.
 */
public static byte []
digestMessage(SIGRecord sig, Message msg, byte [] previous) {
	DNSOutput out = new DNSOutput();
	digestSIG(out, sig);

	if (previous != null)
		out.writeByteArray(previous);
	
	msg.toWire(out);
	return out.toByteArray();
}

/**
 * A DNSSEC exception.
 */
public static class DNSSECException extends Exception {
	DNSSECException(String s) {
		super(s);
	}
}

/**
 * An algorithm is unsupported by this DNSSEC implementation.
 */
public static class UnsupportedAlgorithmException extends DNSSECException {
	UnsupportedAlgorithmException(int alg) {
		super("Unsupported algorithm: " + alg);
	}
}

/**
 * The cryptographic data in a DNSSEC key is malformed.
 */
public static class MalformedKeyException extends DNSSECException {
	MalformedKeyException(KEYBase rec) {
		super("Invalid key data: " + rec.rdataToString());
	}
}

/**
 * A DNSSEC verification failed because fields in the DNSKEY and RRSIG records
 * do not match.
 */
public static class KeyMismatchException extends DNSSECException {
	private KEYBase key;
	private SIGBase sig;

	KeyMismatchException(KEYBase key, SIGBase sig) {
		super("key " +
		      key.getName() + "/" +
		      DNSSEC.Algorithm.string(key.getAlgorithm()) + "/" +
		      key.getFootprint() + " " +
		      "does not match signature " +
		      sig.getSigner() + "/" +
		      DNSSEC.Algorithm.string(sig.getAlgorithm()) + "/" +
		      sig.getFootprint());
	}
}

/**
 * A DNSSEC verification failed because the signature has expired.
 */
public static class SignatureExpiredException extends DNSSECException {
	private Date when, now;

	SignatureExpiredException(Date when, Date now) {
		super("signature expired");
		this.when = when;
		this.now = now;
	}

	/**
	 * @return When the signature expired
	 */
	public Date
	getExpiration() {
		return when;
	}

	/**
	 * @return When the verification was attempted
	 */
	public Date
	getVerifyTime() {
		return now;
	}
}

/**
 * A DNSSEC verification failed because the signature has not yet become valid.
 */
public static class SignatureNotYetValidException extends DNSSECException {
	private Date when, now;

	SignatureNotYetValidException(Date when, Date now) {
		super("signature is not yet valid");
		this.when = when;
		this.now = now;
	}

	/**
	 * @return When the signature will become valid
	 */
	public Date
	getExpiration() {
		return when;
	}

	/**
	 * @return When the verification was attempted
	 */
	public Date
	getVerifyTime() {
		return now;
	}
}

/**
 * A DNSSEC verification failed because the cryptographic signature
 * verification failed.
 */
public static class SignatureVerificationException extends DNSSECException {
	SignatureVerificationException() {
		super("signature verification failed");
	}
}

/**
 * The key data provided is inconsistent.
 */
public static class IncompatibleKeyException extends IllegalArgumentException {
	IncompatibleKeyException() {
		super("incompatible keys");
	}
}

private static int
BigIntegerLength(BigInteger i) {
	return (i.bitLength() + 7) / 8;
}

private static BigInteger
readBigInteger(DNSInput in, int len) throws IOException {
	byte [] b = in.readByteArray(len);
	return new BigInteger(1, b);
}

private static BigInteger
readBigInteger(DNSInput in) {
	byte [] b = in.readByteArray();
	return new BigInteger(1, b);
}

private static void
writeBigInteger(DNSOutput out, BigInteger val) {
	byte [] b = val.toByteArray();
	if (b[0] == 0)
		out.writeByteArray(b, 1, b.length - 1);
	else
		out.writeByteArray(b);
}

private static PublicKey
toRSAPublicKey(KEYBase r) throws IOException, GeneralSecurityException {
	DNSInput in = new DNSInput(r.getKey()); 
	int exponentLength = in.readU8();
	if (exponentLength == 0)
		exponentLength = in.readU16();
	BigInteger exponent = readBigInteger(in, exponentLength);
	BigInteger modulus = readBigInteger(in);

	KeyFactory factory = KeyFactory.getInstance("RSA");
	return factory.generatePublic(new RSAPublicKeySpec(modulus, exponent));
}

private static PublicKey
toDSAPublicKey(KEYBase r) throws IOException, GeneralSecurityException,
	MalformedKeyException
{
	DNSInput in = new DNSInput(r.getKey()); 

	int t = in.readU8();
	if (t > 8)
		throw new MalformedKeyException(r);

	BigInteger q = readBigInteger(in, 20);
	BigInteger p = readBigInteger(in, 64 + t*8);
	BigInteger g = readBigInteger(in, 64 + t*8);
	BigInteger y = readBigInteger(in, 64 + t*8);

	KeyFactory factory = KeyFactory.getInstance("DSA");
	return factory.generatePublic(new DSAPublicKeySpec(y, p, q, g));
}

/** Converts a KEY/DNSKEY record into a PublicKey */
static PublicKey
toPublicKey(KEYBase r) throws DNSSECException {
	int alg = r.getAlgorithm();
	try {
		switch (alg) {
		case Algorithm.RSAMD5:
		case Algorithm.RSASHA1:
		case Algorithm.RSA_NSEC3_SHA1:
		case Algorithm.RSASHA256:
		case Algorithm.RSASHA512:
			return toRSAPublicKey(r);
		case Algorithm.DSA:
		case Algorithm.DSA_NSEC3_SHA1:
			return toDSAPublicKey(r);
		default:
			throw new UnsupportedAlgorithmException(alg);
		}
	}
	catch (IOException e) {
		throw new MalformedKeyException(r);
	}
	catch (GeneralSecurityException e) {
		throw new DNSSECException(e.toString());
	}
}

private static byte []
fromRSAPublicKey(RSAPublicKey key) {
	DNSOutput out = new DNSOutput();
	BigInteger exponent = key.getPublicExponent();
	BigInteger modulus = key.getModulus();
	int exponentLength = BigIntegerLength(exponent);

	if (exponentLength < 256)
		out.writeU8(exponentLength);
	else {
		out.writeU8(0);
		out.writeU16(exponentLength);
	}
	writeBigInteger(out, exponent);
	writeBigInteger(out, modulus);

	return out.toByteArray();
}

private static byte []
fromDSAPublicKey(DSAPublicKey key) {
	DNSOutput out = new DNSOutput();
	BigInteger q = key.getParams().getQ();
	BigInteger p = key.getParams().getP();
	BigInteger g = key.getParams().getG();
	BigInteger y = key.getY();
	int t = (p.toByteArray().length - 64) / 8;

	out.writeU8(t);
	writeBigInteger(out, q);
	writeBigInteger(out, p);
	writeBigInteger(out, g);
	writeBigInteger(out, y);

	return out.toByteArray();
}

/** Builds a DNSKEY record from a PublicKey */
static byte []
fromPublicKey(PublicKey key, int alg) throws DNSSECException
{
	byte [] data = null;

	switch (alg) {
	case Algorithm.RSAMD5:
	case Algorithm.RSASHA1:
	case Algorithm.RSA_NSEC3_SHA1:
	case Algorithm.RSASHA256:
	case Algorithm.RSASHA512:
		if (! (key instanceof RSAPublicKey))
			throw new IncompatibleKeyException();
		return fromRSAPublicKey((RSAPublicKey) key);
	case Algorithm.DSA:
	case Algorithm.DSA_NSEC3_SHA1:
		if (! (key instanceof DSAPublicKey))
			throw new IncompatibleKeyException();
		return fromDSAPublicKey((DSAPublicKey) key);
	default:
		throw new UnsupportedAlgorithmException(alg);
	}
}

/**
 * Convert an algorithm number to the corresponding JCA string.
 * @param alg The algorithm number.
 * @throws UnsupportedAlgorithmException The algorithm is unknown.
 */
public static String
algString(int alg) throws UnsupportedAlgorithmException {
	switch (alg) {
	case Algorithm.RSAMD5:
		return "MD5withRSA";
	case Algorithm.DSA:
	case Algorithm.DSA_NSEC3_SHA1:
		return "SHA1withDSA";
	case Algorithm.RSASHA1:
	case Algorithm.RSA_NSEC3_SHA1:
		return "SHA1withRSA";
	case Algorithm.RSASHA256:
		return "SHA256withRSA";
	case Algorithm.RSASHA512:
		return "SHA512withRSA";
	default:
		throw new UnsupportedAlgorithmException(alg);
	}
}

private static final int ASN1_SEQ = 0x30;
private static final int ASN1_INT = 0x2;

private static final int DSA_LEN = 20;

private static byte []
DSASignaturefromDNS(byte [] dns) throws DNSSECException, IOException {
	if (dns.length != 1 + DSA_LEN * 2)
		throw new SignatureVerificationException();

	DNSInput in = new DNSInput(dns);
	DNSOutput out = new DNSOutput();

	int t = in.readU8(); 

	byte [] r = in.readByteArray(DSA_LEN);
	int rlen = DSA_LEN;
	if (r[0] < 0)
		rlen++;

	byte [] s = in.readByteArray(DSA_LEN);
        int slen = DSA_LEN;
        if (s[0] < 0)
                slen++;

	out.writeU8(ASN1_SEQ);
	out.writeU8(rlen + slen + 4);

	out.writeU8(ASN1_INT);
	out.writeU8(rlen);
	if (rlen > DSA_LEN)
		out.writeU8(0);
	out.writeByteArray(r);

	out.writeU8(ASN1_INT);
	out.writeU8(slen);
	if (slen > DSA_LEN)
		out.writeU8(0);
	out.writeByteArray(s);

	return out.toByteArray();
}

private static byte []
DSASignaturetoDNS(byte [] key, int t) throws IOException {
	DNSInput in = new DNSInput(key);
	DNSOutput out = new DNSOutput();

	out.writeU8(t);

	int tmp = in.readU8();
	if (tmp != ASN1_SEQ)
		throw new IOException();
	int seqlen = in.readU8();

	tmp = in.readU8();
	if (tmp != ASN1_INT)
		throw new IOException();
	int rlen = in.readU8();
	if (rlen == DSA_LEN + 1) {
		if (in.readU8() != 0)
			throw new IOException();
	} else if (rlen != DSA_LEN)
		throw new IOException();
	byte [] bytes = in.readByteArray(DSA_LEN);
	out.writeByteArray(bytes);

	tmp = in.readU8();
	if (tmp != ASN1_INT)
		throw new IOException();
	int slen = in.readU8();
	if (slen == DSA_LEN + 1) {
		if (in.readU8() != 0)
			throw new IOException();
	} else if (slen != DSA_LEN)
		throw new IOException();
	bytes = in.readByteArray(DSA_LEN);
	out.writeByteArray(bytes);

	return out.toByteArray();
}

private static void
verify(PublicKey key, int alg, byte [] data, byte [] signature)
throws DNSSECException
{
	if (key instanceof DSAPublicKey) {
		try {
			signature = DSASignaturefromDNS(signature);
		}
		catch (IOException e) {
			throw new IllegalStateException();
		}
	}

	try {
		Signature s = Signature.getInstance(algString(alg));
		s.initVerify(key);
		s.update(data);
		if (!s.verify(signature))
			throw new SignatureVerificationException();
	}
	catch (GeneralSecurityException e) {
		throw new DNSSECException(e.toString());
	}
}

private static boolean
matches(SIGBase sig, KEYBase key)
{
	return (key.getAlgorithm() == sig.getAlgorithm() &&
		key.getFootprint() == sig.getFootprint() &&
		key.getName().equals(sig.getSigner()));
}

/**
 * Verify a DNSSEC signature.
 * @param rrset The data to be verified.
 * @param rrsig The RRSIG record containing the signature.
 * @param key The DNSKEY record to verify the signature with.
 * @throws UnsupportedAlgorithmException The algorithm is unknown
 * @throws MalformedKeyException The key is malformed
 * @throws KeyMismatchException The key and signature do not match
 * @throws SignatureExpiredException The signature has expired
 * @throws SignatureNotYetValidException The signature is not yet valid
 * @throws SignatureVerificationException The signature does not verify.
 * @throws DNSSECException Some other error occurred.
 */
public static void
verify(RRset rrset, RRSIGRecord rrsig, DNSKEYRecord key) throws DNSSECException
{
	if (!matches(rrsig, key))
		throw new KeyMismatchException(key, rrsig);

	Date now = new Date();
	if (now.compareTo(rrsig.getExpire()) > 0)
		throw new SignatureExpiredException(rrsig.getExpire(), now);
	if (now.compareTo(rrsig.getTimeSigned()) < 0)
		throw new SignatureNotYetValidException(rrsig.getTimeSigned(),
							now);

	verify(key.getPublicKey(), rrsig.getAlgorithm(),
	       digestRRset(rrsig, rrset), rrsig.getSignature());
}

private static byte []
sign(PrivateKey privkey, PublicKey pubkey, int alg, byte [] data,
     String provider) throws DNSSECException
{
	byte [] signature;
	try {
		Signature s;
		if (provider != null)
			s = Signature.getInstance(algString(alg), provider);
		else
			s = Signature.getInstance(algString(alg));
		s.initSign(privkey);
		s.update(data);
		signature = s.sign();
	}
	catch (GeneralSecurityException e) {
		throw new DNSSECException(e.toString());
	}

	if (pubkey instanceof DSAPublicKey) {
		try {
			DSAPublicKey dsa = (DSAPublicKey) pubkey;
			BigInteger P = dsa.getParams().getP();
			int t = (BigIntegerLength(P) - 64) / 8;
			signature = DSASignaturetoDNS(signature, t);
		}
		catch (IOException e) {
			throw new IllegalStateException();
		}
	}

	return signature;
}

static void
checkAlgorithm(PrivateKey key, int alg) throws UnsupportedAlgorithmException
{
	switch (alg) {
	case Algorithm.RSAMD5:
	case Algorithm.RSASHA1:
	case Algorithm.RSA_NSEC3_SHA1:
	case Algorithm.RSASHA256:
	case Algorithm.RSASHA512:
		if (! (key instanceof RSAPrivateKey))
			throw new IncompatibleKeyException();
		break;
	case Algorithm.DSA:
	case Algorithm.DSA_NSEC3_SHA1:
		if (! (key instanceof DSAPrivateKey))
			throw new IncompatibleKeyException();
		break;
	default:
		throw new UnsupportedAlgorithmException(alg);
	}
}

/**
 * Generate a DNSSEC signature.  key and privateKey must refer to the
 * same underlying cryptographic key.
 * @param rrset The data to be signed
 * @param key The DNSKEY record to use as part of signing
 * @param privkey The PrivateKey to use when signing
 * @param inception The time at which the signatures should become valid
 * @param expiration The time at which the signatures should expire
 * @throws UnsupportedAlgorithmException The algorithm is unknown
 * @throws MalformedKeyException The key is malformed
 * @throws DNSSECException Some other error occurred.
 * @return The generated signature
 */
public static RRSIGRecord
sign(RRset rrset, DNSKEYRecord key, PrivateKey privkey,
     Date inception, Date expiration) throws DNSSECException
{
	return sign(rrset, key, privkey, inception, expiration, null);
}

/**
 * Generate a DNSSEC signature.  key and privateKey must refer to the
 * same underlying cryptographic key.
 * @param rrset The data to be signed
 * @param key The DNSKEY record to use as part of signing
 * @param privkey The PrivateKey to use when signing
 * @param inception The time at which the signatures should become valid
 * @param expiration The time at which the signatures should expire
 * @param provider The name of the JCA provider.  If non-null, it will be
 * passed to JCA getInstance() methods.
 * @throws UnsupportedAlgorithmException The algorithm is unknown
 * @throws MalformedKeyException The key is malformed
 * @throws DNSSECException Some other error occurred.
 * @return The generated signature
 */
public static RRSIGRecord
sign(RRset rrset, DNSKEYRecord key, PrivateKey privkey,
     Date inception, Date expiration, String provider) throws DNSSECException
{
	int alg = key.getAlgorithm();
	checkAlgorithm(privkey, alg);

	RRSIGRecord rrsig = new RRSIGRecord(rrset.getName(), rrset.getDClass(),
					    rrset.getTTL(), rrset.getType(),
					    alg, rrset.getTTL(),
					    expiration, inception,
					    key.getFootprint(),
					    key.getName(), null);

	rrsig.setSignature(sign(privkey, key.getPublicKey(), alg,
				digestRRset(rrsig, rrset), provider));
	return rrsig;
}

static SIGRecord
signMessage(Message message, SIGRecord previous, KEYRecord key,
	    PrivateKey privkey, Date inception, Date expiration)
	throws DNSSECException
{
	int alg = key.getAlgorithm();
	checkAlgorithm(privkey, alg);

	SIGRecord sig = new SIGRecord(Name.root, DClass.ANY, 0, 0,
					    alg, 0, expiration, inception,
					    key.getFootprint(),
					    key.getName(), null);
	DNSOutput out = new DNSOutput();
	digestSIG(out, sig);
	if (previous != null)
		out.writeByteArray(previous.getSignature());
	message.toWire(out);

	sig.setSignature(sign(privkey, key.getPublicKey(),
			      alg, out.toByteArray(), null));
	return sig;
}

static void
verifyMessage(Message message, byte [] bytes, SIGRecord sig, SIGRecord previous,
	      KEYRecord key) throws DNSSECException
{
	if (!matches(sig, key))
		throw new KeyMismatchException(key, sig);

	Date now = new Date();
	if (now.compareTo(sig.getExpire()) > 0)
		throw new SignatureExpiredException(sig.getExpire(), now);
	if (now.compareTo(sig.getTimeSigned()) < 0)
		throw new SignatureNotYetValidException(sig.getTimeSigned(),
							now);

	DNSOutput out = new DNSOutput();
	digestSIG(out, sig);
	if (previous != null)
		out.writeByteArray(previous.getSignature());

	Header header = (Header) message.getHeader().clone();
	header.decCount(Section.ADDITIONAL);
	out.writeByteArray(header.toWire());

	out.writeByteArray(bytes, Header.LENGTH,
			   message.sig0start - Header.LENGTH);

	verify(key.getPublicKey(), sig.getAlgorithm(),
	       out.toByteArray(), sig.getSignature());
}

static byte []
generateDS(DNSKEYRecord key, int digestid)
{
	DNSOutput out = new DNSOutput();

	out.writeU16(key.getFootprint());
	out.writeU8(key.getAlgorithm());
	out.writeU8(digestid);

	MessageDigest digest;
	try {
		switch (digestid) {
		case DSRecord.Digest.SHA1:
			digest = MessageDigest.getInstance("sha-1");
			break;
		case DSRecord.Digest.SHA256:
			digest = MessageDigest.getInstance("sha-256");
			break;
		default:
			throw new IllegalArgumentException(
					"unknown DS digest type " + digestid);
		}
	}
	catch (NoSuchAlgorithmException e) {
		throw new IllegalStateException("no message digest support");
	}
	digest.update(key.getName().toWire());
	digest.update(key.rdataToWireCanonical());
	out.writeByteArray(digest.digest());

	return out.toByteArray();
}

}
