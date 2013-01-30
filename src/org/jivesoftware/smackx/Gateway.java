package org.jivesoftware.smackx;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.PacketCollector;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.PacketIDFilter;
import org.jivesoftware.smack.filter.PacketTypeFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Registration;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.packet.DiscoverInfo;
import org.jivesoftware.smackx.packet.DiscoverInfo.Identity;

/**
 * This class provides an abstract view to gateways/transports. This class handles all
 * actions regarding gateways and transports.
 * @author Till Klocke
 *
 */
public class Gateway {
	
	private Connection connection;
	private ServiceDiscoveryManager sdManager;
	private Roster roster;
	private String entityJID;
	private Registration registerInfo;
	private Identity identity;
	private DiscoverInfo info;
	
	Gateway(Connection connection, String entityJID){
		this.connection = connection;
		this.roster = connection.getRoster();
		this.sdManager = ServiceDiscoveryManager.getInstanceFor(connection);
		this.entityJID = entityJID;
	}
	
	Gateway(Connection connection, String entityJID, DiscoverInfo info, Identity identity){
		this(connection, entityJID);
		this.info = info;
		this.identity = identity;
	}
	
	private void discoverInfo() throws XMPPException{
		info = sdManager.discoverInfo(entityJID);
		Iterator<Identity> iterator = info.getIdentities();
		while(iterator.hasNext()){
			Identity temp = iterator.next();
			if(temp.getCategory().equalsIgnoreCase("gateway")){
				this.identity = temp;
				break;
			}
		}
	}
	
	private Identity getIdentity() throws XMPPException{
		if(identity==null){
			discoverInfo();
		}
		return identity;
	}
	
	private Registration getRegisterInfo(){
		if(registerInfo==null){
			refreshRegisterInfo();
		}
		return registerInfo;
	}
	
	private void refreshRegisterInfo(){
		Registration packet = new Registration();
		packet.setFrom(connection.getUser());
		packet.setType(IQ.Type.GET);
		packet.setTo(entityJID);
		PacketCollector collector = 
			connection.createPacketCollector(new PacketIDFilter(packet.getPacketID()));
		connection.sendPacket(packet);
		Packet result = collector.nextResult(SmackConfiguration.getPacketReplyTimeout());
		collector.cancel();
		if(result instanceof Registration && result.getError()==null){ 
			Registration register = (Registration)result;
			this.registerInfo = register;
		}
	}
	
	/**
	 * Checks if this gateway supports In-Band registration
	 * @return true if In-Band registration is supported
	 * @throws XMPPException
	 */
	public boolean canRegister() throws XMPPException{
		if(info==null){
			discoverInfo();
		}
		return info.containsFeature("jabber:iq:register");
	}
	
	/**
	 * Returns all fields that are required to register to this gateway
	 * @return a list of required fields
	 */
	public List<String> getRequiredFields(){
		return getRegisterInfo().getRequiredFields();
	}
	
	/**
	 * Returns the name as proposed in this gateways identity discovered via service
	 * discovery
	 * @return a String of its name
	 * @throws XMPPException
	 */
	public String getName() throws XMPPException{
		if(identity==null){
			discoverInfo();
		}
		return identity.getName();
	}
	
	/**
	 * Returns the type as proposed in this gateways identity discovered via service
	 * discovery. See {@link http://xmpp.org/registrar/disco-categories.html} for 
	 * possible types
	 * @return a String describing the type
	 * @throws XMPPException
	 */
	public String getType() throws XMPPException{
		if(identity==null){
			discoverInfo();
		}
		return identity.getType();
	}
	
	/**
	 * Returns true if the registration informations indicates that you are already 
	 * registered with this gateway
	 * @return true if already registered
	 * @throws XMPPException
	 */
	public boolean isRegistered() throws XMPPException{
		return getRegisterInfo().isRegistered();
	}
	
	/**
	 * Returns the value of specific field of the registration information. Can be used
	 * to retrieve for example to retrieve username/password used on an already registered
	 * gateway.
	 * @param fieldName name of the field
	 * @return a String containing the value of the field or null
	 */
	public String getField(String fieldName){
		return getRegisterInfo().getField(fieldName);
	}
	
	/**
	 * Returns a List of Strings of all field names which contain values.
	 * @return a List of field names
	 */
	public List<String> getFieldNames(){
		return getRegisterInfo().getFieldNames();
	}
	
	/**
	 * A convenience method for retrieving the username of an existing account 
	 * @return String describing the username
	 */
	public String getUsername(){
		return getField("username");
	}
	
	/**
	 * A convenience method for retrieving the password of an existing accoung
	 * @return String describing the password
	 */
	public String getPassword(){
		return getField("password");
	}
	
