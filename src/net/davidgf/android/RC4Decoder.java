
/*
 * Java WhatsApp API implementation
 * Written by David Guillen Fandos (david@davidgf.net) based 
 * on the sources of WhatsAPI PHP implementation and whatsapp
 * for libpurple.
 *
 * Share and enjoy!
 *
 */
 
package net.davidgf.android;

public class RC4Decoder {
	public int [] s;
	public int i,j;
	public void swap (int i, int j) {
		int t = s[i];
		s[i] = s[j];
		s[j] = t;
	}
	public RC4Decoder(byte [] key, int drop) {
		s = new int[256];
		for (int k = 0; k < 256; k++) s[k] = k;
		i = j = 0;
		do {
			int k = key[i % key.length] & 0xFF;
			j = (j + k + s[i]) & 0xFF;
			swap(i,j);
			i = (i+1) & 0xFF;
		} while (i != 0);
		i = j = 0;
		
		byte [] temp = new byte[drop];
		for (int k = 0; k < drop; k++)
			temp[k] = (byte)0;
		cipher(temp);
	}
	
	public byte [] cipher (byte [] data) {
		byte [] out = new byte[data.length];
		for (int c = 0; c < data.length; c++) {
			i = (i+1) & 0xFF;
			j = (j + s[i]) & 0xFF;
			swap(i,j);
			int idx = (s[i]+s[j]) & 0xFF;
			out[c] = (byte)((data[c] ^ s[idx]) & 0xFF);
		}
		return out;
	}
};


