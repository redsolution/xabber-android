
import java.security.*;
import java.util.*;

public class KeyGenerator {

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
		
			//PKCS5_PBKDF2_HMAC_SHA1 (hashhex,32,(unsigned char*)salt,saltlen,16,20,(unsigned char*)out);
		}
		catch (Exception e) {
			return new byte[0];
		}
	}
	public static byte[] generateKeyV2(String pw, byte [] salt) {
		try {
			byte [] decpass = MiscUtil.base64_decode(pw.getBytes());
		
			//PKCS5_PBKDF2_HMAC_SHA1 (dec.c_str(),20,(unsigned char*)salt,saltlen,16,20,(unsigned char*)out);
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
		
			//PKCS5_PBKDF2_HMAC_SHA1 (hashhex,32,(unsigned char*)salt,saltlen,16,20,(unsigned char*)out);
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
	
	private	static byte [] HMAC_SHA1(byte [] text, byte [] key) {
		try {
			byte [] AppendBuf1 = new byte [text.length+64];

			byte [] SHA1_Key = new byte[4096];
			for (int i = 0; i < 4096; i++)
				SHA1_Key[i] = 0;
		
			byte [] m_ipad = new byte[64];
			byte [] m_opad = new byte[64];
			for (int i = 0; i < 64; i++) {
				m_ipad[i] = 0x36;
				m_opad[i] = 0x5c;
			}
		
			if (key.length > 64) {
				byte [] t = MessageDigest.getInstance("SHA").digest(key);
				for (int i = 0; i < t.length; i++)
					SHA1_Key[i] = t[i];
			}
			else {
				for (int i = 0; i < key.length; i++)
					SHA1_Key[i] = key[i];
			}

			for (int i = 0; i < 64; i++)
				m_ipad[i] ^= SHA1_Key[i];              

			for (int i = 0; i < 64; i++)
				AppendBuf1[i] = m_ipad[i];
			for (int i = 0; i < text.length; i++)
				AppendBuf1[i+64] = text[i];

			byte [] szReport = MessageDigest.getInstance("SHA").digest(AppendBuf1);

			for (int j = 0; j < 64; j++)
				m_opad[j] ^= SHA1_Key[j];

			byte [] AppendBuf2 = new byte [4096];
			for (int i = 0; i < 64; i++)
				AppendBuf2[i] = m_opad[i];
			for (int i = 0; i < szReport.length; i++)
				AppendBuf2[i+64] = szReport[i];

			return MessageDigest.getInstance("SHA").digest(AppendBuf2);
		}
		catch (Exception e) {
			return new byte[0];
		}
	}
};


