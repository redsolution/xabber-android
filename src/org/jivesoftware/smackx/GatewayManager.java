package org.jivesoftware.smackx;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.packet.DiscoverInfo;
import org.jivesoftware.smackx.packet.DiscoverItems;
import org.jivesoftware.smackx.packet.DiscoverInfo.Identity;
import org.jivesoftware.smackx.packet.DiscoverItems.Item;

/**
 * This class is the general entry point to gateway interaction (XEP-0100). 
 * This class discovers available gateways on the users servers, and
 * can give you also a list of gateways the you user is registered with which
 * are not on his server. All actual interaction with a gateway is handled in the
 * class {@see Gateway}.
 * @author Till Klocke
 *
 */
public class GatewayManager {
	
	private static Map<Connection,GatewayManager> instances = 
		new HashMap<Connection,GatewayManager>();
	
	private ServiceDiscoveryManager sdManager;
	
	private Map<String,Gateway> localGateways = new HashMap<String,Gateway>();
	
	private Map<String,Gateway> nonLocalGateways = new HashMap<String,Gateway>();
	
	private Map<String,Gateway> gateways = new HashMap<String,Gateway>();
	
	private Connection connection;
	
	private Roster roster;
	
	private GatewayManager(){
		
	}
	
	/**
	 * Creates a new instance of GatewayManager
	 * @param connection
	 * @throws XMPPException
	 */
	private GatewayManager(Connection connection) throws XMPPException{
		this.connection = connection;
		this.roster = connection.getRoster();
		sdManager = ServiceDiscoveryManager.getInstanceFor(connection);
	}
	
	/**
	 * Loads all gateways the users server offers
	 * @throws XMPPException
	 */
	private void loadLocalGateways() throws XMPPException{
		DiscoverItems items = sdManager.discoverItems(connection.getHost());
		Iterator<Item> iter = items.getItems();
		while(iter.hasNext()){
			String itemJID = iter.next().getEntityID();
			discoverGateway(itemJID);
		}
	}
	
	/**
	 * Discovers {@link DiscoveryInfo} and {@link DiscoveryInfo.Identity} of a gateway
	 * and creates a {@link Gateway} object representing this gateway.
	 * @param itemJID
	 * @throws XMPPException
	 */
	private void discoverGateway(String itemJID) throws XMPPException{
		DiscoverInfo info = sdManager.discoverInfo(itemJID);
		Iterator<Identity> i = info.getIdentities();
		
		while(i.hasNext()){
			Identity identity = i.next();
			String category = identity.getCategory();
			if(category.toLowerCase().equals("gateway")){
				gateways.put(itemJID, new Gateway(connection,itemJID));
				if(itemJID.contains(connection.getHost())){
					localGateways.put(itemJID, 
							new Gateway(connection,itemJID,info,identity));
				}
				else{
					nonLocalGateways.put(itemJID, 
							new Gateway(connection,itemJID,info,identity));
				}
				break;
			}
		}
	}
	
	/**
	 * Loads all getways which are in the users roster, but are not supplied by the
	 * users server
	 * @throws XMPPException
	 */
	private void loadNonLocalGateways() throws XMPPException{
		if(roster!=null){
			for(RosterEntry entry : roster.getEntries()){
				if(entry.getUser().equalsIgnoreCase(StringUtils.parseServer(entry.getUser())) &&
						!entry.getUser().contains(connection.getHost())){
					discoverGateway(entry.getUser());
				}
			}
		}
	}
	
	/**
	 * Returns an instance of GatewayManager for the given connection. If no instance for
	 * this connection exists a new one is created and stored in a Map.
	 * @param connection
	 * @return an instance of GatewayManager
	 * @throws XMPPException
	 */
	public GatewayManager getInstanceFor(Connection connection) throws XMPPException{
		synchronized(instances){
			if(instances.containsKey(connection)){
				return instances.get(connection);
			}
			GatewayManager instance = new GatewayManager(connection);
			instances.put(connection, instance);
			return instance;
		}
	}
	
	/**
	 * Returns a list of gateways which are offered by the users server, wether the
	 * user is registered to them or not.
	 * @return a List of Gateways
	 * @throws XMPPException
	 */
	public List<Gateway> getLocalGateways() throws XMPPException{
		if(localGateways.size()==0){
			loadLocalGateways();
		}
		return new ArrayList<Gateway>(localGateways.values());
	}
	
	/**
	 * Returns a list of gateways the user has in his roster, but which are offered by
	 * remote servers. But note that this list isn't automatically refreshed. You have to
	 * refresh is manually if needed.
	 * @return a list of gateways
	 * @throws XMPPException
	 */
	public List<Gateway> getNonLocalGateways() throws XMPPException{
		if(nonLocalGateways.size()==0){
			loadNonLocalGateways();
		}
		return new ArrayList<Gateway>(nonLocalGateways.values());
	}
	
	/**
	 * Refreshes the list of gateways offered by remote servers.
	 * @throws XMPPException
	 */
	public void refreshNonLocalGateways() throws XMPPException{
		loadNonLocalGateways();
	}
	
	/**
	 * Returns a Gateway object for a given JID. Please note that it is not checked if
	 * the JID belongs to valid gateway. If this JID doesn't belong to valid gateway
	 * all operations on this Gateway object should fail with a XMPPException. But there is
	 * no guarantee for that.
	 * @param entityJID
	 * @return a Gateway object
	 */
	public Gateway getGateway(String entityJID){
		if(localGateways.containsKey(entityJID)){
			return localGateways.get(entityJID);
		}
		if(nonLocalGateways.containsKey(entityJID)){
			return nonLocalGateways.get(entityJID);
		}
		if(gateways.containsKey(entityJID)){
			return gateways.get(entityJID);
		}
		Gateway gateway = new Gateway(connection,entityJID);
		if(entityJID.contains(connection.getHost())){
			localGateways.put(entityJID, gateway);
		}
		else{
			nonLocalGateways.put(entityJID, gateway);
		}
		gateways.put(entityJID, gateway);
		return gateway;
	}

}
