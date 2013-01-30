// Copyright (c) 2003-2004 Brian Wellington (bwelling@xbill.org)
// Parts of this are derived from lib/dns/xfrin.c from BIND 9; its copyright
// notice follows.

/*
 * Copyright (C) 1999-2001  Internet Software Consortium.
 *
 * Permission to use, copy, modify, and distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND INTERNET SOFTWARE CONSORTIUM
 * DISCLAIMS ALL WARRANTIES WITH REGARD TO THIS SOFTWARE INCLUDING ALL
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL
 * INTERNET SOFTWARE CONSORTIUM BE LIABLE FOR ANY SPECIAL, DIRECT,
 * INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package org.xbill.DNS;

import java.io.*;
import java.net.*;
import java.util.*;

/**
 * An incoming DNS Zone Transfer.  To use this class, first initialize an
 * object, then call the run() method.  If run() doesn't throw an exception
 * the result will either be an IXFR-style response, an AXFR-style response,
 * or an indication that the zone is up to date.
 *
 * @author Brian Wellington
 */

public class ZoneTransferIn {

private static final int INITIALSOA	= 0;
private static final int FIRSTDATA	= 1;
private static final int IXFR_DELSOA	= 2;
private static final int IXFR_DEL	= 3;
private static final int IXFR_ADDSOA	= 4;
private static final int IXFR_ADD	= 5;
private static final int AXFR		= 6;
private static final int END		= 7;

private Name zname;
private int qtype;
private int dclass;
private long ixfr_serial;
private boolean want_fallback;

private SocketAddress localAddress;
private SocketAddress address;
private TCPClient client;
private TSIG tsig;
private TSIG.StreamVerifier verifier;
private long timeout = 900 * 1000;

private int state;
private long end_serial;
private long current_serial;
private Record initialsoa;

private int rtype;

private List axfr;
private List ixfr;

public static class Delta {
	/**
	 * All changes between two versions of a zone in an IXFR response.
	 */

	/** The starting serial number of this delta. */
	public long start;

	/** The ending serial number of this delta. */
	public long end;

	/** A list of records added between the start and end versions */
	public List adds;

	/** A list of records deleted between the start and end versions */
	public List deletes;

