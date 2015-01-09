// Copyright (c) 1999-2004 Brian Wellington (bwelling@xbill.org)

package org.xbill.DNS;

import java.io.*;

/**
 * Start of Authority - describes properties of a zone.
 *
 * @author Brian Wellington
 */

public class SOARecord extends Record {

private static final long serialVersionUID = 1049740098229303931L;

private Name host, admin;
private long serial, refresh, retry, expire, minimum;

SOARecord() {}

Record
getObject() {
	return new SOARecord();
}

/**
 * Creates an SOA Record from the given data
 * @param host The primary name server for the zone
 * @param admin The zone administrator's address
 * @param serial The zone's serial number
 * @param refresh The amount of time until a secondary checks for a new serial
 * number
 * @param retry The amount of time between a secondary's checks for a new
 * serial number
 * @param expire The amount of time until a secondary expires a zone
 * @param minimum The minimum TTL for records in the zone
*/
public
SOARecord(Name name, int dclass, long ttl, Name host, Name admin,
	  long serial, long refresh, long retry, long expire, long minimum)
{
	super(name, Type.SOA, dclass, ttl);
	this.host = checkName("host", host);
	this.admin = checkName("admin", admin);
	this.serial = checkU32("serial", serial);
	this.refresh = checkU32("refresh", refresh);
	this.retry = checkU32("retry", retry);
	this.expire = checkU32("expire", expire);
	this.minimum = checkU32("minimum", minimum);
}

void
rrFromWire(DNSInput in) throws IOException {
	host = new Name(in);
	admin = new Name(in);
	serial = in.readU32();
	refresh = in.readU32();
	retry = in.readU32();
	expire = in.readU32();
	minimum = in.readU32();
}

void
rdataFromString(Tokenizer st, Name origin) throws IOException {
	host = st.getName(origin);
	admin = st.getName(origin);
	serial = st.getUInt32();
	refresh = st.getTTLLike();
	retry = st.getTTLLike();
	expire = st.getTTLLike();
	minimum = st.getTTLLike();
}

/** Convert to a String */
String
rrToString() {
	StringBuffer sb = new StringBuffer();
	sb.append(host);
	sb.append(" ");
	sb.append(admin);
	if (Options.check("multiline")) {
		sb.append(" (\n\t\t\t\t\t");
		sb.append(serial);
		sb.append("\t; serial\n\t\t\t\t\t");
		sb.append(refresh);
		sb.append("\t; refresh\n\t\t\t\t\t");
		sb.append(retry);
		sb.append("\t; retry\n\t\t\t\t\t");
		sb.append(expire);
		sb.append("\t; expire\n\t\t\t\t\t");
		sb.append(minimum);
		sb.append(" )\t; minimum");
	} else {
		sb.append(" ");
		sb.append(serial);
		sb.append(" ");
		sb.append(refresh);
		sb.append(" ");
		sb.append(retry);
		sb.append(" ");
		sb.append(expire);
		sb.append(" ");
		sb.append(minimum);
	}
	return sb.toString();
}

/** Returns the primary name server */
public Name
getHost() {  
	return host;
}       

/** Returns the zone administrator's address */
public Name
getAdmin() {  
	return admin;
}       

/** Returns the zone's serial number */
public long
getSerial() {  
	return serial;
}       

/** Returns the zone refresh interval */
public long
getRefresh() {  
	return refresh;
}       

/** Returns the zone retry interval */
public long
getRetry() {  
	return retry;
}       

/** Returns the time until a secondary expires a zone */
public long
getExpire() {  
	return expire;
}       

/** Returns the minimum TTL for records in the zone */
public long
getMinimum() {  
	return minimum;
}       

void
rrToWire(DNSOutput out, Compression c, boolean canonical) {
	host.toWire(out, c, canonical);
	admin.toWire(out, c, canonical);
	out.writeU32(serial);
	out.writeU32(refresh);
	out.writeU32(retry);
	out.writeU32(expire);
	out.writeU32(minimum);
}

}
