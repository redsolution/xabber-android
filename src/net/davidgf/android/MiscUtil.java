
package net.davidgf.android;

import java.security.MessageDigest;
import java.math.BigInteger;
import java.util.*;

public class MiscUtil {

	private static byte [] base64_chars = new byte []{'A','B','C','D','E','F','G','H','I','J','K','L',
	'M','N','O','P','Q','R','S','T','U','V','W','X','Y','Z','a','b','c','d','e','f','g','h','i','j','k',
	'l','m','n','o','p','q','r','s','t','u','v','w','x','y','z','0','1','2','3','4','5','6','7','8','9','+','/'};

	private static boolean is_base64(byte cc) {
		for (int c = 0; c < base64_chars.length; c++)
			if (base64_chars[c] == cc)
				return true;
		return false;
	}
	
	private static byte findarray(byte what) {
		for (int c = 0; c < base64_chars.length; c++)
			if (base64_chars[c] == what)
				return (byte)c;
		return 0;
	}
	
	private static byte [] concat(byte [] array, byte c) {
		byte [] bb = new byte[array.length+1];
		for (int i = 0; i < array.length; i++)
			bb[i] = array[i];
		
		bb[array.length] = c;
		
		return bb;
	}
	
	public static byte [] concat(byte [] array, byte [] array2) {
		byte [] ret = Arrays.copyOf(array, array.length + array2.length);
		System.arraycopy(array2,0, ret,ret.length, array2.length);
		return ret;
	}
	
	public static byte [] base64_decode(byte [] encoded_string) {
		int in_len = encoded_string.length;
		int i = 0;
		int j = 0;
		int in_ = 0;
		byte [] char_array_4 = new byte [4];
		byte [] char_array_3 = new byte [3];
		byte [] ret = new byte[0];

		while (in_len-- > 0 && ( encoded_string[in_] != '=') && is_base64(encoded_string[in_])) {
			char_array_4[i++] = encoded_string[in_]; in_++;
			if (i ==4) {
				for (i = 0; i <4; i++)
					char_array_4[i] = findarray(char_array_4[i]);

				char_array_3[0] = (byte)((char_array_4[0] << 2) + ((char_array_4[1] & 0x30) >>> 4));
				char_array_3[1] = (byte)(((char_array_4[1] & 0xf) << 4) + ((char_array_4[2] & 0x3c) >>> 2));
				char_array_3[2] = (byte)(((char_array_4[2] & 0x3) << 6) + char_array_4[3]);

				for (i = 0; (i < 3); i++)
					ret = concat(ret,char_array_3[i]);
				i = 0;
			}
		}

		if (i != 0) {
			for (j = i; j <4; j++)
				char_array_4[j] = 0;

			for (j = 0; j <4; j++)
				char_array_4[j] = findarray(char_array_4[j]);

			char_array_3[0] = (byte)((char_array_4[0] << 2) + ((char_array_4[1] & 0x30) >>> 4));
			char_array_3[1] = (byte)(((char_array_4[1] & 0xf) << 4) + ((char_array_4[2] & 0x3c) >>> 2));
			char_array_3[2] = (byte)(((char_array_4[2] & 0x3) << 6) + char_array_4[3]);

			for (j = 0; (j < i - 1); j++)
				ret = concat(ret,char_array_3[j]);
		}

		return ret;
	}
	
	
	private static String [] dictionary = new String[] {
	"", "", "", "account", "ack", "action", "active", "add", "after", "all", "allow", "apple",
	"auth", "author", "available", "bad-protocol", "bad-request", "before", "body", "broadcast",
	"cancel", "category", "challenge", "chat", "clean", "code", "composing", "config", "contacts",
	"count", "create", "creation", "debug", "default", "delete", "delivery", "delta", "deny",
	"digest", "dirty", "duplicate", "elapsed", "enable", "encoding", "error", "event",
	"expiration", "expired", "fail", "failure", "false", "favorites", "feature", "features",
	"feature-not-implemented", "field", "first", "free", "from", "g.us", "get", "google", "group",
	"groups", "http://etherx.jabber.org/streams", "http://jabber.org/protocol/chatstates", "ib",
	"id", "image", "img", "index", "internal-server-error", "ip", "iq", "item-not-found", "item",
	"jabber:iq:last", "jabber:iq:privacy", "jabber:x:event", "jid", "kind", "last", "leave",
	"list", "max", "mechanism", "media", "message_acks", "message", "method", "microsoft",
	"missing", "modify", "mute", "name", "nokia", "none", "not-acceptable", "not-allowed",
	"not-authorized", "notification", "notify", "off", "offline", "order", "owner", "owning",
	"p_o", "p_t", "paid", "participant", "participants", "participating", "paused", "picture",
	"pin", "ping", "platform", "port", "presence", "preview", "probe", "prop", "props", "query",
	"raw", "read", "reason", "receipt", "received", "relay", "remote-server-timeout", "remove",
	"request", "required", "resource-constraint", "resource", "response", "result", "retry",
	"rim", "s_o", "s_t", "s.us", "s.whatsapp.net", "seconds", "server-error", "server",
	"service-unavailable", "set", "show", "silent", "stat", "status", "stream:error",
	"stream:features", "subject", "subscribe", "success", "sync", "t", "text", "timeout",
	"timestamp", "to", "true", "type", "unavailable", "unsubscribe", "uri", "url",
	"urn:ietf:params:xml:ns:xmpp-sasl", "urn:ietf:params:xml:ns:xmpp-stanzas",
	"urn:ietf:params:xml:ns:xmpp-streams", "urn:xmpp:ping", "urn:xmpp:receipts",
	"urn:xmpp:whatsapp:account", "urn:xmpp:whatsapp:dirty", "urn:xmpp:whatsapp:mms",
	"urn:xmpp:whatsapp:push", "urn:xmpp:whatsapp", "user", "user-not-found", "value",
	"version", "w:g", "w:p:r", "w:p", "w:profile:picture", "w", "wait", "WAUTH-2",
	"x", "xmlns:stream", "xmlns", "1", "chatstate", "crypto", "enc", "class", "off_cnt",
	"w:g2", "promote", "demote", "creator"
	};

