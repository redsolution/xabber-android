
package net.davidgf.android;

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
	
	
	private static String [] dictionary = new String[]
	{ "","","","","",  "account","ack","action","active","add","after",
	"ib","all","allow","apple","audio","auth","author","available","bad-protocol","bad-request",
	"before","Bell.caf","body","Boing.caf","cancel","category","challenge","chat","clean","code",
	"composing","config","conflict","contacts","count","create","creation","default","delay",
	"delete","delivered","deny","digest","DIGEST-MD5-1","DIGEST-MD5-2","dirty","elapsed","broadcast",
	"enable","encoding","duplicate","error","event","expiration","expired","fail","failure","false",
	"favorites","feature","features","field","first","free","from","g.us","get","Glass.caf","google",
	"group","groups","g_notify","g_sound","Harp.caf","http://etherx.jabber.org/streams",
	"http://jabber.org/protocol/chatstates","id","image","img","inactive","index","internal-server-error",
	"invalid-mechanism","ip","iq","item","item-not-found","user-not-found","jabber:iq:last","jabber:iq:privacy",
	"jabber:x:delay","jabber:x:event","jid","jid-malformed","kind","last","latitude","lc","leave","leave-all",
	"lg","list","location","longitude","max","max_groups","max_participants","max_subject","mechanism",
	"media","message","message_acks","method","microsoft","missing","modify","mute","name","nokia","none",
	"not-acceptable","not-allowed","not-authorized","notification","notify","off","offline","order","owner",
	"owning","paid","participant","participants","participating","password","paused","picture","pin","ping",
	"platform","pop_mean_time","pop_plus_minus","port","presence","preview","probe","proceed","prop","props",
	"p_o","p_t","query","raw","reason","receipt","receipt_acks","received","registration","relay",
	"remote-server-timeout","remove","Replaced by new connection","request","required","resource",
	"resource-constraint","response","result","retry","rim","s.whatsapp.net","s.us","seconds","server",
	"server-error","service-unavailable","set","show","sid","silent","sound","stamp","unsubscribe","stat",
	"status","stream:error","stream:features","subject","subscribe","success","sync","system-shutdown",
	"s_o","s_t","t","text","timeout","TimePassing.caf","timestamp","to","Tri-tone.caf","true","type",
	"unavailable","uri","url","urn:ietf:params:xml:ns:xmpp-sasl","urn:ietf:params:xml:ns:xmpp-stanzas",
	"urn:ietf:params:xml:ns:xmpp-streams","urn:xmpp:delay","urn:xmpp:ping","urn:xmpp:receipts",
	"urn:xmpp:whatsapp","urn:xmpp:whatsapp:account","urn:xmpp:whatsapp:dirty","urn:xmpp:whatsapp:mms",
	"urn:xmpp:whatsapp:push","user","username","value","vcard","version","video","w","w:g","w:p","w:p:r",
	"w:profile:picture","wait","x","xml-not-well-formed","xmlns","xmlns:stream","Xylophone.caf","1","WAUTH-1",
	"","","","","","","","","","","","XXX","","","","","","","" };
	
	public static String getDecoded(int n) {
		return new String(dictionary[n & 255]);
	}
	public static int lookupDecoded(String value) {
		for (int i = 0; i < 256; i++) {
			if (dictionary[i].equals(value))
				return i;
		}
		return 0;
	}

}


