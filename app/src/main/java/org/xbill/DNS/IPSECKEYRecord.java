// Copyright (c) 2004 Brian Wellington (bwelling@xbill.org)

package org.xbill.DNS;

import java.io.*;
import java.net.*;
import org.xbill.DNS.utils.*;

/**
 * IPsec Keying Material (RFC 4025)
 *
 * @author Brian Wellington
 */

public class IPSECKEYRecord extends Record {

private static final long serialVersionUID = 3050449702765909687L;

public static class Algorithm {
	private Algorithm() {}

	public static final int DSA = 1;
	public static final int RSA = 2;
}

public static class Gateway {
	private Gateway() {}

	public static final int None = 0;
	public static final int IPv4 = 1;
	public static final int IPv6 = 2;
	public static final int Name = 3;
}

private int precedence;
private int gatewayType;
private int algorithmType;
private Object gateway;
private byte [] key;

IPSECKEYRecord() {} 

Record
getObject() {
	return new IPSECKEYRecord();
}

/**
 * Creates an IPSECKEY Record from the given data.
 * @param precedence The record's precedence.
 * @param gatewayType The record's gateway type.
 * @param algorithmType The record's algorithm type.
 * @param gateway The record's gateway.
 * @param key The record's public key.
 */
public
IPSECKEYRecord(Name name, int dclass, long ttl, int precedence,
	       int gatewayType, int algorithmType, Object gateway,
	       byte [] key)
{
	super(name, Type.IPSECKEY, dclass, ttl);
	this.precedence = checkU8("precedence", precedence);
	this.gatewayType = checkU8("gatewayType", gatewayType);
	this.algorithmType = checkU8("algorithmType", algorithmType);
	switch (gatewayType) {
	case Gateway.None:
		this.gateway = null;
		break;
	case Gateway.IPv4:
		if (!(gateway instanceof InetAddress))
			throw new IllegalArgumentException("\"gateway\" " +
							   "must be an IPv4 " +
							   "address");
		this.gateway = gateway;
		break;
	case Gateway.IPv6:
		if (!(gateway instanceof Inet6Address))
			throw new IllegalArgumentException("\"gateway\" " +
							   "must be an IPv6 " +
							   "address");
		this.gateway = gateway;
		break;
	case Gateway.Name:
		if (!(gateway instanceof Name))
			throw new IllegalArgumentException("\"gateway\" " +
							   "must be a DNS " +
							   "name");
		this.gateway = checkName("gateway", (Name) gateway);
		break;
	default:
		throw new IllegalArgumentException("\"gatewayType\" " +
						   "must be between 0 and 3");
	}

	this.key = key;
}

void
rrFromWire(DNSInput in) throws IOException {
	precedence = in.readU8();
	gatewayType = in.readU8();
	algorithmType = in.readU8();
	switch (gatewayType) {
	case Gateway.None:
		gateway = null;
		break;
	case Gateway.IPv4:
		gateway = InetAddress.getByAddress(in.readByteArray(4));
		break;
	case Gateway.IPv6:
		gateway = InetAddress.getByAddress(in.readByteArray(16));
		break;
	case Gateway.Name:
		gateway = new Name(in);
		break;
	default:
		throw new WireParseException("invalid gateway type");
	}
	if (in.remaining() > 0)
		key = in.readByteArray();
}

void
rdataFromString(Tokenizer st, Name origin) throws IOException {
	precedence = st.getUInt8();
	gatewayType = st.getUInt8();
	algorithmType = st.getUInt8();
	switch (gatewayType) {
	case Gateway.None:
		String s = st.getString();
		if (!s.equals("."))
			throw new TextParseException("invalid gateway format");
		gateway = null;
		break;
	case Gateway.IPv4:
		gateway = st.getAddress(Address.IPv4);
		break;
	case Gateway.IPv6:
		gateway = st.getAddress(Address.IPv6);
		break;
	case Gateway.Name:
		gateway = st.getName(origin);
		break;
	default:
		throw new WireParseException("invalid gateway type");
	}
	key = st.getBase64(false);
}

String
rrToString() {
	StringBuffer sb = new StringBuffer();
	sb.append(precedence);
	sb.append(" ");
	sb.append(gatewayType);
	sb.append(" ");
	sb.append(algorithmType);
	sb.append(" ");
	switch (gatewayType) {
	case Gateway.None:
		sb.append(".");
		break;
	case Gateway.IPv4:
	case Gateway.IPv6:
		InetAddress gatewayAddr = (InetAddress) gateway;
		sb.append(gatewayAddr.getHostAddress());
		break;
	case Gateway.Name:
		sb.append(gateway);
		break;
	}
	if (key != null) {
		sb.append(" ");
		sb.append(base64.toString(key));
	}
	return sb.toString();
}

/** Returns the record's precedence. */
public int
getPrecedence() {
	return precedence;
}

/** Returns the record's gateway type. */
public int
getGatewayType() {
	return gatewayType;
}

/** Returns the record's algorithm type. */
public int
getAlgorithmType() {
	return algorithmType;
}

/** Returns the record's gateway. */
public Object
getGateway() {
	return gateway;
}

/** Returns the record's public key */
public byte []
getKey() {
	return key;
}

void
rrToWire(DNSOutput out, Compression c, boolean canonical) {
	out.writeU8(precedence);
	out.writeU8(gatewayType);
	out.writeU8(algorithmType);
	switch (gatewayType) {
	case Gateway.None:
		break;
	case Gateway.IPv4:
	case Gateway.IPv6:
		InetAddress gatewayAddr = (InetAddress) gateway;
		out.writeByteArray(gatewayAddr.getAddress());
		break;
	case Gateway.Name:
		Name gatewayName = (Name) gateway;
		gatewayName.toWire(out, null, canonical);
		break;
	}
	if (key != null)
		out.writeByteArray(key);
}

}