	private static String [] dictionary2 = new String[] {
	"Bell.caf", "Boing.caf", "Glass.caf", "Harp.caf", "TimePassing.caf", "Tri-tone.caf",
	"Xylophone.caf", "background", "backoff", "chunked", "context", "full", "in", "interactive",
	"out", "registration", "sid", "urn:xmpp:whatsapp:sync", "flt", "s16", "u8", "adpcm",
	"amrnb", "amrwb", "mp3", "pcm", "qcelp", "wma", "h263", "h264", "jpeg", "mpeg4", "wmv",
	"audio/3gpp", "audio/aac", "audio/amr", "audio/mp4", "audio/mpeg", "audio/ogg", "audio/qcelp",
	"audio/wav", "audio/webm", "audio/x-caf", "audio/x-ms-wma", "image/gif", "image/jpeg",
	"image/png", "video/3gpp", "video/avi", "video/mp4", "video/mpeg", "video/quicktime",
	"video/x-flv", "video/x-ms-asf", "302", "400", "401", "402", "403", "404", "405", "406",
	"407", "409", "500", "501", "503", "504", "abitrate", "acodec", "app_uptime", "asampfmt",
	"asampfreq", "audio", "bb_db", "clear", "conflict", "conn_no_nna", "cost", "currency",
	"duration", "extend", "file", "fps", "g_notify", "g_sound", "gcm", "google_play", "hash",
	"height", "invalid", "jid-malformed", "latitude", "lc", "lg", "live", "location", "log",
	"longitude", "max_groups", "max_participants", "max_subject", "mimetype", "mode",
	"napi_version", "normalize", "orighash", "origin", "passive", "password", "played",
	"policy-violation", "pop_mean_time", "pop_plus_minus", "price", "pricing", "redeem",
	"Replaced by new connection", "resume", "signature", "size", "sound", "source",
	"system-shutdown", "username", "vbitrate", "vcard", "vcodec", "video", "width",
	"xml-not-well-formed", "checkmarks", "image_max_edge", "image_max_kbytes", "image_quality",
	"ka", "ka_grow", "ka_shrink", "newmedia", "library", "caption", "forward", "c0", "c1", "c2",
	"c3", "clock_skew", "cts", "k0", "k1", "login_rtt", "m_id", "nna_msg_rtt", "nna_no_off_count",
	"nna_offline_ratio", "nna_push_rtt", "no_nna_con_count", "off_msg_rtt", "on_msg_rtt",
	"stat_name", "sts", "suspect_conn", "lists", "self", "qr", "web", "w:b", "recipient",
	"w:stats", "forbidden", "aurora.m4r", "bamboo.m4r", "chord.m4r", "circles.m4r", "complete.m4r",
	"hello.m4r", "input.m4r", "keys.m4r", "note.m4r", "popcorn.m4r", "pulse.m4r", "synth.m4r",
	"filehash"
	};

	
	public static String getDecoded(int n) {
		if (n < 236)
			return new String(dictionary[n & 255]);
		return null;
	}
	public static String getDecodedExt(int n, int n2) {
		if (n == 236)
			return new String(dictionary2[n2 & 255]);
		return "";
	}
	public static int lookupDecoded(String value) {
		for (int i = 0; i < dictionary.length; i++) {
			if (dictionary[i].equals(value))
				return i;
		}
		for (int i = 0; i < dictionary2.length; i++) {
			if (dictionary2[i].equals(value))
				return i | 0x100;
		}
		return 0;
	}
	
	public static String bytesToUTF8(byte [] ba) {
		try {
			return new String(ba, "UTF-8");
		}
		catch (Exception e) {
			return new String();
		}
	}
	
	public static byte [] UTF8ToBytes(String st) {
		try {
			return st.getBytes("UTF-8");
		}
		catch (Exception e) {
			return new byte [0];
		}
	}
	
	public static String getUser(String user) {
		return user.split("@")[0];
	}
	
	public static String getUserAndResource(String user) {
		String [] re = user.split("/");
		return user.split("@")[0] + "/" + re[re.length-1];
	}

	public static String getEncodedSha1Sum( byte [] data ) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA1");
			md.update(data);
			return new BigInteger(1, md.digest()).toString(16);
		}
		catch (Exception e) {
			return new String("");
		}
	}

	public static byte [] md5raw( byte [] data ) {
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			md.update(data);
			return md.digest();
		}
		catch (Exception e) {
			return new byte[0];
		}
	}
	public static String md5hex( byte [] data ) {
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			md.update(data);
			return new BigInteger(1, md.digest()).toString(16);
		}
		catch (Exception e) {
			return new String("");
		}
	}


}


