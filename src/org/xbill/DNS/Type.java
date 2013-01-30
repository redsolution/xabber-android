// Copyright (c) 1999-2004 Brian Wellington (bwelling@xbill.org)

package org.xbill.DNS;

import java.util.HashMap;

/**
 * Constants and functions relating to DNS Types
 *
 * @author Brian Wellington
 */

public final class Type {

/** Address */
public static final int A		= 1;

/** Name server */
public static final int NS		= 2;

/** Mail destination */
public static final int MD		= 3;

/** Mail forwarder */
public static final int MF		= 4;

/** Canonical name (alias) */
public static final int CNAME		= 5;

/** Start of authority */
public static final int SOA		= 6;

/** Mailbox domain name */
public static final int MB		= 7;

/** Mail group member */
public static final int MG		= 8;

/** Mail rename name */
public static final int MR		= 9;

/** Null record */
public static final int NULL		= 10;

/** Well known services */
public static final int WKS		= 11;

/** Domain name pointer */
public static final int PTR		= 12;

/** Host information */
public static final int HINFO		= 13;

/** Mailbox information */
public static final int MINFO		= 14;

/** Mail routing information */
public static final int MX		= 15;

/** Text strings */
public static final int TXT		= 16;

/** Responsible person */
public static final int RP		= 17;

/** AFS cell database */
public static final int AFSDB		= 18;

/** X.25 calling address */
public static final int X25		= 19;

/** ISDN calling address */
public static final int ISDN		= 20;

/** Router */
public static final int RT		= 21;

/** NSAP address */
public static final int NSAP		= 22;

/** Reverse NSAP address (deprecated) */
public static final int NSAP_PTR	= 23;

/** Signature */
public static final int SIG		= 24;

/** Key */
public static final int KEY		= 25;

/** X.400 mail mapping */
public static final int PX		= 26;

/** Geographical position (withdrawn) */
public static final int GPOS		= 27;

/** IPv6 address */
public static final int AAAA		= 28;

/** Location */
public static final int LOC		= 29;

/** Next valid name in zone */
public static final int NXT		= 30;

/** Endpoint identifier */
public static final int EID		= 31;

/** Nimrod locator */
public static final int NIMLOC		= 32;

/** Server selection */
public static final int SRV		= 33;

/** ATM address */
public static final int ATMA		= 34;

/** Naming authority pointer */
public static final int NAPTR		= 35;

/** Key exchange */
public static final int KX		= 36;

/** Certificate */
public static final int CERT		= 37;

/** IPv6 address (experimental) */
public static final int A6		= 38;

/** Non-terminal name redirection */
public static final int DNAME		= 39;

/** Options - contains EDNS metadata */
public static final int OPT		= 41;

/** Address Prefix List */
public static final int APL		= 42;

/** Delegation Signer */
public static final int DS		= 43;

/** SSH Key Fingerprint */
public static final int SSHFP		= 44;

/** IPSEC key */
public static final int IPSECKEY	= 45;

/** Resource Record Signature */
public static final int RRSIG		= 46;

/** Next Secure Name */
public static final int NSEC		= 47;

/** DNSSEC Key */
public static final int DNSKEY		= 48;

/** Dynamic Host Configuration Protocol (DHCP) ID */
public static final int DHCID		= 49;

/** Next SECure, 3rd edition, RFC 5155 */
public static final int NSEC3		= 50;

public static final int NSEC3PARAM	= 51;

/** Sender Policy Framework (experimental) */
public static final int SPF		= 99;

/** Transaction key - used to compute a shared secret or exchange a key */
public static final int TKEY		= 249;

/** Transaction signature */
public static final int TSIG		= 250;

/** Incremental zone transfer */
public static final int IXFR		= 251;

/** Zone transfer */
public static final int AXFR		= 252;

/** Transfer mailbox records */
public static final int MAILB		= 253;

/** Transfer mail agent records */
public static final int MAILA		= 254;

/** Matches any type */
public static final int ANY		= 255;

/** DNSSEC Lookaside Validation, RFC 4431 . */
public static final int DLV		= 32769;


private static class TypeMnemonic extends Mnemonic {
	private HashMap objects;

	public
	TypeMnemonic() {
		super("Type", CASE_UPPER);
		setPrefix("TYPE");
		objects = new HashMap();
	}

	public void
	add(int val, String str, Record proto) {
		super.add(val, str);
		objects.put(Mnemonic.toInteger(val), proto);
	}
	
	public void
	check(int val) {
		Type.check(val);
	}

