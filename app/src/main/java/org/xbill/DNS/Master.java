// Copyright (c) 1999-2004 Brian Wellington (bwelling@xbill.org)

package org.xbill.DNS;

import java.io.*;
import java.util.*;

/**
 * A DNS master file parser.  This incrementally parses the file, returning
 * one record at a time.  When directives are seen, they are added to the
 * state and used when parsing future records.
 *
 * @author Brian Wellington
 */

public class Master {

private Name origin;
private File file;
private Record last = null;
private long defaultTTL;
private Master included = null;
private Tokenizer st;
private int currentType;
private int currentDClass;
private long currentTTL;
private boolean needSOATTL;

private Generator generator;
private List generators;
private boolean noExpandGenerate;

Master(File file, Name origin, long initialTTL) throws IOException {
	if (origin != null && !origin.isAbsolute()) {
		throw new RelativeNameException(origin);
	}
	this.file = file;
	st = new Tokenizer(file);
	this.origin = origin;
	defaultTTL = initialTTL;
}

/**
 * Initializes the master file reader and opens the specified master file.
 * @param filename The master file.
 * @param origin The initial origin to append to relative names.
 * @param ttl The initial default TTL.
 * @throws IOException The master file could not be opened.
 */
public
Master(String filename, Name origin, long ttl) throws IOException {
	this(new File(filename), origin, ttl);
}

/**
 * Initializes the master file reader and opens the specified master file.
 * @param filename The master file.
 * @param origin The initial origin to append to relative names.
 * @throws IOException The master file could not be opened.
 */
public
Master(String filename, Name origin) throws IOException {
	this(new File(filename), origin, -1);
}

/**
 * Initializes the master file reader and opens the specified master file.
 * @param filename The master file.
 * @throws IOException The master file could not be opened.
 */
public
Master(String filename) throws IOException {
	this(new File(filename), null, -1);
}

/**
 * Initializes the master file reader.
 * @param in The input stream containing a master file.
 * @param origin The initial origin to append to relative names.
 * @param ttl The initial default TTL.
 */
public
Master(InputStream in, Name origin, long ttl) {
	if (origin != null && !origin.isAbsolute()) {
		throw new RelativeNameException(origin);
	}
	st = new Tokenizer(in);
	this.origin = origin;
	defaultTTL = ttl;
}

/**
 * Initializes the master file reader.
 * @param in The input stream containing a master file.
 * @param origin The initial origin to append to relative names.
 */
public
Master(InputStream in, Name origin) {
	this(in, origin, -1);
}

/**
 * Initializes the master file reader.
 * @param in The input stream containing a master file.
 */
public
Master(InputStream in) {
	this(in, null, -1);
}

private Name
parseName(String s, Name origin) throws TextParseException {
	try {
		return Name.fromString(s, origin);
	}
	catch (TextParseException e) {
		throw st.exception(e.getMessage());
	}
}

private void
parseTTLClassAndType() throws IOException {
	String s;
	boolean seen_class = false;


	// This is a bit messy, since any of the following are legal:
	//   class ttl type
	//   ttl class type
	//   class type
	//   ttl type
	//   type
	seen_class = false;
	s = st.getString();
	if ((currentDClass = DClass.value(s)) >= 0) {
		s = st.getString();
		seen_class = true;
	}

	currentTTL = -1;
	try {
		currentTTL = TTL.parseTTL(s);
		s = st.getString();
	}
	catch (NumberFormatException e) {
		if (defaultTTL >= 0)
			currentTTL = defaultTTL;
		else if (last != null)
			currentTTL = last.getTTL();
	}

	if (!seen_class) {
		if ((currentDClass = DClass.value(s)) >= 0) {
			s = st.getString();
		} else {
			currentDClass = DClass.IN;
		}
	}

	if ((currentType = Type.value(s)) < 0)
		throw st.exception("Invalid type '" + s + "'");

	// BIND allows a missing TTL for the initial SOA record, and uses
	// the SOA minimum value.  If the SOA is not the first record,
	// this is an error.
	if (currentTTL < 0) {
		if (currentType != Type.SOA)
			throw st.exception("missing TTL");
		needSOATTL = true;
		currentTTL = 0;
	}
}

private long
parseUInt32(String s) {
	if (!Character.isDigit(s.charAt(0)))
		return -1;
	try {
		long l = Long.parseLong(s);
		if (l < 0 || l > 0xFFFFFFFFL)
			return -1;
		return l;
	}
	catch (NumberFormatException e) {
		return -1;
	}
}

private void
startGenerate() throws IOException {
	String s;
	int n;

	// The first field is of the form start-end[/step]
	// Regexes would be useful here.
	s = st.getIdentifier();
	n = s.indexOf("-");
	if (n < 0)
		throw st.exception("Invalid $GENERATE range specifier: " + s);
	String startstr = s.substring(0, n);
	String endstr = s.substring(n + 1);
	String stepstr = null;
	n = endstr.indexOf("/");
	if (n >= 0) {
		stepstr = endstr.substring(n + 1);
		endstr = endstr.substring(0, n);
	}
	long start = parseUInt32(startstr);
	long end = parseUInt32(endstr);
	long step;
	if (stepstr != null)
		step = parseUInt32(stepstr);
	else
		step = 1;
	if (start < 0 || end < 0 || start > end || step <= 0)
		throw st.exception("Invalid $GENERATE range specifier: " + s);

	// The next field is the name specification.
	String nameSpec = st.getIdentifier();

	// Then the ttl/class/type, in the same form as a normal record.
	// Only some types are supported.
	parseTTLClassAndType();
	if (!Generator.supportedType(currentType))
		throw st.exception("$GENERATE does not support " +
				   Type.string(currentType) + " records");

	// Next comes the rdata specification.
	String rdataSpec = st.getIdentifier();

	// That should be the end.  However, we don't want to move past the
	// line yet, so put back the EOL after reading it.
	st.getEOL();
	st.unget();

	generator = new Generator(start, end, step, nameSpec,
				  currentType, currentDClass, currentTTL,
				  rdataSpec, origin);
	if (generators == null)
		generators = new ArrayList(1);
	generators.add(generator);
}

private void
endGenerate() throws IOException {
	// Read the EOL that we put back before.
	st.getEOL();

	generator = null;
}

private Record
nextGenerated() throws IOException {
	try {
		return generator.nextRecord();
	}
	catch (Tokenizer.TokenizerException e) {
		throw st.exception("Parsing $GENERATE: " + e.getBaseMessage());
	}
	catch (TextParseException e) {
		throw st.exception("Parsing $GENERATE: " + e.getMessage());
	}
}

/**
 * Returns the next record in the master file.  This will process any
 * directives before the next record.
 * @return The next record.
 * @throws IOException The master file could not be read, or was syntactically
 * invalid.
 */
public Record
_nextRecord() throws IOException {
	Tokenizer.Token token;
	String s;

	if (included != null) {
		Record rec = included.nextRecord();
		if (rec != null)
			return rec;
		included = null;
	}
	if (generator != null) {
		Record rec = nextGenerated();
		if (rec != null)
			return rec;
		endGenerate();
	}
	while (true) {
		Name name;

		token = st.get(true, false);
		if (token.type == Tokenizer.WHITESPACE) {
			Tokenizer.Token next = st.get();
			if (next.type == Tokenizer.EOL)
				continue;
			else if (next.type == Tokenizer.EOF)
				return null;
			else
				st.unget();
			if (last == null)
				throw st.exception("no owner");
			name = last.getName();
		}
		else if (token.type == Tokenizer.EOL)
			continue;
		else if (token.type == Tokenizer.EOF)
			return null;
		else if (((String) token.value).charAt(0) == '$') {
			s = token.value;

			if (s.equalsIgnoreCase("$ORIGIN")) {
				origin = st.getName(Name.root);
				st.getEOL();
				continue;
			} else if (s.equalsIgnoreCase("$TTL")) {
				defaultTTL = st.getTTL();
				st.getEOL();
				continue;
			} else  if (s.equalsIgnoreCase("$INCLUDE")) {
				String filename = st.getString();
				File newfile;
				if (file != null) {
					String parent = file.getParent();
					newfile = new File(parent, filename);
				} else {
					newfile = new File(filename);
				}
				Name incorigin = origin;
				token = st.get();
				if (token.isString()) {
					incorigin = parseName(token.value,
							      Name.root);
					st.getEOL();
				}
				included = new Master(newfile, incorigin,
						      defaultTTL);
				/*
				 * If we continued, we wouldn't be looking in
				 * the new file.  Recursing works better.
				 */
				return nextRecord();
			} else  if (s.equalsIgnoreCase("$GENERATE")) {
				if (generator != null)
					throw new IllegalStateException
						("cannot nest $GENERATE");
				startGenerate();
				if (noExpandGenerate) {
					endGenerate();
					continue;
				}
				return nextGenerated();
			} else {
				throw st.exception("Invalid directive: " + s);
			}
		} else {
			s = token.value;
			name = parseName(s, origin);
			if (last != null && name.equals(last.getName())) {
				name = last.getName();
			}
		}

		parseTTLClassAndType();
		last = Record.fromString(name, currentType, currentDClass,
					 currentTTL, st, origin);
		if (needSOATTL) {
			long ttl = ((SOARecord)last).getMinimum();
			last.setTTL(ttl);
			defaultTTL = ttl;
			needSOATTL = false;
		}
		return last;
	}
}

/**
 * Returns the next record in the master file.  This will process any
 * directives before the next record.
 * @return The next record.
 * @throws IOException The master file could not be read, or was syntactically
 * invalid.
 */
public Record
nextRecord() throws IOException {
	Record rec = null;
	try {
		rec = _nextRecord();
	}
	finally {
		if (rec == null) {
			st.close();
		}
	}
	return rec;
}

/**
 * Specifies whether $GENERATE statements should be expanded.  Whether
 * expanded or not, the specifications for generated records are available
 * by calling {@link #generators}.  This must be called before a $GENERATE
 * statement is seen during iteration to have an effect.
 */
public void
expandGenerate(boolean wantExpand) {
	noExpandGenerate = !wantExpand;
}

/**
 * Returns an iterator over the generators specified in the master file; that
 * is, the parsed contents of $GENERATE statements.
 * @see Generator
 */
public Iterator
generators() {
	if (generators != null)
		return Collections.unmodifiableList(generators).iterator();
	else
		return Collections.EMPTY_LIST.iterator();
}

protected void
finalize() {
	st.close();
}

}
