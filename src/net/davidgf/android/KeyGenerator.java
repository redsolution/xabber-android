
package net.davidgf.android;

import java.security.*;
import java.util.*;
import javax.crypto.*;
import javax.crypto.spec.*;

public class KeyGenerator {

	public static byte [] PKCS5_PBKDF2_HMAC_SHA1(byte [] pass, byte [] salt) throws Exception {
		char [] password = new char[pass.length];
		for (int i = 0; i < pass.length; i++)
			password[i] = (char)(pass[i]&0xFF);
		
		SecretKeyFactory f = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
		PBEKeySpec ks = new PBEKeySpec(password,salt,2,20*8);  // 2 rounds, 20*8 bits output
		SecretKey s = f.generateSecret(ks);
		
		return s.getEncoded();
	}

	private static byte [] hexmap = new byte[]{ '0','1','2','3','4','5','6','7','8','9','a','b','c','d','e','f' };

	public static byte[][] generateKeyV14(String pw, byte [] salt) {
		try {
			byte [] decpass = MiscUtil.base64_decode(pw.getBytes());
			
			byte [][] keys = new byte[4][];
			for (int i = 0; i < 4; i++) {
				byte salt2 [] = Arrays.copyOf(salt,salt.length + 1);
				salt2[salt.length] = (byte)(i+1);
				keys[i] = PKCS5_PBKDF2_HMAC_SHA1 (decpass,salt2);
			}
			
			return keys;
		}
		catch (Exception e) {
			return new byte[0][0];
		}
	}
	public static byte [] calc_hmac(byte [] data, byte [] key, int seq) {
		byte [] dataext = Arrays.copyOf(data, data.length + 4);
		dataext[data.length +0] = (byte)(seq >> 24);
		dataext[data.length +1] = (byte)(seq >> 16);
		dataext[data.length +2] = (byte)(seq >>  8);
		dataext[data.length +3] = (byte)(seq      );
		
		byte [] hash = HMAC_SHA1 (dataext,key);
		
		return Arrays.copyOf(hash,4);
	}
	
	private static byte [] HMAC_SHA1(byte [] text, byte [] key) {
		try {
			SecretKeySpec signingKey = new SecretKeySpec(key, "HmacSHA1");
			Mac mac = Mac.getInstance("HmacSHA1");
			mac.init(signingKey);
			// Compute the hmac on input data bytes
			return mac.doFinal(text);
		} catch (Exception e) {
			return new byte[0];
		}
	}
	
};