	/**
	 * Returns instructions for registering with this gateway
	 * @return String containing instructions
	 */
	public String getInstructions(){
		return getRegisterInfo().getInstructions();	
	}
	
	/**
	 * With this method you can register with this gateway or modify an existing registration
	 * @param username String describing the username
	 * @param password String describing the password
	 * @param fields additional fields like email.
	 * @throws XMPPException 
	 */
	public void register(String username, String password, Map<String,String> fields)throws XMPPException{
		if(getRegisterInfo().isRegistered()) {
			throw new IllegalStateException("You are already registered with this gateway");
		}
		Registration register = new Registration();
		register.setFrom(connection.getUser());
		register.setTo(entityJID);
		register.setType(IQ.Type.SET);
		register.setUsername(username);
		register.setPassword(password);
		for(String s : fields.keySet()){
			register.addAttribute(s, fields.get(s));
		}
		PacketCollector resultCollector = 
			connection.createPacketCollector(new PacketIDFilter(register.getPacketID())); 
		connection.sendPacket(register);
		Packet result = 
			resultCollector.nextResult(SmackConfiguration.getPacketReplyTimeout());
		resultCollector.cancel();
		if(result!=null && result instanceof IQ){
			IQ resultIQ = (IQ)result;
			if(resultIQ.getError()!=null){
				throw new XMPPException(resultIQ.getError());
			}
			if(resultIQ.getType()==IQ.Type.ERROR){
				throw new XMPPException(resultIQ.getError());
			}
			connection.addPacketListener(new GatewayPresenceListener(), 
					new PacketTypeFilter(Presence.class));
			roster.createEntry(entityJID, getIdentity().getName(), new String[]{});
		}
		else{
			throw new XMPPException("Packet reply timeout");
		}
	}
	
	/**
	 * A convenience method for registering or modifying an account on this gateway without
	 * additional fields
	 * @param username String describing the username
	 * @param password String describing the password
	 * @throws XMPPException
	 */
	public void register(String username, String password) throws XMPPException{
		register(username, password,new HashMap<String,String>());
	}
	
	/**
	 * This method removes an existing registration from this gateway
	 * @throws XMPPException
	 */
	public void unregister() throws XMPPException{
		Registration register = new Registration();
		register.setFrom(connection.getUser());
		register.setTo(entityJID);
		register.setType(IQ.Type.SET);
		register.setRemove(true);
		PacketCollector resultCollector = 
			connection.createPacketCollector(new PacketIDFilter(register.getPacketID()));
		connection.sendPacket(register);
		Packet result = resultCollector.nextResult(SmackConfiguration.getPacketReplyTimeout());
		resultCollector.cancel();
		if(result!=null && result instanceof IQ){
			IQ resultIQ = (IQ)result;
			if(resultIQ.getError()!=null){
				throw new XMPPException(resultIQ.getError());
			}
			if(resultIQ.getType()==IQ.Type.ERROR){
				throw new XMPPException(resultIQ.getError());
			}
			RosterEntry gatewayEntry = roster.getEntry(entityJID);
			roster.removeEntry(gatewayEntry);
		}
		else{
			throw new XMPPException("Packet reply timeout");
		}
	}
	
	/**
	 * Lets you login manually in this gateway. Normally a gateway logins you when it
	 * receives the first presence broadcasted by your server. But it is possible to
	 * manually login and logout by sending a directed presence. This method sends an
	 * empty available presence direct to the gateway.
	 */
	public void login(){
		Presence presence = new Presence(Presence.Type.available);
		login(presence);
	}
	
	/**
	 * This method lets you send the presence direct to the gateway. Type, To and From
	 * are modified.
	 * @param presence the presence used to login to gateway
	 */
	public void login(Presence presence){
		presence.setType(Presence.Type.available);
		presence.setTo(entityJID);
		presence.setFrom(connection.getUser());
		connection.sendPacket(presence);
	}
	
	/**
	 * This method logs you out from this gateway by sending an unavailable presence
	 * to directly to this gateway.
	 */
	public void logout(){
		Presence presence = new Presence(Presence.Type.unavailable);
		presence.setTo(entityJID);
		presence.setFrom(connection.getUser());
		connection.sendPacket(presence);
	}
	
	private class GatewayPresenceListener implements PacketListener{

		public void processPacket(Packet packet) {
			if(packet instanceof Presence){
				Presence presence = (Presence)packet;
				if(entityJID.equals(presence.getFrom()) && 
						roster.contains(presence.getFrom()) &&
						presence.getType().equals(Presence.Type.subscribe)){
					Presence response = new Presence(Presence.Type.subscribed);
					response.setTo(presence.getFrom());
					response.setFrom(StringUtils.parseBareAddress(connection.getUser()));
					connection.sendPacket(response);
				}
			}
			
		}
	}

}
