
/*
 * WhatsApp API extension for smack-xabber
 * Written by David Guillen Fandos (david@davidgf.net) based 
 * on the sources of WhatsAPI PHP implementation and whatsapp
 * for libpurple.
 *
 * Share and enjoy!
 *
 */

package org.jivesoftware.smack;

import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.XMPPError;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Registration;
import org.jivesoftware.smack.packet.RosterPacket;
import org.jivesoftware.smack.packet.IQ;
import com.xabber.xmpp.vcard.VCard;
import com.xabber.xmpp.vcard.VCardProperty;
import com.xabber.xmpp.vcard.BinaryPhoto;
import com.xabber.android.data.connection.ConnectionThread;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.connection.ConnectionItem;

import net.davidgf.android.WhatsappConnection;
import net.davidgf.android.WAContacts;

import org.apache.harmony.javax.security.auth.callback.Callback;
import org.apache.harmony.javax.security.auth.callback.CallbackHandler;
import org.apache.harmony.javax.security.auth.callback.PasswordCallback;
import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyStore;
import java.security.Provider;
import java.security.SecureRandom;
import java.security.Security;
import java.util.*;
import java.util.concurrent.*;

/**
 * Creates a socket connection to a WA server.
 * 
 * @see Connection
 * @author David Guillen Fandos
 */
public class WAConnection extends Connection {

    private ConnectionThread cthread;
    /**
     * The socket which is used for this connection.
     */
    protected Socket socket;

    String connectionID = null;
    private String user = null;
    private boolean connected = false;
    private boolean waconnected = false;
    private String account_name = null;
    /**
     * Flag that indicates if the user is currently authenticated with the server.
     */
    private boolean authenticated = false;

    private OutputStream ostream;
    private InputStream istream;
    
    private Thread writerThread, readerThread;
    
    byte [] inbuffer;
    byte [] outbuffer;
    byte [] outbuffer_mutex;
    WhatsappConnection waconnection;
    Semaphore readwait,writewait;
    
    private ExecutorService listenerExecutor;
    int msgid;
    
    private static final String waUA = "Android-2.11.151-443";

    private String nickname = "";
    Roster roster = null;

    /**
     * Creates a new connection to the specified XMPP server. A DNS SRV lookup will be
     * performed to determine the IP address and port corresponding to the
     * service name; if that lookup fails, it's assumed that server resides at
     * <tt>serviceName</tt> with the default port of 443.
     * This is the simplest constructor for connecting to an WA server. Alternatively,
     * you can get fine-grained control over connection settings using the
     * {@link #WAConnection(ConnectionConfiguration)} constructor.<p>
     * <p/>
     * Note that WAConnection constructors do not establish a connection to the server
     * and you must call {@link #connect()}.<p>
     * <p/>
     * The CallbackHandler is ignored.
     *
     * @param serviceName the name of the WA server to connect to; e.g. <tt>example.com</tt>.
     * @param callbackHandler ignored and should be set to null
     */
    public WAConnection(ConnectionThread ct, String serviceName, CallbackHandler callbackHandler) {
        // Create the configuration for this new connection
        super(new ConnectionConfiguration(serviceName));
        config.setCompressionEnabled(false);
        config.setSASLAuthenticationEnabled(true);
        config.setDebuggerEnabled(DEBUG_ENABLED);
        this.cthread = ct;
        commonInit();
    }

    /**
     * Creates a new WA connection in the same way {@link #WAConnection(String,CallbackHandler)} does, but
     * with no callback handler for password prompting of the keystore.
     *
     * @param serviceName the name of the WA server to connect to; e.g. <tt>example.com</tt>.
     */
    public WAConnection(ConnectionThread ct, String serviceName) {
        // Create the configuration for this new connection
        super(new ConnectionConfiguration(serviceName));
        config.setCompressionEnabled(false);
        config.setSASLAuthenticationEnabled(true);
        config.setDebuggerEnabled(DEBUG_ENABLED);
        this.cthread = ct;
        commonInit();
    }

