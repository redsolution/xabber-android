
/*
 * JAVA WhatsApp API implementation
 * Written by David Guillen Fandos (david@davidgf.net) based 
 * on the sources of WhatsAPI PHP implementation and whatsapp
 * for libpurple.
 * Updated to WA protocol v1.4
 *
 * Share and enjoy!
 *
 */

package net.davidgf.android;

import java.util.*;

public class DataBuffer {
	private byte [] buffer;

	public DataBuffer (byte [] buf) {
		buffer = new byte[buf.length];
		for (int c = 0; c < buf.length; c++)
			buffer[c] = buf[c];
	}
	public DataBuffer () {
		buffer = new byte[0];
	}
	public DataBuffer (DataBuffer other) {
		buffer = new byte[other.buffer.length];
		for (int c = 0; c < other.buffer.length; c++)
			buffer[c] = other.buffer[c];
	}
	
	DataBuffer addBuf(DataBuffer other) {
		DataBuffer ret = new DataBuffer();
		ret.buffer = new byte[this.buffer.length + other.buffer.length];
		
		for (int c = 0; c < this.buffer.length; c++)
			ret.buffer[c] = this.buffer[c];
		for (int c = 0; c < other.buffer.length; c++)
			ret.buffer[c+this.buffer.length] = other.buffer[c];
			
		return ret;
	}
	DataBuffer decodedBuffer(RC4Decoder decoder, int clength) {
		byte [] carray, array4;
		carray = decoder.cipher(Arrays.copyOfRange(this.buffer,0,clength-4));
		array4 = Arrays.copyOfRange(this.buffer,clength-4,clength);
		DataBuffer deco = new DataBuffer(carray);
		DataBuffer extra = new DataBuffer(array4);
		return deco.addBuf(extra);
	}
	DataBuffer encodedBuffer(RC4Decoder decoder, byte [] key, boolean dout, int seq) {
		DataBuffer deco = new DataBuffer(Arrays.copyOfRange(this.buffer,0,this.buffer.length));
		
		deco.buffer = decoder.cipher(deco.buffer);
		byte [] hmacint = KeyGenerator.calc_hmac(deco.buffer,key,seq);
		DataBuffer hmac = new DataBuffer(hmacint);
		
		DataBuffer res;
		if (dout)
			return deco.addBuf(hmac);
		else
			return hmac.addBuf(deco);
	}
	byte [] getPtr() {
		return buffer;
	}
	void addData(byte [] dta) {
		byte [] newbuf = new byte[buffer.length + dta.length];
		for (int c = 0; c < buffer.length; c++)
			newbuf[c] = buffer[c];
		for (int c = 0; c < dta.length; c++)
			newbuf[c+buffer.length] = dta[c];
		
		this.buffer = newbuf;
	}
	void popData(int size) {
		if (buffer.length >= size) {
			buffer = Arrays.copyOfRange(buffer,size,buffer.length);
		}
	}
	void crunchData(int size) {
		if (buffer.length >= size) {
			byte [] newbuf = new byte[buffer.length - size];
			for (int c = 0; c < newbuf.length; c++)
				newbuf[c] = buffer[c];
			
			this.buffer = newbuf;
		}
	}
	int getInt(int nbytes, int offset) {
		//if (nbytes > blen)
		//	throw 0;
		int ret = 0;
		for (int i = 0; i < nbytes; i++) {
			ret <<= 8;
			ret |= (int)(buffer[i+offset] & 0xFF);
		}
		return ret;
	}
	void putInt(int value, int nbytes) {
		//assert(nbytes > 0);
		
		byte [] out = new byte[nbytes];
		for (int i = 0; i < nbytes; i++) {
			out[nbytes-i-1] = (byte)((value>>(i<<3)) & 0xFF);
		}
		this.addData(out);
	}
	int readInt(int nbytes) {
		//if (nbytes > blen)
		//	throw 0;
		int ret = getInt(nbytes,0);
		popData(nbytes);
		return ret;
	}
	int readListSize() {
		//if (blen == 0)
		//	throw 0;
		int ret;
		if (buffer[0] == (byte)0xf8 || buffer[0] == (byte)0xf3) {
			ret = (int)buffer[1];
			popData(2);
		}
		else if (buffer[0] == (byte)0xf9) {
			ret = getInt(2,1);
			popData(3);
		}
		else {
			// FIXME throw 0 error
			ret = -1;
			//printf("Parse error!!\n");
		}
		return ret;
	}
	void writeListSize(int size) {
		if (size == 0) {
			putInt(0,1);
		}
		else if (size < 256) {
			putInt(0xf8,1);
			putInt(size,1);
		}
		else {
			putInt(0xf9,1);
			putInt(size,2);
		}
	}
	String readRawString(int size) {
		//if (size < 0 or size > blen)
		//	throw 0;
		String s = new String(buffer,0,size);
		popData(size);
		return s;
	}
	byte [] readRawByteString(int size) {
		byte [] r = Arrays.copyOf(buffer,size);
		popData(size);
		return r;
	}
	byte [] readByteString() {
		int type = readInt(1);
		if (type > 2 && type <= 236) {
			String ret = MiscUtil.getDecoded(type);
			if (ret == null)
				ret = MiscUtil.getDecodedExt(type, readInt(1));
			return ret.getBytes();
		}
		else if (type == 252) {
			int slen = readInt(1);
			return readRawByteString(slen);
		}
		else if (type == 253) {
			int slen = readInt(3);
			return readRawByteString(slen);
		}
		else if (type == 250) {
			String u = readString();
			String s = readString();
			
			if (u.length() > 0 && s.length() > 0)
				return (u + "@" + s).getBytes();
			else if (s.length() > 0)
				return s.getBytes();
		}
		return new byte [0];
	}
	String readString() {
		//if (blen == 0)
		//	throw 0;
		int type = readInt(1);
		if (type > 2 && type <= 236) {
			String ret = MiscUtil.getDecoded(type);
			if (ret == null)
				ret = MiscUtil.getDecodedExt(type, readInt(1));
			return ret;
		}
		else if (type == 0) {
			return "";
		}
		else if (type == 252) {
			int slen = readInt(1);
			return readRawString(slen);
		}
		else if (type == 253) {
			int slen = readInt(3);
			return readRawString(slen);
		}
		else if (type == 250) {
			String u = readString();
			String s = readString();
			
			if (u.length() > 0 && s.length() > 0)
				return u + "@" + s;
			else if (s.length() > 0)
				return s;
			return "";
		}
		return "";
	}
	void putRawString(byte [] s) {
		if (s.length < 256) {
			putInt(0xfc,1);
			putInt(s.length,1);
			addData(s);
		}
		else {
			putInt(0xfd,1);
			putInt(s.length,3);
			addData(s);
		}
	}
	void putString(String s) {
		int lu = MiscUtil.lookupDecoded(s);
		int sub_dict = (lu >> 8);
		
		if (sub_dict != 0)
			putInt(sub_dict + 236 - 1, 1);   // Put dict byte first!
		
		if (lu != 0) {
			putInt(lu,1);
		}
		else if (s.indexOf('@') >= 0) {
			String p1 = s.substring(0,s.indexOf('@'));
			String p2 = s.substring(s.indexOf('@')+1);
			putInt(0xfa,1);
			putString(p1);
			putString(p2);
		}
		else if (s.length() < 256) {
			putInt(0xfc,1);
			putInt(s.length(),1);
			addData(s.getBytes());
		}
		else {
			putInt(0xfd,1);
			putInt(s.length(),3);
			addData(s.getBytes());
		}
	}
	boolean isList() {
		//if (blen == 0)
		//	throw 0;
		return (buffer[0] == (byte)248 || buffer[0] == (byte)0 || buffer[0] == (byte)249);
	}
	Vector <Tree> readList(WhatsappConnection c) {
		Vector <Tree> l = new Vector<Tree>();
		int size = readListSize();
		while (size-- > 0) {
			l.add(c.read_tree(this));
		}
		return l;
	}
	int size() {
		return buffer.length;
	}
	byte [] ToString() {
		byte [] bb = new byte[buffer.length];
		for (int c = 0; c < buffer.length; c++)
			bb[c] = buffer[c];
		return bb;
	}
};