	public Record
	getProto(int val) {
		check(val);
		return (Record) objects.get(toInteger(val));
	}
}

private static TypeMnemonic types = new TypeMnemonic();

static {
	types.add(A, "A", new ARecord());
	types.add(NS, "NS", new NSRecord());
	types.add(MD, "MD", new MDRecord());
	types.add(MF, "MF", new MFRecord());
	types.add(CNAME, "CNAME", new CNAMERecord());
	types.add(SOA, "SOA", new SOARecord());
	types.add(MB, "MB", new MBRecord());
	types.add(MG, "MG", new MGRecord());
	types.add(MR, "MR", new MRRecord());
	types.add(NULL, "NULL", new NULLRecord());
	types.add(WKS, "WKS", new WKSRecord());
	types.add(PTR, "PTR", new PTRRecord());
	types.add(HINFO, "HINFO", new HINFORecord());
	types.add(MINFO, "MINFO", new MINFORecord());
	types.add(MX, "MX", new MXRecord());
	types.add(TXT, "TXT", new TXTRecord());
	types.add(RP, "RP", new RPRecord());
	types.add(AFSDB, "AFSDB", new AFSDBRecord());
	types.add(X25, "X25", new X25Record());
	types.add(ISDN, "ISDN", new ISDNRecord());
	types.add(RT, "RT", new RTRecord());
	types.add(NSAP, "NSAP", new NSAPRecord());
	types.add(NSAP_PTR, "NSAP-PTR", new NSAP_PTRRecord());
	types.add(SIG, "SIG", new SIGRecord());
	types.add(KEY, "KEY", new KEYRecord());
	types.add(PX, "PX", new PXRecord());
	types.add(GPOS, "GPOS", new GPOSRecord());
	types.add(AAAA, "AAAA", new AAAARecord());
	types.add(LOC, "LOC", new LOCRecord());
	types.add(NXT, "NXT", new NXTRecord());
	types.add(EID, "EID");
	types.add(NIMLOC, "NIMLOC");
	types.add(SRV, "SRV", new SRVRecord());
	types.add(ATMA, "ATMA");
	types.add(NAPTR, "NAPTR", new NAPTRRecord());
	types.add(KX, "KX", new KXRecord());
	types.add(CERT, "CERT", new CERTRecord());
	types.add(A6, "A6", new A6Record());
	types.add(DNAME, "DNAME", new DNAMERecord());
	types.add(OPT, "OPT", new OPTRecord());
	types.add(APL, "APL", new APLRecord());
	types.add(DS, "DS", new DSRecord());
	types.add(SSHFP, "SSHFP", new SSHFPRecord());
	types.add(IPSECKEY, "IPSECKEY", new IPSECKEYRecord());
	types.add(RRSIG, "RRSIG", new RRSIGRecord());
	types.add(NSEC, "NSEC", new NSECRecord());
	types.add(DNSKEY, "DNSKEY", new DNSKEYRecord());
	types.add(DHCID, "DHCID", new DHCIDRecord());
	types.add(NSEC3, "NSEC3", new NSEC3Record());
	types.add(NSEC3PARAM, "NSEC3PARAM", new NSEC3PARAMRecord());
	types.add(SPF, "SPF", new SPFRecord());
	types.add(TKEY, "TKEY", new TKEYRecord());
	types.add(TSIG, "TSIG", new TSIGRecord());
	types.add(IXFR, "IXFR");
	types.add(AXFR, "AXFR");
	types.add(MAILB, "MAILB");
	types.add(MAILA, "MAILA");
	types.add(ANY, "ANY");
	types.add(DLV, "DLV", new DLVRecord());
}

private
Type() {
}

/**
 * Checks that a numeric Type is valid.
 * @throws InvalidTypeException The type is out of range.
 */
public static void
check(int val) {
	if (val < 0 || val > 0xFFFF)
		throw new InvalidTypeException(val);
}

/**
 * Converts a numeric Type into a String
 * @param val The type value.
 * @return The canonical string representation of the type
 * @throws InvalidTypeException The type is out of range.
 */
public static String
string(int val) {
	return types.getText(val);
}

/**
 * Converts a String representation of an Type into its numeric value.
 * @param s The string representation of the type
 * @param numberok Whether a number will be accepted or not.
 * @return The type code, or -1 on error.
 */
public static int
value(String s, boolean numberok) {
	int val = types.getValue(s);
	if (val == -1 && numberok) {
		val = types.getValue("TYPE" + s);
	}
	return val;
}

/**
 * Converts a String representation of an Type into its numeric value
 * @return The type code, or -1 on error.
 */
public static int
value(String s) {
	return value(s, false);
}

static Record
getProto(int val) {
	return types.getProto(val);
}

/** Is this type valid for a record (a non-meta type)? */
public static boolean
isRR(int type) {
	switch (type) {
		case OPT:
		case TKEY:
		case TSIG:
		case IXFR:
		case AXFR:
		case MAILB:
		case MAILA:
		case ANY:
			return false;
		default:
			return true;
	}
}

}
