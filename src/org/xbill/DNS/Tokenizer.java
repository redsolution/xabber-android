// Copyright (c) 2003-2004 Brian Wellington (bwelling@xbill.org)
//
// Copyright (C) 2003-2004 Nominum, Inc.
// 
// Permission to use, copy, modify, and distribute this software for any
// purpose with or without fee is hereby granted, provided that the above
// copyright notice and this permission notice appear in all copies.
//
// THE SOFTWARE IS PROVIDED "AS IS" AND NOMINUM DISCLAIMS ALL WARRANTIES
// WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
// MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL NOMINUM BE LIABLE FOR ANY
// SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
// WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
// ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT
// OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
//

package org.xbill.DNS;

import java.io.*;
import java.net.*;

import org.xbill.DNS.utils.*;

/**
 * Tokenizer is used to parse DNS records and zones from text format,
 *
 * @author Brian Wellington
 * @author Bob Halley
 */

public class Tokenizer {

private static String delim = " \t\n;()\"";
private static String quotes = "\"";

/** End of file */
public static final int EOF		= 0;

/** End of line */
public static final int EOL		= 1;

/** Whitespace; only returned when wantWhitespace is set */
public static final int WHITESPACE	= 2;

/** An identifier (unquoted string) */
public static final int IDENTIFIER	= 3;

/** A quoted string */
public static final int QUOTED_STRING	= 4;

/** A comment; only returned when wantComment is set */
public static final int COMMENT		= 5;

private PushbackInputStream is;
private boolean ungottenToken;
private int multiline;
private boolean quoting;
private String delimiters;
private Token current;
private StringBuffer sb;
private boolean wantClose;

private String filename;
private int line;

public static class Token {
	/** The type of token. */
	public int type;

	/** The value of the token, or null for tokens without values. */
	public String value;

	private
	Token() {
		type = -1;
		value = null;
	}

	private Token
	set(int type, StringBuffer value) {
		if (type < 0)
			throw new IllegalArgumentException();
		this.type = type;
		this.value = value == null ? null : value.toString();
		return this;
	}

	/**
	 * Converts the token to a string containing a representation useful
	 * for debugging.
	 */
	public String
	toString() {
		switch (type) {
		case EOF:
			return "<eof>";
		case EOL:
			return "<eol>";
		case WHITESPACE:
			return "<whitespace>";
		case IDENTIFIER:
			return "<identifier: " + value + ">";
		case QUOTED_STRING:
			return "<quoted_string: " + value + ">";
		case COMMENT:
			return "<comment: " + value + ">";
		default:
			return "<unknown>";
		}
	}

	/** Indicates whether this token contains a string. */
	public boolean
	isString() {
		return (type == IDENTIFIER || type == QUOTED_STRING);
	}

	/** Indicates whether this token contains an EOL or EOF. */
	public boolean
	isEOL() {
		return (type == EOL || type == EOF);
	}
}

static class TokenizerException extends TextParseException {
	String message;

	public
	TokenizerException(String filename, int line, String message) {
		super(filename + ":" + line + ": " + message);
		this.message = message;
	}

