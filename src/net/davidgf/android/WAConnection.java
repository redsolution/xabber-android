
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

/**
 * Creates a socket connection to a WA server.
 * 
 * @see Connection
 * @author David Guillen Fandos
 */
public class WAConnection extends Connection {

    /**
     * The socket which is used for this connection.
     */
    protected Socket socket;

    String connectionID = null;
    private String user = null;
    private boolean connected = false;
    /**
     * Flag that indicates if the user is currently authenticated with the server.
     */
    private boolean authenticated = false;
    /**
     * Flag that indicates if the user was authenticated with the server when the connection
     * to the server was closed (abruptly or not).
     */
    private boolean wasAuthenticated = false;

    private OutputStream ostream;
    private InputStream istream;
    
    private Thread writerThread, readerThread;
    
    byte [] inbuffer, outbuffer;

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
    public WAConnection(String serviceName, CallbackHandler callbackHandler) {
        // Create the configuration for this new connection
        super(new ConnectionConfiguration(serviceName));
        config.setCompressionEnabled(false);
        config.setSASLAuthenticationEnabled(true);
        config.setDebuggerEnabled(DEBUG_ENABLED);
    }

    /**
     * Creates a new WA connection in the same way {@link #WAConnection(String,CallbackHandler)} does, but
     * with no callback handler for password prompting of the keystore.
     *
     * @param serviceName the name of the WA server to connect to; e.g. <tt>example.com</tt>.
     */
    public WAConnection(String serviceName) {
        // Create the configuration for this new connection
        super(new ConnectionConfiguration(serviceName));
        config.setCompressionEnabled(false);
        config.setSASLAuthenticationEnabled(true);
        config.setDebuggerEnabled(DEBUG_ENABLED);
    }

    /**
     * Creates a new WA connection in the same way {@link #WAConnection(ConnectionConfiguration,CallbackHandler)} does, but
     * with no callback handler for password prompting of the keystore.
     *
     * @param config the connection configuration.
     */
    public WAConnection(ConnectionConfiguration config) {
        super(config);
    }

    public WAConnection(ConnectionConfiguration config, CallbackHandler callbackHandler) {
        super(config);
        config.setCallbackHandler(callbackHandler);
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

        String response;
        if (config.isSASLAuthenticationEnabled() &&
                saslAuthentication.hasNonAnonymousAuthentication()) {
            // Authenticate using SASL
            if (password != null) {
                response = saslAuthentication.authenticate(username, password, resource);
            }
            else {
                response = saslAuthentication
                        .authenticate(username, resource, config.getCallbackHandler());
            }
        }
        else {
            // Authenticate using Non-SASL
            response = new NonSASLAuthentication(this).authenticate(username, password, resource);
        }

        // Set the user.
        if (response != null) {
            this.user = response;
            // Update the serviceName with the one returned by the server
            config.setServiceName(StringUtils.parseServer(response));
        }
        else {
            this.user = username + "@" + getServiceName();
            if (resource != null) {
                this.user += "/" + resource;
            }
        }

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

        // Stores the authentication for future reconnection
        config.setLoginInfo(username, password, resource);

        // If debugging is enabled, change the the debug window title to include the
        // name we are now logged-in as.
        // If DEBUG_ENABLED was set to true AFTER the connection was created the debugger
        // will be null
        if (config.isDebuggerEnabled() && debugger != null) {
            debugger.userHasLogged(user);
        }
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

	// Set status
        authenticated = false;
        connected = false;
        
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
        wasAuthenticated = false;
    }

    public void sendPacket(Packet packet) {
    	// 
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
        initConnection();
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
        
        // Spawn reader and writer threads
        writerThread = new Thread() {
            public void run() {
                writePackets(this,ostream);
            }
        };
        writerThread.setName("Socket data writer");
        writerThread.setDaemon(true);

        readerThread = new Thread() {
            public void run() {
                readPackets(this,istream);
            }
        };
        readerThread.setName("Socket data reader");
        readerThread.setDaemon(true);

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

        // If debugging is enabled, we open a window and write out all network traffic.
        initDebugger();

	reader = new AliveReader(reader);
    }

    public boolean isUsingCompression() {
        return false;
    }
    
    private void writePackets(Thread thisThread, OutputStream ostream) {
    	
    }
    
    private void readPackets(Thread thisThread, InputStream istream) {
    	try {
    		int r;
	    	do {
	    		byte [] buf = new byte[1024];
	    		r = istream.read(buf,0,buf.length);
	    		if (r > 0) {
		    		synchronized (inbuffer) {
		    			// Extend the array size and add the new bytes
		    			inbuffer = Arrays.copyOf(inbuffer, inbuffer.length + r);
		    			Arrays.copyOfRange(inbuffer,inbuffer.length-r,r);
		    			System.arraycopy(buf,0, inbuffer,inbuffer.length-r, r);
		    		}
		    	}
	    	} while (r >= 0);
	}catch (IOException e) {
		//
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
    public void connect() throws XMPPException {
        // Stablishes the connection, readers and writers
        connectUsingConfiguration(config);
        // Automatically makes the login if the user was previouslly connected successfully
        // to the server and the connection was terminated abruptly
        if (connected && wasAuthenticated) {
            // Make the login
            try {
                login(config.getUsername(), config.getPassword(), config.getResource());
            }
            catch (XMPPException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Sets whether the connection has already logged in the server.
     *
     * @param wasAuthenticated true if the connection has already been authenticated.
     */
    private void setWasAuthenticated(boolean wasAuthenticated) {
        if (!this.wasAuthenticated) {
            this.wasAuthenticated = wasAuthenticated;
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
	 * Wrapper for server keep alive.
	 * 
	 * @author alexander.ivanov
	 * 
	 */
	private class AliveReader extends Reader {
		final Reader wrappedReader;

		public AliveReader(Reader wrappedReader) {
			this.wrappedReader = wrappedReader;
		}

		private void onRead() {
			//packetWriter.responseReceived();
		}

		@Override
		public int read(char[] buf, int offset, int count) throws IOException {
			final int result = wrappedReader.read(buf, offset, count);
			onRead();
			return result;
		}

		public void close() throws IOException {
			wrappedReader.close();
		}

		public int read() throws IOException {
			final int result = wrappedReader.read();
			onRead();
			return result;
		}

		public int read(char buf[]) throws IOException {
			final int result = wrappedReader.read(buf);
			onRead();
			return result;
		}

		public long skip(long n) throws IOException {
			return wrappedReader.skip(n);
		}

		public boolean ready() throws IOException {
			return wrappedReader.ready();
		}

		public boolean markSupported() {
			return wrappedReader.markSupported();
		}

		public void mark(int readAheadLimit) throws IOException {
			wrappedReader.mark(readAheadLimit);
		}

		public void reset() throws IOException {
			wrappedReader.reset();
		}
	}
}

