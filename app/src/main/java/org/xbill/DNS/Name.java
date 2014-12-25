// Copyright (c) 1999-2004 Brian Wellington (bwelling@xbill.org)

package org.xbill.DNS;

import java.io.*;
import java.text.*;

/**
 * A representation of a domain name.  It may either be absolute (fully
 * qualified) or relative.
 *
 * @author Brian Wellington
 */

public class Name implements Comparable, Serializable {

private static final long serialVersionUID = -7257019940971525644L;

private static final int LABEL_NORMAL = 0;
private static final int LABEL_COMPRESSION = 0xC0;
private static final int LABEL_MASK = 0xC0;

/* The name data */
private byte [] name;

/*
 * Effectively an 8 byte array, where the low order byte stores the number
 * of labels and the 7 higher order bytes store per-label offsets.
 */
private long offsets;

/* Precomputed hashcode. */
private int hashcode;

private static final byte [] emptyLabel = new byte[] {(byte)0};
private static final byte [] wildLabel = new byte[] {(byte)1, (byte)'*'};

/** The root name */
public static final Name root;

/** The root name */
public static final Name empty;

/** The maximum length of a Name */
private static final int MAXNAME = 255;

/** The maximum length of a label a Name */
private static final int MAXLABEL = 63;

/** The maximum number of labels in a Name */
private static final int MAXLABELS = 128;

/** The maximum number of cached offsets */
private static final int MAXOFFSETS = 7;

/* Used for printing non-printable characters */
private static final DecimalFormat byteFormat = new DecimalFormat();

/* Used to efficiently convert bytes to lowercase */
private static final byte lowercase[] = new byte[256];

/* Used in wildcard names. */
private static final Name wild;

static {
	byteFormat.setMinimumIntegerDigits(3);
	for (int i = 0; i < lowercase.length; i++) {
		if (i < 'A' || i > 'Z')
			lowercase[i] = (byte)i;
		else
			lowercase[i] = (byte)(i - 'A' + 'a');
	}
	root = new Name();
	root.appendSafe(emptyLabel, 0, 1);
	empty = new Name();
	empty.name = new byte[0];
	wild = new Name();
	wild.appendSafe(wildLabel, 0, 1);
}

private
Name() {
}

private final void
setoffset(int n, int offset) {
	if (n >= MAXOFFSETS)
		return;
	int shift = 8 * (7 - n);
	offsets &= (~(0xFFL << shift));
	offsets |= ((long)offset << shift);
}

private final int
offset(int n) {
	if (n == 0 && getlabels() == 0)
		return 0;
	if (n < 0 || n >= getlabels())
		throw new IllegalArgumentException("label out of range");
	if (n < MAXOFFSETS) {
		int shift = 8 * (7 - n);
		return ((int)(offsets >>> shift) & 0xFF);
	} else {
		int pos = offset(MAXOFFSETS - 1);
		for (int i = MAXOFFSETS - 1; i < n; i++)
			pos += (name[pos] + 1);
		return (pos);
	}
}

private final void
setlabels(int labels) {
	offsets &= ~(0xFF);
	offsets |= labels;
}

private final int
getlabels() {
	return (int)(offsets & 0xFF);
}

private static final void
copy(Name src, Name dst) {
	if (src.offset(0) == 0) {
		dst.name = src.name;
		dst.offsets = src.offsets;
	} else {
		int offset0 = src.offset(0);
		int namelen = src.name.length - offset0;
		int labels = src.labels();
		dst.name = new byte[namelen];
		System.arraycopy(src.name, offset0, dst.name, 0, namelen);
		for (int i = 0; i < labels && i < MAXOFFSETS; i++)
			dst.setoffset(i, src.offset(i) - offset0);
		dst.setlabels(labels);
	}
}

private final void
append(byte [] array, int start, int n) throws NameTooLongException {
	int length = (name == null ? 0 : (name.length - offset(0)));
	int alength = 0;
	for (int i = 0, pos = start; i < n; i++) {
		int len = array[pos];
		if (len > MAXLABEL)
			throw new IllegalStateException("invalid label");
		len++;
		pos += len;
		alength += len;
	}
	int newlength = length + alength;
	if (newlength > MAXNAME)
		throw new NameTooLongException();
	int labels = getlabels();
	int newlabels = labels + n;
	if (newlabels > MAXLABELS)
		throw new IllegalStateException("too many labels");
	byte [] newname = new byte[newlength];
	if (length != 0)
		System.arraycopy(name, offset(0), newname, 0, length);
	System.arraycopy(array, start, newname, length, alength);
	name = newname;
	for (int i = 0, pos = length; i < n; i++) {
		setoffset(labels + i, pos);
		pos += (newname[pos] + 1);
	}
	setlabels(newlabels);
}

private static TextParseException
parseException(String str, String message) {
	return new TextParseException("'" + str + "': " + message);
}

private final void
appendFromString(String fullName, byte [] array, int start, int n)
throws TextParseException
{
	try {
		append(array, start, n);
	}
	catch (NameTooLongException e) {
		throw parseException(fullName, "Name too long");
	}
}

private final void
appendSafe(byte [] array, int start, int n) {
	try {
		append(array, start, n);
	}
	catch (NameTooLongException e) {
	}
}

/**
 * Create a new name from a string and an origin.  This does not automatically
 * make the name absolute; it will be absolute if it has a trailing dot or an
 * absolute origin is appended.
 * @param s The string to be converted
 * @param origin If the name is not absolute, the origin to be appended.
 * @throws TextParseException The name is invalid.
 */
public
Name(String s, Name origin) throws TextParseException {
	if (s.equals(""))
		throw parseException(s, "empty name");
	else if (s.equals("@")) {
		if (origin == null)
			copy(empty, this);
		else
			copy(origin, this);
		return;
	} else if (s.equals(".")) {
		copy(root, this);
		return;
	}
	int labelstart = -1;
	int pos = 1;
	byte [] label = new byte[MAXLABEL + 1];
	boolean escaped = false;
	int digits = 0;
	int intval = 0;
	boolean absolute = false;
	for (int i = 0; i < s.length(); i++) {
		byte b = (byte) s.charAt(i);
		if (escaped) {
			if (b >= '0' && b <= '9' && digits < 3) {
				digits++;
				intval *= 10;
				intval += (b - '0');
				if (intval > 255)
					throw parseException(s, "bad escape");
				if (digits < 3)
					continue;
				b = (byte) intval;
			}
			else if (digits > 0 && digits < 3)
				throw parseException(s, "bad escape");
			if (pos > MAXLABEL)
				throw parseException(s, "label too long");
			labelstart = pos;
			label[pos++] = b;
			escaped = false;
		} else if (b == '\\') {
			escaped = true;
			digits = 0;
			intval = 0;
		} else if (b == '.') {
			if (labelstart == -1)
				throw parseException(s, "invalid empty label");
			label[0] = (byte)(pos - 1);
			appendFromString(s, label, 0, 1);
			labelstart = -1;
			pos = 1;
		} else {
			if (labelstart == -1)
				labelstart = i;
			if (pos > MAXLABEL)
				throw parseException(s, "label too long");
			label[pos++] = b;
		}
	}
	if (digits > 0 && digits < 3)
		throw parseException(s, "bad escape");
	if (escaped)
		throw parseException(s, "bad escape");
	if (labelstart == -1) {
		appendFromString(s, emptyLabel, 0, 1);
		absolute = true;
	} else {
		label[0] = (byte)(pos - 1);
		appendFromString(s, label, 0, 1);
	}
	if (origin != null && !absolute)
		appendFromString(s, origin.name, 0, origin.getlabels());
}

/**
 * Create a new name from a string.  This does not automatically make the name
 * absolute; it will be absolute if it has a trailing dot.
 * @param s The string to be converted
 * @throws TextParseException The name is invalid.
 */
public
Name(String s) throws TextParseException {
	this(s, null);
}

/**
 * Create a new name from a string and an origin.  This does not automatically
 * make the name absolute; it will be absolute if it has a trailing dot or an
 * absolute origin is appended.  This is identical to the constructor, except
 * that it will avoid creating new objects in some cases.
 * @param s The string to be converted
 * @param origin If the name is not absolute, the origin to be appended.
 * @throws TextParseException The name is invalid.
 */
public static Name
fromString(String s, Name origin) throws TextParseException {
	if (s.equals("@") && origin != null)
		return origin;
	else if (s.equals("."))
		return (root);

	return new Name(s, origin);
}

/**
 * Create a new name from a string.  This does not automatically make the name
 * absolute; it will be absolute if it has a trailing dot.  This is identical
 * to the constructor, except that it will avoid creating new objects in some
 * cases.
 * @param s The string to be converted
 * @throws TextParseException The name is invalid.
 */
public static Name
fromString(String s) throws TextParseException {
	return fromString(s, null);
}

/**
 * Create a new name from a constant string.  This should only be used when
 the name is known to be good - that is, when it is constant.
 * @param s The string to be converted
 * @throws IllegalArgumentException The name is invalid.
 */
public static Name
fromConstantString(String s) {
	try {
		return fromString(s, null);
	}
	catch (TextParseException e) {
		throw new IllegalArgumentException("Invalid name '" + s + "'");
	}
}

/**
 * Create a new name from DNS a wire format message
 * @param in A stream containing the DNS message which is currently
 * positioned at the start of the name to be read.
 */
public
Name(DNSInput in) throws WireParseException {
	int len, pos;
	boolean done = false;
	byte [] label = new byte[MAXLABEL + 1];
	boolean savedState = false;

	while (!done) {
		len = in.readU8();
		switch (len & LABEL_MASK) {
		case LABEL_NORMAL:
			if (getlabels() >= MAXLABELS)
				throw new WireParseException("too many labels");
			if (len == 0) {
				append(emptyLabel, 0, 1);
				done = true;
			} else {
				label[0] = (byte)len;
				in.readByteArray(label, 1, len);
				append(label, 0, 1);
			}
			break;
		case LABEL_COMPRESSION:
			pos = in.readU8();
			pos += ((len & ~LABEL_MASK) << 8);
			if (Options.check("verbosecompression"))
				System.err.println("currently " + in.current() +
						   ", pointer to " + pos);

			if (pos >= in.current() - 2)
				throw new WireParseException("bad compression");
			if (!savedState) {
				in.save();
				savedState = true;
			}
			in.jump(pos);
			if (Options.check("verbosecompression"))
				System.err.println("current name '" + this +
						   "', seeking to " + pos);
			break;
		default:
			throw new WireParseException("bad label type");
		}
	}
	if (savedState) {
		in.restore();
	}
}

/**
 * Create a new name from DNS wire format
 * @param b A byte array containing the wire format of the name.
 */
public
Name(byte [] b) throws IOException {
	this(new DNSInput(b));
}

/**
 * Create a new name by removing labels from the beginning of an existing Name
 * @param src An existing Name
 * @param n The number of labels to remove from the beginning in the copy
 */
public
Name(Name src, int n) {
	int slabels = src.labels();
	if (n > slabels)
		throw new IllegalArgumentException("attempted to remove too " +
						   "many labels");
	name = src.name;
	setlabels(slabels - n);
	for (int i = 0; i < MAXOFFSETS && i < slabels - n; i++)
		setoffset(i, src.offset(i + n));
}

/**
 * Creates a new name by concatenating two existing names.
 * @param prefix The prefix name.
 * @param suffix The suffix name.
 * @return The concatenated name.
 * @throws NameTooLongException The name is too long.
 */
public static Name
concatenate(Name prefix, Name suffix) throws NameTooLongException {
	if (prefix.isAbsolute())
		return (prefix);
	Name newname = new Name();
	copy(prefix, newname);
	newname.append(suffix.name, suffix.offset(0), suffix.getlabels());
	return newname;
}

/**
 * If this name is a subdomain of origin, return a new name relative to
 * origin with the same value. Otherwise, return the existing name.
 * @param origin The origin to remove.
 * @return The possibly relativized name.
 */
public Name
relativize(Name origin) {
	if (origin == null || !subdomain(origin))
		return this;
	Name newname = new Name();
	copy(this, newname);
	int length = length() - origin.length();
	int labels = newname.labels() - origin.labels();
	newname.setlabels(labels);
	newname.name = new byte[length];
	System.arraycopy(name, offset(0), newname.name, 0, length);
	return newname;
}

/**
 * Generates a new Name with the first n labels replaced by a wildcard 
 * @return The wildcard name
 */
public Name
wild(int n) {
	if (n < 1)
		throw new IllegalArgumentException("must replace 1 or more " +
						   "labels");
	try {
		Name newname = new Name();
		copy(wild, newname);
		newname.append(name, offset(n), getlabels() - n);
		return newname;
	}
	catch (NameTooLongException e) {
		throw new IllegalStateException
					("Name.wild: concatenate failed");
	}
}

/**
 * Generates a new Name to be used when following a DNAME.
 * @param dname The DNAME record to follow.
 * @return The constructed name.
 * @throws NameTooLongException The resulting name is too long.
 */
public Name
fromDNAME(DNAMERecord dname) throws NameTooLongException {
	Name dnameowner = dname.getName();
	Name dnametarget = dname.getTarget();
	if (!subdomain(dnameowner))
		return null;

	int plabels = labels() - dnameowner.labels();
	int plength = length() - dnameowner.length();
	int pstart = offset(0);

	int dlabels = dnametarget.labels();
	int dlength = dnametarget.length();

	if (plength + dlength > MAXNAME)
		throw new NameTooLongException();

	Name newname = new Name();
	newname.setlabels(plabels + dlabels);
	newname.name = new byte[plength + dlength];
	System.arraycopy(name, pstart, newname.name, 0, plength);
	System.arraycopy(dnametarget.name, 0, newname.name, plength, dlength);

	for (int i = 0, pos = 0; i < MAXOFFSETS && i < plabels + dlabels; i++) {
		newname.setoffset(i, pos);
		pos += (newname.name[pos] + 1);
	}
	return newname;
}

/**
 * Is this name a wildcard?
 */
public boolean
isWild() {
	if (labels() == 0)
		return false;
	return (name[0] == (byte)1 && name[1] == (byte)'*');
}

/**
 * Is this name absolute?
 */
public boolean
isAbsolute() {
	if (labels() == 0)
		return false;
	return (name[name.length - 1] == 0);
}

/**
 * The length of the name.
 */
public short
length() {
	if (getlabels() == 0)
		return 0;
	return (short)(name.length - offset(0));
}

/**
 * The number of labels in the name.
 */
public int
labels() {
	return getlabels();
}

/**
 * Is the current Name a subdomain of the specified name?
 */
public boolean
subdomain(Name domain) {
	int labels = labels();
	int dlabels = domain.labels();
	if (dlabels > labels)
		return false;
	if (dlabels == labels)
		return equals(domain);
	return domain.equals(name, offset(labels - dlabels));
}

private String
byteString(byte [] array, int pos) {
	StringBuffer sb = new StringBuffer();
	int len = array[pos++];
	for (int i = pos; i < pos + len; i++) {
		int b = array[i] & 0xFF;
		if (b <= 0x20 || b >= 0x7f) {
			sb.append('\\');
			sb.append(byteFormat.format(b));
		}
		else if (b == '"' || b == '(' || b == ')' || b == '.' ||
			 b == ';' || b == '\\' || b == '@' || b == '$')
		{
			sb.append('\\');
			sb.append((char)b);
		}
		else
			sb.append((char)b);
	}
	return sb.toString();
}

/**
 * Convert a Name to a String
 * @return The representation of this name as a (printable) String.
 */
public String
toString() {
	int labels = labels();
	if (labels == 0)
		return "@";
	else if (labels == 1 && name[offset(0)] == 0)
		return ".";
	StringBuffer sb = new StringBuffer();
	for (int i = 0, pos = offset(0); i < labels; i++) {
		int len = name[pos];
		if (len > MAXLABEL)
			throw new IllegalStateException("invalid label");
		if (len == 0)
			break;
		sb.append(byteString(name, pos));
		sb.append('.');
		pos += (1 + len);
	}
	if (!isAbsolute())
		sb.deleteCharAt(sb.length() - 1);
	return sb.toString();
}

/**
 * Retrieve the nth label of a Name.  This makes a copy of the label; changing
 * this does not change the Name.
 * @param n The label to be retrieved.  The first label is 0.
 */
public byte []
getLabel(int n) {
	int pos = offset(n);
	byte len = (byte)(name[pos] + 1);
	byte [] label = new byte[len];
	System.arraycopy(name, pos, label, 0, len);
	return label;
}

/**
 * Convert the nth label in a Name to a String
 * @param n The label to be converted to a (printable) String.  The first
 * label is 0.
 */
public String
getLabelString(int n) {
	int pos = offset(n);
	return byteString(name, pos);
}

/**
 * Emit a Name in DNS wire format
 * @param out The output stream containing the DNS message.
 * @param c The compression context, or null of no compression is desired.
 * @throws IllegalArgumentException The name is not absolute.
 */
public void
toWire(DNSOutput out, Compression c) {
	if (!isAbsolute())
		throw new IllegalArgumentException("toWire() called on " +
						   "non-absolute name");
	
	int labels = labels();
	for (int i = 0; i < labels - 1; i++) {
		Name tname;
		if (i == 0)
			tname = this;
		else
			tname = new Name(this, i);
		int pos = -1;
		if (c != null)
			pos = c.get(tname);
		if (pos >= 0) {
			pos |= (LABEL_MASK << 8);
			out.writeU16(pos);
			return;
		} else {
			if (c != null)
				c.add(out.current(), tname);
			int off = offset(i);
			out.writeByteArray(name, off, name[off] + 1);
		}
	}
	out.writeU8(0);
}

/**
 * Emit a Name in DNS wire format
 * @throws IllegalArgumentException The name is not absolute.
 */
public byte []
toWire() {
	DNSOutput out = new DNSOutput();
	toWire(out, null);
	return out.toByteArray();
}

/**
 * Emit a Name in canonical DNS wire format (all lowercase)
 * @param out The output stream to which the message is written.
 */
public void
toWireCanonical(DNSOutput out) {
	byte [] b = toWireCanonical();
	out.writeByteArray(b);
}

/**
 * Emit a Name in canonical DNS wire format (all lowercase)
 * @return The canonical form of the name.
 */
public byte []
toWireCanonical() {
	int labels = labels();
	if (labels == 0)
		return (new byte[0]);
	byte [] b = new byte[name.length - offset(0)];
	for (int i = 0, spos = offset(0), dpos = 0; i < labels; i++) {
		int len = name[spos];
		if (len > MAXLABEL)
			throw new IllegalStateException("invalid label");
		b[dpos++] = name[spos++];
		for (int j = 0; j < len; j++)
			b[dpos++] = lowercase[(name[spos++] & 0xFF)];
	}
	return b;
}

/**
 * Emit a Name in DNS wire format
 * @param out The output stream containing the DNS message.
 * @param c The compression context, or null of no compression is desired.
 * @param canonical If true, emit the name in canonicalized form
 * (all lowercase).
 * @throws IllegalArgumentException The name is not absolute.
 */
public void
toWire(DNSOutput out, Compression c, boolean canonical) {
	if (canonical)
		toWireCanonical(out);
	else
		toWire(out, c);
}

private final boolean
equals(byte [] b, int bpos) {
	int labels = labels();
	for (int i = 0, pos = offset(0); i < labels; i++) {
		if (name[pos] != b[bpos])
			return false;
		int len = name[pos++];
		bpos++;
		if (len > MAXLABEL)
			throw new IllegalStateException("invalid label");
		for (int j = 0; j < len; j++)
			if (lowercase[(name[pos++] & 0xFF)] !=
			    lowercase[(b[bpos++] & 0xFF)])
				return false;
	}
	return true;
}

/**
 * Are these two Names equivalent?
 */
public boolean
equals(Object arg) {
	if (arg == this)
		return true;
	if (arg == null || !(arg instanceof Name))
		return false;
	Name d = (Name) arg;
	if (d.hashcode == 0)
		d.hashCode();
	if (hashcode == 0)
		hashCode();
	if (d.hashcode != hashcode)
		return false;
	if (d.labels() != labels())
		return false;
	return equals(d.name, d.offset(0));
}

/**
 * Computes a hashcode based on the value
 */
public int
hashCode() {
	if (hashcode != 0)
		return (hashcode);
	int code = 0;
	for (int i = offset(0); i < name.length; i++)
		code += ((code << 3) + lowercase[(name[i] & 0xFF)]);
	hashcode = code;
	return hashcode;
}

/**
 * Compares this Name to another Object.
 * @param o The Object to be compared.
 * @return The value 0 if the argument is a name equivalent to this name;
 * a value less than 0 if the argument is less than this name in the canonical 
 * ordering, and a value greater than 0 if the argument is greater than this
 * name in the canonical ordering.
 * @throws ClassCastException if the argument is not a Name.
 */
public int
compareTo(Object o) {
	Name arg = (Name) o;

	if (this == arg)
		return (0);

	int labels = labels();
	int alabels = arg.labels();
	int compares = labels > alabels ? alabels : labels;

	for (int i = 1; i <= compares; i++) {
		int start = offset(labels - i);
		int astart = arg.offset(alabels - i);
		int length = name[start];
		int alength = arg.name[astart];
		for (int j = 0; j < length && j < alength; j++) {
			int n = lowercase[(name[j + start + 1]) & 0xFF] -
				lowercase[(arg.name[j + astart + 1]) & 0xFF];
			if (n != 0)
				return (n);
		}
		if (length != alength)
			return (length - alength);
	}
	return (labels - alabels);
}

}