    /**
     * Creates a new WA connection in the same way {@link #WAConnection(ConnectionConfiguration,CallbackHandler)} does, but
     * with no callback handler for password prompting of the keystore.
     *
     * @param config the connection configuration.
     */
    public WAConnection(ConnectionThread ct, ConnectionConfiguration config) {
        super(config);
        this.cthread = ct;
        commonInit();
    }

    public WAConnection(ConnectionThread ct, ConnectionConfiguration config, CallbackHandler callbackHandler) {
        super(config);
        config.setCallbackHandler(callbackHandler);
        this.cthread = ct;
        commonInit();
    }
    
    private void commonInit() {
        outbuffer_mutex = new byte[1];
        readwait = new Semaphore(0);
        writewait = new Semaphore(0);
        
	this.listenerExecutor = Executors.newSingleThreadExecutor(new ThreadFactory() {
		public Thread newThread(Runnable runnable) {
			Thread thread = new Thread(runnable,
			"WA Listener Processor");
			thread.setDaemon(true);
			return thread;
		}
	});
	
	ConnectionItem citem = cthread.getConnectionItem();
	AccountItem aitem = ((AccountItem)citem);
	account_name = aitem.getAccount();
	
	nickname = aitem.getConnectionSettings().getResource();
    }

    public String getConnectionID() {
        if (!isConnected()) {
            return null;
        }
        return connectionID;
    }

    public String getUser() {
        if (!isAuthenticated()) {
            return null;
        }
        return user;
    }

