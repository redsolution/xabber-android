// Copyright (c) 2004 Brian Wellington (bwelling@xbill.org)

package org.xbill.DNS;

import java.util.HashMap;

/**
 * A utility class for converting between numeric codes and mnemonics
 * for those codes.  Mnemonics are case insensitive.
 *
 * @author Brian Wellington
 */

class Mnemonic {

private static Integer cachedInts[] = new Integer[64];

static {
	for (int i = 0; i < cachedInts.length; i++) {
		cachedInts[i] = new Integer(i);
	}
}

/* Strings are case-sensitive. */
static final int CASE_SENSITIVE = 1;

/* Strings will be stored/searched for in uppercase. */
static final int CASE_UPPER = 2;

/* Strings will be stored/searched for in lowercase. */
static final int CASE_LOWER = 3;

private HashMap strings;
private HashMap values;
private String description;
private int wordcase;
private String prefix;
private int max;
private boolean numericok;

/**
 * Creates a new Mnemonic table.
 * @param description A short description of the mnemonic to use when
 * @param wordcase Whether to convert strings into uppercase, lowercase,
 * or leave them unchanged.
 * throwing exceptions.
 */
public
Mnemonic(String description, int wordcase) {
	this.description = description;
	this.wordcase = wordcase;
	strings = new HashMap();
	values = new HashMap();
	max = Integer.MAX_VALUE;
}

/** Sets the maximum numeric value */
public void
setMaximum(int max) {
	this.max = max;
}

/**
 * Sets the prefix to use when converting to and from values that don't
 * have mnemonics.
 */
public void
setPrefix(String prefix) {
	this.prefix = sanitize(prefix);
}

/**
 * Sets whether numeric values stored in strings are acceptable.
 */
public void
setNumericAllowed(boolean numeric) {
	this.numericok = numeric;
}

/**
 * Converts an int into a possibly cached Integer.
 */
public static Integer
toInteger(int val) {
	if (val >= 0 && val < cachedInts.length)
		return (cachedInts[val]);
	return new Integer(val);
}       

/**
 * Checks that a numeric value is within the range [0..max]
 */
public void
check(int val) {
	if (val < 0 || val > max) {
		throw new IllegalArgumentException(description + " " + val +
						   "is out of range");
	}
}

/* Converts a String to the correct case. */
private String
sanitize(String str) {
	if (wordcase == CASE_UPPER)
		return str.toUpperCase();
	else if (wordcase == CASE_LOWER)
		return str.toLowerCase();
	return str;
}

private int
parseNumeric(String s) {
	try {
		int val = Integer.parseInt(s);
		if (val >= 0 && val <= max)
			return val;
	}
	catch (NumberFormatException e) {
	}
	return -1;
}

/**
 * Defines the text representation of a numeric value.
 * @param val The numeric value
 * @param string The text string
 */
public void
add(int val, String str) {
	check(val);
	Integer value = toInteger(val);
	str = sanitize(str);
	strings.put(str, value);
	values.put(value, str);
}

/**
 * Defines an additional text representation of a numeric value.  This will
 * be used by getValue(), but not getText().
 * @param val The numeric value
 * @param string The text string
 */
public void
addAlias(int val, String str) {
	check(val);
	Integer value = toInteger(val);
	str = sanitize(str);
	strings.put(str, value);
}

/**
 * Copies all mnemonics from one table into another.
 * @param val The numeric value
 * @param string The text string
 * @throws IllegalArgumentException The wordcases of the Mnemonics do not
 * match.
 */
public void
addAll(Mnemonic source) {
	if (wordcase != source.wordcase)
		throw new IllegalArgumentException(source.description +
						   ": wordcases do not match");
	strings.putAll(source.strings);
	values.putAll(source.values);
}

/**
 * Gets the text mnemonic corresponding to a numeric value.
 * @param val The numeric value
 * @return The corresponding text mnemonic.
 */
public String
getText(int val) {
	check(val);
	String str = (String) values.get(toInteger(val));
	if (str != null)
		return str;
	str = Integer.toString(val);
	if (prefix != null)
		return prefix + str;
	return str;
}

/**
 * Gets the numeric value corresponding to a text mnemonic.
 * @param str The text mnemonic
 * @return The corresponding numeric value, or -1 if there is none
 */
public int
getValue(String str) {
	str = sanitize(str);
	Integer value = (Integer) strings.get(str);
	if (value != null) {
		return value.intValue();
	}
	if (prefix != null) {
		if (str.startsWith(prefix)) {
			int val = parseNumeric(str.substring(prefix.length()));
			if (val >= 0) {
				return val;
			}
		}
	}
	if (numericok) {
		return parseNumeric(str);
	}
	return -1;
}

}
