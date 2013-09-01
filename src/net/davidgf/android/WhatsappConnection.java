
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
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.RosterPacket;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smackx.packet.MessageEvent;
import org.jivesoftware.smackx.packet.Nick;
import org.jivesoftware.smackx.packet.MUCUser;
import org.jivesoftware.smackx.packet.DelayInformation;
import org.jivesoftware.smackx.packet.MUCInitialPresence;
import com.xabber.android.data.extension.muc.MUCManager;
import com.xabber.xmpp.vcard.VCard;
import com.xabber.xmpp.avatar.VCardUpdate;

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
	
	private Vector <Packet> received_packets;
	private Vector <Contact> contacts;
	private Map <String,Group> groups;
	boolean groups_updated;
	int gq_stat;
	int gw1,gw2;
	
	private int iqid;
	private String mymessage = "";
	private String account_name = null;

	public WhatsappConnection(String phone, String pass, String nick, String aname) {
		session_key = new byte[20];
		
		this.phone = phone;
		this.password = pass.trim();
		this.conn_status = SessionStatus.SessionNone;
		this.nickname = nick;
		this.mypresence = "available";
		outbuffer = new DataBuffer();
		received_packets = new Vector <Packet>();
		contacts = new Vector <Contact> ();
		iqid = 0;
		
		groups = new HashMap <String,Group> ();
		groups_updated = false;
		gq_stat = 0;
		gw1 = gw2 = 0;
		
		account_name = aname;
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
			if (!t.getTag().equals("treeerr"))
				this.processPacket(t);
				
			System.out.println(t.toString(0));
		} while (!t.getTag().equals("treeerr") && db.size() >= 3);
		
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
			this.updateGroups();
			
			// Resend contact status query (for already added contacts)
			for (int i = 0; i < contacts.size(); i++) {
				subscribePresence(contacts.get(i).phone);
				queryPreview(contacts.get(i).phone);
			}
		}
		else if (t.getTag().equals("presence")) {
			// Receives the presence of the user
			if ( t.hasAttribute("from") && t.hasAttribute("type") ) {
				Presence.Mode mode = Presence.Mode.away;
				if (t.getAttribute("type").equals("available"))
					mode = Presence.Mode.available;
				
				Presence presp = new Presence(Presence.Type.available);
				presp.setMode(mode);
				presp.setFrom(MiscUtil.getUser(t.getAttribute("from")));
				presp.setTo(MiscUtil.getUser(phone));
				received_packets.add(presp);
			}
		}
		else if (t.getTag().equals("iq")) {
			// PING
			if (t.hasAttribute("from") && t.hasAttribute("id") && t.hasChild("ping")) {
				this.doPong(t.getAttribute("id"),t.getAttribute("from"));
			}
			
			// Preview query
			if (t.hasAttributeValue("type","result") && t.hasAttribute("from")) {
				Tree tb = t.getChild("picture");
				if (tb != null) {
					if (tb.hasAttributeValue("type","preview"))
						this.addPreviewPicture(t.getAttribute("from"),tb.getData());
				}
			}
			
			// Group stuff
			Vector <Tree> childs = t.getChildren();
			int acc = 0;
			for (int j = 0; j < childs.size(); j++) {
				if (childs.get(j).getTag().equals("group")) {
					boolean rep = groups.containsKey(MiscUtil.getUser(childs.get(j).getAttribute("id")));
					if (!rep) {
						groups.put(
							MiscUtil.getUser(childs.get(j).getAttribute("id")),
							new Group(	MiscUtil.getUser(childs.get(j).getAttribute("id")),
									childs.get(j).getAttribute("subject"),
									MiscUtil.getUser(childs.get(j).getAttribute("owner")) )  );

						// Query group participants
						final String iid = String.valueOf(++iqid);
						final String pid = childs.get(j).getAttribute("id");
						final String subj = childs.get(j).getAttribute("subject");
						Tree iq = new Tree("list",new HashMap < String,String >(){{put("xmlns","w:g");}});
						Tree req = new Tree("iq", 
							new HashMap < String,String >(){{
								put("id",iid);
								put("type","get");
								put("to",pid+"@g.us");
								}});
						req.addChild(iq);
						outbuffer = outbuffer.addBuf(new DataBuffer(serialize_tree(req,true)));
						
						// Add group as a contact
						pushGroupUpdate();
					}
				}
				else if (childs.get(j).getTag().equals("participant")) {
					String gid = MiscUtil.getUser(t.getAttribute("from"));
					String pt = MiscUtil.getUser(childs.get(j).getAttribute("jid"));
					if (groups.containsKey(gid)) {
						groups.get(gid).participants.add(pt);
						
						pushGroupUpdate();
					}
				}
			}
			
			Tree tb = t.getChild("group");
			if (tb != null) {
				if (tb.hasAttributeValue("type","preview"))
					this.addPreviewPicture(t.getAttribute("from"),t.getData());
			}
		}
		else if (t.getTag().equals("message")) {
			if (t.hasAttributeValue("type","chat") && t.hasAttribute("from")) {
				long time = 0;
				if (t.hasAttribute("t"))
					time = Integer.parseInt(t.getAttribute("t"));
				String from = t.getAttribute("from");
				String id = t.getAttribute("id");
				String author = t.getAttribute("author");
				
				// Group nickname
				if (from.contains("@g.us") && t.hasChild("notify")) {
					author = t.getChild("notify").getAttribute("name");
					from = from + "/" + author;
				}
				
				Tree tb = t.getChild("body");
				if (tb != null) {
					// TODO: Add user here and at UI in case we don't have it
					this.receiveMessage(
					 	new ChatMessage(from,time,id,MiscUtil.bytesToUTF8(tb.getData()),author));
					addContact(from,false);
				}
				
				tb = t.getChild("media");
				if (tb != null) {
					// Photo/audio
					if (tb.hasAttributeValue("type","image")) {
						this.receiveMessage(
							new ImageMessage(from,time,id,tb.getAttribute("url"),tb.getData(),author));
						addContact(from,false);
					}
				}
			}
			if (t.hasChild("composing")) {
				gotTyping(t.getAttribute("from"),true);
			}
			if (t.hasChild("paused")) {
				gotTyping(t.getAttribute("from"),false);
			}
			
			// Received ACK
			if (t.hasAttribute("type") && t.hasAttribute("from")) {
				String answer = "received";
				if (t.hasChild("received"))
					answer = "ack";
				DataBuffer reply = generateResponse(t.getAttribute("from"),
									t.getAttribute("type"),
									t.getAttribute("id"),
									answer);
				outbuffer = outbuffer.addBuf(reply);

			}
		}
		/*else if (treelist[i].getTag() == "failure") {
			if (conn_status == SessionWaitingAuthOK)
				this->notifyError(errorAuth);
			else
				this->notifyError(errorUnknown);
		}*/
	}
	
	public void pushGroupUpdate() {
		for (Map.Entry <String, Group> entry : groups.entrySet()) {
			// Create room
			MUCManager.getInstance().createRoom(
				account_name, entry.getValue().id, phone, "", false, entry.getValue().subject);
			
			// Send the room status (chek MultiUserChat.java),matched the filter
			/*Presence.Mode mode = Presence.Mode.chat;
			Presence presp = new Presence(Presence.Type.available);
			presp.setMode(mode);
			presp.setFrom(entry.getValue().id + "/" + phone);
			presp.setTo(MiscUtil.getUser(phone));
			// Add MUC User
			MUCUser user = new MUCUser();
			user.setItem(new MUCUser.Item("member","participant"));
			presp.addExtension(user);
			received_packets.add(presp);*/
			
			for (int i = 0; i < entry.getValue().participants.size(); i++) {
				// Send the room status (chek MultiUserChat.java),matched the filter
				Presence.Mode mode = Presence.Mode.chat;
				Presence presp = new Presence(Presence.Type.available);
				presp.setMode(mode);
				presp.setFrom(entry.getValue().id + "/" + entry.getValue().participants.get(i));
				presp.setTo(MiscUtil.getUser(phone));
				// Add MUC User
				MUCUser user = new MUCUser();
				user.setItem(new MUCUser.Item("member","participant"));
				presp.addExtension(user);
				received_packets.add(presp);
			}
			
			//rr.addRosterItem(new RosterPacket.Item(, entry.getValue().subject));
		}

/*		// Add group as a contact
		RosterPacket rr = new RosterPacket();
		rr.setType(IQ.Type.SET);
		for (Map.Entry <String, Group> entry : groups.entrySet()) {
			rr.addRosterItem(new RosterPacket.Item(entry.getValue().id, entry.getValue().subject));
		}
		received_packets.add(rr);
		
		// Set group presence to "Chat"
		for (Map.Entry <String, Group> entry : groups.entrySet()) {
			Presence.Mode mode = Presence.Mode.chat;
			Presence presp = new Presence(Presence.Type.available);
			presp.setMode(mode);
			presp.setFrom(entry.getValue().id);
			presp.setTo(MiscUtil.getUser(phone));
			
			// MUC
			presp.addExtension(new MUCInitialPresence());
			
			received_packets.add(presp);
		}*/
	}
	
	private DataBuffer generateResponse(final String from, final String type, final String id, final String ans) {
		Tree received = new Tree(ans,new HashMap < String,String >() {{ put("xmlns","urn:xmpp:receipts"); }} );
		Tree mes = new Tree("message",new HashMap < String,String >() {{ 
			put("to",from); put("type",type); put("id",id); }} );
		mes.addChild(received);
		return serialize_tree(mes,true);
	}
	
	private void queryPreview(final String user) {
		final String fuser = user+"@"+whatsappserver;
		final String reqid = String.valueOf(++iqid);
		Tree pic = new Tree ("picture", 
			new HashMap < String,String >() {{ put("xmlns","w:profile:picture"); put("type","preview"); }} );
		Tree req = new Tree("iq", 
			new HashMap < String,String >() {{ put("id",reqid); put("type","get"); put("to",fuser); }} );

		req.addChild(pic);
		
		outbuffer = outbuffer.addBuf(new DataBuffer(serialize_tree(req,true)));
	}
	
	private void updateGroups() {
		groups.clear();
		{
			final String reqid = String.valueOf(++iqid);
			gw1 = iqid;
			Tree iq = new Tree("list",new HashMap < String,String >() {{ put("xmlns","w:g"); put("type","owning");}} );
			Tree req = new Tree("iq",
				new HashMap < String,String >() {{ put("id",reqid); put("type","get"); put("to","g.us");}} );
			
			req.addChild(iq);
			outbuffer = outbuffer.addBuf(new DataBuffer(serialize_tree(req,true)));
		}
		{
			final String reqid = String.valueOf(++iqid);
			gw2 = iqid;
			Tree iq = new Tree("list",
				new HashMap < String,String >() {{ put("xmlns","w:g"); put("type","participating");}} );
			Tree req = new Tree("iq",
				new HashMap < String,String >() {{ put("id",reqid); put("type","get"); put("to","g.us");}} );
			req.addChild(iq);
			outbuffer = outbuffer.addBuf(new DataBuffer(serialize_tree(req,true)));
		}
		gq_stat = 1;  // Queried the groups
	}

	private void manageParticipant(final String group, final String participant, final String command) {
		Tree part = new Tree("participant",new HashMap < String,String >() {{ put("jid",participant); }} );
		Tree iq = new Tree(command,new HashMap < String,String >() {{ put("xmlns","w:g"); }} );
		iq.addChild(part);
		final String reqid = String.valueOf(++iqid);
		Tree req = new Tree("iq",
			new HashMap < String,String >() {{ put("id",reqid); put("type","set"); put("to",group+"@g.us");}} );
		req.addChild(iq);
	
		outbuffer = outbuffer.addBuf(new DataBuffer(serialize_tree(req,true)));
	}
	
	private void leaveGroup(final String group) {
		Tree gr = new Tree("group", new HashMap < String,String >() {{ put("id",group+"@g.us"); }} );
		Tree iq = new Tree("leave", new HashMap < String,String >() {{ put("xmlns","w:g"); }} );
		iq.addChild(gr);
		final String reqid = String.valueOf(++iqid);
		Tree req = new Tree("iq",
			new HashMap < String,String >() {{ put("id",reqid); put("type","set"); put("to","g.us");}} );
		req.addChild(iq);
	
		outbuffer = outbuffer.addBuf(new DataBuffer(serialize_tree(req,true)));
	}

	void addGroup(final String subject) {
		Tree gr = new Tree("group", 
			new HashMap < String,String >() {{ put("xmlns","w:g"); put("action","create"); put("subject",subject);}} );
		final String reqid = String.valueOf(++iqid);
		Tree req = new Tree("iq",
			new HashMap < String,String >() {{ put("id",reqid); put("type","set"); put("to","g.us");}} );
		req.addChild(gr);
	
		outbuffer = outbuffer.addBuf(new DataBuffer(serialize_tree(req,true)));
	}

	public byte[] getUserAvatar(String user) {
		// Look for preview 
		user = MiscUtil.getUser(user);
		for (int i = 0; i < contacts.size(); i++) {
			if (contacts.get(i).phone.equals(user)) {
				return contacts.get(i).ppprev;
			}
		}
		return new byte[0];
	}

	private void addPreviewPicture(String user, byte [] picture) {
		user = MiscUtil.getUser(user);
		
		// Save preview 
		for (int i = 0; i < contacts.size(); i++) {
			if (contacts.get(i).phone.equals(user)) {
				contacts.get(i).ppprev = picture;
				break;
			}
		}
		
		VCardUpdate vc = new VCardUpdate();
		vc.setPhotoHash(MiscUtil.getEncodedSha1Sum(picture));
		Presence p = new Presence(Presence.Type.subscribed);
		p.setTo(phone);
		p.setFrom(user);
		p.addExtension(vc);
		received_packets.add(p);
	}
	
	private void receiveMessage(AbstractMessage msg) {
		received_packets.add(msg.serializePacket());
	}
	
	private void gotTyping(final String user, boolean typing) {
	        Message msg = new Message();
		msg.setTo(MiscUtil.getUser(phone));
		msg.setFrom(MiscUtil.getUser(user));

		// Create a MessageEvent Package and add it to the message
	        MessageEvent messageEvent = new MessageEvent();
	        messageEvent.setComposing(true);
        	msg.addExtension(messageEvent);
	        // Send the packet
        	received_packets.add(msg);
	}
	
	public Packet getNextPacket() {
		if (received_packets.size() == 0)
			return null;
		Packet r = received_packets.get(0);
		received_packets.remove(0);
		
		return r;
	}
	
	public void setMyPresence(String pres, String msg) {
		mypresence = pres;
		mymessage = msg;
		if (conn_status == SessionStatus.SessionConnected)
			notifyMyPresence();
	}
	
	private void notifyMyPresence() {
		// Send the nickname and the current status
		Tree pres = new Tree("presence", new HashMap < String,String >() {{ put("name",nickname); put("type",mypresence); }} );
		
		outbuffer = outbuffer.addBuf(new DataBuffer(serialize_tree(pres,true)));
		
		Tree xhash = new Tree("x", new HashMap < String,String >() {{ put("xmlns","jabber:x:event"); }} );
		xhash.addChild(new Tree("server"));
		Tree tbody = new Tree ("body"); tbody.setData(MiscUtil.UTF8ToBytes(this.mymessage));
		
		String stime = String.valueOf(System.currentTimeMillis()/1000);
		final String iqtime_id = stime + "-" + String.valueOf(++iqid);
		Tree mes = new Tree("message", new HashMap < String,String >() {{ 
			put("to","s.us"); put("type","chat"); put("id",iqtime_id); }} );
		mes.addChild(xhash); mes.addChild(tbody);

		//outbuffer = outbuffer.addBuf(new DataBuffer(serialize_tree(mes,true)));
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

	public void addContact(String user, boolean user_request) {
		user = MiscUtil.getUser(user);
		if (user.contains("-")) return; // Do not add groups as contacts
		
		boolean found = false;
		for (int i = 0; i < contacts.size(); i++)
			if (contacts.get(i).phone.equals(user)) {
				found = true;
				return;
			}
		
		if (!found) {
			Contact c = new Contact(user, user_request);
			contacts.add(c);
		}
		
		if (conn_status == SessionStatus.SessionConnected) {
			subscribePresence(user);
			queryPreview(user);
		}
	}
	
	public void subscribePresence(String user) {
		final String username = MiscUtil.getUser(user)+"@"+whatsappserver;
		Tree request = new Tree("presence",
			new HashMap < String,String >() {{ put("type","subscribe"); put("to",username); }} );
		
		outbuffer = outbuffer.addBuf(new DataBuffer(serialize_tree(request,true)));
		
		// Meanwhile add the user presence...
		Presence.Mode mode = Presence.Mode.away;
		Presence presp = new Presence(Presence.Type.available);
		presp.setMode(mode);
		presp.setFrom(MiscUtil.getUser(user));
		presp.setTo(MiscUtil.getUser(phone));
		received_packets.add(presp);
	}

	
	public byte [] getWriteData() {
		byte [] r = outbuffer.getPtr();
		System.out.println("Sending some bytes ... " + String.valueOf(r.length));
		outbuffer = new DataBuffer();
		return r;
	}
	
	// Helper for Message class
	public byte [] serializeMessage(final String to, String message, int id) {
		try {
		Tree request = new Tree ("request",new HashMap < String,String >() {{ put("xmlns","urn:xmpp:receipts"); }} );
		Tree notify  = new Tree ("notify", new HashMap < String,String >() {{ put("xmlns","urn:xmpp:whatsapp"); put("name",nickname); }} );
		Tree xhash   = new Tree ("x",      new HashMap < String,String >() {{ put("xmlns","jabber:x:event"); }} );
		xhash.addChild(new Tree("server"));
		Tree tbody = new Tree("body");
		tbody.setData(message.getBytes("UTF-8"));
		
		long epoch = System.currentTimeMillis()/1000;
		String stime = String.valueOf(epoch);
		Map < String,String > attrs = new HashMap <String,String>();
		String full_to = to + "@" + (to.contains("-") ? whatsappservergroup : whatsappserver);
		attrs.put("to",full_to);
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
	
	public abstract class AbstractMessage {
		protected String from, id, author;
		protected long time;
		
		public AbstractMessage(String from, long time, String id, String author) {
			this.from = from;
			this.id = id;
			this.author = author;
			this.time = time;
		}
		
		public abstract Packet serializePacket();
	}
	
	public class ChatMessage extends AbstractMessage {
		private String message;
		public ChatMessage(String from, long time, String id, String message, String author) {
			super(from, time, id, author);
			this.message = message;
		}
		
		public Packet serializePacket() {
			Message message = new Message();
			message.setTo(MiscUtil.getUser(phone));
			message.setFrom(MiscUtil.getUserAndResource(this.from));
			message.setType(Message.Type.chat);
			message.setBody(this.message);
			
			// XXX: Criteria for adding Delay info is
			// if the timestamp and the current time differ in more than 10 seconds
			DelayInformation d = new DelayInformation(new Date(time*1000));
			long epoch = System.currentTimeMillis()/1000;
			if (Math.abs(time - epoch) > 10)
				message.addExtension(d);
			
			return message;
		}
	}
	public class ImageMessage extends AbstractMessage {
		private String url;
		private byte [] preview;
		public ImageMessage(String from, long time, String id, String url, byte [] prev, String author) {
			super(from, time, id, author);
			this.url = url;
			this.preview = prev;
		}
		
		public Packet serializePacket() {
			Message message = new Message();
			message.setTo(MiscUtil.getUser(phone));
			message.setFrom(MiscUtil.getUserAndResource(this.from));
			message.setType(Message.Type.chat);
			message.setBody(url);
			
			if (author != null && author.length() != 0)
				message.addExtension(new Nick(author));
			
			// XXX: Criteria for adding Delay info is
			// if the timestamp and the current time differ in more than 10 seconds
			DelayInformation d = new DelayInformation(new Date(time*1000));
			long epoch = System.currentTimeMillis()/1000;
			if (Math.abs(time - epoch) > 10)
				message.addExtension(d);
			
			return message;
		}
	}
	
	
	public class Contact {
		String phone, name;
		String presence, typing;
		String status;
		long last_seen, last_status;
		boolean mycontact;
		byte[] ppprev, pppicture;
		boolean subscribed;

		Contact(String phone, boolean myc) {
			this.phone = phone;
			this.mycontact = myc;
			this.last_seen = 0;
			this.subscribed = false;
			this.typing = "paused";
			this.status = "";
		}
	};

	public class Group {
		String id, subject, owner;
		Vector <String> participants;

		Group(String id, String subject, String owner) {
			this.id = id;
			this.subject = subject;
			this.owner = owner;
			participants = new Vector <String> ();
		}
	};

}


