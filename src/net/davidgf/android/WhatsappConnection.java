
/*
 * Java WhatsApp API implementation.
 * Written by David Guillen Fandos (david@davidgf.net) based 
 * on the sources of WhatsAPI PHP implementation and whatsapp
 * for libpurple.
 *
 * Share and enjoy!
 *
 */

package net.davidgf.android;

import java.util.*;

public class WhatsappConnection {
	private RC4Decoder in, out;
	private byte session_key[];
	private DataBuffer outbuffer;
	private byte []challenge_data;
	
	private enum SessionStatus { SessionNone, SessionConnecting, SessionWaitingChallenge, SessionWaitingAuthOK, SessionConnected };
	private SessionStatus conn_status;
	
	private String phone, password, nickname;
	private static String whatsappserver = "s.whatsapp.net";
	private static String whatsappservergroup = "g.us";
	
	private String account_type, account_status, account_expiration, account_creation;
	private String mypresence;

	public WhatsappConnection(String phone, String pass, String nick) {
		session_key = new byte[20];
		
		this.phone = phone;
		this.password = pass.trim();
		this.conn_status = SessionStatus.SessionNone;
		this.nickname = nick;
		this.mypresence = "available";
		outbuffer = new DataBuffer();
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
			t.setData(data.readByteString());
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
	
	public int pushIncomingData(byte [] data) {
		if (data.length < 3) return 0;
		
		DataBuffer db = new DataBuffer(data);
		
		Tree t;
		do {
			t = this.parse_tree(db);
			if (t.getTag() != "treeerr")
				this.processPacket(t);
				
			System.out.println("Received tree!\n");
			System.out.println(t.toString(0));
		} while (t.getTag() != "treeerr" && db.size() >= 3);
		
		return data.length - db.size();
	}
	
	public boolean isConnected() {
		return conn_status == SessionStatus.SessionConnected;
	}
	
	private void processPacket(Tree t) {
		// Now process the tree list!
		if (t.getTag().equals("challenge")) {
			// Generate a session key using the challege & the password
			assert(conn_status == SessionStatus.SessionWaitingChallenge);
			
			if (password.length() == 15) {
				session_key = KeyGenerator.generateKeyImei(password,t.getData());
			}
			else if (password.contains(":")) {
				session_key = KeyGenerator.generateKeyMAC(password,t.getData());
			}
			else {
				session_key = KeyGenerator.generateKeyV2(password,t.getData());
			}
			
			this.in  = new RC4Decoder(session_key, 256);
			this.out = new RC4Decoder(session_key, 256);
			
			conn_status = SessionStatus.SessionWaitingAuthOK;
			challenge_data = t.getData();
			
			this.sendAuthResponse();
		}
		else if (t.getTag().equals("success")) {
			// Notifies the success of the auth
			conn_status = SessionStatus.SessionConnected;
			if (t.hasAttribute("status"))
				this.account_status = t.getAttributes().get("status");
			if (t.hasAttribute("kind"))
				this.account_type = t.getAttributes().get("kind");
			if (t.hasAttribute("expiration"))
				this.account_expiration = t.getAttributes().get("expiration");
			if (t.hasAttribute("creation"))
				this.account_creation = t.getAttributes().get("creation");
				
			this.notifyMyPresence();
			//this->sendInitial();
			//this->updateGroups();
			
			//std::cout << "Logged in!!!" << std::endl;
			//std::cout << "Account " << phone << " status: " << account_status << " kind: " << account_type <<
			//	" expires: " << account_expiration << " creation: " << account_creation << std::endl;
		}
		else if (t.getTag().equals("iq")) {
			if (t.hasAttribute("from") && t.hasAttribute("id") && t.hasChild("ping")) {
				this.doPong(t.getAttribute("id"),t.getAttribute("from"));
			}
		}
		/*else if (treelist[i].getTag() == "failure") {
			if (conn_status == SessionWaitingAuthOK)
				this->notifyError(errorAuth);
			else
				this->notifyError(errorUnknown);
		}*/
	}
	
	private void notifyMyPresence() {
		// Send the nickname and the current status
		Tree pres = new Tree("presence", new HashMap < String,String >() {{ put("name",nickname); put("type",mypresence); }} );
	
		outbuffer = outbuffer.addBuf(new DataBuffer(serialize_tree(pres,true)));
	}

	void doPong(final String id, final String from) {
		Tree t = new Tree("iq",new HashMap < String,String >() {{ put("to",from); put("id",id); put("type","result"); }} );
		outbuffer = outbuffer.addBuf(new DataBuffer(serialize_tree(t,true)));
	}
	
	public DataBuffer write_tree(Tree tree) {
		DataBuffer bout = new DataBuffer();
		int len = 1;
	
		if (tree.getAttributes().size() != 0) len += tree.getAttributes().size()*2;
		if (tree.getChildren().size() != 0) len++;
		if (tree.getData().length != 0 || tree.forcedData()) len++;
	
		bout.writeListSize(len);
		if (tree.getTag().equals("start")) bout.putInt(1,1);
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
	
	void sendAuthResponse() {
		Tree t = new Tree("response",new HashMap < String,String >() {{ put("xmlns","urn:ietf:params:xml:ns:xmpp-sasl"); }});
	
		long epoch = System.currentTimeMillis()/1000;
		String stime = String.valueOf(epoch);
		DataBuffer eresponse = new DataBuffer();
		eresponse.addData(phone.getBytes());
		eresponse.addData(challenge_data);
		eresponse.addData(stime.getBytes());
		
		eresponse = eresponse.encodedBuffer(this.out,this.session_key,false);
		t.setData(eresponse.getPtr());

		outbuffer = outbuffer.addBuf(new DataBuffer(serialize_tree(t,false)));
	}

	
	public byte [] getWriteData() {
		byte [] r = outbuffer.getPtr();
		System.out.println("retrieveing data to send " + String.valueOf(r.length));
		outbuffer = new DataBuffer();
		return r;
	}
	
	// Helper for Message class
	public byte [] serializeMessage(final String to, String message, int id) {
		try {
		Tree request = new Tree ("request",new HashMap < String,String >() {{ put("xmlns","urn:xmpp:receipts"); }} );
		Tree notify  = new Tree ("notify", new HashMap < String,String >() {{ put("xmlns","urn:xmpp:whatsapp"); put("name",to); }} );
		Tree xhash   = new Tree ("x",      new HashMap < String,String >() {{ put("xmlns","jabber:x:event"); }} );
		xhash.addChild(new Tree("server"));
		Tree tbody = new Tree("body");
		tbody.setData(message.getBytes("UTF-8"));
		
		long epoch = System.currentTimeMillis()/1000;
		String stime = String.valueOf(epoch);
		Map < String,String > attrs = new HashMap <String,String>();
		attrs.put("to",to+"@"+whatsappserver);
		attrs.put("type","chat");
		attrs.put("id",stime+"-"+String.valueOf(id));
		attrs.put("t",stime);
		
		Tree mes = new Tree("message",attrs);
		mes.addChild(xhash); mes.addChild(notify);
		mes.addChild(request); mes.addChild(tbody);
		
		return serialize_tree(mes,true).getPtr();
		}catch (Exception e) {
		return new byte[0];
		}
	}
}


