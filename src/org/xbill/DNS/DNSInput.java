// Copyright (c) 2004 Brian Wellington (bwelling@xbill.org)

package org.xbill.DNS;

/**
 * An class for parsing DNS messages.
 *
 * @author Brian Wellington
 */

public class DNSInput {

private byte [] array;
private int pos;
private int end;
private int saved_pos;
private int saved_end;

/**
 * Creates a new DNSInput
 * @param input The byte array to read from
 */
public
DNSInput(byte [] input) {
	array = input;
	pos = 0;
	end = array.length;
	saved_pos = -1;
	saved_end = -1;
}

/**
 * Returns the current position.
 */
public int
current() {
	return pos;
}

/**
 * Returns the number of bytes that can be read from this stream before
 * reaching the end.
 */
public int
remaining() {
	return end - pos;
}

private void
require(int n) throws WireParseException{
	if (n > remaining()) {
		throw new WireParseException("end of input");
	}
}

/**
 * Marks the following bytes in the stream as active.
 * @param len The number of bytes in the active region.
 * @throws IllegalArgumentException The number of bytes in the active region
 * is longer than the remainder of the input.
 */
public void
setActive(int len) {
	if (len > array.length - pos) {
		throw new IllegalArgumentException("cannot set active " +
						   "region past end of input");
	}
	end = pos + len;
}

/**
 * Clears the active region of the string.  Further operations are not
 * restricted to part of the input.
 */
public void
clearActive() {
	end = array.length;
}

/**
 * Returns the position of the end of the current active region.
 */
public int
saveActive() {
	return end;
}

/**
 * Restores the previously set active region.  This differs from setActive() in
 * that restoreActive() takes an absolute position, and setActive takes an
 * offset from the current location.
 * @param pos The end of the active region.
 */
public void
restoreActive(int pos) {
	if (pos > array.length) {
		throw new IllegalArgumentException("cannot set active " +
						   "region past end of input");
	}
	end = pos;
}

/**
 * Resets the current position of the input stream to the specified index,
 * and clears the active region.
 * @param index The position to continue parsing at.
 * @throws IllegalArgumentException The index is not within the input.
 */
public void
jump(int index) {
	if (index >= array.length) {
		throw new IllegalArgumentException("cannot jump past " +
						   "end of input");
	}
	pos = index;
	end = array.length;
}

/**
 * Saves the current state of the input stream.  Both the current position and
 * the end of the active region are saved.
 * @throws IllegalArgumentException The index is not within the input.
 */
public void
save() {
	saved_pos = pos;
	saved_end = end;
}

/**
 * Restores the input stream to its state before the call to {@link #save}.
 */
public void
restore() {
	if (saved_pos < 0) {
		throw new IllegalStateException("no previous state");
	}
	pos = saved_pos;
	end = saved_end;
	saved_pos = -1;
	saved_end = -1;
}

/**
 * Reads an unsigned 8 bit value from the stream, as an int.
 * @return An unsigned 8 bit value.
 * @throws WireParseException The end of the stream was reached.
 */
public int
readU8() throws WireParseException {
	require(1);
	return (array[pos++] & 0xFF);
}

/**
 * Reads an unsigned 16 bit value from the stream, as an int.
 * @return An unsigned 16 bit value.
 * @throws WireParseException The end of the stream was reached.
 */
public int
readU16() throws WireParseException {
	require(2);
	int b1 = array[pos++] & 0xFF;
	int b2 = array[pos++] & 0xFF;
	return ((b1 << 8) + b2);
}

/**
 * Reads an unsigned 32 bit value from the stream, as a long.
 * @return An unsigned 32 bit value.
 * @throws WireParseException The end of the stream was reached.
 */
public long
readU32() throws WireParseException {
	require(4);
	int b1 = array[pos++] & 0xFF;
	int b2 = array[pos++] & 0xFF;
	int b3 = array[pos++] & 0xFF;
	int b4 = array[pos++] & 0xFF;
	return (((long)b1 << 24) + (b2 << 16) + (b3 << 8) + b4);
}

/**
 * Reads a byte array of a specified length from the stream into an existing
 * array.
 * @param b The array to read into.
 * @param off The offset of the array to start copying data into.
 * @param len The number of bytes to copy.
 * @throws WireParseException The end of the stream was reached.
 */
public void
readByteArray(byte [] b, int off, int len) throws WireParseException {
	require(len);
	System.arraycopy(array, pos, b, off, len);
	pos += len;
}

/**
 * Reads a byte array of a specified length from the stream.
 * @return The byte array.
 * @throws WireParseException The end of the stream was reached.
 */
public byte []
readByteArray(int len) throws WireParseException {
	require(len);
	byte [] out = new byte[len];
	System.arraycopy(array, pos, out, 0, len);
	pos += len;
	return out;
}

/**
 * Reads a byte array consisting of the remainder of the stream (or the
 * active region, if one is set.
 * @return The byte array.
 */
public byte []
readByteArray() {
	int len = remaining();
	byte [] out = new byte[len];
	System.arraycopy(array, pos, out, 0, len);
	pos += len;
	return out;
}

/**
 * Reads a counted string from the stream.  A counted string is a one byte
 * value indicating string length, followed by bytes of data.
 * @return A byte array containing the string.
 * @throws WireParseException The end of the stream was reached.
 */
public byte []
readCountedString() throws WireParseException {
	require(1);
	int len = array[pos++] & 0xFF;
	return readByteArray(len);
}

}
