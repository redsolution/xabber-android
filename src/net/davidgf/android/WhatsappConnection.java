
/*
 * Java WhatsApp API implementation.
 * Written by David Guillen Fandos (david@davidgf.net) based 
 * on the sources of WhatsAPI PHP implementation and whatsapp
 * for libpurple.
 *
 * Share and enjoy!
 *
 */


import java.util.*;

public class WhatsappConnection {


	private RC4Decoder in, out;
	private byte session_key[];
	private DataBuffer outbuffer;
	
	private enum SessionStatus { SessionNone, SessionConnecting, SessionWaitingChallenge, SessionWaitingAuthOK, SessionConnected };
	private SessionStatus conn_status;
	
	private String phone, password;
	private String whatsappserver;
	private String whatsappservergroup;


	public WhatsappConnection(String phone, String password, String nick) {
		session_key = new byte[20];
		
		this.phone = phone;
		this.password = password.trim();
		this.conn_status = SessionStatus.SessionNone;
		this.whatsappserver = "s.whatsapp.net";
		this.whatsappservergroup = "g.us";
	}

	public Tree read_tree(DataBuffer data) {
		int lsize = data.readListSize();
		int type = data.getInt(1,0);
		if (type == 1) {
			data.popData(1);
			Tree t = new Tree("start");
			t.readAttributes(data,lsize);
			return t;
		}else if (type == 2) {
			data.popData(1);
			return new Tree("treeerr"); // No data in this tree...
		}
	
		Tree t = new Tree(data.readString());
		t.readAttributes(data,lsize);
	
		if ((lsize & 1) == 1) {
			return t;
		}
	
		if (data.isList()) {
			t.setChildren(data.readList(this));
		}else{
			t.setData(data.readString().getBytes());
		}
	
		return t;
	}
	
	Tree parse_tree(DataBuffer data) {
		int bflag = (data.getInt(1,0) & 0xF0)>>4;
		int bsize = data.getInt(2,1);
		if (bsize > data.size()-3) {
			return new Tree("treeerr");  // Next message incomplete, return consumed data
		}
		data.popData(3);

		if ((bflag & 0x8) != 0) {
			// Decode data, buffer conversion
			if (this.in != null) {
				DataBuffer decoded_data = data.decodedBuffer(this.in,bsize,false);
			
				// Remove hash
				decoded_data.popData(4);
			
				// Call recursive
				data.popData(bsize); // Pop data unencrypted for next parsing!
				return read_tree(decoded_data);
			}else{
				data.popData(bsize);
				return new Tree("treeerr");
			}
		}else{
			return read_tree(data);
		}
	}
	
	public DataBuffer write_tree(Tree tree) {
		DataBuffer bout = new DataBuffer();
		int len = 1;
	
		if (tree.getAttributes().size() != 0) len += tree.getAttributes().size()*2;
		if (tree.getChildren().size() != 0) len++;
		if (tree.getData().length != 0 || tree.forcedData()) len++;
	
		bout.writeListSize(len);
		if (tree.getTag() == "start") bout.putInt(1,1);
		else bout.putString(tree.getTag());
		tree.writeAttributes(bout);
	
		if (tree.getData().length > 0 || tree.forcedData())
			bout.putRawString(tree.getData());
		if (tree.getChildren().size() > 0) {
			bout.writeListSize(tree.getChildren().size());
		
			for (int i = 0; i < tree.getChildren().size(); i++) {
				DataBuffer tt = write_tree(tree.getChildren().get(i));
				bout = bout.addBuf(tt);
			}
		}
		return bout;
	}
	
	public DataBuffer serialize_tree(Tree tree, boolean crypt) {
		DataBuffer data = write_tree(tree);
		int flag = 0;
		if (crypt) {
			data = data.encodedBuffer(this.out,this.session_key,true);
			flag = 0x10;
		}
		
		DataBuffer ret = new DataBuffer();
		ret.putInt(flag,1);
		ret.putInt(data.size(),2);
		return ret.addBuf(data);
	}
	
	
	public void doLogin(String useragent) {
		DataBuffer first = new DataBuffer();
	
		{
		Map < String,String > auth = new HashMap <String,String>();
		auth.put("resource", useragent);
		auth.put("to", whatsappserver);
		Tree t = new Tree("start",auth);
		first.addData( new byte [] {'W','A',1,2} );
		first = first.addBuf(serialize_tree(t,false));
		}

		// Send features
		{
		Tree p = new Tree("stream:features");
		p.addChild(new Tree("receipt_acks"));
		p.addChild(new Tree("w:profile:picture",new HashMap < String,String >() {{ put("type","all"); }} ));
		p.addChild(new Tree("w:profile:picture",new HashMap < String,String >() {{ put("type","group"); }} ));
		p.addChild(new Tree("notification",new HashMap < String,String >() {{ put("type","participant"); }} ));
		p.addChild(new Tree("status"));
		first = first.addBuf(serialize_tree(p,false));
		}
		
		// Send auth request
		{
		Map < String,String > auth = new HashMap <String,String>();
		auth.put("xmlns", "urn:ietf:params:xml:ns:xmpp-sasl");
		auth.put("mechanism", "WAUTH-1");
		auth.put("user", phone);
		Tree t = new Tree("auth",auth);
		t.forceDataWrite();
		first = first.addBuf(serialize_tree(t,false));
		}
	
		conn_status = SessionStatus.SessionWaitingChallenge;
		outbuffer = first;
	}
}


