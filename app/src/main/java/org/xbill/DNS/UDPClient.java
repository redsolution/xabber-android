// Copyright (c) 2005 Brian Wellington (bwelling@xbill.org)

package org.xbill.DNS;

import java.io.*;
import java.net.*;
import java.security.SecureRandom;
import java.nio.*;
import java.nio.channels.*;

final class UDPClient extends Client {

private static final int EPHEMERAL_START = 1024;
private static final int EPHEMERAL_STOP  = 65535;
private static final int EPHEMERAL_RANGE  = EPHEMERAL_STOP - EPHEMERAL_START;

private static SecureRandom prng = new SecureRandom();
private static volatile boolean prng_initializing = true;

/*
 * On some platforms (Windows), the SecureRandom module initialization involves
 * a call to InetAddress.getLocalHost(), which can end up here if using a
 * dnsjava name service provider.
 *
 * This can cause problems in multiple ways.
 *   - If the SecureRandom seed generation process calls into here, and this
 *     module attempts to seed the local SecureRandom object, the thread hangs.
 *   - If something else calls InetAddress.getLocalHost(), and that causes this
 *     module to seed the local SecureRandom object, the thread hangs.
 *
 * To avoid both of these, check at initialization time to see if InetAddress
 * is in the call chain.  If so, initialize the SecureRandom object in a new
 * thread, and disable port randomization until it completes.
 */
static {
	new Thread(new Runnable() {
			   public void run() {
			   int n = prng.nextInt();
			   prng_initializing = false;
		   }}).start();
}

private boolean bound = false;

public
UDPClient(long endTime) throws IOException {
	super(DatagramChannel.open(), endTime);
}

private void
bind_random(InetSocketAddress addr) throws IOException
{
	if (prng_initializing) {
		try {
			Thread.sleep(2);
		}
		catch (InterruptedException e) {
		}
		if (prng_initializing)
			return;
	}

	DatagramChannel channel = (DatagramChannel) key.channel();
	InetSocketAddress temp;

	for (int i = 0; i < 1024; i++) {
		try {
			int port = prng.nextInt(EPHEMERAL_RANGE) +
				   EPHEMERAL_START;
			if (addr != null)
				temp = new InetSocketAddress(addr.getAddress(),
							     port);
			else
				temp = new InetSocketAddress(port);
			channel.socket().bind(temp);
			bound = true;
			return;
		}
		catch (SocketException e) {
		}
	}
}

void
bind(SocketAddress addr) throws IOException {
	if (addr == null ||
	    (addr instanceof InetSocketAddress &&
	     ((InetSocketAddress)addr).getPort() == 0))
	{
		bind_random((InetSocketAddress) addr);
		if (bound)
			return;
	}

	if (addr != null) {
		DatagramChannel channel = (DatagramChannel) key.channel();
		channel.socket().bind(addr);
		bound = true;
	}
}

void
connect(SocketAddress addr) throws IOException {
	if (!bound)
		bind(null);
	DatagramChannel channel = (DatagramChannel) key.channel();
	channel.connect(addr);
}

void
send(byte [] data) throws IOException {
	DatagramChannel channel = (DatagramChannel) key.channel();
	verboseLog("UDP write", data);
	channel.write(ByteBuffer.wrap(data));
}

byte []
recv(int max) throws IOException {
	DatagramChannel channel = (DatagramChannel) key.channel();
	byte [] temp = new byte[max];
	key.interestOps(SelectionKey.OP_READ);
	try {
		while (!key.isReadable())
			blockUntil(key, endTime);
	}
	finally {
		if (key.isValid())
			key.interestOps(0);
	}
	long ret = channel.read(ByteBuffer.wrap(temp));
	if (ret <= 0)
		throw new EOFException();
	int len = (int) ret;
	byte [] data = new byte[len];
	System.arraycopy(temp, 0, data, 0, len);
	verboseLog("UDP read", data);
	return data;
}

static byte []
sendrecv(SocketAddress local, SocketAddress remote, byte [] data, int max,
	 long endTime)
throws IOException
{
	UDPClient client = new UDPClient(endTime);
	try {
		client.bind(local);
		client.connect(remote);
		client.send(data);
		return client.recv(max);
	}
	finally {
		client.cleanup();
	}
}

static byte []
sendrecv(SocketAddress addr, byte [] data, int max, long endTime)
throws IOException
{
	return sendrecv(null, addr, data, max, endTime);
}

}