    @Override
    public synchronized void login(String username, String password, String resource) throws XMPPException {
        if (!isConnected()) {
            throw new IllegalStateException("Not connected to server.");
        }
        if (authenticated) {
            throw new IllegalStateException("Already logged in to server.");
        }
        // Do partial version of nameprep on the username.
        username = username.toLowerCase().trim();

        this.user = username + "@" + getServiceName();
        this.user += "/" + resource;

        // Indicate that we're now authenticated.
        authenticated = true;

		if (config.isRosterLoadedAtLogin()) {
			// Create the roster if it is not a reconnection or roster already
			// created by getRoster()
			if (this.roster == null) {
				if (rosterStorage == null) {
					this.roster = new Roster(this);
				} else {
					this.roster = new Roster(this, rosterStorage);
				}
        	}
            this.roster.reload();
        }

        // If debugging is enabled, change the the debug window title to include the
        // name we are now logged-in as.
        // If DEBUG_ENABLED was set to true AFTER the connection was created the debugger
        // will be null
        if (config.isDebuggerEnabled() && debugger != null) {
            debugger.userHasLogged(user);
        }
        
        // Start login!
        synchronized (waconnection) {
        	try {
		        waconnection.doLogin(waUA);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
        popWriteData();
    }

    @Override
    public synchronized void loginAnonymously() throws XMPPException {
        // No anonymous login supported by WA
    }

    public Roster getRoster() {
        // synchronize against login()
        synchronized(this) {
            // if connection is authenticated the roster is already set by login() 
            // or a previous call to getRoster()
            if (!isAuthenticated()) {
                if (roster == null) {
                    roster = new Roster(this);
                }
                return roster;
            }
        }

        if (!config.isRosterLoadedAtLogin()) {
			if (roster == null) {
				roster = new Roster(this);
			}
            roster.reload();
        }
        // If this is the first time the user has asked for the roster after calling
        // login, we want to wait for the server to send back the user's roster. This
        // behavior shields API users from having to worry about the fact that roster
        // operations are asynchronous, although they'll still have to listen for
        // changes to the roster. Note: because of this waiting logic, internal
        // Smack code should be wary about calling the getRoster method, and may need to
        // access the roster object directly.
        if (!roster.rosterInitialized) {
            try {
                synchronized (roster) {
                    long waitTime = SmackConfiguration.getPacketReplyTimeout();
                    long start = System.currentTimeMillis();
                    while (!roster.rosterInitialized) {
                        if (waitTime <= 0) {
                            break;
                        }
                        roster.wait(waitTime);
                        long now = System.currentTimeMillis();
                        waitTime -= now - start;
                        start = now;
                    }
                }
            }
            catch (InterruptedException ie) {
                // Ignore.
            }
        }
        return roster;
    }

    /**
     * Returns roster immediately without waiting for initialization.
     *
     * @return the user's roster, or <tt>null</tt> if the user has not logged in
     *      or has not received roster yet.
     */
    public Roster getRosterImmediately() {
        return roster;
    }

    public boolean isConnected() {
        return connected;
    }

    /**
     *
     * Returns true if the connection to the server is using legacy SSL or has successfully
     * negotiated TLS. Once TLS has been negotiatied the connection has been secured. @see #isUsingTLS. @see #isUsingSSL.
     *
     * @return true if a secure connection to the server.
     */
    public boolean isSecureConnection() {
        return false;
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    public boolean isAnonymous() {
        return false;
    }

    /**
     * Closes the connection by setting presence to unavailable then closing the stream to
     * the XMPP server. The shutdown logic will be used during a planned disconnection or when
     * dealing with an unexpected disconnection. Unlike {@link #disconnect()} the connection's
     * packet reader, packet writer, and {@link Roster} will not be removed; thus
     * connection's state is kept.
     *
     * @param unavailablePresence the presence packet to send during shutdown.
     */
    protected void shutdown(Presence unavailablePresence) {
        // Close connection
        istream = null;
        ostream = null;
        writerThread = null;
        readerThread = null;
        outbuffer = null;
        inbuffer = null;

	// Set status
        authenticated = false;
        connected = false;
        waconnected = false;
        
	// Socket close
        try {
            if (socket != null)
                socket.close();
        }
        catch (Exception e) {
            // Ignore.
        }
        socket = null;
    }

    public void disconnect(Presence unavailablePresence) {
        shutdown(unavailablePresence);

        if (roster != null) {
            roster.cleanup();
            roster = null;
        }
    }

    public void sendPacket(Packet packet) {
        this.firePacketInterceptors(packet);
            
    	// If the packet if a Message, serialize and send it!
	if (packet instanceof Message) {
		Message m = (Message)packet;
		synchronized (outbuffer_mutex) {
			msgid++;
			byte [] msg = waconnection.serializeMessage(m.getTo(), m.getBody(null), msgid);

			// Put data in the output buffer
    			outbuffer = Arrays.copyOf(outbuffer, outbuffer.length + msg.length);
    			System.arraycopy(msg,0, outbuffer,outbuffer.length-msg.length, msg.length);
			writewait.release();
		}
	}
	if (packet instanceof Registration) {
		Registration r = (Registration)packet;
		System.out.println(r.getChildElementXML());
	}
	if (packet instanceof VCard) {
		// The request for a VCard has been queued,  now we should provide the Avatar!
		packet.setFrom(packet.getTo());
		VCard vc = (VCard)packet;
		
		// Last seen at notes
		vc.getProperties().put(VCardProperty.NOTE,waconnection.getNotes(packet.getFrom()));

		BinaryPhoto bp = new BinaryPhoto();
		bp.setData(waconnection.getUserAvatar(packet.getFrom()));
		vc.getPhotos().add(bp);
		submitPacket(packet);
	}
	if (packet instanceof Presence) {
		// Sending our presence :) 
		String status = "unavailable";
		Presence pres = (Presence)packet;
		if (pres.getMode() == Presence.Mode.chat || pres.getMode() == Presence.Mode.available)
			status = "available";
			
		waconnection.setMyPresence(status, pres.getStatus());
	}
	if (packet instanceof RosterPacket) {
		RosterPacket r = (RosterPacket)packet;
		// Check Add/Remove Contact/Group:
		if (r.getType() == IQ.Type.SET) {
			Collection<RosterPacket.Item> items = r.getRosterItems();
			if (items.size() == 1) {
				RosterPacket.Item it = ((RosterPacket.Item)(items.toArray()[0]));
				if (it.getItemType() == RosterPacket.ItemType.remove) {
					// Bypasss roster send/rcv for WA, as we do not store them in the server
					submitPacket(r);
					// Remove fromm storage
					WAContacts.getInstance().removeContact(config.getUsername(), it.getUser());
				}else{
					// Adding contact, notify underlying connection for status query
					waconnection.addContact(it.getUser(),true);
					// Bypasss roster send/rcv for WA, as we do not store them in the server
					submitPacket(r);
					// Add the contact to the storage
					WAContacts.getInstance().addContact(config.getUsername(), it.getUser(), it.getName());
				}
			}
		}
		// Query contacts!
		if (r.getType() == IQ.Type.GET) {
			RosterPacket rr = new RosterPacket();
			rr.setType(IQ.Type.SET);
			
			// Add saved contacts!
			Vector < Vector < String > > saved_contacts = WAContacts.getInstance().getContacts(config.getUsername());
			for (int i = 0; i < saved_contacts.size(); i++) {
				String user = saved_contacts.get(i).get(0);
				String name = saved_contacts.get(i).get(1);
				rr.addRosterItem(new RosterPacket.Item(user, name));
				// Subscribe presence
				waconnection.addContact(user,true);
			}

			submitPacket(rr);
			waconnection.pushGroupUpdate();
		}
		
		System.out.println(r.toXML());
	}

	// Notify others
	this.firePacketSendingListeners(packet);
	
	// DO stuff again
	processLoop();
    }
    
    private void popWriteData() {
    	// Fill outbuffer with fresh data ready to be written
    	byte [] data;
    	synchronized (waconnection) {
	    	data = waconnection.getWriteData();
    	}
	synchronized (outbuffer_mutex) {
		outbuffer = Arrays.copyOf(outbuffer, outbuffer.length + data.length);
		System.arraycopy(data,0, outbuffer,outbuffer.length-data.length, data.length);
		writewait.release();
	}
    }

    /**
     * Registers a packet interceptor with this connection. The interceptor will be
     * invoked every time a packet is about to be sent by this connection. Interceptors
     * may modify the packet to be sent. A packet filter determines which packets
     * will be delivered to the interceptor.
     *
     * @param packetInterceptor the packet interceptor to notify of packets about to be sent.
     * @param packetFilter      the packet filter to use.
     * @deprecated replaced by {@link Connection#addPacketInterceptor(PacketInterceptor, PacketFilter)}.
     */
    public void addPacketWriterInterceptor(PacketInterceptor packetInterceptor,
            PacketFilter packetFilter) {
        addPacketInterceptor(packetInterceptor, packetFilter);
    }

    /**
     * Removes a packet interceptor.
     *
     * @param packetInterceptor the packet interceptor to remove.
     * @deprecated replaced by {@link Connection#removePacketInterceptor(PacketInterceptor)}.
     */
    public void removePacketWriterInterceptor(PacketInterceptor packetInterceptor) {
        removePacketInterceptor(packetInterceptor);
    }

    /**
     * Registers a packet listener with this connection. The listener will be
     * notified of every packet that this connection sends. A packet filter determines
     * which packets will be delivered to the listener. Note that the thread
     * that writes packets will be used to invoke the listeners. Therefore, each
     * packet listener should complete all operations quickly or use a different
     * thread for processing.
     *
     * @param packetListener the packet listener to notify of sent packets.
     * @param packetFilter   the packet filter to use.
     * @deprecated replaced by {@link #addPacketSendingListener(PacketListener, PacketFilter)}.
     */
    public void addPacketWriterListener(PacketListener packetListener, PacketFilter packetFilter) {
        addPacketSendingListener(packetListener, packetFilter);
    }

    /**
     * Removes a packet listener for sending packets from this connection.
     *
     * @param packetListener the packet listener to remove.
     * @deprecated replaced by {@link #removePacketSendingListener(PacketListener)}.
     */
    public void removePacketWriterListener(PacketListener packetListener) {
        removePacketSendingListener(packetListener);
    }

    private void connectUsingConfiguration(ConnectionConfiguration config) throws XMPPException {
        String host = config.getHost();
        int port = config.getPort();

	// Create WA connection API object                
        // FIXME: Set proper nickname
        msgid = 0;
        waconnection = new WhatsappConnection(config.getUsername(), config.getPassword(), this.nickname, account_name);

        try {
            if (config.getSocketFactory() == null) {
                this.socket = new Socket(host, port);
            }
            else {
                this.socket = config.getSocketFactory().createSocket(host, port);
            }
        }
        catch (UnknownHostException uhe) {
            String errorMessage = "Could not connect to " + host + ":" + port + ".";
            throw new XMPPException(errorMessage, new XMPPError(
                    XMPPError.Condition.remote_server_timeout, errorMessage),
                    uhe);
        }
        catch (IOException ioe) {
            String errorMessage = "XMPPError connecting to " + host + ":"
                    + port + ".";
            throw new XMPPException(errorMessage, new XMPPError(
                    XMPPError.Condition.remote_server_error, errorMessage), ioe);
        }
        System.out.println("Connected to " + host + " " + String.valueOf(port));
        initConnection();
        connected = true;
    }

    /**
     * Initializes the connection by creating a packet reader and writer and opening a
     * XMPP stream to the server.
     *
     * @throws XMPPException if establishing a connection to the server fails.
     */
    private void initConnection() throws XMPPException {
        // Set the stream reader/writer
        initReaderAndWriter();
        outbuffer = new byte[0];
        inbuffer = new byte[0];
        
        // Spawn reader and writer threads
        writerThread = new Thread() {
            public void run() {
                writePackets(this,ostream);
            }
        };
        writerThread.setName("Socket data writer");
        writerThread.setDaemon(true);
        writerThread.start();
        
        readerThread = new Thread() {
            public void run() {
                readPackets(this,istream);
            }
        };
        readerThread.setName("Socket data reader");
        readerThread.setDaemon(true);
        readerThread.start();

        // Make note of the fact that we're now connected.
        connected = true;

        for (ConnectionCreationListener listener : getConnectionCreationListeners()) {
            listener.connectionCreated(this);
        }
    }

    private void initReaderAndWriter() throws XMPPException {
        try {
                istream = socket.getInputStream();
                ostream = socket.getOutputStream();
        }
        catch (IOException ioe) {
            throw new XMPPException(
                    "XMPPError establishing connection with server.",
                    new XMPPError(XMPPError.Condition.remote_server_error,
                            "XMPPError establishing connection with server."),
                    ioe);
        }

	//reader = new AliveReader(reader);
    }

    public boolean isUsingCompression() {
        return false;
    }
    
    private void writePackets(Thread thisThread, OutputStream ostream) {
	try {
	while (outbuffer != null && ostream == this.ostream) {
		if (outbuffer.length > 0) {
			// Try to write the whole buffer
			System.out.println("Writing packets...\n");
			byte [] t;
			synchronized (outbuffer_mutex) {
				t = Arrays.copyOf(outbuffer,outbuffer.length);
			}
			ostream.write(t,0,t.length);
			synchronized (outbuffer_mutex) {
				// Pop the written data (the outbuffer may grow while writing t
				outbuffer = Arrays.copyOfRange(outbuffer,t.length,outbuffer.length);
			}
		}
		System.out.println("Sleeping while no packets are availbles...\n");
		writewait.acquire();
	}
	}catch (Exception e) {
		System.out.println("Error!\n" + e.toString());
	}
	System.out.println("Exiting writepackets thread (WA)\n");
    }
    
    private void submitPacket(Packet p) {
	System.out.println("Received message!\n");
	for (PacketCollector collector: getPacketCollectors()) {
		collector.processPacket(p);
	}
	listenerExecutor.submit(new ListenerNotification(p));
    }
    
    private void readPackets(Thread thisThread, InputStream istream) {
    	try {
    		int r;
	    	do {
	    		System.out.println("Reading packets...\n");
	    		byte [] buf = new byte[1024];
	    		r = istream.read(buf,0,buf.length);
	    		if (r > 0) {
		    		synchronized (inbuffer) {
		    			// Extend the array size and add the new bytes
		    			inbuffer = Arrays.copyOf(inbuffer, inbuffer.length + r);
		    			System.arraycopy(buf,0, inbuffer,inbuffer.length-r, r);
		    		}
		    	}
		    	
		    	processLoop();
	    	} while (r >= 0);
	}catch (IOException e) {
		System.out.println("Error!\n" + e.toString());
	}
	System.out.println("Exiting readpackets thread (WA)\n");
	disconnect(new Presence(Presence.Type.unavailable));
	cthread.connectionClosed();
	// Signal the writer thread so it can also end
	writewait.release();
    }
    
    private void processLoop() {
    	// Proceed to push data to underlying connection class
    	if (inbuffer == null) return;
    	
    	synchronized ( waconnection ) {
    		synchronized (inbuffer) {
    			int used = waconnection.pushIncomingData(inbuffer);
    			inbuffer = Arrays.copyOfRange(inbuffer, used, inbuffer.length);
    		}
    	}
    	
    	// Process stuff
    	this.popWriteData();  // Ready data might be waiting ...
    	
    	if (waconnection.isConnected() && !waconnected) {
    		connectionOK();  // Notify the connection status
    		waconnected = true;
    	}
    	
	if (listenerExecutor == null) {
		System.out.println("Null!!! Shit\n");
	}
    	
    	Packet p;
    	synchronized (waconnection) {
	    	p = waconnection.getNextPacket();
	}
	while (p != null) {
		submitPacket(p);
		synchronized (waconnection) {
			p = waconnection.getNextPacket();
		}
	}
    }

    /**
     * Establishes a connection to the XMPP server and performs an automatic login
     * only if the previous connection state was logged (authenticated). It basically
     * creates and maintains a socket connection to the server.<p>
     * <p/>
     * Listeners will be preserved from a previous connection if the reconnection
     * occurs after an abrupt termination.
     *
     * @throws XMPPException if an error occurs while trying to establish the connection.
     *      Two possible errors can occur which will be wrapped by an XMPPException --
     *      UnknownHostException (XMPP error code 504), and IOException (XMPP error code
     *      502). The error codes and wrapped exceptions can be used to present more
     *      appropiate error messages to end-users.
     */
     
     // Specify pass at connect time
    @Override
    public void connect(final String user, final String pass, final String res) throws XMPPException {
        config.setLoginInfo(user, pass, res);
    	connect();
    }

    public void connect() throws XMPPException {
        // Stablishes the connection, readers and writers
        connectUsingConfiguration(config);
        // Automatically makes the login if the user was previouslly connected successfully
        // to the server and the connection was terminated abruptly
    }
    
    public void connectionOK() {
        for (ConnectionListener listener : getConnectionListeners()) {
            try {
                listener.reconnectionSuccessful();
            }
            catch (Exception e) {
                // Catch and print any exception so we can recover
                // from a faulty listener
                e.printStackTrace();
            }
        }
    }

	@Override
	public void setRosterStorage(RosterStorage storage)
			throws IllegalStateException {
		if(roster!=null){
			throw new IllegalStateException("Roster is already initialized");
		}
		this.rosterStorage = storage;
	}

	/**
	 * Returns whether connection with server is alive.
	 * 
	 * @return <code>false</code> if timeout occur.
	 */
	@Override
	public boolean isAlive() {
		//PacketWriter packetWriter = this.packetWriter;
		//return packetWriter == null || packetWriter.isAlive();
		// FIXME
		return true;
	}

	
    /**
     * A runnable to notify all listeners of a packet.
     */
    private class ListenerNotification implements Runnable {

        private Packet packet;

        public ListenerNotification(Packet packet) {
            this.packet = packet;
        }

        public void run() {
            for (ListenerWrapper listenerWrapper : recvListeners.values()) {
                try {
                    listenerWrapper.notifyListener(packet);
                } catch (RuntimeException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}