	private
	Delta() {
		adds = new ArrayList();
		deletes = new ArrayList();
	}
}

private
ZoneTransferIn() {}

private
ZoneTransferIn(Name zone, int xfrtype, long serial, boolean fallback,
	       SocketAddress address, TSIG key)
{
	this.address = address;
	this.tsig = key;
	if (zone.isAbsolute())
		zname = zone;
	else {
		try {
			zname = Name.concatenate(zone, Name.root);
		}
		catch (NameTooLongException e) {
			throw new IllegalArgumentException("ZoneTransferIn: " +
							   "name too long");
		}
	}
	qtype = xfrtype;
	dclass = DClass.IN;
	ixfr_serial = serial;
	want_fallback = fallback;
	state = INITIALSOA;
}

/**
 * Instantiates a ZoneTransferIn object to do an AXFR (full zone transfer).
 * @param zone The zone to transfer.
 * @param address The host/port from which to transfer the zone.
 * @param key The TSIG key used to authenticate the transfer, or null.
 * @return The ZoneTransferIn object.
 * @throws UnknownHostException The host does not exist.
 */
public static ZoneTransferIn
newAXFR(Name zone, SocketAddress address, TSIG key) {
	return new ZoneTransferIn(zone, Type.AXFR, 0, false, address, key);
}

/**
 * Instantiates a ZoneTransferIn object to do an AXFR (full zone transfer).
 * @param zone The zone to transfer.
 * @param host The host from which to transfer the zone.
 * @param port The port to connect to on the server, or 0 for the default.
 * @param key The TSIG key used to authenticate the transfer, or null.
 * @return The ZoneTransferIn object.
 * @throws UnknownHostException The host does not exist.
 */
public static ZoneTransferIn
newAXFR(Name zone, String host, int port, TSIG key)
throws UnknownHostException
{
	if (port == 0)
		port = SimpleResolver.DEFAULT_PORT;
	return newAXFR(zone, new InetSocketAddress(host, port), key);
}

/**
 * Instantiates a ZoneTransferIn object to do an AXFR (full zone transfer).
 * @param zone The zone to transfer.
 * @param host The host from which to transfer the zone.
 * @param key The TSIG key used to authenticate the transfer, or null.
 * @return The ZoneTransferIn object.
 * @throws UnknownHostException The host does not exist.
 */
public static ZoneTransferIn
newAXFR(Name zone, String host, TSIG key)
throws UnknownHostException
{
	return newAXFR(zone, host, 0, key);
}

/**
 * Instantiates a ZoneTransferIn object to do an IXFR (incremental zone
 * transfer).
 * @param zone The zone to transfer.
 * @param serial The existing serial number.
 * @param fallback If true, fall back to AXFR if IXFR is not supported.
 * @param address The host/port from which to transfer the zone.
 * @param key The TSIG key used to authenticate the transfer, or null.
 * @return The ZoneTransferIn object.
 * @throws UnknownHostException The host does not exist.
 */
public static ZoneTransferIn
newIXFR(Name zone, long serial, boolean fallback, SocketAddress address,
	TSIG key)
{
	return new ZoneTransferIn(zone, Type.IXFR, serial, fallback, address,
				  key);
}

/**
 * Instantiates a ZoneTransferIn object to do an IXFR (incremental zone
 * transfer).
 * @param zone The zone to transfer.
 * @param serial The existing serial number.
 * @param fallback If true, fall back to AXFR if IXFR is not supported.
 * @param host The host from which to transfer the zone.
 * @param port The port to connect to on the server, or 0 for the default.
 * @param key The TSIG key used to authenticate the transfer, or null.
 * @return The ZoneTransferIn object.
 * @throws UnknownHostException The host does not exist.
 */
public static ZoneTransferIn
newIXFR(Name zone, long serial, boolean fallback, String host, int port,
	TSIG key)
throws UnknownHostException
{
	if (port == 0)
		port = SimpleResolver.DEFAULT_PORT;
	return newIXFR(zone, serial, fallback,
		       new InetSocketAddress(host, port), key);
}

/**
 * Instantiates a ZoneTransferIn object to do an IXFR (incremental zone
 * transfer).
 * @param zone The zone to transfer.
 * @param serial The existing serial number.
 * @param fallback If true, fall back to AXFR if IXFR is not supported.
 * @param host The host from which to transfer the zone.
 * @param key The TSIG key used to authenticate the transfer, or null.
 * @return The ZoneTransferIn object.
 * @throws UnknownHostException The host does not exist.
 */
public static ZoneTransferIn
newIXFR(Name zone, long serial, boolean fallback, String host, TSIG key)
throws UnknownHostException
{
	return newIXFR(zone, serial, fallback, host, 0, key);
}

/**
 * Gets the name of the zone being transferred.
 */
public Name
getName() {
	return zname;
}

/**
 * Gets the type of zone transfer (either AXFR or IXFR).
 */
public int
getType() {
	return qtype;
}

/**
 * Sets a timeout on this zone transfer.  The default is 900 seconds (15
 * minutes).
 * @param secs The maximum amount of time that this zone transfer can take.
 */
public void
setTimeout(int secs) {
	if (secs < 0)
		throw new IllegalArgumentException("timeout cannot be " +
						   "negative");
	timeout = 1000L * secs;
}

/**
 * Sets an alternate DNS class for this zone transfer.
 * @param dclass The class to use instead of class IN.
 */
public void
setDClass(int dclass) {
	DClass.check(dclass);
	this.dclass = dclass;
}

/**
 * Sets the local address to bind to when sending messages.
 * @param addr The local address to send messages from.
 */
public void
setLocalAddress(SocketAddress addr) {
	this.localAddress = addr;
}

private void
openConnection() throws IOException {
	long endTime = System.currentTimeMillis() + timeout;
	client = new TCPClient(endTime);
	if (localAddress != null)
		client.bind(localAddress);
	client.connect(address);
}

private void
sendQuery() throws IOException {
	Record question = Record.newRecord(zname, qtype, dclass);

	Message query = new Message();
	query.getHeader().setOpcode(Opcode.QUERY);
	query.addRecord(question, Section.QUESTION);
	if (qtype == Type.IXFR) {
		Record soa = new SOARecord(zname, dclass, 0, Name.root,
					   Name.root, ixfr_serial,
					   0, 0, 0, 0);
		query.addRecord(soa, Section.AUTHORITY);
	}
	if (tsig != null) {
		tsig.apply(query, null);
		verifier = new TSIG.StreamVerifier(tsig, query.getTSIG());
	}
	byte [] out = query.toWire(Message.MAXLENGTH);
	client.send(out);
}

private long
getSOASerial(Record rec) {
	SOARecord soa = (SOARecord) rec;
	return soa.getSerial();
}

private void
logxfr(String s) {
	if (Options.check("verbose"))
		System.out.println(zname + ": " + s);
}

private void
fail(String s) throws ZoneTransferException {
	throw new ZoneTransferException(s);
}

private void
fallback() throws ZoneTransferException {
	if (!want_fallback)
		fail("server doesn't support IXFR");

	logxfr("falling back to AXFR");
	qtype = Type.AXFR;
	state = INITIALSOA;
}

private void
parseRR(Record rec) throws ZoneTransferException {
	int type = rec.getType();
	Delta delta;

	switch (state) {
	case INITIALSOA:
		if (type != Type.SOA)
			fail("missing initial SOA");
		initialsoa = rec;
		// Remember the serial number in the initial SOA; we need it
		// to recognize the end of an IXFR.
		end_serial = getSOASerial(rec);
		if (qtype == Type.IXFR &&
		    Serial.compare(end_serial, ixfr_serial) <= 0)
		{
			logxfr("up to date");
			state = END;
			break;
		}
		state = FIRSTDATA;
		break;

	case FIRSTDATA:
		// If the transfer begins with 1 SOA, it's an AXFR.
		// If it begins with 2 SOAs, it's an IXFR.
		if (qtype == Type.IXFR && type == Type.SOA &&
		    getSOASerial(rec) == ixfr_serial)
		{
			rtype = Type.IXFR;
			ixfr = new ArrayList();
			logxfr("got incremental response");
			state = IXFR_DELSOA;
		} else {
			rtype = Type.AXFR;
			axfr = new ArrayList();
			axfr.add(initialsoa);
			logxfr("got nonincremental response");
			state = AXFR;
		}
		parseRR(rec); // Restart...
		return;

	case IXFR_DELSOA:
		delta = new Delta();
		ixfr.add(delta);
		delta.start = getSOASerial(rec);
		delta.deletes.add(rec);
		state = IXFR_DEL;
		break;

	case IXFR_DEL:
		if (type == Type.SOA) {
			current_serial = getSOASerial(rec);
			state = IXFR_ADDSOA;
			parseRR(rec); // Restart...
			return;
		}
		delta = (Delta) ixfr.get(ixfr.size() - 1);
		delta.deletes.add(rec);
		break;

	case IXFR_ADDSOA:
		delta = (Delta) ixfr.get(ixfr.size() - 1);
		delta.end = getSOASerial(rec);
		delta.adds.add(rec);
		state = IXFR_ADD;
		break;

	case IXFR_ADD:
		if (type == Type.SOA) {
			long soa_serial = getSOASerial(rec);
			if (soa_serial == end_serial) {
				state = END;
				break;
			} else if (soa_serial != current_serial) {
				fail("IXFR out of sync: expected serial " +
				     current_serial + " , got " + soa_serial);
			} else {
				state = IXFR_DELSOA;
				parseRR(rec); // Restart...
				return;
			}
		}
		delta = (Delta) ixfr.get(ixfr.size() - 1);
		delta.adds.add(rec);
		break;

	case AXFR:
		// Old BINDs sent cross class A records for non IN classes.
		if (type == Type.A && rec.getDClass() != dclass)
			break;
		axfr.add(rec);
		if (type == Type.SOA) {
			state = END;
		}
		break;

	case END:
		fail("extra data");
		break;

	default:
		fail("invalid state");
		break;
	}
}

private void
closeConnection() {
	try {
		if (client != null)
			client.cleanup();
	}
	catch (IOException e) {
	}
}

private Message
parseMessage(byte [] b) throws WireParseException {
	try {
		return new Message(b);
	}
	catch (IOException e) {
		if (e instanceof WireParseException)
			throw (WireParseException) e;
		throw new WireParseException("Error parsing message");
	}
}

private void
doxfr() throws IOException, ZoneTransferException {
	sendQuery();
	while (state != END) {
		byte [] in = client.recv();
		Message response =  parseMessage(in);
		if (response.getHeader().getRcode() == Rcode.NOERROR &&
		    verifier != null)
		{
			TSIGRecord tsigrec = response.getTSIG();

			int error = verifier.verify(response, in);
			if (error != Rcode.NOERROR)
				fail("TSIG failure");
		}

		Record [] answers = response.getSectionArray(Section.ANSWER);

		if (state == INITIALSOA) {
			int rcode = response.getRcode();
			if (rcode != Rcode.NOERROR) {
				if (qtype == Type.IXFR &&
				    rcode == Rcode.NOTIMP)
				{
					fallback();
					doxfr();
					return;
				}
				fail(Rcode.string(rcode));
			}

			Record question = response.getQuestion();
			if (question != null && question.getType() != qtype) {
				fail("invalid question section");
			}

			if (answers.length == 0 && qtype == Type.IXFR) {
				fallback();
				doxfr();
				return;
			}
		}

		for (int i = 0; i < answers.length; i++) {
			parseRR(answers[i]);
		}

		if (state == END && verifier != null &&
		    !response.isVerified())
			fail("last message must be signed");
	}
}

/**
 * Does the zone transfer.
 * @return A list, which is either an AXFR-style response (List of Records),
 * and IXFR-style response (List of Deltas), or null, which indicates that
 * an IXFR was performed and the zone is up to date.
 * @throws IOException The zone transfer failed to due an IO problem.
 * @throws ZoneTransferException The zone transfer failed to due a problem
 * with the zone transfer itself.
 */
public List
run() throws IOException, ZoneTransferException {
	try {
		openConnection();
		doxfr();
	}
	finally {
		closeConnection();
	}
	if (axfr != null)
		return axfr;
	return ixfr;
}

/**
 * Returns true if the response is an AXFR-style response (List of Records).
 * This will be true if either an IXFR was performed, an IXFR was performed
 * and the server provided a full zone transfer, or an IXFR failed and
 * fallback to AXFR occurred.
 */
public boolean
isAXFR() {
	return (rtype == Type.AXFR);
}

/**
 * Gets the AXFR-style response.
 */
public List
getAXFR() {
	return axfr;
}

/**
 * Returns true if the response is an IXFR-style response (List of Deltas).
 * This will be true only if an IXFR was performed and the server provided
 * an incremental zone transfer.
 */
public boolean
isIXFR() {
	return (rtype == Type.IXFR);
}

/**
 * Gets the IXFR-style response.
 */
public List
getIXFR() {
	return ixfr;
}

/**
 * Returns true if the response indicates that the zone is up to date.
 * This will be true only if an IXFR was performed.
 */
public boolean
isCurrent() {
	return (axfr == null && ixfr == null);
}

}