	public String
	getBaseMessage() {
		return message;
	}
}

/**
 * Creates a Tokenizer from an arbitrary input stream.
 * @param is The InputStream to tokenize.
 */
public
Tokenizer(InputStream is) {
	if (!(is instanceof BufferedInputStream))
		is = new BufferedInputStream(is);
	this.is = new PushbackInputStream(is, 2);
	ungottenToken = false;
	multiline = 0;
	quoting = false;
	delimiters = delim;
	current = new Token();
	sb = new StringBuffer();
	filename = "<none>";
	line = 1;
}

/**
 * Creates a Tokenizer from a string.
 * @param s The String to tokenize.
 */
public
Tokenizer(String s) {
	this(new ByteArrayInputStream(s.getBytes()));
}

/**
 * Creates a Tokenizer from a file.
 * @param f The File to tokenize.
 */
public
Tokenizer(File f) throws FileNotFoundException {
	this(new FileInputStream(f));
	wantClose = true;
	filename = f.getName();
}

private int
getChar() throws IOException {
	int c = is.read();
	if (c == '\r') {
		int next = is.read();
		if (next != '\n')
			is.unread(next);
		c = '\n';
	}
	if (c == '\n')
		line++;
	return c;
}

private void
ungetChar(int c) throws IOException {
	if (c == -1)
		return;
	is.unread(c);
	if (c == '\n')
		line--;
}

private int
skipWhitespace() throws IOException {
	int skipped = 0;
	while (true) {
		int c = getChar();
		if (c != ' ' && c != '\t') {
	                if (!(c == '\n' && multiline > 0)) {
				ungetChar(c);
				return skipped;
			}
		}
		skipped++;
	}
}

private void
checkUnbalancedParens() throws TextParseException {
	if (multiline > 0)
		throw exception("unbalanced parentheses");
}

/**
 * Gets the next token from a tokenizer.
 * @param wantWhitespace If true, leading whitespace will be returned as a
 * token.
 * @param wantComment If true, comments are returned as tokens.
 * @return The next token in the stream.
 * @throws TextParseException The input was invalid.
 * @throws IOException An I/O error occurred.
 */
public Token
get(boolean wantWhitespace, boolean wantComment) throws IOException {
	int type;
	int c;

	if (ungottenToken) {
		ungottenToken = false;
		if (current.type == WHITESPACE) {
			if (wantWhitespace)
				return current;
		} else if (current.type == COMMENT) {
			if (wantComment)
				return current;
		} else {
			if (current.type == EOL)
				line++;
			return current;
		}
	}
	int skipped = skipWhitespace();
	if (skipped > 0 && wantWhitespace)
		return current.set(WHITESPACE, null);
	type = IDENTIFIER;
	sb.setLength(0);
	while (true) {
		c = getChar();
		if (c == -1 || delimiters.indexOf(c) != -1) {
			if (c == -1) {
				if (quoting)
					throw exception("EOF in " +
							"quoted string");
				else if (sb.length() == 0)
					return current.set(EOF, null);
				else
					return current.set(type, sb);
			}
			if (sb.length() == 0 && type != QUOTED_STRING) {
				if (c == '(') {
					multiline++;
					skipWhitespace();
					continue;
				} else if (c == ')') {
					if (multiline <= 0)
						throw exception("invalid " +
								"close " +
								"parenthesis");
					multiline--;
					skipWhitespace();
					continue;
				} else if (c == '"') {
					if (!quoting) {
						quoting = true;
						delimiters = quotes;
						type = QUOTED_STRING;
					} else {
						quoting = false;
						delimiters = delim;
						skipWhitespace();
					}
					continue;
				} else if (c == '\n') {
					return current.set(EOL, null);
				} else if (c == ';') {
					while (true) {
						c = getChar();
						if (c == '\n' || c == -1)
							break;
						sb.append((char)c);
					}
					if (wantComment) {
						ungetChar(c);
						return current.set(COMMENT, sb);
					} else if (c == -1 &&
						   type != QUOTED_STRING)
					{
						checkUnbalancedParens();
						return current.set(EOF, null);
					} else if (multiline > 0) {
						skipWhitespace();
						sb.setLength(0);
						continue;
					} else
						return current.set(EOL, null);
				} else
					throw new IllegalStateException();
			} else
				ungetChar(c);
			break;
		} else if (c == '\\') {
			c = getChar();
			if (c == -1)
				throw exception("unterminated escape sequence");
			sb.append('\\');
		} else if (quoting && c == '\n') {
			throw exception("newline in quoted string");
		}
		sb.append((char)c);
	}
	if (sb.length() == 0 && type != QUOTED_STRING) {
		checkUnbalancedParens();
		return current.set(EOF, null);
	}
	return current.set(type, sb);
}

/**
 * Gets the next token from a tokenizer, ignoring whitespace and comments.
 * @return The next token in the stream.
 * @throws TextParseException The input was invalid.
 * @throws IOException An I/O error occurred.
 */
public Token
get() throws IOException {
	return get(false, false);
}

/**
 * Returns a token to the stream, so that it will be returned by the next call
 * to get().
 * @throws IllegalStateException There are already ungotten tokens.
 */
public void
unget() {
	if (ungottenToken)
		throw new IllegalStateException
				("Cannot unget multiple tokens");
	if (current.type == EOL)
		line--;
	ungottenToken = true;
}

/**
 * Gets the next token from a tokenizer and converts it to a string.
 * @return The next token in the stream, as a string.
 * @throws TextParseException The input was invalid or not a string.
 * @throws IOException An I/O error occurred.
 */
public String
getString() throws IOException {
	Token next = get();
	if (!next.isString()) {
		throw exception("expected a string");
	}
	return next.value;
}

private String
_getIdentifier(String expected) throws IOException {
	Token next = get();
	if (next.type != IDENTIFIER)
		throw exception("expected " + expected);
	return next.value;
}

/**
 * Gets the next token from a tokenizer, ensures it is an unquoted string,
 * and converts it to a string.
 * @return The next token in the stream, as a string.
 * @throws TextParseException The input was invalid or not an unquoted string.
 * @throws IOException An I/O error occurred.
 */
public String
getIdentifier() throws IOException {
	return _getIdentifier("an identifier");
}

/**
 * Gets the next token from a tokenizer and converts it to a long.
 * @return The next token in the stream, as a long.
 * @throws TextParseException The input was invalid or not a long.
 * @throws IOException An I/O error occurred.
 */
public long
getLong() throws IOException {
	String next = _getIdentifier("an integer");
	if (!Character.isDigit(next.charAt(0)))
		throw exception("expected an integer");
	try {
		return Long.parseLong(next);
	} catch (NumberFormatException e) {
		throw exception("expected an integer");
	}
}

/**
 * Gets the next token from a tokenizer and converts it to an unsigned 32 bit
 * integer.
 * @return The next token in the stream, as an unsigned 32 bit integer.
 * @throws TextParseException The input was invalid or not an unsigned 32
 * bit integer.
 * @throws IOException An I/O error occurred.
 */
public long
getUInt32() throws IOException {
	long l = getLong();
	if (l < 0 || l > 0xFFFFFFFFL)
		throw exception("expected an 32 bit unsigned integer");
	return l;
}

/**
 * Gets the next token from a tokenizer and converts it to an unsigned 16 bit
 * integer.
 * @return The next token in the stream, as an unsigned 16 bit integer.
 * @throws TextParseException The input was invalid or not an unsigned 16
 * bit integer.
 * @throws IOException An I/O error occurred.
 */
public int
getUInt16() throws IOException {
	long l = getLong();
	if (l < 0 || l > 0xFFFFL)
		throw exception("expected an 16 bit unsigned integer");
	return (int) l;
}

/**
 * Gets the next token from a tokenizer and converts it to an unsigned 8 bit
 * integer.
 * @return The next token in the stream, as an unsigned 8 bit integer.
 * @throws TextParseException The input was invalid or not an unsigned 8
 * bit integer.
 * @throws IOException An I/O error occurred.
 */
public int
getUInt8() throws IOException {
	long l = getLong();
	if (l < 0 || l > 0xFFL)
		throw exception("expected an 8 bit unsigned integer");
	return (int) l;
}

/**
 * Gets the next token from a tokenizer and parses it as a TTL.
 * @return The next token in the stream, as an unsigned 32 bit integer.
 * @throws TextParseException The input was not valid.
 * @throws IOException An I/O error occurred.
 * @see TTL
 */
public long
getTTL() throws IOException {
	String next = _getIdentifier("a TTL value");
	try {
		return TTL.parseTTL(next);
	}
	catch (NumberFormatException e) {
		throw exception("expected a TTL value");
	}
}

/**
 * Gets the next token from a tokenizer and parses it as if it were a TTL.
 * @return The next token in the stream, as an unsigned 32 bit integer.
 * @throws TextParseException The input was not valid.
 * @throws IOException An I/O error occurred.
 * @see TTL
 */
public long
getTTLLike() throws IOException {
	String next = _getIdentifier("a TTL-like value");
	try {
		return TTL.parse(next, false);
	}
	catch (NumberFormatException e) {
		throw exception("expected a TTL-like value");
	}
}

/**
 * Gets the next token from a tokenizer and converts it to a name.
 * @param origin The origin to append to relative names.
 * @return The next token in the stream, as a name.
 * @throws TextParseException The input was invalid or not a valid name.
 * @throws IOException An I/O error occurred.
 * @throws RelativeNameException The parsed name was relative, even with the
 * origin.
 * @see Name
 */
public Name
getName(Name origin) throws IOException {
	String next = _getIdentifier("a name");
	try {
		Name name = Name.fromString(next, origin);
		if (!name.isAbsolute())
			throw new RelativeNameException(name);
		return name;
	}
	catch (TextParseException e) {
		throw exception(e.getMessage());
	}
}

/**
 * Gets the next token from a tokenizer and converts it to an IP Address.
 * @param family The address family.
 * @return The next token in the stream, as an InetAddress
 * @throws TextParseException The input was invalid or not a valid address.
 * @throws IOException An I/O error occurred.
 * @see Address
 */
public InetAddress
getAddress(int family) throws IOException {
	String next = _getIdentifier("an address");
	try {
		return Address.getByAddress(next, family);
	}
	catch (UnknownHostException e) {
		throw exception(e.getMessage());
	}
}

/**
 * Gets the next token from a tokenizer, which must be an EOL or EOF.
 * @throws TextParseException The input was invalid or not an EOL or EOF token.
 * @throws IOException An I/O error occurred.
 */
public void
getEOL() throws IOException {
	Token next = get();
	if (next.type != EOL && next.type != EOF) {
		throw exception("expected EOL or EOF");
	}
}

/**
 * Returns a concatenation of the remaining strings from a Tokenizer.
 */
private String
remainingStrings() throws IOException {
        StringBuffer buffer = null;
        while (true) {
                Tokenizer.Token t = get();
                if (!t.isString())
                        break;
                if (buffer == null)
                        buffer = new StringBuffer();
                buffer.append(t.value);
        }
        unget();
        if (buffer == null)
                return null;
        return buffer.toString();
}

/**
 * Gets the remaining string tokens until an EOL/EOF is seen, concatenates
 * them together, and converts the base64 encoded data to a byte array.
 * @param required If true, an exception will be thrown if no strings remain;
 * otherwise null be be returned.
 * @return The byte array containing the decoded strings, or null if there
 * were no strings to decode.
 * @throws TextParseException The input was invalid.
 * @throws IOException An I/O error occurred.
 */
public byte []
getBase64(boolean required) throws IOException {
	String s = remainingStrings();
	if (s == null) {
		if (required)
			throw exception("expected base64 encoded string");
		else
			return null;
	}
	byte [] array = base64.fromString(s);
	if (array == null)
		throw exception("invalid base64 encoding");
	return array;
}

/**
 * Gets the remaining string tokens until an EOL/EOF is seen, concatenates
 * them together, and converts the base64 encoded data to a byte array.
 * @return The byte array containing the decoded strings, or null if there
 * were no strings to decode.
 * @throws TextParseException The input was invalid.
 * @throws IOException An I/O error occurred.
 */
public byte []
getBase64() throws IOException {
	return getBase64(false);
}

/**
 * Gets the remaining string tokens until an EOL/EOF is seen, concatenates
 * them together, and converts the hex encoded data to a byte array.
 * @param required If true, an exception will be thrown if no strings remain;
 * otherwise null be be returned.
 * @return The byte array containing the decoded strings, or null if there
 * were no strings to decode.
 * @throws TextParseException The input was invalid.
 * @throws IOException An I/O error occurred.
 */
public byte []
getHex(boolean required) throws IOException {
	String s = remainingStrings();
	if (s == null) {
		if (required)
			throw exception("expected hex encoded string");
		else
			return null;
	}
	byte [] array = base16.fromString(s);
	if (array == null)
		throw exception("invalid hex encoding");
	return array;
}

/**
 * Gets the remaining string tokens until an EOL/EOF is seen, concatenates
 * them together, and converts the hex encoded data to a byte array.
 * @return The byte array containing the decoded strings, or null if there
 * were no strings to decode.
 * @throws TextParseException The input was invalid.
 * @throws IOException An I/O error occurred.
 */
public byte []
getHex() throws IOException {
	return getHex(false);
}

/**
 * Gets the next token from a tokenizer and decodes it as hex.
 * @return The byte array containing the decoded string.
 * @throws TextParseException The input was invalid.
 * @throws IOException An I/O error occurred.
 */
public byte []
getHexString() throws IOException {
	String next = _getIdentifier("a hex string");
	byte [] array = base16.fromString(next);
	if (array == null)
		throw exception("invalid hex encoding");
	return array;
}

/**
 * Gets the next token from a tokenizer and decodes it as base32.
 * @param b32 The base32 context to decode with.
 * @return The byte array containing the decoded string.
 * @throws TextParseException The input was invalid.
 * @throws IOException An I/O error occurred.
 */
public byte []
getBase32String(base32 b32) throws IOException {
	String next = _getIdentifier("a base32 string");
	byte [] array = b32.fromString(next);
	if (array == null)
		throw exception("invalid base32 encoding");
	return array;
}

/**
 * Creates an exception which includes the current state in the error message
 * @param s The error message to include.
 * @return The exception to be thrown
 */
public TextParseException
exception(String s) {
	return new TokenizerException(filename, line, s);
}

/**
 * Closes any files opened by this tokenizer.
 */
public void
close() {
	if (wantClose) {
		try {
			is.close();
		}
		catch (IOException e) {
		}
	}
}

protected void
finalize() {
	close();
}

}
