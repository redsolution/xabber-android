// Copyright (c) 2004 Brian Wellington (bwelling@xbill.org)

package org.xbill.DNS;

import java.io.*;
import java.util.*;

/**
 * A representation of a $GENERATE statement in a master file.
 *
 * @author Brian Wellington
 */

public class Generator {

/** The start of the range. */
public long start;

/** The end of the range. */
public long end;

/** The step value of the range. */
public long step;

/** The pattern to use for generating record names. */
public final String namePattern;

/** The type of the generated records. */
public final int type;

/** The class of the generated records. */
public final int dclass;

/** The ttl of the generated records. */
public final long ttl;

/** The pattern to use for generating record data. */
public final String rdataPattern;

/** The origin to append to relative names. */
public final Name origin;

private long current;

/**
 * Indicates whether generation is supported for this type.
 * @throws InvalidTypeException The type is out of range.
 */
public static boolean
supportedType(int type) {
	Type.check(type);
	return (type == Type.PTR || type == Type.CNAME || type == Type.DNAME ||
		type == Type.A || type == Type.AAAA || type == Type.NS);
}

/**
 * Creates a specification for generating records, as a $GENERATE
 * statement in a master file.
 * @param start The start of the range.
 * @param end The end of the range.
 * @param step The step value of the range.
 * @param namePattern The pattern to use for generating record names.
 * @param type The type of the generated records.  The supported types are
 * PTR, CNAME, DNAME, A, AAAA, and NS.
 * @param dclass The class of the generated records.
 * @param ttl The ttl of the generated records.
 * @param rdataPattern The pattern to use for generating record data.
 * @param origin The origin to append to relative names.
 * @throws IllegalArgumentException The range is invalid.
 * @throws IllegalArgumentException The type does not support generation.
 * @throws IllegalArgumentException The dclass is not a valid class.
 */
public
Generator(long start, long end, long step, String namePattern,
	  int type, int dclass, long ttl, String rdataPattern, Name origin)
{
	if (start < 0 || end < 0 || start > end || step <= 0)
		throw new IllegalArgumentException
				("invalid range specification");
	if (!supportedType(type))
		throw new IllegalArgumentException("unsupported type");
	DClass.check(dclass);

	this.start = start;
	this.end = end;
	this.step = step;
	this.namePattern = namePattern;
	this.type = type;
	this.dclass = dclass;
	this.ttl = ttl;
	this.rdataPattern = rdataPattern;
	this.origin = origin;
	this.current = start;
}

private String
substitute(String spec, long n) throws IOException {
	boolean escaped = false;
	byte [] str = spec.getBytes();
	StringBuffer sb = new StringBuffer();

	for (int i = 0; i < str.length; i++) {
		char c = (char)(str[i] & 0xFF);
		if (escaped) {
			sb.append(c);
			escaped = false;
		} else if (c == '\\') {
			if (i + 1 == str.length)
				throw new TextParseException
						("invalid escape character");
			escaped = true;
		} else if (c == '$') {
			boolean negative = false;
			long offset = 0;
			long width = 0;
			long base = 10;
			boolean wantUpperCase = false;
			if (i + 1 < str.length && str[i + 1] == '$') {
				// '$$' == literal '$' for backwards
				// compatibility with old versions of BIND.
				c = (char)(str[++i] & 0xFF);
				sb.append(c);
				continue;
			} else if (i + 1 < str.length && str[i + 1] == '{') {
				// It's a substitution with modifiers.
				i++;
				if (i + 1 < str.length && str[i + 1] == '-') {
					negative = true;
					i++;
				}
				while (i + 1 < str.length) {
					c = (char)(str[++i] & 0xFF);
					if (c == ',' || c == '}')
						break;
					if (c < '0' || c > '9')
						throw new TextParseException(
							"invalid offset");
					c -= '0';
					offset *= 10;
					offset += c;
				}
				if (negative)
					offset = -offset;

				if (c == ',') {
					while (i + 1 < str.length) {
						c = (char)(str[++i] & 0xFF);
						if (c == ',' || c == '}')
							break;
						if (c < '0' || c > '9')
							throw new
							   TextParseException(
							   "invalid width");
						c -= '0';
						width *= 10;
						width += c;
					}
				}

				if (c == ',') {
					if  (i + 1 == str.length)
						throw new TextParseException(
							   "invalid base");
					c = (char)(str[++i] & 0xFF);
					if (c == 'o')
						base = 8;
					else if (c == 'x')
						base = 16;
					else if (c == 'X') {
						base = 16;
						wantUpperCase = true;
					}
					else if (c != 'd')
						throw new TextParseException(
							   "invalid base");
				}

				if (i + 1 == str.length || str[i + 1] != '}')
					throw new TextParseException
						("invalid modifiers");
				i++;
			}
			long v = n + offset;
			if (v < 0)
				throw new TextParseException
						("invalid offset expansion");
			String number;
			if (base == 8)
				number = Long.toOctalString(v);
			else if (base == 16)
				number = Long.toHexString(v);
			else
				number = Long.toString(v);
			if (wantUpperCase)
				number = number.toUpperCase();
			if (width != 0 && width > number.length()) {
				int zeros = (int)width - number.length();
				while (zeros-- > 0)
					sb.append('0');
			}
			sb.append(number);
		} else {
			sb.append(c);
		}
	}
	return sb.toString();
}

/**
 * Constructs and returns the next record in the expansion.
 * @throws IOException The name or rdata was invalid after substitutions were
 * performed.
 */
public Record
nextRecord() throws IOException {
	if (current > end)
		return null;
	String namestr = substitute(namePattern, current);
	Name name = Name.fromString(namestr, origin);
	String rdata = substitute(rdataPattern, current);
	current += step;
	return Record.fromString(name, type, dclass, ttl, rdata, origin);
}

/**
 * Constructs and returns all records in the expansion.
 * @throws IOException The name or rdata of a record was invalid after
 * substitutions were performed.
 */
public Record []
expand() throws IOException {
	List list = new ArrayList();
	for (long i = start; i < end; i += step) {
		String namestr = substitute(namePattern, current);
		Name name = Name.fromString(namestr, origin);
		String rdata = substitute(rdataPattern, current);
		list.add(Record.fromString(name, type, dclass, ttl,
					   rdata, origin));
	}
	return (Record []) list.toArray(new Record[list.size()]);
}

/**
 * Converts the generate specification to a string containing the corresponding
 * $GENERATE statement.
 */
public String
toString() {
	StringBuffer sb = new StringBuffer();
	sb.append("$GENERATE ");
	sb.append(start + "-" + end);
	if (step > 1)
		sb.append("/" + step);
	sb.append(" ");
	sb.append(namePattern + " ");
	sb.append(ttl + " ");
	if (dclass != DClass.IN || !Options.check("noPrintIN"))
		sb.append(DClass.string(dclass) + " ");
	sb.append(Type.string(type) + " ");
	sb.append(rdataPattern + " ");
	return sb.toString();
}

}
