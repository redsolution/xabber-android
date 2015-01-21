// Copyright (c) 1999-2004 Brian Wellington (bwelling@xbill.org)

package org.xbill.DNS.utils;

/**
 * A routine to produce a nice looking hex dump
 *
 * @author Brian Wellington
 */

public class hexdump {

    private static final char[] hex = "0123456789ABCDEF".toCharArray();

    /**
     * Dumps a byte array into hex format.
     *
     * @param description If not null, a description of the data.
     * @param b           The data to be printed.
     * @param offset      The start of the data in the array.
     * @param length      The length of the data in the array.
     */
    public static String
    dump(String description, byte[] b, int offset, int length) {
        StringBuffer sb = new StringBuffer();

        sb.append(length + "b");
        if (description != null)
            sb.append(" (" + description + ")");
        sb.append(':');

        int prefixlen = sb.toString().length();
        prefixlen = (prefixlen + 8) & ~7;
        sb.append('\t');

        int perline = (80 - prefixlen) / 3;
        for (int i = 0; i < length; i++) {
            if (i != 0 && i % perline == 0) {
                sb.append('\n');
                for (int j = 0; j < prefixlen / 8; j++)
                    sb.append('\t');
            }
            int value = (int) (b[i + offset]) & 0xFF;
            sb.append(hex[(value >> 4)]);
            sb.append(hex[(value & 0xF)]);
            sb.append(' ');
        }
        sb.append('\n');
        return sb.toString();
    }

    public static String
    dump(String s, byte[] b) {
        return dump(s, b, 0, b.length);
    }

}
