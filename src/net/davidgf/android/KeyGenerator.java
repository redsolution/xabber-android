
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
		PBEKeySpec ks = new PBEKeySpec(password,salt,16,20*8);
		SecretKey s = f.generateSecret(ks);
		
		return s.getEncoded();
	}

	private static byte [] hexmap = new byte[]{ '0','1','2','3','4','5','6','7','8','9','a','b','c','d','e','f' };

	public static byte [] generateKeyImei(String imei, byte [] salt) {
		try {
			String imeir = (new StringBuilder(imei)).reverse().toString();
		
			byte [] hash = MessageDigest.getInstance("MD5").digest(imeir.getBytes());
		
			byte [] hashhex = new byte[32];
			for (int i = 0; i < 16; i++) {
				hashhex[2*i] = hexmap[(hash[i]>>4)&0xF];
				hashhex[2*i+1] = hexmap[hash[i]&0xF];
			}
		
			return PKCS5_PBKDF2_HMAC_SHA1 (hashhex,salt);
		}
		catch (Exception e) {
			return new byte[0];
		}
	}
	public static byte[] generateKeyV2(String pw, byte [] salt) {
		try {
			byte [] decpass = MiscUtil.base64_decode(pw.getBytes());
			
			return PKCS5_PBKDF2_HMAC_SHA1 (decpass,salt);
		}
		catch (Exception e) {
			return new byte[0];
		}
	}
	public static byte [] generateKeyMAC(String macaddr, byte [] salt) {
		try {
			macaddr = macaddr+macaddr;
		
			byte [] hash = MessageDigest.getInstance("MD5").digest(macaddr.getBytes());
		
			byte [] hashhex = new byte[32];
			for (int i = 0; i < 16; i++) {
				hashhex[2*i] = hexmap[(hash[i]>>4)&0xF];
				hashhex[2*i+1] = hexmap[hash[i]&0xF];
			}
		
			return PKCS5_PBKDF2_HMAC_SHA1 (hashhex,salt);
		}
		catch (Exception e) {
			return new byte[0];
		}
	}
	public static byte [] calc_hmac(byte [] data, byte [] key) {
		byte [] hash = HMAC_SHA1 (data,key);
		byte [] ret = new byte[4];
		for (int i = 0; i < 4; i++)
			ret[i] = hash[i];
		return ret;
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


